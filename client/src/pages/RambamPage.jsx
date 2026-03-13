import React, { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';

const RambamPage = () => {
  const [searchParams] = useSearchParams();
  const dateParam = searchParams.get('date');
  const [data, setData] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      const url = dateParam 
        ? `https://www.sefaria.org/api/calendars?date=${dateParam}&timezone=Asia/Jerusalem`
        : 'https://www.sefaria.org/api/calendars?timezone=Asia/Jerusalem';
      
      const res = await fetch(url);
      const json = await res.json();
      // שליפת רמב"ם 3 פרקים
      const item = json.calendar_items.find(i => i.title.en === 'Mishneh Torah');
      setData(item);
    };
    fetchData();
  }, [dateParam]);

  if (!data) return <div className="p-8 text-center text-gray-500">טוען רמב"ם...</div>;

  return (
    <div className="p-6 max-w-4xl mx-auto" dir="rtl">
      <h1 className="text-3xl font-black mb-6 text-gray-900">רמב"ם יומי (3 פרקים)</h1>
      <div className="bg-white p-6 rounded-2xl border border-emerald-100 shadow-sm">
        <h2 className="text-2xl font-bold text-emerald-700 mb-2">
          {data.displayValue.he || data.displayValue}
        </h2>
        <p className="text-gray-500 text-lg mb-6 dir-ltr text-right">
          {data.ref}
        </p>
        <a 
          href={`https://www.sefaria.org.il/${data.url}`} 
          target="_blank" 
          rel="noopener noreferrer" 
          className="block w-full bg-emerald-600 text-white text-center py-3 rounded-xl font-bold hover:bg-emerald-700 transition"
        >
          התחל ללמוד
        </a>
      </div>
    </div>
  );
};

export default RambamPage;