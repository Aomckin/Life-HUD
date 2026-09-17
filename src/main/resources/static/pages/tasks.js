import { api } from "../api/client.js?v=1.0.0";
import { taskCopy as c } from "../content/copy.js?v=1.0.1";
import { empty, error, escapeHtml, toast } from "../components/ui.js";

let dreamsCache = [];
let tasksData = null; // {daily: [...], special: [...]} — today's active instances only
let poolData = null;  // {daily: [...], special: [...]} — full definition pool
let view = "today";   // "today" | "pool"

export async function tasks(root) {
  root.innerHTML = '<section class="panel">正在读取任务…</section>';
  try {
    if (view === "pool") {
      const [pool, dreams] = await Promise.all([api.taskPool.all(), api.dreams.all()]);
      poolData = pool;
      dreamsCache = dreams;
      renderPool(root);
    } else {
      const [directions, dreams] = await Promise.all([api.taskDirections.all(), api.dreams.all()]);
      dreamsCache = dreams;
      tasksData = directions;
      render(root);
    }
  } catch (reason) { root.innerHTML = error(reason.message); }
}

/* ---------- helpers ---------- */

const all = () => [...tasksData.daily, ...tasksData.special];
const done = () => all().filter(t => t.done);
const directionText = d => [d.dreamTitle, d.goalTitle, d.dreamMilestoneTitle].filter(Boolean).join(" › ");
const directionPath = d => ["梦想: " + (d.dreamTitle || "—"), "方向: " + (d.goalTitle || "—"),
  "里程碑: " + (d.dreamMilestoneTitle || "—")].join("  ·  ");
const escapeAttribute = value => escapeHtml(value).replaceAll('"', "&quot;").replaceAll("'", "&#39;");

/* ---------- today view ---------- */

function render(root) {
  const total = all().length, completed = done().length;
  const remaining = total - completed;
  const pct = total ? Math.round(completed / total * 100) : 0;

  root.innerHTML = `<div class="direction-page">
    <section class="panel tasks-header">
      <div class="tasks-head-text"><div class="eyebrow">Action Desk</div>
        <h2>${c.title}</h2>
        <p>${total ? (remaining > 0 ? `今天还有 <strong>${remaining}</strong> 件事` : c.allDoneToday) : c.emptyTasks}</p></div>
      <div class="tasks-progress">
        <small>${completed} / ${total} 已完成</small>
        <div class="progress"><span style="width:${pct}%"></span></div>
        <button class="text-link" id="open-pool">${c.poolEntry}</button>
      </div>
    </section>

    <section class="panel"><div class="section-head"><h2>${c.dailyTitle}</h2></div>
      <div class="daily-grid">${cards(tasksData.daily, "grid")}</div>
      <div class="completed-wrap">${fold(tasksData.daily)}</div></section>

    <section class="panel"><div class="section-head"><h2>${c.specialTitle}</h2></div>
      <div class="special-grid">${cards(tasksData.special, "grid")}</div>
      <div class="completed-wrap">${fold(tasksData.special)}</div></section>

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
    || (doneCards.length ? `<div class="all-done-hint">${c.allDoneInPool}</div>` : emptyFor(source));
  return pendingHtml;
}
function fold(list) {
  const doneCards = list.filter(t => t.done);
  if (!doneCards.length) return "";
  return `<details class="completed-fold"><summary>${c.completedFoldLabel} · ${doneCards.length}</summary>
    <div class="completed-list">${doneCards.map(doneCard).join("")}</div></details>`;
}
function emptyFor(source) {
  return source === "daily" ? empty(c.dailyEmpty) : empty(c.specialEmpty);
}

function pendingCard(t) {
  const mark = t.source === "daily" ? "○" : "◇";
  const reward = t.source === "daily" ? `Energy +${t.energy}` : `EXP +${t.exp}`;
  return `<article class="card task-card pending ${t.source === "special" ? "special" : ""}">
    <div class="task-main">
      <span class="task-mark">${mark}</span>
      <div class="task-text"><strong>${escapeHtml(t.name)}</strong>
        <small>${t.source === "daily" ? c.dailyTag : c.specialTag} · ${reward}</small>
        ${t.note ? `<p class="task-note">${escapeHtml(t.note)}</p>` : ""}
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
        <small>${c.doneBadge}</small>${t.note ? `<p class="task-note">${escapeHtml(t.note)}</p>` : ""}</div></div></article>`;
}
function directionChip(d) {
  const short = d.dreamTitle || d.goalTitle || d.dreamMilestoneTitle;
  if (!short) return "";
  const full = [d.dreamTitle && "梦想 · " + d.dreamTitle, d.goalTitle && "方向 · " + d.goalTitle,
    d.dreamMilestoneTitle && "里程碑 · " + d.dreamMilestoneTitle].filter(Boolean).join("  ·  ");
  return `<span class="direction-chip" title="${escapeHtml(full)}">✦ ${escapeHtml(short)}</span>`;
}

/* ---------- pool view ---------- */

function renderPool(root) {
  const defRow = t => {
    const dir = t.direction;
    const chip = (dir.dreamTitle || dir.goalTitle || dir.dreamMilestoneTitle)
      ? `<span class="direction-chip" title="${escapeHtml(directionPath(dir))}">✦ ${escapeHtml(directionText(dir))}</span>` : "";
    const rewardText = t.source === "daily" ? `Energy +${t.energy}` : `EXP +${t.exp}`;
    const rewardValue = t.source === "daily" ? t.energy : t.exp;
    const rewardLabel = t.source === "daily" ? c.energyLabel : c.specialExpLabel;
    return `<div class="pool-row" data-source="${t.source}" data-task="${t.taskId}">
      <div class="pool-main"><strong>${escapeHtml(t.name)}</strong>
        <small>${rewardText}${chip ? " · " + chip : ""}</small>
        ${t.note ? `<p class="task-note">${escapeHtml(t.note)}</p>` : ""}</div>
      <div class="row-actions pool-actions">
        <span class="badge">${t.enabled ? c.poolEnabled : c.poolDisabled}</span>
        <button class="text-link" data-pool-edit>${c.poolEdit}</button>
        <button class="text-link" data-pool-toggle="${t.source}|${t.taskId}|${t.enabled ? 0 : 1}">${t.enabled ? c.poolDisable || "停用" : c.poolEnabled}</button>
        <button class="text-link danger-link" data-pool-delete="${t.source}|${t.taskId}">${"删除"}</button>
      </div>
      <form class="pool-edit-form" hidden>
        <label class="field"><span>${c.nameLabel}</span><input name="name" value="${escapeAttribute(t.name)}" maxlength="60" required></label>
        <label class="field pool-note-field"><span>${c.noteLabel}</span><input name="note" value="${escapeAttribute(t.note || "")}" maxlength="160" placeholder="${c.notePlaceholder}"></label>
        <label class="field pool-reward-field"><span>${rewardLabel}</span><input name="reward" type="number" min="0" value="${rewardValue}"></label>
        <div class="row-actions"><button class="button button-primary" type="submit">${c.save}</button><button class="button button-ghost" type="button" data-pool-cancel>${c.cancel}</button></div>
      </form></div>`;
  };
  root.innerHTML = `<div class="direction-page">
    <section class="panel"><div class="section-head"><div><div class="eyebrow">${c.poolTitle}</div>
      <h2>${c.poolTitle}</h2><p class="muted">${c.poolHint}</p></div>
      <button class="text-link" id="back-tasks">${c.backToTasks}</button></div>
      <section class="pool-group"><h3>${c.poolDailyTitle}</h3>
        <form class="inline-form pool-add-form" data-pool-add="daily"><input name="name" placeholder="${c.nameLabel}" maxlength="60" required>
          <input name="note" placeholder="${c.noteLabel}" maxlength="160">
          <input name="energy" type="number" min="0" placeholder="${c.energyLabel}" style="max-width:140px">
          <button class="button button-secondary" type="submit">${c.poolAdd}</button></form>
        ${poolData.daily.map(defRow).join("") || empty("任务池是空的。")}
      </section>
      <section class="pool-group"><h3>${c.poolSpecialTitle}</h3>
        <form class="inline-form pool-add-form" data-pool-add="special"><input name="name" placeholder="${c.nameLabel}" maxlength="60" required>
          <input name="note" placeholder="${c.noteLabel}" maxlength="160">
          <input name="exp" type="number" min="0" placeholder="${c.specialExpLabel}" style="max-width:140px">
          <button class="button button-secondary" type="submit">${c.poolAdd}</button></form>
        ${poolData.special.map(defRow).join("") || empty("还没有特殊行动模板。")}
      </section>
    </section></div>`;
  bindPool(root);
}

function bindPool(root) {
  root.querySelector("#back-tasks").addEventListener("click", () => { view = "today"; tasks(root); });
  root.querySelectorAll(".pool-add-form").forEach(form => form.addEventListener("submit", async event => {
    event.preventDefault();
    const type = form.dataset.poolAdd;
    const body = {name: form.name.value.trim(), note: form.note.value.trim(), energy: Number(form.energy?.value) || 0, exp: Number(form.exp?.value) || 0};
    try {
      if (type === "daily") await api.taskPool.createDaily(body); else await api.taskPool.createSpecial(body);
      toast("已加入任务池"); tasks(root);
    } catch (reason) { toast(reason.message, true); }
  }));
  root.querySelectorAll("[data-pool-edit]").forEach(button => button.addEventListener("click", () => {
    const row = button.closest(".pool-row");
    row.querySelector(".pool-main").hidden = true;
    row.querySelector(".pool-actions").hidden = true;
    row.querySelector(".pool-edit-form").hidden = false;
    row.querySelector("[name=name]").focus();
  }));
  root.querySelectorAll("[data-pool-cancel]").forEach(button => button.addEventListener("click", () => {
    const row = button.closest(".pool-row");
    row.querySelector(".pool-edit-form").hidden = true;
    row.querySelector(".pool-main").hidden = false;
    row.querySelector(".pool-actions").hidden = false;
  }));
  root.querySelectorAll(".pool-edit-form").forEach(form => form.addEventListener("submit", async event => {
    event.preventDefault();
    const row = form.closest(".pool-row");
    const source = row.dataset.source, taskId = row.dataset.task;
    const current = (source === "daily" ? poolData.daily : poolData.special).find(task => task.taskId === taskId);
    const reward = Math.max(0, Number(form.reward.value) || 0);
    const body = {name: form.name.value.trim(), note: form.note.value.trim(),
      energy: source === "daily" ? reward : 0, exp: source === "special" ? reward : Number(current?.exp) || 0};
    try {
      if (source === "daily") await api.taskPool.updateDaily(taskId, body);
      else await api.taskPool.updateSpecial(taskId, body);
      toast("已更新"); tasks(root);
    } catch (reason) { toast(reason.message, true); }
  }));
  root.querySelectorAll("[data-pool-toggle]").forEach(button => button.addEventListener("click", async () => {
    const [source, taskId, enabled] = button.dataset.poolToggle.split("|");
    try {
      if (source === "daily") await api.taskPool.setDailyEnabled(taskId, enabled === "1");
      else await api.taskPool.setSpecialEnabled(taskId, enabled === "1");
      tasks(root);
    } catch (reason) { toast(reason.message, true); }
  }));
  root.querySelectorAll("[data-pool-delete]").forEach(button => button.addEventListener("click", async () => {
    if (!confirm("删除这条任务定义？今日实例与历史记录不受影响。")) return;
    const [source, taskId] = button.dataset.poolDelete.split("|");
    try {
      if (source === "daily") await api.taskPool.deleteDaily(taskId); else await api.taskPool.deleteSpecial(taskId);
      tasks(root);
    } catch (reason) { toast(reason.message, true); }
  }));
}

/* ---------- bindings (today view) ---------- */

function bind(root) {
  root.querySelector("#open-pool").addEventListener("click", () => { view = "pool"; tasks(root); });

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
