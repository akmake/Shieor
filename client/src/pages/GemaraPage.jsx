import { useEffect, useRef, useState } from 'react';
import { BookOpen, Upload, Loader2, FileText, CheckCircle2, AlertCircle, ChevronDown } from 'lucide-react';

// ── תצוגת טקסט עם bold ────────────────────────────────────────
function RichText({ text }) {
  const parts = text.split(/(\*\*[^*]+\*\*)/g);
  return (
    <p className="mb-2 leading-loose">
      {parts.map((p, i) =>
        p.startsWith('**') && p.endsWith('**')
          ? <strong key={i} className="font-bold text-[var(--ink)]">{p.slice(2, -2)}</strong>
          : <span key={i}>{p}</span>
      )}
    </p>
  );
}

// ── עמוד ראשי ─────────────────────────────────────────────────
export default function GemaraPage() {
  const [tab, setTab]         = useState('upload'); // 'upload' | 'browse'
  const [file, setFile]       = useState(null);
  const [tractate, setTractate] = useState('');
  const [status, setStatus]   = useState('');
  const [progress, setProgress] = useState({ page: 0, total: 0 });
  const [chunks, setChunks]   = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState('');
  const [done, setDone]       = useState(null);

  const [tractates, setTractates]   = useState([]);
  const [selected, setSelected]     = useState('');
  const [savedPages, setSavedPages] = useState([]);
  const [loadingPages, setLoadingPages] = useState(false);

  const inputRef  = useRef();
  const bottomRef = useRef();

  // טעינת מסכתות שמורות
  useEffect(() => {
    if (tab === 'browse') {
      fetch('/api/gemara/tractates')
        .then(r => r.json())
        .then(setTractates)
        .catch(() => {});
    }
  }, [tab]);

  const loadPages = async (tr) => {
    setSelected(tr);
    setLoadingPages(true);
    const res = await fetch(`/api/gemara/pages/${encodeURIComponent(tr)}`);
    const data = await res.json();
    setSavedPages(data);
    setLoadingPages(false);
  };

  const handleFile = (f) => {
    if (!f || f.type !== 'application/pdf') return;
    setFile(f);
    setChunks([]);
    setError('');
    setDone(null);
  };

  const handleExtract = async () => {
    if (!file || !tractate.trim()) return;
    setLoading(true);
    setError('');
    setChunks([]);
    setDone(null);

    const form = new FormData();
    form.append('pdf', file);
    form.append('tractate', tractate.trim());

    try {
      const res = await fetch('/api/gemara/extract', { method: 'POST', body: form });
      const reader  = res.body.getReader();
      const decoder = new TextDecoder();
      let buf = '';

      while (true) {
        const { done: streamDone, value } = await reader.read();
        if (streamDone) break;
        buf += decoder.decode(value, { stream: true });
        const lines = buf.split('\n');
        buf = lines.pop();
        for (const line of lines) {
          if (!line.startsWith('data: ')) continue;
          try {
            const msg = JSON.parse(line.slice(6));
            if (msg.type === 'status')   setStatus(msg.message);
            if (msg.type === 'progress') setProgress({ page: msg.page, total: msg.total });
            if (msg.type === 'chunk')    { setChunks(p => [...p, msg]); setTimeout(() => bottomRef.current?.scrollIntoView({ behavior: 'smooth' }), 50); }
            if (msg.type === 'done')     setDone(msg);
            if (msg.type === 'error')    setError(msg.message);
          } catch {}
        }
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="mx-auto max-w-4xl px-4 pb-16 pt-6" dir="rtl">
      {/* כותרת */}
      <div className="mb-6 flex items-center gap-3">
        <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[var(--brand)] text-white">
          <BookOpen size={22} />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-[var(--ink)]">גמרא שוטנשטיין</h1>
          <p className="text-sm text-[var(--muted)]">העלאה, חילוץ ביאור, שמירה</p>
        </div>
      </div>

      {/* טאבים */}
      <div className="mb-6 flex gap-2">
        {[['upload','העלאה'],['browse','צפייה']].map(([id, label]) => (
          <button key={id} onClick={() => setTab(id)}
            className={`rounded-full px-5 py-2 text-sm font-medium transition ${tab === id ? 'bg-[var(--brand)] text-white' : 'bg-white text-[var(--muted)] border border-[var(--line)] hover:border-[var(--brand)]'}`}>
            {label}
          </button>
        ))}
      </div>

      {/* ── טאב העלאה ── */}
      {tab === 'upload' && (
        <>
          {/* שדה מסכת */}
          <input
            value={tractate}
            onChange={e => setTractate(e.target.value)}
            placeholder="שם המסכת (למשל: סוטה)"
            className="mb-3 w-full rounded-2xl border border-[var(--line)] bg-white px-4 py-3 text-sm outline-none focus:border-[var(--brand)]"
          />

          {/* העלאת קובץ */}
          <div
            onDrop={e => { e.preventDefault(); handleFile(e.dataTransfer.files[0]); }}
            onDragOver={e => e.preventDefault()}
            onClick={() => !file && inputRef.current.click()}
            className={`mb-4 flex cursor-pointer flex-col items-center justify-center gap-3 rounded-3xl border-2 border-dashed p-10 transition-all
              ${file ? 'border-[var(--brand)] bg-teal-50/40' : 'border-[var(--line)] bg-white hover:border-[var(--brand)]'}`}
          >
            <input ref={inputRef} type="file" accept="application/pdf" className="hidden" onChange={e => handleFile(e.target.files[0])} />
            {file ? (
              <>
                <FileText size={36} className="text-[var(--brand)]" />
                <p className="font-semibold text-[var(--ink)]">{file.name}</p>
                <p className="text-sm text-[var(--muted)]">{(file.size/1024/1024).toFixed(1)} MB</p>
                <button onClick={e => { e.stopPropagation(); setFile(null); }} className="text-xs text-red-400 underline">הסר</button>
              </>
            ) : (
              <>
                <Upload size={36} className="text-[var(--muted)]" />
                <p className="font-medium text-[var(--ink)]">גרור PDF לכאן או לחץ לבחירה</p>
              </>
            )}
          </div>

          <button onClick={handleExtract} disabled={!file || !tractate.trim() || loading}
            className="mb-6 flex w-full items-center justify-center gap-2 rounded-2xl bg-[var(--brand)] py-3 text-base font-semibold text-white transition hover:opacity-90 disabled:opacity-40">
            {loading ? <><Loader2 size={18} className="animate-spin" />מעבד...</> : <><BookOpen size={18} />חלץ ושמור</>}
          </button>

          {/* סטטוס */}
          {(loading || status) && !done && (
            <div className="mb-4 flex items-center gap-3 rounded-2xl bg-teal-50 px-4 py-3 text-sm text-teal-800">
              {loading && <Loader2 size={16} className="shrink-0 animate-spin" />}
              <span>{status}</span>
              {progress.total > 0 && <span className="mr-auto text-xs">{progress.page}/{progress.total}</span>}
            </div>
          )}

          {done && (
            <div className="mb-4 flex items-center gap-2 rounded-2xl bg-green-50 px-4 py-3 text-sm text-green-800">
              <CheckCircle2 size={16} />
              נשמרו {done.biurCount} דפי ביאור מתוך {done.total} דפים
            </div>
          )}

          {error && (
            <div className="mb-4 flex items-center gap-2 rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-700">
              <AlertCircle size={16} />
              {error}
            </div>
          )}

          {/* תוצאות בזמן אמת */}
          {chunks.length > 0 && (
            <div className="rounded-3xl border border-[var(--line)] bg-white">
              <div className="border-b border-[var(--line)] px-5 py-3 font-semibold text-[var(--ink)]">
                תוצאות בזמן אמת
              </div>
              <div className="max-h-[60vh] overflow-y-auto p-5 text-sm text-[var(--ink)]">
                {chunks.map((c, i) => (
                  <div key={i} className="mb-6">
                    <p className="mb-2 text-xs font-bold text-[var(--brand)]">{c.dafRef || `דף ${c.pageNum}`}</p>
                    {c.text.split('\n').filter(Boolean).map((line, j) => <RichText key={j} text={line} />)}
                  </div>
                ))}
                <div ref={bottomRef} />
              </div>
            </div>
          )}
        </>
      )}

      {/* ── טאב צפייה ── */}
      {tab === 'browse' && (
        <>
          {tractates.length === 0 ? (
            <p className="text-center text-sm text-[var(--muted)]">אין מסכתות שמורות עדיין</p>
          ) : (
            <div className="mb-4 flex flex-wrap gap-2">
              {tractates.map(tr => (
                <button key={tr} onClick={() => loadPages(tr)}
                  className={`rounded-full px-4 py-2 text-sm font-medium transition border ${selected === tr ? 'bg-[var(--brand)] text-white border-[var(--brand)]' : 'bg-white text-[var(--ink)] border-[var(--line)] hover:border-[var(--brand)]'}`}>
                  {tr}
                </button>
              ))}
            </div>
          )}

          {loadingPages && <div className="flex justify-center py-10"><Loader2 size={24} className="animate-spin text-[var(--brand)]" /></div>}

          {savedPages.length > 0 && (
            <div className="space-y-6">
              {savedPages.map(p => (
                <div key={p._id} className="rounded-3xl border border-[var(--line)] bg-white p-5">
                  <p className="mb-3 text-xs font-bold text-[var(--brand)]">{p.dafRef || `דף ${p.pageNum}`}</p>
                  <div className="text-sm leading-loose text-[var(--ink)]">
                    {p.text.split('\n').filter(Boolean).map((line, i) => <RichText key={i} text={line} />)}
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}
