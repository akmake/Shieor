import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowRight, BookOpen, Edit, Save, X } from 'lucide-react';

export default function ArticlePage() {
  const { id } = useParams();
  const [article, setArticle] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [isEditing, setIsEditing] = useState(false);
  const [editTitle, setEditTitle] = useState('');
  const [editRawText, setEditRawText] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    async function fetchArticle() {
      try {
        const res = await fetch(`/api/articles/${id}`);
        if (!res.ok) throw new Error('המאמר לא נמצא');
        const data = await res.json();
        setArticle(data);
        setEditTitle(data.title || '');
        setEditRawText(data.rawText || '');
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    }
    fetchArticle();
  }, [id]);

  const handleSave = async () => {
    setSaving(true);
    try {
      const res = await fetch(`/api/articles/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title: editTitle, rawText: editRawText })
      });
      if (!res.ok) throw new Error('שגיאה בשמירת המאמר');
      const data = await res.json();
      setArticle(data);
      setIsEditing(false);
    } catch (err) {
      alert(err.message);
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="p-8 text-center text-xl text-[var(--muted)]">טוען מאמר...</div>;
  if (error) return <div className="p-8 text-center text-xl text-red-500">{error}</div>;
  if (!article) return null;

  // פונקציה שמייצרת את הטקסט עם המילה הראשונה מודגשת בגדול לאורך המאמר
  const renderText = (text) => {
    if (!text) return null;
    const paragraphs = text.split('\n').filter(p => p.trim());
    return paragraphs.map((p, pIdx) => {
      const words = p.trim().split(' ');
      if (words.length === 0) return null;
      
      const firstWord = words[0];
      const restOfText = words.slice(1).join(' ');

      return (
        <p key={pIdx} className="mb-6 font-['SBL_Hebrew'] text-xl leading-loose text-[var(--ink)]">
          <span className="text-3xl font-bold text-[var(--brand)] ml-2 leading-none inline-block">
            {firstWord}
          </span>
          {restOfText}
        </p>
      );
    });
  };

  return (
    <div className="mx-auto w-full max-w-4xl px-4 pb-16 pt-6 sm:px-6 lg:px-8">
      <div className="mb-6 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-2 text-slate-500 hover:text-slate-800 transition-colors">
          <ArrowRight className="h-5 w-5" />
          חזרה לראשי
        </Link>
        {!isEditing && (
          <button onClick={() => setIsEditing(true)} className="flex items-center gap-2 text-blue-600 font-medium hover:bg-blue-50 px-3 py-1.5 rounded-lg transition-colors">
            <Edit className="h-4 w-4" />
            עריכה
          </button>
        )}
      </div>
      
      {isEditing ? (
        <div className="glass-panel p-6 sm:p-8 animate-in fade-in slide-in-from-bottom-2">
          <label className="block mb-2 font-bold text-lg text-[var(--brand)]">כותרת המאמר:</label>
          <input 
            type="text" 
            className="w-full mb-6 p-3 border rounded text-2xl font-bold font-['BA_HaYetzira'] focus:outline-none focus:ring-2 focus:ring-[var(--brand)]"
            value={editTitle}
            onChange={(e) => setEditTitle(e.target.value)}
          />
          
          <label className="block mb-2 font-bold text-lg text-[var(--brand)]">תוכן המאמר:</label>
          <textarea 
            className="w-full p-4 border rounded font-[SBL_Hebrew] text-xl leading-loose focus:outline-none focus:ring-2 focus:ring-[var(--brand)]" 
            style={{ minHeight: '600px' }}
            value={editRawText}
            onChange={(e) => setEditRawText(e.target.value)}
          />
          
          <div className="mt-6 flex justify-end gap-3">
            <button 
              onClick={() => {
                setIsEditing(false);
                setEditTitle(article.title || '');
                setEditRawText(article.rawText || '');
              }} 
              className="flex items-center gap-2 px-6 py-2 rounded-full border border-slate-300 text-slate-600 hover:bg-slate-50 transition-colors"
            >
              <X className="h-4 w-4" />
              ביטול
            </button>
            <button 
              onClick={handleSave} 
              disabled={saving}
              className="flex items-center gap-2 px-6 py-2 rounded-full bg-[var(--brand)] text-white hover:bg-teal-700 transition-colors disabled:opacity-50"
            >
              <Save className="h-4 w-4" />
              {saving ? 'שומר...' : 'שמירה'}
            </button>
          </div>
        </div>
      ) : (
        <div className="glass-panel p-6 sm:p-10 animate-in fade-in slide-in-from-bottom-2">
          <h1 className="text-4xl sm:text-5xl font-bold mb-10 text-center text-[var(--brand)] font-['BA_HaYetzira']">
            {article.title || 'מאמר שיעור יומי'}
          </h1>
          <div className="study-content">
            {renderText(article.rawText)}
          </div>
        </div>
      )}
    </div>
  );
}
