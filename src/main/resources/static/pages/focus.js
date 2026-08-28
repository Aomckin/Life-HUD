import { api } from "../api/client.js?v=0.3.0-r5";
import { empty, error, escapeHtml, toast } from "../components/ui.js";

const MODE_LABELS = {IRON_CURTAIN: "铁幕", POMODORO: "番茄", FREE: "自由专注"};
const STATUS_LABELS = {RUNNING: "专注中", PAUSED: "已暂停", COMPLETED: "已完成", INTERRUPTED: "已中断"};
const SEGMENT_LABELS = {FOCUS: "专注", BREAK: "休息", INTERRUPTION: "中断"};
let timerId;

const duration = seconds => {
  const total = Math.max(0, Math.floor(seconds || 0));
  const hours = Math.floor(total / 3600);
  const minutes = Math.floor(total % 3600 / 60);
  const secs = total % 60;
  return [hours, minutes, secs].map(value => String(value).padStart(2, "0")).join(":");
};

const compactDuration = seconds => {
  const total = Math.max(0, Math.floor(seconds || 0));
  if (total > 0 && total < 60) return "<1min";
  const hours = Math.floor(total / 3600);
  const minutes = Math.floor(total % 3600 / 60);
  return hours ? `${hours}h ${minutes}min` : `${minutes}min`;
};

const dateTime = value => value ? new Intl.DateTimeFormat("zh-CN", {
  month: "numeric", day: "numeric", hour: "2-digit", minute: "2-digit"
}).format(new Date(value)) : "—";

export async function focus(root) {
  window.clearInterval(timerId);
  root.innerHTML = `<div class="focus-page"><section class="panel focus-loading">正在恢复 Focus 状态…</section></div>`;
  try {
    const [current, today, history] = await Promise.all([
      api.focus.current(), api.focus.today(), api.focus.history()
    ]);
    render(root, current, today, history);
  } catch (reason) {
    root.innerHTML = `<div class="focus-page">${error(reason.message)}</div>`;
  }
}

function render(root, current, today, history) {
  document.body.classList.toggle("focus-active", Boolean(current));
  document.body.classList.toggle("iron-curtain-active", current?.mode === "IRON_CURTAIN");
  root.innerHTML = [
    '<div class="focus-page">',
    current ? activeTemplate(current) : startTemplate(),
    summaryTemplate(today),
    historyTemplate(history),
    '</div>'
  ].join("");
  current ? bindActive(root, current) : bindStart(root);
  bindHistory(root);
}

async function renderBreak(root, completed) {
  document.body.classList.remove("focus-active");
  document.body.classList.remove("iron-curtain-active");
  const [today, history] = await Promise.all([api.focus.today(), api.focus.history()]);
  const rest = completed.breakMinutes || 5;
  root.innerHTML = `<div class="focus-page"><section class="panel focus-break-card"><div class="eyebrow">Round 完成 · ${compactDuration(completed.effectiveSeconds)}</div><h1>先松开一会儿。</h1><p>休息可以跳过，也可以直接进入下一轮。</p><div class="break-timer" id="break-timer">${duration(rest * 60).slice(3)}</div><div class="focus-break-actions"><button class="button button-primary" id="start-break">开始 ${rest} 分钟休息</button><button class="button button-secondary" id="skip-break">跳过休息</button><button class="button button-secondary" id="next-round">继续专注</button></div></section>${summaryTemplate(today)}${historyTemplate(history)}</div>`;
  root.querySelector("#skip-break").addEventListener("click", () => focus(root));
  root.querySelector("#next-round").addEventListener("click", async event => { event.currentTarget.disabled = true; try { await api.focus.start({mode: "POMODORO", title: completed.title, plannedMinutes: completed.plannedMinutes, breakMinutes: rest, relatedTaskIds: completed.relatedTaskIds}); focus(root); } catch (reason) { toast(reason.message, true); event.currentTarget.disabled = false; } });
  root.querySelector("#start-break").addEventListener("click", event => {
    event.currentTarget.disabled = true;
    let remaining = rest * 60;
    const display = root.querySelector("#break-timer");
    window.clearInterval(timerId);
    timerId = window.setInterval(() => {
      remaining -= 1;
      display.textContent = duration(remaining).slice(3);
      if (remaining <= 0) { window.clearInterval(timerId); toast("休息结束，准备好再继续"); focus(root); }
    }, 1000);
  });
}

async function showCompletion(root, completed) {
  const ironCurtain = completed.mode === "IRON_CURTAIN";
  const details = ironCurtain ? `<div class="complete-metrics"><span>实际铁幕 <strong>${compactDuration(completed.actualSeconds)}</strong></span><span>有效专注 <strong>${compactDuration(completed.effectiveSeconds)}</strong></span></div><ul class="complete-segments">${(completed.segments || []).map(item => `<li><span>${escapeHtml(item.title)} · ${SEGMENT_LABELS[item.type]}</span><small>${compactDuration(item.actualSeconds)}</small></li>`).join("")}</ul>` : "";
  root.innerHTML = `<div class="focus-page"><section class="panel focus-complete-flash ${ironCurtain ? "iron-complete-flash" : ""}"><div class="complete-mark" aria-hidden="true">✓</div><div class="eyebrow">${ironCurtain ? "铁幕落幕" : "Focus 完成"}</div><strong>${duration(completed.actualSeconds)}</strong>${details}<p>${ironCurtain ? "世界重新展开，这段过程已经留下来了。" : "这段时间留下来了。"}</p></section></div>`;
  await new Promise(resolve => window.setTimeout(resolve, ironCurtain ? 1800 : 420));
  document.body.classList.remove("focus-active", "iron-curtain-active");
}

async function showIronOpening(root, session) {
  document.body.classList.add("focus-active", "iron-curtain-active");
  root.innerHTML = `<div class="focus-page"><section class="panel iron-opening"><div class="iron-opening-line"></div><div class="eyebrow">Iron Curtain</div><h1>铁幕已开幕</h1><p>${escapeHtml(session.title)}</p><small>现在，只做这一件事。</small></section></div>`;
  await new Promise(resolve => window.setTimeout(resolve, 520));
}

function startTemplate() {
  return `<section class="panel focus-start-card focus-enter">
    <div class="focus-intro"><div class="eyebrow">专注 · Focus</div><h1>现在，认真做一件事。</h1><p>选一种适合此刻的节奏。计时器只负责记住时间，不替你决定什么时候停下。</p></div>
    <form id="focus-start-form" class="focus-form">
      <label class="field focus-title-field"><span>今天准备做什么？</span><input id="focus-title" name="title" maxlength="120" placeholder="例如：开发 Life HUD v0.3" autofocus></label>
      <fieldset class="mode-picker"><legend>模式</legend>
        <label><input type="radio" name="mode" value="IRON_CURTAIN" checked><span><strong>铁幕</strong><small>长时间、低干扰</small></span></label>
        <label><input type="radio" name="mode" value="POMODORO"><span><strong>番茄</strong><small>有计划，也可超时</small></span></label>
        <label><input type="radio" name="mode" value="FREE"><span><strong>自由专注</strong><small>轻量正向计时</small></span></label>
      </fieldset>
      <div class="pomodoro-options" id="pomodoro-options" hidden><span>专注时长</span><div class="preset-row">${[25,45,60,90].map(value => `<button type="button" class="preset ${value === 25 ? "selected" : ""}" data-minutes="${value}">${value} min</button>`).join("")}</div><div class="pomodoro-custom-row"><label class="pomodoro-time-field"><span>自定义专注</span><span class="time-input"><input id="custom-minutes" type="number" min="1" max="720" placeholder="25"><small>min</small></span></label><label class="pomodoro-time-field"><span>休息时长</span><span class="time-input"><input id="break-minutes" type="number" min="1" max="180" value="5"><small>min</small></span></label></div></div>
      <label class="field focus-task-field"><span>关联任务 <small>可选</small></span><input id="focus-task" maxlength="80" placeholder="任务 ID 或名称"></label>
      <button class="button button-primary focus-start-button" type="submit">进入铁幕</button>
    </form>
  </section>`;
}

function activeTemplate(session) {
  const ironCurtain = session.mode === "IRON_CURTAIN";
  const stateLabel = ironCurtain ? (session.status === "PAUSED" ? "铁幕已暂停" : "铁幕进行中") : STATUS_LABELS[session.status];
  return `<section class="panel focus-running-card focus-enter mode-${session.mode.toLowerCase()}" data-status="${session.status}" data-mode="${session.mode}">
    <div class="focus-running-head"><div><div class="eyebrow">${MODE_LABELS[session.mode]}</div><div class="focus-status"><span class="status-dot"></span>${stateLabel}</div></div></div>
    <div class="focus-center"><h1>${escapeHtml(session.title)}</h1><div class="focus-timer" id="focus-timer">${duration(session.actualSeconds)}</div><div class="focus-timer-caption" id="focus-timer-caption">${timerCaption(session)}</div>${ironCurtain ? segmentTemplate(session) : ""}<div class="focus-timeup" id="focus-timeup" hidden><strong>计划时间到了。</strong><span>你可以完成本轮，也可以安静地继续。</span><div><button class="button button-primary" id="finish-at-timeup">完成本轮</button><button class="button button-secondary" id="continue-overtime">继续专注</button></div></div></div>
    <div class="focus-controls">${session.status === "RUNNING" ? `<button class="button button-secondary" id="focus-pause">${ironCurtain ? "暂停铁幕" : "暂停"}</button>` : `<button class="button button-primary" id="focus-resume">${ironCurtain ? "恢复铁幕" : "恢复"}</button>`}<button class="button button-end" id="focus-end">${ironCurtain ? "落幕并结算" : "结束并结算"}</button></div>
    <div class="focus-settlement ${ironCurtain ? "iron-settlement" : ""}" id="focus-settlement" hidden><label class="field"><span>${ironCurtain ? "为落幕留一句话" : "完成了什么？"} <small>可选</small></span><textarea id="focus-note" rows="3" maxlength="500" placeholder="${ironCurtain ? "这道铁幕里，完成了什么？" : "为这段时间留一句话"}"></textarea></label><div><button class="button button-primary" id="focus-complete">${ironCurtain ? "确认落幕" : "完成"}</button><button class="button button-ghost" id="focus-interrupt">标记为中断</button><button class="button button-ghost" id="focus-cancel-settle">返回计时</button></div></div>
  </section>`;
}

function segmentTemplate(session) {
  const active = (session.segments || []).find(item => item.active);
  const previousFocus = [...(session.segments || [])].reverse().find(item => item.type === "FOCUS" && !item.active);
  const rows = (session.segments || []).map(item => `<li class="segment-${item.type.toLowerCase()} ${item.active ? "active" : ""}"><time>${new Date(item.startedAt).toLocaleTimeString("zh-CN", {hour:"2-digit", minute:"2-digit"})}</time><span>${escapeHtml(item.title)}</span><small>${item.active ? "进行中" : compactDuration(item.actualSeconds)}</small></li>`).join("");
  const resume = active && active.type !== "FOCUS" ? `<button class="button button-secondary" data-segment="FOCUS" data-title="${escapeHtml(previousFocus?.title || session.title)}" data-task="${escapeHtml(previousFocus?.relatedTaskId || "")}">继续上一事项</button>` : "";
  return `<div class="iron-process"><div class="iron-current"><span>当前事项</span><strong>${escapeHtml(active?.title || "等待恢复")}</strong><small>${active ? SEGMENT_LABELS[active.type] : "已暂停"} · 有效 ${duration(session.effectiveSeconds)}</small></div>${session.status === "RUNNING" ? `<div class="segment-actions"><button class="button button-secondary" id="segment-switch">切换事项</button><button class="button button-ghost" data-segment="BREAK">休息</button><button class="button button-ghost" data-segment="INTERRUPTION">中断</button>${resume}</div>` : ""}<ol class="segment-timeline">${rows}</ol><form class="segment-form" id="segment-form" hidden><label class="field"><span id="segment-prompt">接下来做什么？</span><input id="segment-title" maxlength="120" placeholder="输入事项或中断原因"></label><label class="field"><span>关联任务 <small>可选</small></span><input id="segment-task" maxlength="80"></label><div><button class="button button-primary" type="submit">开始下一段</button><button class="button button-ghost" type="button" id="segment-cancel">取消</button></div></form></div>`;
}

function timerCaption(session) {
  if (session.mode !== "POMODORO") return session.status === "PAUSED" ? "时间已为你停住" : "已持续时间";
  const planned = session.plannedMinutes * 60;
  return session.actualSeconds >= planned ? `已超时 ${duration(session.actualSeconds - planned)}` : `计划 ${session.plannedMinutes} 分钟`;
}

function summaryTemplate(today) {
  const modes = today.modeSeconds || {};
  const emptyNote = today.sessionCount === 0 ? '<small class="focus-empty-note">今天还没有留下专注时间。</small>' : '';
  return `<section class="focus-summary"><article class="card focus-stat"><span>今日有效 Focus</span><strong>${compactDuration(today.totalSeconds)}</strong>${emptyNote}</article><article class="card focus-stat"><span>Session</span><strong>${today.sessionCount}<small class="stat-unit"> 次</small></strong></article><article class="card focus-stat"><span>最长一次</span><strong>${compactDuration(today.longestSeconds)}</strong></article><article class="card focus-stat focus-mode-stat"><span>模式分布</span><small>铁幕 ${compactDuration(modes.IRON_CURTAIN)} · 番茄 ${compactDuration(modes.POMODORO)} · 自由 ${compactDuration(modes.FREE)}</small></article></section>`;
}

function historyTemplate(history) {
  const rows = history.map(session => { const details = (session.segments || []).map(item => { const edit = encodeURIComponent(JSON.stringify({sessionId:session.id,id:item.id,type:item.type,title:item.title,relatedTaskId:item.relatedTaskId || "",note:item.note || ""})); return `<li><span>${SEGMENT_LABELS[item.type]} · ${escapeHtml(item.title)}</span><small>${compactDuration(item.actualSeconds)}</small>${item.active ? "" : `<button class="button button-ghost segment-edit" data-edit="${edit}">编辑</button>`}</li>`; }).join(""); return `<details class="focus-history-entry status-${session.status.toLowerCase()}"><summary class="focus-history-row"><span class="focus-mode-badge mode-${session.mode.toLowerCase()}">${MODE_LABELS[session.mode]}</span><div class="focus-history-main"><strong title="${escapeHtml(session.title)}">${escapeHtml(session.title)}</strong><small>${dateTime(session.startedAt)}${session.endedAt ? ` – ${dateTime(session.endedAt)}` : " · 进行中"}</small></div><div class="focus-history-duration"><strong>${compactDuration(session.effectiveSeconds)}</strong><small>有效专注 · 实际 ${compactDuration(session.actualSeconds)}</small></div></summary>${details ? `<ul class="history-segments">${details}</ul>` : ""}</details>`; }).join("");
  return `<section class="panel focus-history"><div class="section-head"><div><div class="eyebrow">History</div><h2>最近专注</h2></div><button class="button button-ghost" id="manual-focus">＋ 补录</button></div><form class="manual-focus-form" id="manual-focus-form" hidden><label class="field"><span>标题 / 总结</span><input id="manual-title" required maxlength="120"></label><div class="manual-time-grid"><label class="field"><span>开始</span><input id="manual-start" type="datetime-local" required></label><label class="field"><span>结束</span><input id="manual-end" type="datetime-local" required></label></div><label class="field"><span>完成了什么？</span><textarea id="manual-note" maxlength="500"></textarea></label><div><button class="button button-primary" type="submit">保存补录</button><button class="button button-ghost" type="button" id="manual-cancel">取消</button></div></form>${rows ? `<div class="focus-history-list">${rows}</div>` : empty("还没有 Focus 记录。从一次专注开始。")}</section>`;
}

function bindStart(root) {
  const form = root.querySelector("#focus-start-form");
  const pomodoro = root.querySelector("#pomodoro-options");
  const startButton = root.querySelector(".focus-start-button");
  let plannedMinutes = 25;
  form.querySelectorAll('[name="mode"]').forEach(input => input.addEventListener("change", () => {
    pomodoro.hidden = input.value !== "POMODORO" || !input.checked;
    if (input.checked) startButton.textContent = input.value === "IRON_CURTAIN" ? "进入铁幕" : input.value === "POMODORO" ? "开始番茄" : "开始自由专注";
  }));
  root.querySelectorAll("[data-minutes]").forEach(button => button.addEventListener("click", () => {
    plannedMinutes = Number(button.dataset.minutes);
    root.querySelector("#custom-minutes").value = "";
    root.querySelectorAll("[data-minutes]").forEach(item => item.classList.toggle("selected", item === button));
  }));
  root.querySelector("#custom-minutes").addEventListener("input", event => {
    plannedMinutes = Number(event.target.value);
    root.querySelectorAll("[data-minutes]").forEach(item => item.classList.remove("selected"));
  });
  form.addEventListener("submit", async event => {
    event.preventDefault();
    const button = form.querySelector("[type=submit]");
    button.disabled = true;
    const mode = new FormData(form).get("mode");
    if (mode === "POMODORO" && (!Number.isInteger(plannedMinutes) || plannedMinutes < 1 || plannedMinutes > 720)) {
      toast("请输入 1 到 720 分钟的番茄时长", true);
      button.disabled = false;
      root.querySelector("#custom-minutes").focus();
      return;
    }
    try {
      const started = await api.focus.start({mode, title: root.querySelector("#focus-title").value, taskId: root.querySelector("#focus-task").value, plannedMinutes: mode === "POMODORO" ? plannedMinutes : null, breakMinutes: mode === "POMODORO" ? Number(root.querySelector("#break-minutes").value) : null});
      toast(mode === "IRON_CURTAIN" ? "铁幕已开幕" : "Focus 已开始");
      if (mode === "IRON_CURTAIN") await showIronOpening(root, started);
      focus(root);
    } catch (reason) { toast(reason.message, true); button.disabled = false; }
  });
}

function bindActive(root, session) {
  let baseSeconds = session.actualSeconds;
  const receivedAt = Date.now();
  let overtimeAcknowledged = false;
  const tick = () => {
    if (!root.isConnected) return window.clearInterval(timerId);
    const actual = baseSeconds + (session.status === "RUNNING" ? Math.floor((Date.now() - receivedAt) / 1000) : 0);
    const timer = root.querySelector("#focus-timer");
    if (!timer) return;
    if (session.mode === "POMODORO") {
      const remaining = session.plannedMinutes * 60 - actual;
      timer.textContent = remaining >= 0 ? duration(remaining) : `+ ${duration(-remaining)}`;
      timer.classList.toggle("overtime", remaining < 0);
      timer.classList.toggle("ending-soon", remaining >= 0 && remaining <= 60);
      root.querySelector("#focus-timer-caption").textContent = remaining > 60 ? `计划 ${session.plannedMinutes} 分钟` : remaining >= 0 ? "最后一分钟，保持现在的节奏" : "超过计划时间后的额外专注";
      root.querySelector("#focus-timeup").hidden = remaining >= 0 || overtimeAcknowledged || session.status === "PAUSED";
    } else timer.textContent = duration(actual);
    const effective = root.querySelector(".iron-current small");
    const activeSegment = (session.segments || []).find(item => item.active);
    if (effective) effective.textContent = `${activeSegment ? SEGMENT_LABELS[activeSegment.type] : "已暂停"} · 有效 ${duration(session.effectiveSeconds + (session.status === "RUNNING" && activeSegment?.type === "FOCUS" ? Math.floor((Date.now() - receivedAt) / 1000) : 0))}`;
  };
  tick();
  timerId = window.setInterval(tick, 1000);
  const action = async (call, success, button) => {
    try { await call(); toast(success); window.clearInterval(timerId); focus(root); }
    catch (reason) { toast(reason.message, true); if (button) button.disabled = false; }
  };
  root.querySelector("#focus-pause")?.addEventListener("click", event => { event.currentTarget.disabled = true; action(() => api.focus.pause(session.id), "Focus 已暂停", event.currentTarget); });
  root.querySelector("#focus-resume")?.addEventListener("click", event => { event.currentTarget.disabled = true; action(() => api.focus.resume(session.id), "继续专注", event.currentTarget); });
  const settlement = root.querySelector("#focus-settlement");
  const showSettlement = () => { settlement.hidden = false; root.querySelector(".focus-center").classList.add("settling"); root.querySelector(".focus-running-card").classList.toggle("iron-settling", session.mode === "IRON_CURTAIN"); root.querySelector("#focus-note").focus(); };
  root.querySelector("#focus-end").addEventListener("click", showSettlement);
  root.querySelector("#finish-at-timeup")?.addEventListener("click", showSettlement);
  root.querySelector("#continue-overtime")?.addEventListener("click", () => { overtimeAcknowledged = true; root.querySelector("#focus-timeup").hidden = true; toast("继续，按自己的节奏来"); });
  root.querySelector("#focus-cancel-settle").addEventListener("click", () => { settlement.hidden = true; root.querySelector(".focus-center").classList.remove("settling"); root.querySelector(".focus-running-card").classList.remove("iron-settling"); });
  root.querySelector("#focus-complete").addEventListener("click", async event => {
    event.currentTarget.disabled = true;
    try {
      const completed = await api.focus.complete(session.id, root.querySelector("#focus-note").value);
      toast("本轮 Focus 已完成");
      window.clearInterval(timerId);
      await showCompletion(root, completed);
      if (session.mode === "POMODORO") await renderBreak(root, completed); else focus(root);
    } catch (reason) { toast(reason.message, true); event.currentTarget.disabled = false; }
  });
  root.querySelector("#focus-interrupt").addEventListener("click", event => { event.currentTarget.disabled = true; action(() => api.focus.interrupt(session.id, root.querySelector("#focus-note").value), "本轮已记录为中断", event.currentTarget); });

  let nextType = "FOCUS";
  const form = root.querySelector("#segment-form");
  const openSegment = (type, title = "") => { nextType = type; form.hidden = false; root.querySelector("#segment-prompt").textContent = type === "INTERRUPTION" ? "发生了什么？" : "接下来做什么？"; root.querySelector("#segment-title").value = title; root.querySelector("#segment-title").focus(); };
  root.querySelector("#segment-switch")?.addEventListener("click", () => openSegment("FOCUS"));
  root.querySelectorAll("[data-segment]").forEach(button => button.addEventListener("click", () => {
    const type = button.dataset.segment;
    if (type === "BREAK" || (type === "FOCUS" && button.dataset.title)) action(() => api.focus.switchSegment(session.id, {type, title: button.dataset.title || "休息", relatedTaskId: button.dataset.task || null}), type === "BREAK" ? "开始休息" : "继续专注", button);
    else openSegment(type, button.dataset.title || "");
  }));
  root.querySelector("#segment-cancel")?.addEventListener("click", () => { form.hidden = true; });
  form?.addEventListener("submit", event => { event.preventDefault(); const button=form.querySelector("[type=submit]"); button.disabled=true; action(() => api.focus.switchSegment(session.id,{type:nextType,title:root.querySelector("#segment-title").value,relatedTaskId:root.querySelector("#segment-task").value,note:nextType === "INTERRUPTION" ? root.querySelector("#segment-title").value : null}), "过程已记录", button); });
}

function bindHistory(root) {
  const form = root.querySelector("#manual-focus-form");
  root.querySelector("#manual-focus")?.addEventListener("click", () => { form.hidden = false; root.querySelector("#manual-title").focus(); });
  root.querySelector("#manual-cancel")?.addEventListener("click", () => { form.hidden = true; });
  form?.addEventListener("submit", async event => { event.preventDefault(); const button=form.querySelector("[type=submit]"); button.disabled=true; try { await api.focus.manual({title:root.querySelector("#manual-title").value,startedAt:new Date(root.querySelector("#manual-start").value).toISOString(),endedAt:new Date(root.querySelector("#manual-end").value).toISOString(),note:root.querySelector("#manual-note").value}); toast("铁幕补录已保存"); focus(root); } catch(reason) { toast(reason.message,true); button.disabled=false; } });
  root.querySelectorAll(".segment-edit").forEach(button => button.addEventListener("click", async () => {
    const item = JSON.parse(decodeURIComponent(button.dataset.edit));
    const title = window.prompt("事项标题", item.title); if (title === null) return;
    const type = window.prompt("类型：FOCUS / BREAK / INTERRUPTION", item.type); if (type === null) return;
    const relatedTaskId = window.prompt("关联任务（可留空）", item.relatedTaskId); if (relatedTaskId === null) return;
    const note = window.prompt("备注（可留空）", item.note); if (note === null) return;
    button.disabled = true;
    try { await api.focus.updateSegment(item.sessionId, item.id, {title, type:type.trim().toUpperCase(), relatedTaskId, note}); toast("过程记录已更新"); focus(root); }
    catch (reason) { toast(reason.message, true); button.disabled = false; }
  }));
}
