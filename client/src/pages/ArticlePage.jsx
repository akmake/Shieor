import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowRight, BookOpen } from 'lucide-react';

export default function ArticlePage() {
  const { id } = useParams();
  const [article, setArticle] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    async function fetchArticle() {
      try {
        const res = await fetch(`/api/articles/${id}`);
        if (!res.ok) throw new Error('המאמר לא נמצא');
        const data = await res.json();
        setArticle(data);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    }
    fetchArticle();
  }, [id]);

  if (loading) return <div className="p-8 text-center text-xl text-[var(--muted)]">טוען מאמר...</div>;
  if (error) return <div className="p-8 text-center text-xl text-red-500">{error}</div>;
  if (!article) return null;

  return (
    <div className="mx-auto w-full max-w-4xl px-4 pb-16 pt-6 sm:px-6 lg:px-8">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-3xl font-bold text-[var(--ink)] flex items-center gap-3">
          <BookOpen className="text-blue-600" />
          {article.title || 'מאמר'}
        </h1>
        <Link to="/" className="flex items-center gap-2 text-blue-600 hover:text-blue-800 transition-colors">
          <ArrowRight className="h-4 w-4" />
          חזרה לראשי
        </Link>
      </div>
      
      <div className="glass-panel p-6 sm:p-8">
        <div className="study-text" style={{ whiteSpace: 'pre-wrap', lineHeight: '2' }}>
          {article.rawText}
        </div>
      </div>
    </div>
  );
}
