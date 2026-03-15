// buildRambam.js
// Downloads all Mishneh Torah chapters from Sefaria and saves a day-indexed schedule.
// Run: node server/scripts/buildRambam.js
// Output: server/data/rambam.json

import { writeFileSync, mkdirSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const OUT_DIR  = join(__dirname, '..', 'data');
const OUT_FILE = join(OUT_DIR, 'rambam.json');

// Chabad cycle start: 11 Nissan 5744 = April 14 1984, chapter 1 of Mishneh Torah
const CYCLE_START = new Date(Date.UTC(1984, 3, 14)); // month is 0-indexed

// Complete Mishneh Torah in canonical order – Sefaria API names + Hebrew name + chapter count
const SECTIONS = [
  // ── Book 1: Sefer HaMadda ──────────────────────────────────────────────────
  { ref: 'Mishneh Torah, Foundations of the Torah',                    he: 'הלכות יסודי התורה',               chapters: 10 },
  { ref: 'Mishneh Torah, Human Dispositions',                          he: 'הלכות דעות',                      chapters:  7 },
  { ref: 'Mishneh Torah, Torah Study',                                 he: 'הלכות תלמוד תורה',                chapters:  7 },
  { ref: 'Mishneh Torah, Foreign Worship and Customs of the Nations',  he: 'הלכות עבודה זרה',                 chapters: 12 },
  { ref: 'Mishneh Torah, Repentance',                                  he: 'הלכות תשובה',                     chapters: 10 },
  // ── Book 2: Sefer Ahavah ───────────────────────────────────────────────────
  { ref: 'Mishneh Torah, Reading of Shema',                            he: 'הלכות קריאת שמע',                 chapters:  4 },
  { ref: 'Mishneh Torah, Prayer and the Priestly Blessing',            he: 'הלכות תפילה',                     chapters: 15 },
  { ref: 'Mishneh Torah, Tefillin, Mezuzah and the Torah Scroll',      he: 'הלכות תפילין מזוזה וספר תורה',   chapters: 10 },
  { ref: 'Mishneh Torah, Fringes',                                     he: 'הלכות ציצית',                     chapters:  3 },
  { ref: 'Mishneh Torah, Mezuzah',                                     he: 'הלכות מזוזה',                     chapters:  6 },
  { ref: 'Mishneh Torah, Scroll of the Torah',                         he: 'הלכות ספר תורה',                  chapters: 10 },
  { ref: 'Mishneh Torah, Phylacteries',                                he: 'הלכות תפילין',                    chapters: 10 },
  { ref: 'Mishneh Torah, Blessings',                                   he: 'הלכות ברכות',                     chapters: 11 },
  // ── Book 3: Sefer Zmanim ───────────────────────────────────────────────────
  { ref: 'Mishneh Torah, Sabbath',                                     he: 'הלכות שבת',                       chapters: 30 },
  { ref: 'Mishneh Torah, Eruvin',                                      he: 'הלכות עירובין',                   chapters:  8 },
  { ref: 'Mishneh Torah, Rest on the Tenth of Tishrei',                he: 'הלכות שביתת עשור',                chapters:  3 },
  { ref: 'Mishneh Torah, Rest on a Holiday',                           he: 'הלכות שביתת יום טוב',             chapters:  8 },
  { ref: 'Mishneh Torah, Leavened and Unleavened Bread',               he: 'הלכות חמץ ומצה',                  chapters:  8 },
  { ref: 'Mishneh Torah, Shofar, Sukkah and Lulav',                   he: 'הלכות שופר סוכה ולולב',           chapters:  8 },
  { ref: 'Mishneh Torah, Sheqel Dues',                                 he: 'הלכות שקלים',                     chapters:  4 },
  { ref: 'Mishneh Torah, Sanctification of the New Month',             he: 'הלכות קידוש החודש',               chapters: 19 },
  { ref: 'Mishneh Torah, Fasts',                                       he: 'הלכות תעניות',                    chapters:  5 },
  { ref: 'Mishneh Torah, Scroll of Esther and Hanukkah',              he: 'הלכות מגילה וחנוכה',              chapters:  4 },
  // ── Book 4: Sefer Nashim ───────────────────────────────────────────────────
  { ref: 'Mishneh Torah, Marriage',                                    he: 'הלכות אישות',                     chapters: 25 },
  { ref: 'Mishneh Torah, Divorce',                                     he: 'הלכות גירושין',                   chapters: 13 },
  { ref: 'Mishneh Torah, Levirate Marriage and Release',               he: 'הלכות יבום וחליצה',               chapters:  8 },
  { ref: 'Mishneh Torah, Virgin Maiden',                               he: 'הלכות נערה בתולה',                chapters:  3 },
  { ref: 'Mishneh Torah, Woman Suspected of Infidelity',               he: 'הלכות סוטה',                      chapters:  4 },
  // ── Book 5: Sefer Kedushah ─────────────────────────────────────────────────
  { ref: 'Mishneh Torah, Forbidden Intercourse',                       he: 'הלכות איסורי ביאה',               chapters: 22 },
  { ref: 'Mishneh Torah, Forbidden Foods',                             he: 'הלכות מאכלות אסורות',             chapters: 17 },
  { ref: 'Mishneh Torah, Ritual Slaughter',                            he: 'הלכות שחיטה',                     chapters: 14 },
  // ── Book 6: Sefer Hafla'ah ─────────────────────────────────────────────────
  { ref: 'Mishneh Torah, Oaths',                                       he: 'הלכות שבועות',                    chapters: 12 },
  { ref: 'Mishneh Torah, Vows',                                        he: 'הלכות נדרים',                     chapters: 13 },
  { ref: 'Mishneh Torah, Naziriteship',                                he: 'הלכות נזירות',                    chapters: 10 },
  { ref: 'Mishneh Torah, Appraisals and Devoted Property',             he: 'הלכות ערכין וחרמין',              chapters:  8 },
  // ── Book 7: Sefer Zera'im ──────────────────────────────────────────────────
  { ref: 'Mishneh Torah, Diverse Species',                             he: 'הלכות כלאיים',                    chapters: 10 },
  { ref: 'Mishneh Torah, Gifts to the Poor',                           he: 'הלכות מתנות עניים',               chapters: 10 },
  { ref: 'Mishneh Torah, Heave Offerings',                             he: 'הלכות תרומות',                    chapters: 15 },
  { ref: 'Mishneh Torah, Tithes',                                      he: 'הלכות מעשרות',                    chapters: 14 },
  { ref: 'Mishneh Torah, Second Tithe and Fourth Year Produce',        he: 'הלכות מעשר שני',                  chapters: 11 },
  { ref: 'Mishneh Torah, First Fruits and other Offerings',            he: 'הלכות ביכורים',                   chapters: 12 },
  { ref: 'Mishneh Torah, Sabbatical Year and the Jubilee',             he: 'הלכות שמיטה ויובל',               chapters: 13 },
  // ── Book 8: Sefer Avodah ──────────────────────────────────────────────────
  { ref: 'Mishneh Torah, The Chosen Temple',                           he: 'הלכות בית הבחירה',                chapters:  8 },
  { ref: 'Mishneh Torah, Vessels of the Sanctuary and Those who Serve Therein', he: 'הלכות כלי המקדש',        chapters: 10 },
  { ref: 'Mishneh Torah, Admission into the Sanctuary',                he: 'הלכות ביאת המקדש',                chapters:  9 },
  { ref: 'Mishneh Torah, Things Forbidden on the Altar',               he: 'הלכות איסורי המזבח',              chapters:  7 },
  { ref: 'Mishneh Torah, Procedure of the Offerings',                  he: 'הלכות מעשה הקרבנות',              chapters: 19 },
  { ref: 'Mishneh Torah, Daily Offerings and Additional Offerings',    he: 'הלכות תמידים ומוספים',            chapters: 10 },
  { ref: 'Mishneh Torah, Sacrificial Procedure',                       he: 'הלכות פסולי המוקדשים',            chapters: 19 },
  { ref: 'Mishneh Torah, Service on Yom Kippur',                       he: 'הלכות עבודת יום הכיפורים',       chapters:  5 },
  { ref: 'Mishneh Torah, Trespass',                                    he: 'הלכות מעילה',                     chapters:  8 },
  // ── Book 9: Sefer Korbanot ─────────────────────────────────────────────────
  { ref: 'Mishneh Torah, Passover Offering',                           he: 'הלכות קרבן פסח',                  chapters: 10 },
  { ref: 'Mishneh Torah, Festival Offering',                           he: 'הלכות חגיגה',                     chapters:  3 },
  { ref: 'Mishneh Torah, Firstborn',                                   he: 'הלכות בכורות',                    chapters:  8 },
  { ref: 'Mishneh Torah, Offerings for Unintentional Transgressions',  he: 'הלכות שגגות',                     chapters: 15 },
  { ref: 'Mishneh Torah, Persons for whom Atonement Offerings are Brought', he: 'הלכות מחוסרי כפרה',          chapters:  5 },
  { ref: 'Mishneh Torah, Substitution',                                he: 'הלכות תמורה',                     chapters:  4 },
  // ── Book 10: Sefer Tahara ──────────────────────────────────────────────────
  { ref: 'Mishneh Torah, Ritual Purity of Matzah',                     he: 'הלכות טומאת מת',                  chapters: 25 },
  { ref: 'Mishneh Torah, Red Heifer',                                  he: 'הלכות פרה אדומה',                 chapters: 15 },
  { ref: 'Mishneh Torah, Ritual Impurity of Corpses',                  he: 'הלכות טומאת צרעת',                chapters: 16 },
  { ref: 'Mishneh Torah, Ritual Impurity of a Zav',                    he: 'הלכות מטמאי משכב ומושב',          chapters:  8 },
  { ref: 'Mishneh Torah, Ritual Impurity of Foodstuffs',               he: 'הלכות שאר אבות הטומאה',           chapters: 20 },
  { ref: 'Mishneh Torah, Immersion Pools',                             he: 'הלכות מקוואות',                   chapters: 11 },
  // ── Book 11: Sefer Nezikin ─────────────────────────────────────────────────
  { ref: 'Mishneh Torah, Sales',                                       he: 'הלכות מכירה',                     chapters: 30 },
  { ref: 'Mishneh Torah, Ownerless Property and Gifts',                he: 'הלכות זכייה ומתנה',               chapters: 12 },
  { ref: 'Mishneh Torah, Neighbors',                                   he: 'הלכות שכנים',                     chapters: 14 },
  { ref: 'Mishneh Torah, Agents and Partners',                         he: 'הלכות שלוחין ושותפין',            chapters: 10 },
  { ref: 'Mishneh Torah, Slaves',                                      he: 'הלכות עבדים',                     chapters:  9 },
  // ── Book 12: Sefer Kinyan ──────────────────────────────────────────────────
  { ref: 'Mishneh Torah, Theft',                                       he: 'הלכות גניבה',                     chapters:  9 },
  { ref: 'Mishneh Torah, Robbery and Lost Property',                   he: 'הלכות גזילה ואבידה',              chapters: 18 },
  { ref: 'Mishneh Torah, Wounding and Damaging',                       he: 'הלכות חובל ומזיק',                chapters:  8 },
  { ref: 'Mishneh Torah, Murderer and the Protection of Life',         he: 'הלכות רוצח ושמירת הנפש',          chapters: 13 },
  // ── Book 13: Sefer Mishpatim ───────────────────────────────────────────────
  { ref: 'Mishneh Torah, Hiring',                                      he: 'הלכות שכירות',                    chapters: 13 },
  { ref: 'Mishneh Torah, Borrowing and Deposit',                       he: 'הלכות שאלה ופיקדון',              chapters:  8 },
  { ref: 'Mishneh Torah, Creditor and Debtor',                         he: 'הלכות מלווה ולווה',               chapters: 27 },
  { ref: 'Mishneh Torah, Plaintiff and Defendant',                     he: 'הלכות טוען ונטען',                chapters: 16 },
  { ref: 'Mishneh Torah, Inheritance',                                 he: 'הלכות נחלות',                     chapters: 11 },
  // ── Book 14: Sefer Shoftim ─────────────────────────────────────────────────
  { ref: 'Mishneh Torah, The Sanhedrin and the Penalties within their Jurisdiction', he: 'הלכות סנהדרין',       chapters: 27 },
  { ref: 'Mishneh Torah, Testimony',                                   he: 'הלכות עדות',                      chapters: 22 },
  { ref: 'Mishneh Torah, Rebels',                                      he: 'הלכות ממרים',                     chapters:  7 },
  { ref: 'Mishneh Torah, Mourning',                                    he: 'הלכות אבל',                       chapters: 14 },
  { ref: 'Mishneh Torah, Kings and Wars',                              he: 'הלכות מלכים ומלחמות',             chapters: 12 },
];

// ── helpers ───────────────────────────────────────────────────────────────────

async function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

async function fetchWithRetry(url, retries = 3, delayMs = 1000) {
  for (let i = 0; i <= retries; i++) {
    try {
      const res = await fetch(url, { headers: { Accept: 'application/json' } });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      if (data?.error) throw new Error(data.error);
      return data;
    } catch (err) {
      if (i === retries) throw err;
      console.warn(`  ↩ retry ${i + 1}/${retries} for ${url}: ${err.message}`);
      await sleep(delayMs * (i + 1));
    }
  }
}

function stripHtml(html) {
  return String(html || '')
    .replace(/<br\s*\/?>/gi, ' ')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/\u05C0/g, '')
    .replace(/\u2009/g, '')
    .trim();
}

// ── main ──────────────────────────────────────────────────────────────────────

async function main() {
  console.log('🔽  Starting Mishneh Torah download from Sefaria...\n');

  const allChapters = []; // flat list: { sectionHe, sectionRef, chapterNum, halakhot: [str] }
  const failed = [];

  for (const section of SECTIONS) {
    process.stdout.write(`  ${section.he} (${section.chapters} ch)... `);
    let sectionOk = true;

    for (let ch = 1; ch <= section.chapters; ch++) {
      const ref = `${section.ref} ${ch}`;
      const url = `https://www.sefaria.org/api/texts/${encodeURI(ref.replace(/ /g, '_'))}?context=0&commentary=0&pad=0&lang=he`;

      try {
        const data = await fetchWithRetry(url);
        const raw = data?.he ?? data?.text ?? [];
        const halakhot = (Array.isArray(raw) ? raw : [raw])
          .flat()
          .map(h => stripHtml(h))
          .filter(Boolean);

        allChapters.push({
          sectionRef: section.ref,
          sectionHe:  section.he,
          chapterNum: ch,
          halakhot,
        });

        await sleep(120); // ~8 req/sec – polite to Sefaria
      } catch (err) {
        console.error(`\n  ✗ FAILED ${ref}: ${err.message}`);
        failed.push(ref);
        sectionOk = false;
        // push placeholder so indices stay aligned
        allChapters.push({ sectionRef: section.ref, sectionHe: section.he, chapterNum: ch, halakhot: [] });
      }
    }

    process.stdout.write(sectionOk ? '✓\n' : '⚠ (some chapters failed)\n');
  }

  // ── build day → chapter mapping ──────────────────────────────────────────
  const totalChapters = allChapters.length;
  console.log(`\n📦  Total chapters downloaded: ${totalChapters}`);

  // day 0 = April 14 1984 → chapters[0..2]
  // day N → chapters[ (N*3) % totalChapters .. (N*3+2) % totalChapters ]
  const schedule = {}; // dayIndex (0-based) → [chIdx0, chIdx1, chIdx2]
  for (let d = 0; d < Math.ceil(totalChapters / 3); d++) {
    schedule[d] = [
      (d * 3)     % totalChapters,
      (d * 3 + 1) % totalChapters,
      (d * 3 + 2) % totalChapters,
    ];
  }

  const output = {
    cycleStart:    '1984-04-14',
    totalChapters,
    totalDays:     Object.keys(schedule).length,
    chapters:      allChapters,
    schedule,
  };

  mkdirSync(OUT_DIR, { recursive: true });
  writeFileSync(OUT_FILE, JSON.stringify(output, null, 0), 'utf8');

  console.log(`✅  Saved → ${OUT_FILE}`);
  if (failed.length) {
    console.warn(`⚠  ${failed.length} chapters failed:`);
    failed.forEach(r => console.warn(`    • ${r}`));
  } else {
    console.log('🎉  All chapters downloaded successfully!');
  }
}

main().catch(err => { console.error('Fatal:', err); process.exit(1); });
