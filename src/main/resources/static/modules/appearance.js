import { wallpaperStore } from './wallpaper-store.js';

const STORAGE_KEY = 'life-hud.appearance.v1';
export const DEFAULT_APPEARANCE = Object.freeze({
  appearanceSettingsVersion: 1,
  wallpaperPositionX: 66,
  wallpaperPositionY: 42,
  wallpaperOverlay: 16,
  wallpaperBlur: 0,
  wallpaperSaturation: 80,
  glassOpacity: 82,
  glassBlur: 18
});

const LIMITS = {
  wallpaperPositionX: [0, 100],
  wallpaperPositionY: [0, 100],
  wallpaperOverlay: [0, 40],
  wallpaperBlur: [0, 8],
  wallpaperSaturation: [70, 120],
  glassOpacity: [60, 95],
  glassBlur: [8, 30]
};

function normalize(candidate = {}) {
  const result = {...DEFAULT_APPEARANCE};
  for (const [key, [min, max]] of Object.entries(LIMITS)) {
    const value = Number(candidate[key]);
    if (Number.isFinite(value)) result[key] = Math.min(max, Math.max(min, value));
  }
  return result;
}

class Appearance {
  settings = {...DEFAULT_APPEARANCE};
  wallpaperUrl = null;
  hasCustomWallpaper = false;

  async initialize() {
    try {
      this.settings = normalize(JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}'));
    } catch {
      this.settings = {...DEFAULT_APPEARANCE};
      localStorage.removeItem(STORAGE_KEY);
    }
    this.apply();
    try {
      const blob = await wallpaperStore.get();
      if (blob instanceof Blob) this.setWallpaperBlob(blob);
    } catch (reason) {
      window.dispatchEvent(new CustomEvent('appearance-error', {detail: reason.message}));
    }
  }

  apply() {
    const root = document.documentElement.style;
    root.setProperty('--wallpaper-position-x', `${this.settings.wallpaperPositionX}%`);
    root.setProperty('--wallpaper-position-y', `${this.settings.wallpaperPositionY}%`);
    root.setProperty('--wallpaper-overlay-opacity', String(this.settings.wallpaperOverlay / 100));
    root.setProperty('--wallpaper-blur', `${this.settings.wallpaperBlur}px`);
    root.setProperty('--wallpaper-saturation', String(this.settings.wallpaperSaturation / 100));
    root.setProperty('--glass-surface-opacity', String(this.settings.glassOpacity / 100));
    root.setProperty('--glass-blur', `${this.settings.glassBlur}px`);
  }

  update(key, value) {
    this.settings = normalize({...this.settings, [key]: value});
    localStorage.setItem(STORAGE_KEY, JSON.stringify(this.settings));
    this.apply();
  }

  resetRecommended() {
    this.settings = {...DEFAULT_APPEARANCE};
    localStorage.setItem(STORAGE_KEY, JSON.stringify(this.settings));
    this.apply();
  }

  setWallpaperBlob(blob) {
    if (this.wallpaperUrl) URL.revokeObjectURL(this.wallpaperUrl);
    this.wallpaperUrl = URL.createObjectURL(blob);
    this.hasCustomWallpaper = true;
    document.documentElement.style.setProperty('--app-wallpaper', `url('${this.wallpaperUrl}')`);
  }

  async useCustomWallpaper(blob) {
    await wallpaperStore.put(blob);
    this.setWallpaperBlob(blob);
  }

  async resetDefault() {
    await wallpaperStore.remove();
    if (this.wallpaperUrl) URL.revokeObjectURL(this.wallpaperUrl);
    this.wallpaperUrl = null;
    this.hasCustomWallpaper = false;
    document.documentElement.style.removeProperty('--app-wallpaper');
    this.resetRecommended();
  }
}

export const appearance = new Appearance();
window.addEventListener('beforeunload', () => {
  if (appearance.wallpaperUrl) URL.revokeObjectURL(appearance.wallpaperUrl);
});
