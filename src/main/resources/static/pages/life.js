import { api } from "../api/client.js?v=0.6.0";
import { confirmDialog, empty, error, escapeHtml, toast } from "../components/ui.js?v=0.6.0";

/**
 * /life — today's input panel. High-frequency entry: short modal forms, sensible
 * default times, immediate feedback. History and the whole river live in /journal.
 */

const MEAL_LABELS = {BREAKFAST: "早餐", LUNCH: "午餐", DINNER: "晚餐", SNACK: "夜宵", OTHER: "一餐"};
const SLEEP_LABELS = {NIGHT: "昨晚", NAP: "午睡", OTHER: "其他睡眠"};
const EXERCISE_LABELS = {STRENGTH: "力量训练", WALK: "散步", CYCLING: "骑行", MAIMAI: "舞萌", RUNNING: "跑步", OTHER: "其他运动"};
const INTENSITY_LABELS = {LOW: "低强度", MEDIUM: "中等强度", HIGH: "高强度"};
const RECORD_LABELS = {WATER: "喝水", CAFFEINE: "咖啡因", ALCOHOL: "饮酒", SUNLIGHT: "晒太阳", SOCIAL: "社交",
  BODY_STATUS: "身体状态", OUTDOOR: "外出", CUSTOM: "自定义"};
const RECORD_UNITS = {WATER: "ml", CAFFEINE: "杯", ALCOHOL: "ml", SUNLIGHT: "min", SOCIAL: "次",
  BODY_STATUS: "级", OUTDOOR: "min", CUSTOM: ""};

const localInputValue = date => {
  const pad = n => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
};
const fromInput = value => value ? new Date(value).toISOString() : null;
const clock = value => new Intl.DateTimeFormat("zh-CN", {hour: "2-digit", minute: "2-digit"}).format(new Date(value));
const today = () => new Intl.DateTimeFormat("sv-SE", {timeZone: "Asia/Shanghai"}).format(new Date());

export async function life(root) {
  root.innerHTML = '<section class="panel">正在铺开今天的生活…</section>';
  try {
    const [latest, todayItems] = await Promise.all([
      api.life.checkIns.latest().catch(() => null),
      api.timeline({date: today(), sources: "life", size: 50})
    ]);
    render(root, latest, [...todayItems].reverse());
  } catch (reason) { root.innerHTML = error(reason.message); }
}

function render(root, latest, todayItems) {
  const status = latest ? `
    <div class="life-status-grid">
      ${[["能量", latest.energy], ["心情", latest.mood], ["专注意愿", latest.focusDesire], ["疲劳", latest.fatigue]]
        .map(([label, value]) => `<div class="life-status-cell"><small>${label}</small><strong>${value}</strong></div>`).join("")}
    </div>
    <small class="muted">${clock(latest.time)} 记录 · 只是最近一次,不是实时</small>`
    : empty("还没有状态记录。花十秒钟,告诉 HUD 你现在怎么样。");
  const quick = [["check-in", "记一次状态"], ["meal", "记一餐"], ["sleep", "记睡眠"], ["exercise", "记运动"], ["record", "其他记录"]];
  root.innerHTML = `<div class="life-page">
    <section class="panel life-hero">
      <div class="section-head"><div><div class="eyebrow">v0.6 · Life</div>
        <h2>今天的生活,从这里进来。</h2></div>
        <a class="text-link" href="/journal" data-link>去时间线看看 →</a></div>
      <div class="life-hero-grid">
        <div class="life-status card"><div class="section-head"><h3>最近状态</h3></div>${status}</div>
        <div class="life-quick card"><div class="section-head"><h3>快速记录</h3></div>
          <div class="life-quick-row">${quick.map(([kind, label]) =>
            `<button class="button button-secondary" data-quick="${kind}">${label}</button>`).join("")}</div>
        </div>
      </div>
    </section>
    <section class="panel"><div class="section-head"><h2>今日生活</h2>
      <small class="muted">${todayItems.length ? `${todayItems.length} 条` : ""}</small></div>
      <div class="life-today">${todayItems.length ? todayItems.map(item => todayCard(item)).join("") : empty("今天还没有留下生活的痕迹。上面挑一个,随手记一笔。")}</div>
    </section>
  </div>`;
  bind(root);
}

const SOURCE_ICONS = {SLEEP: "☾", MEAL: "🍚", EXERCISE: "⚡", CHECK_IN: "◐", LIFE_RECORD: "✦"};

let itemsById = new Map();

function todayCard(item) {
  itemsById.set(item.eventId, item);
  const summary = item.summary ? `<small>${escapeHtml(item.summary)}</small>` : "";
  const thumbs = item.media.length ? `<div class="life-thumbs">${item.media.map(src =>
    `<img src="${escapeHtml(src)}" alt="" data-lightbox="${escapeHtml(src)}">`).join("")}</div>` : "";
  return `<article class="life-entry" data-entry="${item.eventId}">
    <time>${clock(item.occurredAt)}</time>
    <span class="life-icon">${SOURCE_ICONS[item.source.toUpperCase()] || "✦"}</span>
    <div class="life-entry-body"><strong>${escapeHtml(item.title)}</strong>${summary}${thumbs}</div>
    <button class="text-link danger-link" data-remove-entry="${item.eventId}">删除</button>
  </article>`;
}

function bind(root) {
  root.querySelectorAll("[data-quick]").forEach(button =>
    button.addEventListener("click", () => openModal(button.dataset.quick)));
  root.querySelectorAll("[data-lightbox]").forEach(img => img.addEventListener("click", event => {
    event.stopPropagation();
    lightbox(img.dataset.lightbox);
  }));
  root.querySelectorAll("[data-remove-entry]").forEach(button => button.addEventListener("click", async event => {
    event.stopPropagation();
    if (!(await confirmDialog("删除这条生活记录?时间线里的对应足迹也会一并消失。"))) return;
    const item = itemsById.get(button.dataset.removeEntry);
    const target = {SLEEP: api.life.sleep, MEAL: api.life.meals, EXERCISE: api.life.exercises,
      CHECK_IN: api.life.checkIns, LIFE_RECORD: api.life.records}[item.source.toUpperCase()];
    try { await target.remove(item.sourceId); toast("已删除"); life(root); }
    catch (reason) { toast(reason.message, true); }
  }));
  /* click a today card to edit it in the same quick modal */
  root.querySelectorAll("[data-entry]").forEach(card => card.addEventListener("click", async event => {
    if (event.target.closest("[data-remove-entry]") || event.target.closest("[data-lightbox]")) return;
    const item = itemsById.get(card.dataset.entry);
    const kind = {CHECK_IN: "check-in", MEAL: "meal", SLEEP: "sleep", EXERCISE: "exercise", LIFE_RECORD: "record"}[item.source.toUpperCase()];
    if (!kind) return;
    const listers = {CHECK_IN: api.life.checkIns, MEAL: api.life.meals, SLEEP: api.life.sleep,
      EXERCISE: api.life.exercises, LIFE_RECORD: api.life.records};
    try {
      const record = (await listers[item.source.toUpperCase()].all()).find(entry => entry.id === item.sourceId);
      if (record) openModal(kind, record);
      else toast("原记录已不在了", true);
    } catch (reason) { toast(reason.message, true); }
  }));
}

/* ---------- modal quick entry ---------- */

function modalShell(title, bodyHtml) {
  const overlay = document.createElement("div");
  overlay.className = "board-modal-overlay";
  overlay.innerHTML = `<div class="board-modal panel life-modal">
    <div class="section-head"><h2>${title}</h2><button class="text-link" data-close>✕</button></div>
    <form class="life-form">${bodyHtml}
      <div class="row-actions"><button class="button button-primary" type="submit">保存</button>
      <button class="button button-ghost" type="button" data-close>取消</button></div></form></div>`;
  document.body.append(overlay);
  const close = () => overlay.remove();
  overlay.querySelectorAll("[data-close]").forEach(b => b.addEventListener("click", close));
  overlay.addEventListener("click", event => { if (event.target === overlay) close(); });
  return {overlay, close};
}

const slider = (name, label, max, value) => `
  <label class="field life-slider"><span>${label} <output>${value}</output></span>
  <input type="range" name="${name}" min="1" max="${max}" value="${value}"></label>`;
const timeField = (name, label, value) => `
  <label class="field"><span>${label}</span><input type="datetime-local" name="${name}" value="${value}"></label>`;
const nowInput = () => localInputValue(new Date());

function openModal(kind, record) {
  const builders = {
    "check-in": checkInModal, "meal": mealModal, "sleep": sleepModal,
    "exercise": exerciseModal, "record": recordModal
  };
  return builders[kind](record);
}

function checkInModal(record) {
  const value = key => record ? record[key] : 5;
  const {overlay, close} = modalShell(record ? "更新状态" : "记一次状态", `
    ${slider("energy", "能量", 10, value("energy"))}
    ${slider("mood", "心情", 10, value("mood"))}
    ${slider("focusDesire", "专注意愿", 10, value("focusDesire"))}
    ${slider("fatigue", "疲劳", 10, value("fatigue"))}
    ${timeField("time", "什么时候", record ? localInputValue(new Date(record.time)) : nowInput())}
    <label class="field"><span>备注(可选)</span><input name="note" maxlength="120" value="${escapeHtml(record?.note || "")}"></label>`);
  overlay.querySelectorAll("input[type=range]").forEach(input => input.addEventListener("input",
    () => { input.closest("label").querySelector("output").textContent = input.value; }));
  overlay.querySelector("form").addEventListener("submit", async event => {
    event.preventDefault();
    const form = event.target;
    try {
      const body = {energy: +form.energy.value, mood: +form.mood.value, focusDesire: +form.focusDesire.value,
        fatigue: +form.fatigue.value, time: fromInput(form.time.value), note: form.note.value.trim()};
      if (record) await api.life.checkIns.update(record.id, body);
      else await api.life.checkIns.create(body);
      close(); toast(record ? "状态已更新" : "状态已记下");
      life(document.querySelector("#page"));
    } catch (reason) { toast(reason.message, true); }
  });
}

function mealModal(record) {
  const defaultType = record ? record.mealType : (new Date().getHours() < 10 ? "BREAKFAST"
    : new Date().getHours() < 14 ? "LUNCH" : new Date().getHours() < 20 ? "DINNER" : "SNACK");
  const {overlay, close} = modalShell(record ? "更新这一餐" : "记一餐", `
    <div class="milestone-form-grid">
      <label class="field"><span>哪一餐</span><select name="mealType">${Object.entries(MEAL_LABELS)
        .map(([value, label]) => `<option value="${value}" ${value === defaultType ? "selected" : ""}>${label}</option>`).join("")}</select></label>
      ${timeField("time", "什么时候", record ? localInputValue(new Date(record.time)) : nowInput())}
    </div>
    <label class="field"><span>吃了什么</span><input name="description" maxlength="160"
      placeholder="一句话就够" value="${escapeHtml(record?.description || "")}"></label>
    ${slider("satisfaction", "满意度", 5, record?.satisfaction || 3)}
    <label class="field"><span>照片(可选)</span><input type="file" name="images" accept="image/*" multiple></label>
    <label class="field"><span>备注(可选)</span><input name="note" maxlength="120" value="${escapeHtml(record?.note || "")}"></label>`);
  overlay.querySelector("input[type=range]").addEventListener("input",
    event => { event.target.closest("label").querySelector("output").textContent = event.target.value; });
  overlay.querySelector("form").addEventListener("submit", async event => {
    event.preventDefault();
    const form = event.target;
    try {
      const images = [];
      for (const file of form.images.files) {
        const data = new FormData();
        data.append("file", file);
        images.push((await api.uploadImage(data)).path);
      }
      const body = {mealType: form.mealType.value, time: fromInput(form.time.value),
        description: form.description.value.trim(), satisfaction: +form.satisfaction.value,
        note: form.note.value.trim(),
        images: record && !images.length ? record.images : [...(record?.images || []), ...images]};
      if (record) await api.life.meals.update(record.id, body);
      else await api.life.meals.create(body);
      close(); toast(record ? "这一餐已更新" : "这一餐已记下");
      life(document.querySelector("#page"));
    } catch (reason) { toast(reason.message, true); }
  });
}

function sleepModal(record) {
  const {overlay, close} = modalShell(record ? "更新睡眠" : "记睡眠", `
    <div class="milestone-form-grid">
      ${timeField("sleepTime", "入睡", record ? localInputValue(new Date(record.sleepTime))
        : localInputValue(new Date(Date.now() - 8 * 3600 * 1000)))}
      ${timeField("wakeTime", "起床", record ? localInputValue(new Date(record.wakeTime)) : nowInput())}
    </div>
    <div class="milestone-form-grid">
      <label class="field"><span>类型</span><select name="type">${Object.entries(SLEEP_LABELS)
        .map(([value, label]) => `<option value="${value}" ${(record?.type || "NIGHT") === value ? "selected" : ""}>${label}</option>`).join("")}</select></label>
      <label class="field"><span>时长</span><input name="duration" disabled placeholder="自动计算"></label>
    </div>
    ${slider("quality", "质量", 5, record?.quality || 3)}
    <label class="field"><span>备注(可选)</span><input name="note" maxlength="120" value="${escapeHtml(record?.note || "")}"></label>`);
  const form = overlay.querySelector("form");
  const durationHint = () => {
    const ms = new Date(form.wakeTime.value) - new Date(form.sleepTime.value);
    form.duration.value = Number.isFinite(ms) && ms > 0
      ? `${Math.floor(ms / 3600000)} 小时 ${Math.round(ms % 3600000 / 60000)} 分` : "时间不太对";
  };
  form.sleepTime.addEventListener("input", durationHint);
  form.wakeTime.addEventListener("input", durationHint);
  durationHint();
  overlay.querySelector("input[type=range]").addEventListener("input",
    event => { event.target.closest("label").querySelector("output").textContent = event.target.value; });
  overlay.querySelector("form").addEventListener("submit", async event => {
    event.preventDefault();
    const target = event.target;
    try {
      const body = {sleepTime: fromInput(target.sleepTime.value), wakeTime: fromInput(target.wakeTime.value),
        quality: +target.quality.value, type: target.type.value, note: target.note.value.trim()};
      if (record) await api.life.sleep.update(record.id, body);
      else await api.life.sleep.create(body);
      close(); toast(record ? "睡眠已更新" : "睡眠已记下");
      life(document.querySelector("#page"));
    } catch (reason) { toast(reason.message, true); }
  });
}

function exerciseModal(record) {
  const {overlay, close} = modalShell(record ? "更新运动" : "记运动", `
    <div class="milestone-form-grid">
      <label class="field"><span>项目</span><select name="type">${Object.entries(EXERCISE_LABELS)
        .map(([value, label]) => `<option value="${value}" ${(record?.type || "OTHER") === value ? "selected" : ""}>${label}</option>`).join("")}</select></label>
      <label class="field"><span>时长(分钟)</span><input name="durationMinutes" type="number" min="1" max="1440"
        value="${record?.durationMinutes || 30}"></label>
    </div>
    <div class="milestone-form-grid">
      <label class="field"><span>强度</span><select name="intensity">${Object.entries(INTENSITY_LABELS)
        .map(([value, label]) => `<option value="${value}" ${(record?.intensity || "MEDIUM") === value ? "selected" : ""}>${label}</option>`).join("")}</select></label>
      ${timeField("startTime", "开始时间", record ? localInputValue(new Date(record.startTime)) : nowInput())}
    </div>
    <label class="field"><span>备注(可选)</span><input name="note" maxlength="120" value="${escapeHtml(record?.note || "")}"></label>`);
  overlay.querySelector("form").addEventListener("submit", async event => {
    event.preventDefault();
    const form = event.target;
    try {
      const body = {type: form.type.value, durationMinutes: +form.durationMinutes.value,
        intensity: form.intensity.value, startTime: fromInput(form.startTime.value), note: form.note.value.trim()};
      if (record) await api.life.exercises.update(record.id, body);
      else await api.life.exercises.create(body);
      close(); toast(record ? "运动已更新" : "运动已记下");
      life(document.querySelector("#page"));
    } catch (reason) { toast(reason.message, true); }
  });
}

function recordModal(record) {
  const {overlay, close} = modalShell(record ? "更新记录" : "其他记录", `
    <div class="milestone-form-grid">
      <label class="field"><span>类别</span><select name="type">${Object.entries(RECORD_LABELS)
        .map(([value, label]) => `<option value="${value}" ${(record?.type || "WATER") === value ? "selected" : ""}>${label}</option>`).join("")}</select></label>
      <label class="field"><span>数值</span><input name="value" type="number" min="0" step="any" value="${record?.value ?? 250}"></label>
    </div>
    <div class="milestone-form-grid">
      <label class="field"><span>单位</span><input name="unit" maxlength="10" value="${escapeHtml(record?.unit || RECORD_UNITS[record?.type || "WATER"])}"></label>
      ${timeField("time", "什么时候", record ? localInputValue(new Date(record.time)) : nowInput())}
    </div>
    <label class="field" data-custom-label hidden><span>自定义名称</span><input name="label" maxlength="20"
      placeholder="比如:遛狗" value="${escapeHtml(record?.metadata?.label || "")}"></label>
    <label class="field"><span>备注(可选)</span><input name="note" maxlength="120" value="${escapeHtml(record?.note || "")}"></label>`);
  const form = overlay.querySelector("form");
  const syncLabel = () => { form.querySelector("[data-custom-label]").hidden = form.type.value !== "CUSTOM"; };
  form.type.addEventListener("change", () => {
    form.unit.value = RECORD_UNITS[form.type.value];
    syncLabel();
  });
  syncLabel();
  form.addEventListener("submit", async event => {
    event.preventDefault();
    try {
      const metadata = form.type.value === "CUSTOM" ? {label: form.label.value.trim()} : null;
      const body = {type: form.type.value, value: +form.value.value, unit: form.unit.value.trim(),
        time: fromInput(form.time.value), note: form.note.value.trim(), metadata};
      if (record) await api.life.records.update(record.id, body);
      else await api.life.records.create(body);
      close(); toast(record ? "记录已更新" : "已记下");
      life(document.querySelector("#page"));
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
