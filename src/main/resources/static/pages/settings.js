import { appearance, DEFAULT_APPEARANCE } from '../modules/appearance.js';
import { toast } from '../components/ui.js';

const CONTROLS = [
  ['wallpaperPositionX', '壁纸位置 X', 0, 100, '%'],
  ['wallpaperPositionY', '壁纸位置 Y', 0, 100, '%'],
  ['wallpaperOverlay', '背景遮罩', 0, 40, '%'],
  ['wallpaperBlur', '背景模糊', 0, 8, 'px'],
  ['wallpaperSaturation', '背景饱和度', 70, 120, '%'],
  ['glassOpacity', '面板不透明度', 60, 95, '%'],
  ['glassBlur', '面板模糊', 8, 30, 'px']
];

function slider([key, label, min, max, unit]) {
  return `<label class='appearance-control' for='${key}'><span><strong>${label}</strong><output id='${key}-value'>${appearance.settings[key]}${unit}</output></span><input id='${key}' data-appearance-key='${key}' data-unit='${unit}' type='range' min='${min}' max='${max}' step='1' value='${appearance.settings[key]}'></label>`;
}

function previewUrl() {
  return appearance.wallpaperUrl || '/static/assets/wallpapers/summer-sky-default.jpg';
}

async function verifyImage(file) {
  if ('createImageBitmap' in window) {
    const bitmap = await createImageBitmap(file);
    bitmap.close();
    return;
  }
  await new Promise((resolve, reject) => {
    const image = new Image();
    const url = URL.createObjectURL(file);
    image.onload = () => { URL.revokeObjectURL(url); resolve(); };
    image.onerror = () => { URL.revokeObjectURL(url); reject(new Error('图片无法读取或已损坏。')); };
    image.src = url;
  });
}

export async function settings(root) {
  root.innerHTML = `<div class='appearance-page'>
    <section class='panel appearance-intro'>
      <div><div class='eyebrow'>Appearance · Local</div><h1>让这片天空更像你的生活空间</h1><p>自定义壁纸和外观设置仅保存在当前浏览器，不会上传，也不会跨设备同步。</p></div>
      <span class='badge'>Browser Local</span>
    </section>
    <div class='appearance-layout'>
      <section class='panel wallpaper-panel'>
        <div class='section-head'><div><div class='eyebrow'>Wallpaper</div><h2>当前壁纸</h2></div><span id='wallpaper-kind' class='badge'>${appearance.hasCustomWallpaper ? '自定义' : 'Summer Sky'}</span></div>
        <div class='wallpaper-preview' id='wallpaper-preview'></div>
        <input id='wallpaper-file' type='file' accept='image/jpeg,image/png,image/webp' hidden>
        <div class='wallpaper-actions'><button class='button button-primary' id='choose-wallpaper'>选择图片</button><button class='button button-secondary' id='reset-default'>恢复默认壁纸</button></div>
        <p class='appearance-hint'>支持 JPG、PNG、WebP，最大 25 MiB。</p>
      </section>
      <div class='appearance-settings'>
        <section class='panel'><div class='eyebrow'>Composition</div><h2>壁纸构图</h2><div class='control-list'>${CONTROLS.slice(0, 2).map(slider).join('')}</div></section>
        <section class='panel'><div class='eyebrow'>Background</div><h2>背景适配</h2><div class='control-list'>${CONTROLS.slice(2, 5).map(slider).join('')}</div></section>
        <section class='panel'><div class='eyebrow'>Glass</div><h2>玻璃面板</h2><div class='control-list'>${CONTROLS.slice(5).map(slider).join('')}</div></section>
        <section class='panel reset-panel'><div><div class='eyebrow'>Reset</div><h2>恢复推荐值</h2><p>保留当前自定义壁纸，只恢复构图、背景和玻璃参数。</p></div><button class='button button-secondary' id='reset-recommended'>恢复推荐值</button></section>
      </div>
    </div>
  </div>`;

  const preview = root.querySelector('#wallpaper-preview');
  preview.style.backgroundImage = `url('${previewUrl()}')`;

  root.querySelectorAll('[data-appearance-key]').forEach(input => input.addEventListener('input', () => {
    appearance.update(input.dataset.appearanceKey, input.value);
    root.querySelector(`#${input.id}-value`).value = `${input.value}${input.dataset.unit}`;
  }));

  root.querySelector('#choose-wallpaper').addEventListener('click', () => root.querySelector('#wallpaper-file').click());
  root.querySelector('#wallpaper-file').addEventListener('change', async event => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) return toast('请选择 JPG、PNG 或 WebP 图片。', true);
    if (file.size > 25 * 1024 * 1024) return toast('图片超过 25 MiB，未保存。', true);
    try {
      await verifyImage(file);
      await appearance.useCustomWallpaper(file);
      preview.style.backgroundImage = `url('${appearance.wallpaperUrl}')`;
      root.querySelector('#wallpaper-kind').textContent = '自定义';
      document.querySelector('#wallpaper-label').textContent = 'Custom Sky';
      toast('壁纸已保存在当前浏览器。');
    } catch (reason) {
      toast(reason.message || '图片无法读取，未保存。', true);
    }
  });

  root.querySelector('#reset-recommended').addEventListener('click', () => {
    appearance.resetRecommended();
    CONTROLS.forEach(([key, , , , unit]) => {
      root.querySelector(`#${key}`).value = DEFAULT_APPEARANCE[key];
      root.querySelector(`#${key}-value`).value = `${DEFAULT_APPEARANCE[key]}${unit}`;
    });
    toast('已恢复 Summer Sky 推荐参数。');
  });

  root.querySelector('#reset-default').addEventListener('click', async () => {
    try {
      await appearance.resetDefault();
      await settings(root);
      document.querySelector('#wallpaper-label').textContent = 'Summer Sky';
      toast('已恢复默认壁纸和推荐参数。');
    } catch (reason) {
      toast(reason.message, true);
    }
  });
}
