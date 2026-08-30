import { api } from "../api/client.js?v=0.6.1-delete1";
import { confirmDialog, empty, error, escapeHtml, toast } from "../components/ui.js?v=0.6.1";

/**
 * /journal — the life river. One backend aggregation (/api/timeline), grouped by
 * day, coarse filter tabs, light source-specific cards, and a manual journal
 * composer. Long content folds; photos open a lightbox.
 */

const GROUPS = [
  ["", "全部"], ["focus", "专注"], ["task", "任务"], ["life", "生活"],
  ["journal", "日记"], ["growth", "成长"], ["ritual", "仪式"], ["now", "「现在。」"]
];
const SOURCE_LABELS = {
  FOCUS: "专注", TASK: "任务", RITUAL: "仪式", SLEEP: "睡眠", MEAL: "饮食", EXERCISE: "运动",
  CHECK_IN: "状态", LIFE_RECORD: "记录", JOURNAL: "日记", LEVEL: "成长", ACHIEVEMENT: "成就",
  TITLE: "称号", MILESTONE: "里程碑", DREAM: "梦想", NOW: "「现在。」", SYSTEM: "生活", MANUAL: "手动", MEDIA: "媒体"
};
const SOURCE_ICONS = {
  SLEEP: "☾", MEAL: "🍚", EXERCISE: "⚡", CHECK_IN: "◐", LIFE_RECORD: "✦", JOURNAL: "✎",
  FOCUS: "◐", TASK: "✓", RITUAL: "◇", LEVEL: "↑", ACHIEVEMENT: "✿", TITLE: "❖", MILESTONE: "◆", DREAM: "✧", NOW: "✦"
};
const PAGE_SIZE = 50;

let filter = "";
let page = 1;
let items = [];

const dayKey = value => new Intl.DateTimeFormat("sv-SE", {timeZone: "Asia/Shanghai"}).format(new Date(value));
const dayLabel = key => {
  const today = new Intl.DateTimeFormat("sv-SE", {timeZone: "Asia/Shanghai"}).format(new Date());
  if (key === today) return "今天";
  if (key === new Intl.DateTimeFormat("sv-SE",
    {timeZone: "Asia/Shanghai"}).format(new Date(Date.now() - 86400000))) return "昨天";
  return new Intl.DateTimeFormat("zh-CN", {year: "numeric", month: "long", day: "numeric", weekday: "long"}).format(new Date(`${key}T12:00:00`));
};
const clock = value => new Intl.DateTimeFormat("zh-CN", {hour: "2-digit", minute: "2-digit"}).format(new Date(value));

export async function timeline(root) {
  page = 1;
  items = [];
  root.innerHTML = '<section class="panel">正在让时间线汇流…</section>';
  try {
    await loadMore();
    render(root);
  } catch (reason) { root.innerHTML = error(reason.message); }
}

async function loadMore(append = false) {
  const batch = await api.timeline({sources: filter, page, size: PAGE_SIZE});
  items = append ? [...items, ...batch] : batch;
}

function render(root) {
  const days = new Map();
  for (const item of [...items].sort((a, b) => new Date(b.occurredAt) - new Date(a.occurredAt))) {
    const key = dayKey(item.occurredAt);
    if (!days.has(key)) days.set(key, []);
    days.get(key).push(item);
  }
  const composer = `
    <section class="panel journal-composer">
      <div class="section-head"><h2>写点什么</h2><small class="muted">一句话也值得留下</small></div>
      <form id="journal-form">
        <textarea id="journal-content" rows="3" maxlength="4000" placeholder="今天…"></textarea>
        <div class="milestone-form-grid">
          <label class="field"><span>发生时间(可选)</span><input type="datetime-local" id="journal-time"></label>
          <label class="field"><span>标签(逗号分隔,可选)</span><input id="journal-tags" maxlength="120" placeholder="生活, 傍晚"></label>
        </div>
        <div class="row-actions">
          <label class="text-link">附上照片<input type="file" id="journal-images" accept="image/*" multiple hidden></label>
          <small class="muted" id="journal-image-count"></small>
          <button class="button button-primary" type="submit">写下</button>
        </div>
      </form>
    </section>`;
  root.innerHTML = `<div class="timeline-page">
    <section class="panel timeline-intro"><div class="eyebrow">v0.6.1 · Journal</div>
      <h1>发生过的事,按时间留在这里。</h1>
      <p>生活记录、专注、任务、日记——都汇进同一条河。</p></section>
    ${composer}
    <nav class="growth-tabs status-tabs timeline-tabs">${GROUPS.map(([value, label]) =>
      `<button class="${filter === value ? "active" : ""}" data-filter="${value}">${label}</button>`).join("")}</nav>
    <div id="timeline-days">${days.size ? [...days.entries()].map(([key, list]) => `
      <section class="timeline-day"><h2 class="timeline-day-title">${dayLabel(key)}</h2>
      ${[...list].sort((a, b) => new Date(b.occurredAt) - new Date(a.occurredAt)).map(item => itemCard(item)).join("")}
      </section>`).join("")
      : empty(filter === "now"
        ? "「现在。」还没有留下足迹。去 /now 放进一点此刻正在发生的事。"
        : "这条河暂时还是一张白纸。去 /life 记一笔,或写几句日记。")}
    </div>
    <div class="row-actions timeline-more">${items.length >= PAGE_SIZE
      ? `<button class="button button-ghost" id="timeline-load-more">往更早翻</button>` : ""}</div>
  </div>`;
  bind(root);
}

function itemCard(item) {
  const summary = item.summary ? `<p class="timeline-summary">${escapeHtml(item.summary)}</p>` : "";
  const thumbs = item.media.length ? `<div class="life-thumbs">${item.media.map(src =>
    `<img src="${escapeHtml(src)}" alt="" data-lightbox="${escapeHtml(src)}">`).join("")}</div>` : "";
  const meta = item.metadata || {};
  const scales = item.source === "CHECK_IN"
    ? `<small class="timeline-meta">能量 ${meta.energy} · 心情 ${meta.mood} · 专注 ${meta.focusDesire} · 疲劳 ${meta.fatigue}</small>` : "";
  return `<article class="timeline-event ${item.source === "JOURNAL" ? "journal" : ""}" data-item="${item.eventId}">
    <time>${clock(item.occurredAt)}</time>
    <span class="timeline-dot"></span>
    <div class="timeline-body">
      <small><span class="life-icon">${SOURCE_ICONS[item.source.toUpperCase()] || "✦"}</span> ${SOURCE_LABELS[item.source.toUpperCase()] || item.source}</small>
      <h3>${escapeHtml(item.title)}</h3>
      ${summary}${thumbs}${scales}
    </div>
    ${item.source === "JOURNAL"
      ? `<button class="text-link danger-link" data-remove-journal="${item.sourceId}">删除</button>`
      : `<button class="text-link danger-link" data-remove-event="${item.eventId}">删除</button>`}
  </article>`;
}

function bind(root) {
  root.querySelectorAll("[data-filter]").forEach(button => button.addEventListener("click", async () => {
    filter = button.dataset.filter;
    page = 1;
    items = [];
    try { await loadMore(); render(root); }
    catch (reason) { toast(reason.message, true); }
  }));
  root.querySelectorAll("[data-lightbox]").forEach(img => img.addEventListener("click", () => lightbox(img.dataset.lightbox)));
  root.querySelectorAll(".timeline-summary").forEach(summary => summary.addEventListener("click", () => summary.classList.toggle("expanded")));
  root.querySelectorAll("[data-remove-journal]").forEach(button => button.addEventListener("click", async event => {
    event.stopPropagation();
    if (!(await confirmDialog("删除这篇日记?时间线里的对应足迹也会一并消失。"))) return;
    try { await api.journal.remove(button.dataset.removeJournal); toast("已删除"); page = 1; items = []; await loadMore(); render(root); }
    catch (reason) { toast(reason.message, true); }
  }));
  root.querySelectorAll("[data-remove-event]").forEach(button => button.addEventListener("click", async event => {
    event.stopPropagation();
    if (!(await confirmDialog("删除这条 LifeEvent？此操作只会移除时间线事件，不会撤销原业务记录。"))) return;
    try { await api.removeEvent(button.dataset.removeEvent); toast("LifeEvent 已删除"); page = 1; items = []; await loadMore(); render(root); }
    catch (reason) { toast(reason.message, true); }
  }));
  root.querySelector("#timeline-load-more")?.addEventListener("click", async () => {
    page += 1;
    try { await loadMore(true); render(root); }
    catch (reason) { toast(reason.message, true); }
  });

  const images = root.querySelector("#journal-images");
  images?.addEventListener("change", () => {
    root.querySelector("#journal-image-count").textContent = images.files.length ? `已选 ${images.files.length} 张` : "";
  });
  root.querySelector("#journal-form")?.addEventListener("submit", async event => {
    event.preventDefault();
    const content = root.querySelector("#journal-content").value.trim();
    if (!content) { toast("写点什么再留下吧", true); return; }
    const time = root.querySelector("#journal-time").value;
    const tags = root.querySelector("#journal-tags").value.split(/[,，]/).map(t => t.trim()).filter(Boolean);
    try {
      const paths = [];
      for (const file of images.files) {
        const data = new FormData();
        data.append("file", file);
        paths.push((await api.uploadImage(data)).path);
      }
      await api.journal.create({content, occurredAt: time ? new Date(time).toISOString() : null,
        images: paths, tags});
      toast("已写下");
      page = 1; items = [];
      await loadMore();
      render(root);
    } catch (reason) { toast(reason.message, true); }
  });
}

function lightbox(src) {
  const overlay = document.createElement("div");
  overlay.className = "board-modal-overlay life-lightbox";
  overlay.innerHTML = `<img src="${escapeHtml(src)}" alt="">`;
  overlay.addEventListener("click", () => overlay.remove());
  document.body.append(overlay);
}
