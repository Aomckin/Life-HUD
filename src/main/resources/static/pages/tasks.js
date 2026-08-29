import { api } from "../api/client.js?v=0.5.3";
import { taskCopy as c } from "../content/copy.js?v=0.5.3";
import { empty, error, escapeHtml, toast } from "../components/ui.js";

let dreamsCache = [];
let tasksData = null; // {daily: [...], special: [...]}

export async function tasks(root) {
  root.innerHTML = '<section class="panel">正在读取任务…</section>';
  try {
    const [directions, dreams] = await Promise.all([api.taskDirections.all(), api.dreams.all()]);
    dreamsCache = dreams;
    tasksData = directions;
    render(root);
  } catch (reason) { root.innerHTML = error(reason.message); }
}

/* ---------- helpers ---------- */

const all = () => [...tasksData.daily, ...tasksData.special];
const done = () => all().filter(t => t.done);
const directionText = d => [d.dreamTitle, d.goalTitle, d.dreamMilestoneTitle].filter(Boolean).join(" › ");
const directionPath = d => ["梦想: " + (d.dreamTitle || "—"), "方向: " + (d.goalTitle || "—"),
  "里程碑: " + (d.dreamMilestoneTitle || "—")].join("  ·  ");

/* ---------- page ---------- */

function render(root) {
  const total = all().length, completed = done().length;
  const remaining = total - completed;
  const pct = total ? Math.round(completed / total * 100) : 0;

  root.innerHTML = `<div class="direction-page">
    <section class="panel tasks-header">
      <div class="tasks-head-text"><div class="eyebrow">v0.5 · Action Desk</div>
        <h2>${c.title}</h2>
        <p>${total ? (remaining > 0 ? `今天还有 <strong>${remaining}</strong> 件事` : c.allDoneToday) : c.emptyTasks}</p></div>
      <div class="tasks-progress">
        <small>${completed} / ${total} 已完成</small>
        <div class="progress"><span style="width:${pct}%"></span></div>
      </div>
    </section>

    <section class="panel"><div class="section-head"><h2>${c.dailyTitle}</h2></div>
      <div class="daily-grid">${cards(tasksData.daily)}</div></section>

    <section class="panel"><div class="section-head"><h2>${c.specialTitle}</h2></div>
      <div class="special-grid">${cards(tasksData.special)}</div></section>

    <form class="milestone-form" id="link-form" hidden>
      <h3>${c.linkTitle}</h3><small>${c.linkHint}</small>
      <label class="field"><span>${c.dreamLabel}</span><select id="link-dream"><option value="">—</option></select></label>
      <label class="field"><span>${c.goalLabel}</span><select id="link-goal" disabled><option value="">—</option></select></label>
      <label class="field"><span>${c.milestoneLabel}</span><select id="link-milestone" disabled><option value="">—</option></select></label>
      <div><button class="button button-primary" type="submit">${c.save}</button>
      <button class="button button-ghost" type="button" id="cancel-link">${c.cancel}</button></div>
    </form>
  </div>`;
  bind(root);
}

function cards(list) {
  const source = list[0]?.source;
  const pendingCards = list.filter(t => !t.done);
  const doneCards = list.filter(t => t.done);
  const pendingHtml = pendingCards.map(pendingCard).join("")
    || (doneCards.length ? "" : (source === "daily" ? empty(c.dailyEmpty) : empty(c.specialEmpty)));
  const fold = doneCards.length ? `
    <details class="completed-fold"><summary>${c.completedFoldLabel} · ${doneCards.length}</summary>
      <div class="completed-list">${doneCards.map(doneCard).join("")}</div></details>` : "";
  return pendingHtml + fold;
}

function pendingCard(t) {
  const mark = t.source === "daily" ? "○" : "◇";
  return `<article class="card task-card pending ${t.source === "special" ? "special" : ""}">
    <div class="task-main">
      <span class="task-mark">${mark}</span>
      <div class="task-text"><strong>${escapeHtml(t.name)}</strong>
        <small>${t.source === "daily" ? c.dailyTag : c.specialTag}</small>
        ${directionChip(t.direction)}
      </div>
    </div>
    <div class="row-actions task-actions">
      <button class="button button-secondary" data-complete="${t.source}|${t.taskId}">${c.completeLabel}</button>
      <button class="text-link" data-edit="${t.source}|${t.taskId}" title="${c.linkEdit}">⋯</button>
    </div></article>`;
}
function doneCard(t) {
  return `<article class="card task-card done">
    <div class="task-main"><span class="task-mark checked">✓</span>
      <div class="task-text"><strong>${escapeHtml(t.name)}</strong>
        <small>${c.doneBadge}</small></div></div></article>`;
}
function directionChip(d) {
  const short = d.dreamTitle || d.goalTitle || d.dreamMilestoneTitle;
  if (!short) return "";
  const full = [d.dreamTitle && "梦想 · " + d.dreamTitle, d.goalTitle && "方向 · " + d.goalTitle,
    d.dreamMilestoneTitle && "里程碑 · " + d.dreamMilestoneTitle].filter(Boolean).join("  ·  ");
  return `<span class="direction-chip" title="${escapeHtml(full)}">✦ ${escapeHtml(short)}</span>`;
}

/* ---------- bindings ---------- */

function bind(root) {
  root.querySelectorAll("[data-complete]").forEach(button => button.addEventListener("click", async () => {
    button.disabled = true;
    const [source, taskId] = button.dataset.complete.split("|");
    try {
      await api.taskDirections.complete(source, taskId);
      toast(c.completedToast);
      tasks(root);
    } catch (reason) { toast(reason.message, true); button.disabled = false; }
  }));

  root.querySelectorAll("[data-edit]").forEach(button => button.addEventListener("click", () => openLinkEditor(root, button.dataset.edit)));
  root.querySelector("#cancel-link").addEventListener("click", () => root.querySelector("#link-form").hidden = true);
  root.querySelector("#link-form").addEventListener("submit", async event => {
    event.preventDefault();
    const form = root.querySelector("#link-form");
    try {
      await api.taskDirections.link(form.dataset.source, form.dataset.task, {
        dreamId: root.querySelector("#link-dream").value,
        goalId: root.querySelector("#link-goal").value,
        dreamMilestoneId: root.querySelector("#link-milestone").value
      });
      toast("方向已关联");
      tasks(root);
    } catch (reason) { toast(reason.message, true); }
  });
}

function openLinkEditor(root, key) {
  const [source, taskId] = key.split("|");
  const form = root.querySelector("#link-form");
  form.dataset.source = source;
  form.dataset.task = taskId;
  const dreamSelect = root.querySelector("#link-dream");
  const goalSelect = root.querySelector("#link-goal");
  const milestoneSelect = root.querySelector("#link-milestone");
  const taskRow = all().find(t => t.taskId === taskId && t.source === source);
  dreamSelect.innerHTML = '<option value="">—</option>' + dreamsCache.map(d =>
    `<option value="${d.id}">${escapeHtml(d.title)}</option>`).join("");
  goalSelect.innerHTML = '<option value="">—</option>';
  milestoneSelect.innerHTML = '<option value="">—</option>';
  goalSelect.disabled = true;
  milestoneSelect.disabled = true;
  dreamSelect.value = taskRow?.direction.dreamId || "";
  const loadGoals = async () => {
    goalSelect.innerHTML = '<option value="">—</option>';
    milestoneSelect.innerHTML = '<option value="">—</option>';
    if (!dreamSelect.value) { goalSelect.disabled = true; milestoneSelect.disabled = true; return; }
    const detail = await api.dreams.detail(dreamSelect.value);
    goalSelect.disabled = false;
    detail.goals.forEach(({goal}) => goalSelect.insertAdjacentHTML("beforeend",
      `<option value="${goal.id}">${escapeHtml(goal.title)}</option>`));
    goalSelect.value = taskRow?.direction.goalId || "";
    goalSelect.onchange = () => {
      milestoneSelect.innerHTML = '<option value="">—</option>';
      const selected = detail.goals.find(g => g.goal.id === goalSelect.value);
      if (selected && selected.milestones.length) {
        milestoneSelect.disabled = false;
        selected.milestones.forEach(m => milestoneSelect.insertAdjacentHTML("beforeend",
          `<option value="${m.id}">${escapeHtml(m.title)}</option>`));
        milestoneSelect.value = taskRow?.direction.dreamMilestoneId || "";
      } else milestoneSelect.disabled = true;
    };
    goalSelect.onchange();
  };
  loadGoals();
  form.hidden = false;
  form.scrollIntoView({behavior: "smooth"});
}
