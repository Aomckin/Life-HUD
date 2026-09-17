import { sidebar, routes } from './modules/navigation.js';
import { dashboard } from './pages/dashboard.js?v=1.0.0';
import { placeholder } from './pages/placeholder.js';
import { settings } from './pages/settings.js';
import { focus } from './pages/focus.js?v=1.0.0';
import { growth } from './pages/growth.js?v=1.0.0';
import { timeline } from './pages/timeline.js?v=1.0.0';
import { dreams } from './pages/dreams.js?v=1.0.0';
import { rituals } from './pages/rituals.js?v=1.0.0';
import { now } from './pages/now.js?v=1.0.0';
import { life } from './pages/life.js?v=1.0.0';
import { tasks } from './pages/tasks.js?v=1.0.2';
import { media } from './pages/media.js?v=1.0.0';
import { appearance } from './modules/appearance.js';
import { enableImagePaste } from './modules/image-paste.js?v=1.0.0';
import { toast } from './components/ui.js';

const allRoutes = ['/dashboard', ...routes.map(route => route.path).filter(path => path !== '/dashboard'), '/settings'];
const app = document.querySelector('#app');

async function render(replace = false) {
  let path = location.pathname;
  if (!allRoutes.includes(path)) {
    path = '/dashboard';
    if (!replace) history.replaceState({}, '', path);
  }
  document.body.dataset.route = path.slice(1);
  const name = routes.find(route => route.path === path)?.name || (path === '/settings' ? '外观 · Appearance' : 'Today');
  const wallpaperName = appearance.hasCustomWallpaper ? 'Custom Sky' : 'Summer Sky';
  app.innerHTML = `${sidebar(path)}<main class='main'><header class='topbar'><div class='page-title'>${name}</div><div class='top-context'><span id='clock'></span><span class='hud-pill' id='wallpaper-label'>${wallpaperName}</span></div></header><div id='page'></div></main>`;
  app.querySelector('#clock').textContent = new Intl.DateTimeFormat('zh-CN', {month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'}).format(new Date());
  app.querySelectorAll('[data-link]').forEach(link => link.addEventListener('click', event => {
    event.preventDefault();
    navigate(link.getAttribute('href'));
  }));
  const page = app.querySelector('#page');
  if (path !== '/focus') document.body.classList.remove('focus-active', 'iron-curtain-active');
  if (path === '/dashboard') await dashboard(page);
  else if (path === '/focus') await focus(page);
  else if (path === '/growth') await growth(page);
  else if (path === '/journal') await timeline(page);
  else if (path === '/dreams') await dreams(page);
  else if (path === '/life') await life(page);
  else if (path === '/rituals') await rituals(page);
  else if (path === '/now') await now(page);
  else if (path === '/tasks') await tasks(page);
  else if (path === '/media') await media(page);
  else if (path === '/settings') await settings(page);
  else await placeholder(page, path);
}

window.navigate = (path, replace = false) => {
  if (!replace) history.pushState({}, '', path);
  render(true);
};
window.addEventListener('popstate', () => render(true));
window.addEventListener('appearance-error', event => toast(event.detail, true));
document.querySelector('.modal-close')?.addEventListener('click', () => document.querySelector('#app-modal').close());

await appearance.initialize();
enableImagePaste();
render();
