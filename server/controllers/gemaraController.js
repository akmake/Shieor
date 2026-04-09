import * as mupdf from 'mupdf';
import { createCanvas, Image } from 'canvas';
import Anthropic from '@anthropic-ai/sdk';
import mongoose from 'mongoose';
import GemaraPage from '../models/GemaraPage.js';

const SCALE = 1.5;

const PROMPT = `זהו דף מגמרא שוטנשטיין. חלץ את התוכן לפי הכללים הבאים.

━━━ סוגי תוכן שיכולים להופיע ━━━

הדף עשוי להכיל שילוב כלשהו של:
א. פתיחת פרק — כותרת גדולה ומרכזית כגון "פרק ראשון" / "פרק שני"
ב. משנה — טקסט מודגש המסומן בסמל כו"ש (מ"שנה") בתחילתו
ג. גמרא — גוף הביאור בשתי עמודות (ימנית ואחריה שמאלית)
ד. הדרן עלך — כותרת מרכזית גדולה בסוף פרק: "הדרן עלך [שם]"

━━━ כיצד לסמן כל סוג ━━━

- פתיחת פרק:  [פרק: שם הפרק]
- משנה:        [משנה]
                טקסט המשנה
- גמרא:        [גמרא]
                טקסט הגמרא
- הדרן עלך:    [הדרן עלך: שם הפרק]
- מילות גמרא מודגשות (bold) בתוך הטקסט: **כך**

━━━ כותרת הדף ━━━

בפינה העליונה של הדף מופיע מספר הדף:
- "ג."  = דף ג עמוד א  →  כתוב בשורה 1: סוטה ג:א
- "ג:"  = דף ג עמוד ב  →  כתוב בשורה 1: סוטה ג:ב

שורה ראשונה תמיד: [מסכת] [דף]:[עמוד]  (למשל: סוטה ג:א)

━━━ הערות שוליים — הכלל הקריטי ביותר ━━━

בתחתית כל דף יש קו אופקי, ומתחתיו אזור "הערות" בגופן קטן.
אסור בהחלט לכלול שום טקסט מאזור זה — גם אם נראה כגוף הביאור.

סימני זיהוי שאתה נמצא בתוך הערות שוליים (עצור מיד):
• השורה מתחילה במספר ואחריו נקודה: "1." / "2." / "32."
• הטקסט מכיל "ראה עמוד" / "ראה הערה" / "רש"י;" / "תוספות;"
• הגופן קטן משמעותית מגוף הביאור

━━━ מה עוד אסור לכלול ━━━

• הפניות מקור בסוגריים בתוך הטקסט — מחק:
  (ראה עמוד א הערה 32) / (ראה הערה 26) / (ראה לעיל ב, א)
• מספרים בסופרסקריפט [כגון: [32

━━━ פורמט התשובה ━━━

שורה 1: כותרת (סוטה ג:א)
שורות הבאות: תוכן עם סימונים לפי הסוג

החזר רק את הטקסט. ללא הסברים.`;

// ── PDF → PNG buffers ──────────────────────────────────────────
function pdfToBuffers(pdfBuffer) {
  const doc   = mupdf.Document.openDocument(pdfBuffer, 'application/pdf');
  const count = doc.countPages();
  const pages = [];
  for (let i = 0; i < count; i++) {
    const page   = doc.loadPage(i);
    const pixmap = page.toPixmap(mupdf.Matrix.scale(SCALE, SCALE), mupdf.ColorSpace.DeviceRGB, false, true);
    const pngBuf = Buffer.from(pixmap.asPNG());
    const bounds = page.getBounds();
    pages.push({
      pageNum: i + 1,
      pngBuf,
      width:  Math.round((bounds[2] - bounds[0]) * SCALE),
      height: Math.round((bounds[3] - bounds[1]) * SCALE),
      total: count,
    });
  }
  return pages;
}

// ── פילטר מקומי: האם זה דף ביאור? (חינמי, לפני Claude) ────────
function isBiurPage(pngBuf, width, height) {
  const canvas = createCanvas(width, height);
  const ctx    = canvas.getContext('2d');
  const img    = new Image();
  img.src = pngBuf;
  ctx.drawImage(img, 0, 0);

  // בדיקת צפיפות פיקסלים כהים בכותרת (8% עליון)
  const data = ctx.getImageData(0, 0, width, Math.floor(height * 0.08)).data;
  let dark = 0;
  for (let i = 0; i < data.length; i += 4) {
    if ((data[i] + data[i + 1] + data[i + 2]) / 3 < 128) dark++;
  }
  const density = dark / (data.length / 4);
  // דף ביאור: כותרת דלילה (<0.04) | דף וילנא: צפוף (>0.04)
  return density < 0.04;
}

// ── שליחה ל-Claude עם retry ────────────────────────────────────
async function extractWithClaude(client, b64, attempt = 1) {
  try {
    const msg = await client.messages.create({
      model: 'claude-sonnet-4-6',
      max_tokens: 4096,
      messages: [{
        role: 'user',
        content: [
          { type: 'image', source: { type: 'base64', media_type: 'image/png', data: b64 } },
          { type: 'text', text: PROMPT },
        ],
      }],
    });
    return msg.content[0].text.trim();
  } catch (err) {
    const isOverloaded = err?.error?.type === 'overloaded_error' || err?.status === 529;
    if (isOverloaded && attempt <= 4) {
      const wait = attempt * 15000; // 15s, 30s, 45s, 60s
      console.log(`Claude עמוס — ניסיון ${attempt}/4, ממתין ${wait/1000}s...`);
      await new Promise(r => setTimeout(r, wait));
      return extractWithClaude(client, b64, attempt + 1);
    }
    // דף שנחסם ע"י content filtering — דף וילנא שעבר את הפילטר, מדלגים
    const isFiltered = err?.status === 400 &&
      err?.error?.message?.includes('content filtering');
    if (isFiltered) return null;
    throw err;
  }
}

// ── פירוש תוצאת Claude לחלקים מובנים ─────────────────────────
function parseClaudeOutput(result) {
  const lines  = result.split('\n');
  const dafRef = lines[0].trim();
  const body   = lines.slice(1).join('\n').trim();

  // חילוץ עמוד (א/ב) מ-dafRef: "סוטה ג:א" → "א"
  const amudMatch = dafRef.match(/:([אב])$/);
  const amud = amudMatch ? amudMatch[1] : null;

  // פיצול לחלקים לפי סימוני [פרק / משנה / גמרא / הדרן עלך]
  const sections = [];
  const parts = body.split(/(?=\[(?:פרק|משנה|גמרא|הדרן עלך))/);

  for (const part of parts) {
    const trimmed = part.trim();
    if (!trimmed) continue;

    const m = trimmed.match(/^\[(פרק|משנה|גמרא|הדרן עלך)[:\s]*([^\]]*)\]/);
    if (m) {
      const rawType = m[1];
      const label   = m[2].trim();
      const text    = trimmed.slice(m[0].length).trim();
      const type    = rawType === 'הדרן עלך' ? 'הדרן' : rawType;
      // עבור פרק/הדרן — הטקסט הוא השם עצמו (מה שבסוגריים)
      sections.push({ type, text: text || label });
    } else {
      // תוכן ללא סימון מפורש — נחשב גמרא
      sections.push({ type: 'גמרא', text: trimmed });
    }
  }

  // זיהוי שם הפרק בדף זה (אם יש)
  const perekSection = sections.find(s => s.type === 'פרק');
  const perekName = perekSection?.text || null;

  return { dafRef, amud, sections, perekName, text: body };
}

// ── Controller ─────────────────────────────────────────────────
export async function extractGemara(req, res) {
  if (!req.file) return res.status(400).json({ error: 'לא הועלה קובץ PDF' });

  const tractate = (req.body.tractate || 'לא ידוע').trim();
  const bookFile = req.file.originalname;
  const dbReady  = mongoose.connection.readyState === 1;

  res.setHeader('Content-Type', 'text/event-stream');
  res.setHeader('Cache-Control', 'no-cache');
  res.setHeader('Connection', 'keep-alive');

  const send = (data) => res.write(`data: ${JSON.stringify(data)}\n\n`);

  try {
    const client = new Anthropic({ apiKey: process.env.ANTHROPIC_API_KEY });

    send({ type: 'status', message: 'ממיר דפים...' });
    const pages = pdfToBuffers(req.file.buffer);
    send({ type: 'status', message: `${pages.length} דפים — מסנן...` });

    // שלב 1: פילטר מקומי (חינמי)
    const biurCandidates = pages.filter(p => isBiurPage(p.pngBuf, p.width, p.height));
    const skipped = pages.length - biurCandidates.length;

    send({ type: 'status', message: `סוננו ${skipped} דפי מקור — שולח ${biurCandidates.length} דפי ביאור ל-Claude...` });

    let biurCount = 0;
    let currentPerek = null; // עוקב אחרי הפרק הנוכחי לאורך כל הדפים

    for (const { pageNum, pngBuf, total } of biurCandidates) {
      send({ type: 'progress', page: pageNum, total });
      send({ type: 'status', message: `מעבד דף ${pageNum}...` });

      const result = await extractWithClaude(client, pngBuf.toString('base64'));
      if (!result) {
        send({ type: 'status', message: `דף ${pageNum} — נחסם ע"י פילטר, מדלג...` });
        continue;
      }
      const { dafRef, amud, sections, perekName, text } = parseClaudeOutput(result);

      // עדכון הפרק הנוכחי אם הדף פותח פרק חדש
      if (perekName) currentPerek = perekName;

      // שמירה ב-DB אם מחובר
      if (dbReady) {
        await GemaraPage.findOneAndUpdate(
          { tractate, bookFile, pageNum },
          { tractate, bookFile, pageNum, dafRef, amud, perekName: currentPerek, sections, text },
          { upsert: true, new: true }
        );
      }

      biurCount++;
      send({ type: 'chunk', pageNum, dafRef, amud, perekName: currentPerek, sections, text, saved: dbReady });
    }

    send({ type: 'done', biurCount, skipped, total: pages.length, saved: dbReady });
    res.end();
  } catch (err) {
    send({ type: 'error', message: err.message });
    res.end();
  }
}

// ── שליפת דפים שמורים ─────────────────────────────────────────
export async function getPages(req, res) {
  try {
    const pages = await GemaraPage.find({ tractate: req.params.tractate }).sort({ pageNum: 1 }).select('-__v');
    res.json(pages);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
}

export async function getTractates(req, res) {
  try {
    res.json(await GemaraPage.distinct('tractate'));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
}

// ── עריכת דף שמור ─────────────────────────────────────────────
export async function updatePage(req, res) {
  try {
    const { sections, dafRef, amud, perekName } = req.body;

    // בנה מחדש text מלא מהחלקים
    const text = sections.map(s => s.text).join('\n\n');

    const page = await GemaraPage.findByIdAndUpdate(
      req.params.id,
      { sections, dafRef, amud, perekName, text },
      { new: true, runValidators: true }
    ).select('-__v');

    if (!page) return res.status(404).json({ error: 'דף לא נמצא' });
    res.json(page);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
}
