import { api } from "../api/client.js?v=0.5.1";
import { taskCopy as c } from "../content/copy.js?v=0.5.1";
import { empty, error, escapeHtml, toast } from "../components/ui.js";

let dreamsCache = [];

export async function tasks(root) {
  root.innerHTML = '<section class="panel">正在读取任务…</section>';
  try {
    const [directions, dreams] = await Promise.all([api.taskDirections.all(), api.dreams.all()]);
    dreamsCache = dreams;
    render(root, directions);
  } catch (reason) { root.innerHTML = error(reason.message); }
}

function render(root, directions) {
  const row = task => `
    <div class="task-direction-row" data-source="${task.source}" data-task="${task.taskId}">
      <div><strong>${escapeHtml(task.name)}</strong>
        <small>${task.done ? c.doneBadge : c.pendingBadge}</small></div>
      <div class="task-direction-side">
        <span class="direction-text">${directionText(task.direction) || `<em>${c.noDirection}</em>`}</span>
        <button class="text-link" data-link="${task.source}|${task.taskId}">${c.editLink}</button>
        ${hasDirection(task.direction)?`<button class="text-link danger-link" data-unlink="${task.source}|${task.taskId}">${c.unlinkLabel}</button>`:""}
      </div></div>`;
  root.innerHTML = `<div class="direction-page">
    <section class="panel"><div class="section-head"><div><div class="eyebrow">v0.5 · Direction</div><h2>${c.dailyTitle}</h2></div></div>
      ${directions.daily.map(row).join("")||empty(c.emptyTasks)}</section>
    <section class="panel"><div class="section-head"><h2>${c.specialTitle}</h2></div>
      ${directions.special.map(row).join("")||empty(c.emptyTasks)}</section>
    <form class="milestone-form" id="link-form" hidden>
      <h3>${c.linkTitle}</h3>
      <small>${c.linkHint}</small>
      <input type="hidden" id="link-source"><input type="hidden" id="link-task">
      <label class="field"><span>${c.dreamLabel}</span><select id="link-dream"><option value="">—</option></select></label>
      <label class="field"><span>${c.goalLabel}</span><select id="link-goal" disabled><option value="">—</option></select></label>
      <label class="field"><span>${c.milestoneLabel}</span><select id="link-milestone" disabled><option value="">—</option></select></label>
      <div><button class="button button-primary" type="submit">${c.save}</button>
      <button class="button button-ghost" type="button" id="cancel-link">${"取消"}</button></div>
    </form></div>`;

  root.querySelectorAll("[data-link]").forEach(button => button.addEventListener("click", () => openLink(button.dataset.link)));
  root.querySelectorAll("[data-unlink]").forEach(button => button.addEventListener("click", async () => {
    const [source, taskId] = button.dataset.unlink.split("|");
    try { await api.taskDirections.unlink(source, taskId); toast("已取消关联"); tasks(root); }
    catch (reason) { toast(reason.message, true); }
  }));

  function directionText(d) {
    return [d.dreamTitle, d.goalTitle, d.dreamMilestoneTitle].filter(Boolean).join(" › ");
  }
  function hasDirection(d) { return Boolean(d.dreamId || d.goalId || d.dreamMilestoneId); }

  function openLink(key) {
    const [source, taskId] = key.split("|");
    const form = root.querySelector("#link-form");
    form.dataset.source = source;
    form.dataset.task = taskId;
    const dreamSelect = root.querySelector("#link-dream");
    const goalSelect = root.querySelector("#link-goal");
    const milestoneSelect = root.querySelector("#link-milestone");
    dreamSelect.innerHTML = '<option value="">—</option>' + dreamsCache.map(d =>
      `<option value="${d.id}">${escapeHtml(d.title)}</option>`).join("");
    goalSelect.innerHTML = '<option value="">—</option>';
    milestoneSelect.innerHTML = '<option value="">—</option>';
    goalSelect.disabled = true;
    milestoneSelect.disabled = true;
    const taskRow = [...root.querySelectorAll(".task-direction-row")]
      .find(row => row.dataset.task === taskId && row.dataset.source === source);
    const current = taskRow ? taskRow.querySelector(".direction-text") : null;
    dreamSelect.value = "";
    form.hidden = false;
    form.scrollIntoView({behavior: "smooth"});

    dreamSelect.onchange = async () => {
      goalSelect.innerHTML = '<option value="">—</option>';
      milestoneSelect.innerHTML = '<option value="">—</option>';
      goalSelect.disabled = true;
      milestoneSelect.disabled = true;
      if (!dreamSelect.value) return;
      const detail = await api.dreams.detail(dreamSelect.value);
      goalSelect.disabled = false;
      detail.goals.forEach(({goal}) => goalSelect.insertAdjacentHTML("beforeend",
        `<option value="${goal.id}">${escapeHtml(goal.title)}</option>`));
      goalSelect.onchange = () => {
        milestoneSelect.innerHTML = '<option value="">—</option>';
        const selected = detail.goals.find(g => g.goal.id === goalSelect.value);
        if (selected && selected.milestones.length) {
          milestoneSelect.disabled = false;
          selected.milestones.forEach(m => milestoneSelect.insertAdjacentHTML("beforeend",
            `<option value="${m.id}">${escapeHtml(m.title)}</option>`));
        } else milestoneSelect.disabled = true;
      };
    };
  }

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
