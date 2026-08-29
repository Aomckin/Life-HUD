import { api } from '../api/client.js?v=0.6.0';
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
  root.innerHTML = `<section class='panel placeholder'><div class='eyebrow'>Life HUD v0.4.0</div><h1>${labels[route]}</h1><p>这个入口已经接入 Summer Sky Shell。完整体验会在后续版本写进这片天空。</p>${empty('现在先把注意力留给今天。')}</section>`;
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
