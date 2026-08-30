import { api } from "../api/client.js?v=0.6.1";
import { ritualCopy as c } from "../content/copy.js?v=0.6.1";
import { empty, error, escapeHtml, toast } from "../components/ui.js";

const time = value => new Date(value).toLocaleString("zh-CN", {month:"short",day:"numeric",hour:"2-digit",minute:"2-digit"});
const stepTypeLabels = {TEXT:"提示",CHECK:"确认",TIMER:"计时",LINK:"链接",MUSIC_HINT:"音乐",NOTE:"记录"};
let running = null; // {execution, steps, index}

export async function rituals(root) {
  if (running) return renderRunner(root);
  root.innerHTML = '<section class="panel">正在整理仪式…</section>';
  try {
    const rituals = await api.rituals.all();
    const details = await Promise.all(rituals.map(r => api.rituals.detail(r.id)));
    render(root, details);
  } catch (reason) { root.innerHTML = error(reason.message); }
}

function render(root, details) {
  const enabled = details.filter(d => d.ritual.enabled);
  const disabled = details.filter(d => !d.ritual.enabled);
  const card = d => {
    const r = d.ritual;
    const last = d.executions.find(e => e.status === "COMPLETED");
    return `<article class="card ritual-card"><div class="ritual-card-body"><div class="ritual-badges"><span class="badge">${escapeHtml(r.category || "仪式")}</span>
      ${r.triggerTime?`<span class="badge">${escapeHtml(String(r.triggerTime).slice(0,5))}</span>`:""}
      </div><h3>${escapeHtml(r.name)}</h3><p>${escapeHtml(r.description || "")}</p>
      <small>${d.steps.length ? `${d.steps.length} 个步骤 · ${last?`上次完成 ${time(last.completedAt)}`:"还没有完成过"}` : c.noSteps}</small></div>
      <div class="row-actions ritual-actions">
        ${r.enabled && d.steps.length?`<button class="button button-primary" data-start="${r.id}">${c.start}</button>`:""}
        <button class="text-link" data-edit="${r.id}">${c.edit}</button>
        ${r.enabled?`<button class="text-link" data-disable="${r.id}">${c.disable}</button>`:`<button class="text-link" data-enable="${r.id}">${c.enableAction}</button>`}
        <button class="text-link danger-link" data-delete="${r.id}">${"删除"}</button>
      </div></article>`;
  };
  root.innerHTML = `<div class="direction-page"><section class="panel"><div class="section-head"><div><div class="eyebrow">v0.5 · Ritual</div><h2>进入一种状态</h2></div><button class="button button-primary" id="new-ritual">${c.newRitual}</button></div>
    <div class="ritual-grid">${enabled.map(card).join("")||empty(c.empty)}</div>
  </section>
  ${disabled.length?`<section class="panel"><div class="section-head"><h2>${c.disabledTitle}</h2></div><div class="ritual-grid">${disabled.map(card).join("")}</div></section>`:""}
  <form class="panel milestone-form ritual-form-panel" id="ritual-form" hidden>
    <div class="section-head"><h2 id="ritual-form-title">${c.newRitual}</h2></div>
    <label class="field"><span>${c.nameLabel}</span><input id="ritual-name" maxlength="60" required></label>
    <div class="milestone-form-grid">
      <label class="field"><span>${c.descriptionLabel}</span><input id="ritual-description" maxlength="120"></label>
      <label class="field"><span>${c.categoryLabel}</span><input id="ritual-category" maxlength="30" placeholder="晨间 / 夜间 / 创作…"></label>
    </div>
    <div class="milestone-form-grid">
      <label class="field"><span>${c.triggerTimeLabel}</span><input id="ritual-time" type="time"></label>
      <label class="check-field"><input id="ritual-enabled" type="checkbox" checked> ${c.enabledLabel}</label>
    </div>
    <div class="section-head"><h3>${c.stepsTitle}</h3><button class="text-link" type="button" id="add-step">${c.addStep}</button></div>
    <div id="step-editor"></div>
    <div class="row-actions ritual-form-actions"><button class="button button-primary" type="submit">${c.save}</button><button class="button button-ghost" type="button" id="cancel-ritual">${c.cancel}</button></div>
  </form></div>`;

  const stepEditor = root.querySelector("#step-editor");
  const addStepRow = (step = {}) => {
    const row = document.createElement("div");
    row.className = "step-row";
    row.innerHTML = `<select class="step-type">${Object.entries(stepTypeLabels).map(([id,label])=>`<option value="${id}" ${step.type===id?"selected":""}>${label}</option>`).join("")}</select>
      <input class="step-title" placeholder="${c.stepTitleLabel}" maxlength="60" value="${escapeHtml(step.title||"")}">
      <input class="step-content" placeholder="${c.stepContentLabel}" maxlength="200" value="${escapeHtml(step.content||"")}">
      <input class="step-duration" type="number" min="0" placeholder="${c.stepDurationLabel}" value="${step.durationSeconds||""}">
      <input class="step-url" placeholder="${c.stepUrlLabel}" value="${escapeHtml(step.url||"")}">
      <label class="check-field"><input type="checkbox" class="step-required" ${step.required!==false?"checked":""}> ${c.stepRequiredLabel}</label>
      <button class="text-link danger-link" type="button">${c.removeStep}</button>`;
    row.querySelector("button").addEventListener("click", () => row.remove());
    stepEditor.append(row);
  };
  root.querySelector("#add-step").addEventListener("click", () => addStepRow());

  const form = root.querySelector("#ritual-form");
  let editingId = null;
  root.querySelector("#new-ritual").addEventListener("click", () => {
    editingId = null; form.reset(); root.querySelector("#ritual-enabled").checked = true;
    root.querySelector("#ritual-form-title").textContent = c.newRitual;
    stepEditor.innerHTML = ""; addStepRow(); form.hidden = false;
    form.scrollIntoView({behavior:"smooth", block:"start"}); root.querySelector("#ritual-name").focus();
  });
  root.querySelector("#cancel-ritual").addEventListener("click", () => { editingId = null; form.hidden = true; });

  form.onsubmit = async event => {
    event.preventDefault();
    const steps = [...stepEditor.querySelectorAll(".step-row")].map((row, index) => ({
      type: row.querySelector(".step-type").value,
      title: row.querySelector(".step-title").value.trim(),
      content: row.querySelector(".step-content").value.trim(),
      durationSeconds: Number(row.querySelector(".step-duration").value) || 0,
      url: row.querySelector(".step-url").value.trim(),
      sortOrder: index,
      required: row.querySelector(".step-required").checked
    })).filter(step => step.title);
    try {
      const body = {name: v("ritual-name"), description: v("ritual-description"), category: v("ritual-category"),
        triggerTime: v("ritual-time") || null, enabled: root.querySelector("#ritual-enabled").checked, steps};
      if (editingId) await api.rituals.update(editingId, body); else await api.rituals.create(body);
      toast(editingId ? "仪式已更新" : "仪式已创建"); editingId = null; rituals(root);
    } catch (reason) { toast(reason.message, true); }
  };

  root.querySelectorAll("[data-start]").forEach(button => button.addEventListener("click", async () => {
    try {
      const execution = await api.rituals.start(button.dataset.start);
      const view = await api.rituals.execution(execution.id);
      running = {execution: view.execution, steps: view.steps, index: 0};
      renderRunner(root);
    } catch (reason) { toast(reason.message, true); }
  }));
  root.querySelectorAll("[data-edit]").forEach(button => button.addEventListener("click", () => openEdit(button.dataset.edit)));
  root.querySelectorAll("[data-disable]").forEach(button => button.addEventListener("click", async () => {
    await api.rituals.disable(button.dataset.disable); rituals(root);
  }));
  root.querySelectorAll("[data-enable]").forEach(button => button.addEventListener("click", async () => {
    await api.rituals.enable(button.dataset.enable); rituals(root);
  }));
  root.querySelectorAll("[data-delete]").forEach(button => button.addEventListener("click", async () => {
    if (!confirm(c.deleteConfirm)) return;
    try { await api.rituals.remove(button.dataset.delete); rituals(root); }
    catch (reason) { toast(reason.message, true); }
  }));

  function v(id) { return root.querySelector("#" + id)?.value.trim() || ""; }
  async function openEdit(id) {
    const detail = await api.rituals.detail(id);
    const r = detail.ritual;
    editingId = id;
    form.reset();
    root.querySelector("#ritual-form-title").textContent = `编辑「${r.name}」`;
    root.querySelector("#ritual-name").value = r.name;
    root.querySelector("#ritual-description").value = r.description || "";
    root.querySelector("#ritual-category").value = r.category || "";
    root.querySelector("#ritual-time").value = r.triggerTime || "";
    root.querySelector("#ritual-enabled").checked = r.enabled;
    stepEditor.innerHTML = "";
    detail.steps.forEach(step => addStepRow({type: step.type, title: step.title, content: step.content,
      durationSeconds: step.durationSeconds, url: step.url, required: step.required}));
    form.hidden = false;
    form.scrollIntoView({behavior:"smooth", block:"start"});
  }
}

async function renderRunner(root) {
  const {execution, steps, index} = running;
  const step = steps[index];
  const result = execution.stepResults.find(r => r.stepId === step.id) || {};
  const total = steps.length;
  root.innerHTML = `<div class="direction-page"><section class="panel ritual-runner">
    <div class="runner-head"><h2>${escapeHtml(execution.ritualName)}</h2><span class="badge">${c.runnerProgress(index, total)}</span></div>
    <div class="runner-progress"><span style="width:${Math.round(index / total * 100)}%"></span></div>
    <div class="runner-step">
      <span class="badge">${stepTypeLabels[step.type] || step.type}${step.required?"":" · 可跳过"}</span>
      <h3>${escapeHtml(step.title)}</h3>
      ${step.content?`<p>${escapeHtml(step.content)}</p>`:""}
      ${step.type==="TIMER"?`<p class="runner-timer" data-seconds="${step.durationSeconds}">${step.durationSeconds}s</p>`:""}
      ${step.type==="NOTE"?`<textarea id="runner-note" rows="3" placeholder="${c.notePlaceholder}">${escapeHtml(result.note||"")}</textarea>`:""}
      ${step.url?`<a class="button button-secondary" href="${escapeHtml(step.url)}" target="_blank" rel="noopener">${c.openLink}</a>`:""}
    </div>
    <div class="row-actions runner-actions">
      <button class="button button-ghost" id="runner-prev" ${index===0?"disabled":""}>${c.prevStep}</button>
      ${step.required||index===total-1?`<button class="button button-primary" id="runner-next">${index===total-1?c.finishRitual:c.stepDone}</button>`
        :`<button class="button button-secondary" id="runner-skip">${c.skipStep}</button><button class="button button-primary" id="runner-next">${c.stepDone}</button>`}
      <button class="text-link danger-link" id="runner-cancel">${c.cancelRitual}</button>
    </div>
    ${execution.note?`<small>${escapeHtml(execution.note)}</small>`:""}
  </section></div>`;

  let timerHandle = null;
  if (step.type === "TIMER") {
    const display = root.querySelector(".runner-timer");
    let left = step.durationSeconds;
    timerHandle = window.setInterval(() => {
      left = Math.max(0, left - 1);
      display.textContent = `${left}s`;
      if (left === 0) window.clearInterval(timerHandle);
    }, 1000);
  }

  const saveStep = async (done, note) => {
    try { running.execution = await api.rituals.step(execution.id, step.id, {done, note: note ?? null}); }
    catch (reason) { toast(reason.message, true); }
  };
  const go = index => { running.index = Math.max(0, Math.min(total - 1, index)); renderRunner(root); };

  root.querySelector("#runner-prev").addEventListener("click", () => { if (timerHandle) window.clearInterval(timerHandle); go(index - 1); });
  root.querySelector("#runner-next").addEventListener("click", async () => {
    if (timerHandle) window.clearInterval(timerHandle);
    const note = root.querySelector("#runner-note")?.value ?? null;
    await saveStep(true, note);
    if (index === total - 1) return finish();
    go(index + 1);
  });
  root.querySelector("#runner-skip")?.addEventListener("click", () => { if (timerHandle) window.clearInterval(timerHandle); go(index + 1); });
  root.querySelector("#runner-cancel").addEventListener("click", async () => {
    if (!confirm(c.cancelConfirm)) return;
    if (timerHandle) window.clearInterval(timerHandle);
    await api.rituals.cancelExecution(execution.id);
    running = null; rituals(root);
  });

  async function finish() {
    const pending = steps.filter(s => s.required).filter(s => {
      const r = running.execution.stepResults.find(x => x.stepId === s.id);
      return !r || r.status !== "DONE";
    });
    if (pending.length && !confirm(c.requiredPending)) return;
    try {
      await api.rituals.completeExecution(execution.id, running.execution.note || "");
      toast("仪式完成"); running = null; rituals(root);
    } catch (reason) { toast(reason.message, true); }
  }
}
