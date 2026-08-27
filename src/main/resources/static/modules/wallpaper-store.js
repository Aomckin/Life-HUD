const DB_NAME = 'life-hud-appearance';
const STORE_NAME = 'wallpapers';
const DB_VERSION = 1;
const WALLPAPER_KEY = 'custom-wallpaper';

function openDatabase() {
  return new Promise((resolve, reject) => {
    if (!('indexedDB' in window)) return reject(new Error('当前浏览器不支持本地壁纸存储。'));
    const request = indexedDB.open(DB_NAME, DB_VERSION);
    request.onupgradeneeded = () => {
      if (!request.result.objectStoreNames.contains(STORE_NAME)) request.result.createObjectStore(STORE_NAME);
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(new Error('无法打开本地壁纸存储。'));
  });
}

async function transaction(mode, operation) {
  const database = await openDatabase();
  try {
    return await new Promise((resolve, reject) => {
      const tx = database.transaction(STORE_NAME, mode);
      const request = operation(tx.objectStore(STORE_NAME));
      let result;
      request.onsuccess = () => { result = request.result; };
      request.onerror = () => reject(new Error('本地壁纸存储操作失败。'));
      tx.oncomplete = () => resolve(result);
      tx.onabort = () => reject(new Error('本地壁纸存储空间不足或不可用。'));
    });
  } finally {
    database.close();
  }
}

export const wallpaperStore = {
  get: () => transaction('readonly', store => store.get(WALLPAPER_KEY)),
  put: blob => transaction('readwrite', store => store.put(blob, WALLPAPER_KEY)),
  remove: () => transaction('readwrite', store => store.delete(WALLPAPER_KEY))
};
