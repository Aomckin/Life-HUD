import { api } from '../api/client.js';
import { empty, error, escapeHtml, toast } from '../components/ui.js';

const labels = {
  '/dreams': '梦想 · Dreams',
  '/rituals': '仪式 · Ritual',
  '/life': '生活 · Life',
  '/media': '媒体 · Media',
  '/journal': '日记 · Journal',
  '/now': '此刻 · Now',
  '/growth': '成长 · Growth'
};

export async function placeholder(root, route) {
  if (route === '/tasks') return tasks(root);
  if (route === '/focus') return focus(root);
  root.innerHTML = `<section class='panel placeholder'><div class='eyebrow'>Life HUD v0.2.3</div><h1>${labels[route]}</h1><p>这个入口已经接入 Summer Sky Shell。完整体验会在后续版本写进这片天空。</p>${empty('现在先把注意力留给今天。')}</section>`;
}

async function tasks(root) {
  root.innerHTML = `<section class='panel tasks-page'><div class='section-head tasks-page-head'><div><div class='eyebrow'>今日 · Today</div><h1>任务 · Tasks</h1></div><button class='button button-secondary' id='refresh-tasks'>刷新任务</button></div><div id='task-list'>加载中…</div></section>`;
  try {
    const state = await api.state();
    const tasks = state.active_task_views || [];
    root.querySelector('#task-list').innerHTML = tasks.length
      ? `<div class='task-list'>${tasks.map(task => `<div class='task-item'><div><strong>${escapeHtml(task.name || task.id)}</strong><div class='event-meta'>${escapeHtml(task.detail_text || task.reward_text || '')}</div></div><button class='button button-primary' data-index='${task.command_payload?.index}' ${task.button_state === 'normal' ? '' : 'disabled'}>${escapeHtml(task.button_text || '完成')}</button></div>`).join('')}</div>`
      : empty('今天还没有任务。');
    root.querySelectorAll('[data-index]').forEach(button => button.addEventListener('click', async () => {
      button.disabled = true;
      try {
        const result = await api.command('COMPLETE_DAILY_TASK', {index: Number(button.dataset.index)});
        toast(result.message || '已完成');
        tasks(root);
      } catch (reason) {
        toast(reason.message, true);
        button.disabled = false;
      }
    }));
    root.querySelector('#refresh-tasks').addEventListener('click', async () => {
      try {
        const result = await api.command('REFRESH_DAILY_TASKS');
        toast(result.message);
        tasks(root);
      } catch (reason) {
        toast(reason.message, true);
      }
    });
  } catch (reason) {
    root.querySelector('#task-list').innerHTML = error(reason.message);
  }
}

async function focus(root) {
  root.innerHTML = [
    `<div class='focus-page'><section class='panel hero focus-hero'><div class='hero-copy'><div class='eyebrow'>专注 · Focus</div><h1>从一件小事开始</h1><p>把注意力留给当下。选择一个方向，然后只处理眼前这一件事。</p><div class='focus-primary-row'><button class='button button-primary focus-primary' id='start-focus'>开始专注</button><span class='focus-primary-note'>进入专注准备；完整计时流程将在 v0.3 接入。</span></div><div class='focus-quick-group'><div class='focus-quick-label'>Quick actions</div><div class='focus-actions' id='focus-actions'>正在读取可用行动…</div></div></div><div class='hero-actions'><div class='hero-status'><span>Focus Mode</span><strong>准备开始</strong><span>完整专注流程将在 v0.3 到来</span></div></div></section>`,
    `<section class='focus-grid'><section class='panel content-card focus-card'><div class='eyebrow'>今日 Focus</div><h2>留一点安静给自己</h2><p>从一个现在就能做的小动作开始，剩下的交给时间。</p>${empty('今天还没有开始专注。')}</section></section></div>`
  ].join('');
  root.querySelector('#start-focus').addEventListener('click', () => toast('专注入口已准备好；完整 Focus Session 将在 v0.3 接入。'));
  try {
    const state = await api.state();
    const actions = Array.isArray(state.action_views) ? state.action_views : [];
    root.querySelector('#focus-actions').innerHTML = actions.length
      ? actions.map(action => `<button class='button quick-action' data-action='${escapeHtml(action.name)}'>${escapeHtml(action.button_text || action.name)}</button>`).join('')
      : empty('暂无可用行动。');
    root.querySelectorAll('[data-action]').forEach(button => button.addEventListener('click', () => toast('时长选择仍沿用现有流程；Focus 专属流程将在 v0.3 提供。')));
  } catch (reason) {
    root.querySelector('#focus-actions').innerHTML = error(reason.message);
  }
}
