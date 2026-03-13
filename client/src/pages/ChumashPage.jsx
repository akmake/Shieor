import React, { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';

const ChumashPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const dateParam = searchParams.get('date');
  const today = new Date();
  const [date, setDate] = useState(dateParam ? new Date(dateParam) : today);
  const [studyData, setStudyData] = useState({
    item: null,
    loading: true,
  });

  useEffect(() => {
    const fetchContent = async () => {
      setStudyData((prev) => ({ ...prev, loading: true }));
      try {
        const url = date
          ? `https://www.sefaria.org/api/calendars?date=${date.toISOString().slice(0, 10)}&timezone=Asia/Jerusalem`
          : 'https://www.sefaria.org/api/calendars?timezone=Asia/Jerusalem';

        const response = await fetch(url);
        const data = await response.json();

        const item = data.calendar_items.find((item) => item.title.en === 'Parashat Hashavua');
        setStudyData({ item, loading: false });
      } catch (error) {
        console.error('Error fetching Chumash content:', error);
        setStudyData({ item: null, loading: false });
      }
    };

    fetchContent();
  }, [date]);

  function goPrevDay() {
    const newDate = new Date(date);
    newDate.setDate(newDate.getDate() - 1);
    setDate(newDate);
    setSearchParams({ date: newDate.toISOString().slice(0, 10) });
  }

  function goNextDay() {
    const newDate = new Date(date);
    newDate.setDate(newDate.getDate() + 1);
    setDate(newDate);
    setSearchParams({ date: newDate.toISOString().slice(0, 10) });
  }

  if (studyData.loading) {
    return (
      <div className="p-8 flex justify-center text-gray-500">
        טוען חומש ליום {date.toLocaleDateString('he-IL')}...
      </div>
    );
  }

  return (
    <div className="p-6 max-w-4xl mx-auto" dir="rtl">
      <div className="flex justify-between mb-4">
        <button onClick={goPrevDay} className="px-4 py-2 bg-slate-200 rounded-lg hover:bg-slate-300">
          יום קודם
        </button>
        <div className="text-lg font-bold text-blue-900">
          {date.toLocaleDateString('he-IL')}
        </div>
        <button onClick={goNextDay} className="px-4 py-2 bg-slate-200 rounded-lg hover:bg-slate-300">
          יום הבא
        </button>
      </div>

      <h1 className="text-3xl font-black mb-6 text-gray-900">חומש יומי</h1>
      {studyData.item ? (
        <div className="bg-white p-6 rounded-2xl border border-blue-100 shadow-sm">
          <h2 className="text-2xl font-bold text-blue-700 mb-2">
            {studyData.item.displayValue.he || studyData.item.displayValue}
          </h2>
          <p className="text-gray-500 text-lg mb-6 dir-ltr text-right">
            {studyData.item.ref}
          </p>

          <div className="flex gap-3">
            <a
              href={`https://www.sefaria.org.il/${studyData.item.url}&with=Rashi`}
              target="_blank"
              rel="noopener noreferrer"
              className="flex-1 bg-blue-600 text-white text-center py-3 rounded-xl font-bold hover:bg-blue-700 transition"
            >
              פתח בפנים עם רש"י
            </a>
            <a
              href={`https://www.sefaria.org.il/${studyData.item.url}`}
              target="_blank"
              rel="noopener noreferrer"
              className="px-6 py-3 rounded-xl font-bold text-blue-700 bg-blue-50 hover:bg-blue-100 transition"
            >
              פסוקים בלבד
            </a>
          </div>
        </div>
      ) : (
        <div className="p-4 bg-red-50 text-red-600 rounded-xl">
          לא נמצא שיעור חומש לתאריך זה.
        </div>
      )}
    </div>
  );
};

export default ChumashPage;