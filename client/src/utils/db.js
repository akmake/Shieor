const DB_NAME = 'shieor-db';
const DB_VERSION = 1;
const STORE_NAME = 'daily-study';

function openDB() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);
    request.onupgradeneeded = (event) => {
      const db = event.target.result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME, { keyPath: 'date' });
      }
    };
    request.onsuccess = (event) => resolve(event.target.result);
    request.onerror = () => reject(request.error);
  });
}

export async function saveStudyData(date, data) {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite');
    tx.objectStore(STORE_NAME).put({ date, data });
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function getStudyData(date) {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readonly');
    const request = tx.objectStore(STORE_NAME).get(date);
    request.onsuccess = () => resolve(request.result ? request.result.data : null);
    request.onerror = () => reject(request.error);
  });
}
