import { api } from "../api/client.js?v=0.5.2";
import { nowCopy as c } from "../content/copy.js?v=0.5.2";
import { renderBoard } from "./now-playlist.js?v=0.5.2";
import { empty, error, escapeHtml, toast } from "../components/ui.js";

const date = value => value ? new Intl.DateTimeFormat("zh-CN", {year:"numeric",month:"short",day:"numeric"}).format(new Date(value)) : "";
const minutes = seconds => {
  const total = Math.max(0, Math.floor(seconds || 0));
  return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, "0")}`;
};
const listKeys = ["currentGames", "currentAnime", "currentBooks"];

let current = null;
let dreamsList = [];
let goalsList = [];
let pendingImages = []; // {preview, file}
let editing = {};       // which section is in edit mode
let picker = null;
let viewingSnapshot = null;

export async function now(root) {
  root.innerHTML = '<section class="panel">正在读取「现在。」…</section>';
  try {
    [current, dreamsList] = await Promise.all([api.now.current(), api.dreams.all()]);
    const details = await Promise.all(dreamsList.map(d => api.dreams.detail(d.id)));
    goalsList = details.flatMap(d => d.goals.map(g => ({...g.goal, dreamTitle: d.dream.title})));
    if (viewingSnapshot) return renderSnapshot(root, viewingSnapshot);
    renderStage(root);
  } catch (reason) { root.innerHTML = error(reason.message); }
}

/* ---------- stage display page ---------- */

function renderStage(root) {
  const itemEditor = (key, index, item) => `
    <form class="now-item-editor" data-item-editor="${key}|${index}">
      <input name="title" placeholder="${c.itemTitle}" maxlength="60" value="${escapeHtml(item.title)}" required>
      <input name="subtitle" placeholder="${c.itemSubtitle}" maxlength="80" value="${escapeHtml(item.subtitle||"")}">
      <input name="note" placeholder="${c.itemNote}" maxlength="120" value="${escapeHtml(item.note||"")}">
      <div class="row-actions">
        <button class="button button-secondary" type="submit">${c.itemSave}</button>
        <button class="text-link" type="button" data-cancel-item>${c.itemCancel}</button>
        ${index >= 0?`<button class="text-link danger-link" type="button" data-remove-item="${index}">删除</button>`:""}
      </div></form>`;
  const itemCards = key => {
    const items = current[key];
    const editIndex = editing.lists?.[key];
    const body = items.map((item, index) => editIndex === index ? itemEditor(key, index, item) : `
      <article class="now-item-card" data-edit-item="${key}|${index}">
        <strong>${escapeHtml(item.title)}</strong>
        ${item.subtitle?`<small>${escapeHtml(item.subtitle)}</small>`:""}
        ${item.note?`<small class="muted">${escapeHtml(item.note)}</small>`:""}
      </article>`).join("");
    return `${body}${editIndex === "new" ? itemEditor(key, -1, {title:"",subtitle:"",note:""}) : ""}
      <button class="text-link" data-add-item="${key}">${c.addItemLabels[key]}</button>`;
  };

  const dreamCards = current.currentDreamIds
    .map(id => dreamsList.find(d => d.id === id)).filter(Boolean)
    .map(d => `<article class="card dream-chip"><strong>${escapeHtml(d.title)}</strong><small>${escapeHtml(d.meaning || d.status)}</small></article>`).join("");
  const goalChips = current.currentGoalIds
    .map(id => goalsList.find(g => g.id === id)).filter(Boolean)
    .map(g => `<span class="badge goal-chip">${escapeHtml(g.title)}<small> · ${escapeHtml(g.dreamTitle || "")}</small></span>`).join("");

  root.innerHTML = `<div class="direction-page now-page">
    <section class="panel now-hero">
      ${editing.stage ? `
        <div class="now-hero-edit">
          <input id="edit-stage-title" maxlength="60" value="${escapeHtml(current.stageTitle)}" placeholder="${c.stageTitlePlaceholder}">
          <input id="edit-stage-theme" maxlength="120" value="${escapeHtml(current.theme)}" placeholder="${c.themePlaceholder}">
          <div class="row-actions">
            <button class="button button-primary" id="save-stage">${c.save}</button>
            <button class="text-link" id="cancel-stage">${c.itemCancel}</button>
          </div></div>`
      : `<div class="now-hero-display">
          <div class="now-hero-text"><h1>${escapeHtml(current.stageTitle || "「现在。」")}</h1>
          <p>${escapeHtml(current.theme || c.themePlaceholder)}</p></div>
          <div class="row-actions"><button class="text-link" id="edit-stage">${c.editStage}</button>
          <button class="button button-secondary" id="snapshot-now">${c.snapshotButton}</button></div>
        </div>`}
    </section>

    <section class="panel board-panel"><div id="playlist-board"></div></section>
    <section class="now-three-grid">
      ${listKeys.map(key => `<section class="panel"><div class="section-head"><h2>${c.lists[key]}</h2></div>
        <div class="now-item-cards">${itemCards(key)}</div></section>`).join("")}
    </section>

    <section class="panel"><div class="section-head"><h2>${c.currentDreams} / ${c.currentGoals}</h2>
      ${picker?`<button class="text-link" id="close-picker">${c.done}</button>`
        :`<button class="text-link" id="open-picker">${c.chooseDreams}</button>`}</div>
      ${picker?`<div class="now-checks" id="direction-picker">
          <h3>${c.currentDreams}</h3>
          ${dreamsList.filter(d=>d.status==="ACTIVE").map(d=>`<label class="check-field"><input type="checkbox" data-pick-dream="${d.id}" ${current.currentDreamIds.includes(d.id)?"checked":""}> ${escapeHtml(d.title)}</label>`).join("")||empty(c.dreamsEmpty)}
          <h3>${c.currentGoals}</h3>
          ${goalsList.map(g=>`<label class="check-field"><input type="checkbox" data-pick-goal="${g.id}" ${current.currentGoalIds.includes(g.id)?"checked":""}> ${escapeHtml(g.title)}<small> · ${escapeHtml(g.dreamTitle||"")}</small></label>`).join("")||empty(c.dreamsEmpty)}
        </div>`
      : (dreamCards || goalChips
        ? `<div class="dream-grid">${dreamCards}</div><div class="row-actions">${goalChips}</div>`
        : empty(c.dreamsEmpty))}
    </section>

    <section class="panel"><div class="section-head"><h2>${c.quoteLabel}</h2>
      <button class="text-link" data-edit-text="quote">${c.edit}</button></div>
      ${editing.quote?`<div><textarea id="edit-quote" rows="2" maxlength="200" placeholder="${c.quotePlaceholder}">${escapeHtml(current.favoriteQuote)}</textarea>
        <div class="row-actions"><button class="button button-primary" id="save-quote">${c.save}</button></div></div>`
      : (current.favoriteQuote?`<p class="snapshot-quote big">“${escapeHtml(current.favoriteQuote)}”</p>`:empty(c.quotePlaceholder))}
    </section>

    <section class="panel"><div class="section-head"><h2>${c.contentLabel}</h2>
      <button class="text-link" data-edit-text="content">${c.edit}</button></div>
      ${editing.content?`<div><textarea id="edit-content" rows="5" placeholder="${c.contentPlaceholder}">${escapeHtml(current.content)}</textarea>
        <div class="row-actions"><button class="button button-primary" id="save-content">${c.save}</button></div></div>`
      : (current.content?`<p class="snapshot-content">${escapeHtml(current.content)}</p>`:empty(c.contentPlaceholder))}
    </section>

    <section class="panel"><div class="section-head"><h2>${c.images}</h2></div>
      <div class="now-gallery">
        ${current.images.map((img, index) => `<div class="gallery-item"><img src="${escapeHtml(img)}" alt="">
          <button class="gallery-remove" data-remove-image="${index}" title="${c.removeImage}" aria-label="${c.removeImage}">×</button></div>`).join("")}
        ${pendingImages.map((p, index) => `<div class="gallery-item pending"><img src="${p.preview}" alt="">
          <button class="gallery-remove" data-remove-pending="${index}" title="${c.removeImage}" aria-label="${c.removeImage}">×</button></div>`).join("")}
        <label class="gallery-item gallery-add">${c.uploadImage}<input type="file" id="now-image-input" accept="image/*" multiple hidden></label>
      </div>
      ${pendingImages.length?`<div class="row-actions"><button class="button button-primary" id="save-now">${c.save}</button></div>`:""}
    </section>

    <section class="panel"><div class="section-head"><h2>${c.snapshotsTitle}</h2></div>
      <div id="snapshot-list"></div>
    </section>
  </div>`;
  bind(root);
  renderPlaylist(root);
  renderSnapshots(root);
}

function renderPlaylist(root) {
  const el = root.querySelector("#playlist-board");
  if (!el) return;
  renderBoard(el, {
    state: current,
    editable: true,
    onSongsChange: (songs, meta) => { current = {...current, favoriteSongs: songs, ...meta}; renderStage(root); },
    onBackgroundChange: state => { current = state; renderStage(root); }
  });
}

function collectState() {
  return {
    stageTitle: current.stageTitle, theme: current.theme,
    favoriteSongs: current.favoriteSongs,
    currentGames: current.currentGames, currentAnime: current.currentAnime, currentBooks: current.currentBooks,
    currentDreamIds: current.currentDreamIds, currentGoalIds: current.currentGoalIds,
    favoriteQuote: current.favoriteQuote, images: current.images, content: current.content
  };
}

async function saveText(root, patch) {
  try {
    current = await api.now.update({...collectState(), ...patch});
    editing = {};
    renderStage(root);
    toast(c.saved);
  } catch (reason) { toast(reason.message, true); }
}

function bind(root) {
  /* stage header */
  root.querySelector("#edit-stage")?.addEventListener("click", () => { editing.stage = true; renderStage(root); });
  root.querySelector("#cancel-stage")?.addEventListener("click", () => { editing = {}; renderStage(root); });
  root.querySelector("#save-stage")?.addEventListener("click", () => saveText(root, {
    stageTitle: root.querySelector("#edit-stage-title").value.trim(),
    theme: root.querySelector("#edit-stage-theme").value.trim()
  }));

  /* games / anime / books cards */
  root.querySelectorAll("[data-add-item]").forEach(button => button.addEventListener("click", () => {
    editing.lists = {[button.dataset.addItem]: "new"};
    renderStage(root);
  }));
  root.querySelectorAll("[data-edit-item]").forEach(card => card.addEventListener("click", () => {
    const [key, index] = card.dataset.editItem.split("|");
    editing.lists = {[key]: Number(index)};
    renderStage(root);
  }));
  root.querySelectorAll("[data-item-editor]").forEach(form => form.addEventListener("submit", async event => {
    event.preventDefault();
    const [key, rawIndex] = form.dataset.itemEditor.split("|");
    const item = {title: form.title.value.trim(), subtitle: form.subtitle.value.trim(), note: form.note.value.trim()};
    if (!item.title) return;
    const items = [...current[key]];
    if (Number(rawIndex) >= 0) items[Number(rawIndex)] = item; else items.push(item);
    editing = {};
    await saveText(root, {[key]: items});
  }));
  root.querySelectorAll("[data-cancel-item]").forEach(button => button.addEventListener("click", () => { editing = {}; renderStage(root); }));
  root.querySelectorAll("[data-remove-item]").forEach(button => button.addEventListener("click", async event => {
    event.stopPropagation();
    const key = button.closest("[data-item-editor]").dataset.itemEditor.split("|")[0];
    const items = current[key].filter((_, i) => i !== Number(button.dataset.removeItem));
    editing = {};
    await saveText(root, {[key]: items});
  }));

  /* dreams / goals picker */
  root.querySelector("#open-picker")?.addEventListener("click", () => { picker = "dreams"; renderStage(root); });
  root.querySelector("#close-picker")?.addEventListener("click", () => { picker = null; renderStage(root); });
  root.querySelectorAll("[data-pick-dream]").forEach(input => input.addEventListener("change", async () => {
    const id = input.dataset.pickDream;
    const ids = input.checked ? [...current.currentDreamIds, id] : current.currentDreamIds.filter(v => v !== id);
    picker = null;
    await saveText(root, {currentDreamIds: ids});
  }));
  root.querySelectorAll("[data-pick-goal]").forEach(input => input.addEventListener("change", async () => {
    const id = input.dataset.pickGoal;
    const ids = input.checked ? [...current.currentGoalIds, id] : current.currentGoalIds.filter(v => v !== id);
    picker = null;
    await saveText(root, {currentGoalIds: ids});
  }));

  /* quote / content */
  root.querySelectorAll("[data-edit-text]").forEach(button => button.addEventListener("click", () => {
    editing[button.dataset.editText] = true; renderStage(root);
  }));
  root.querySelector("#save-quote")?.addEventListener("click", () => saveText(root, {favoriteQuote: root.querySelector("#edit-quote").value.trim()}));
  root.querySelector("#save-content")?.addEventListener("click", () => saveText(root, {content: root.querySelector("#edit-content").value}));

  /* images: local preview now, upload on save */
  root.querySelector("#now-image-input")?.addEventListener("change", event => {
    for (const file of event.target.files) {
      pendingImages.push({preview: URL.createObjectURL(file), file});
    }
    renderStage(root);
  });
  root.querySelectorAll("[data-remove-image]").forEach(button => button.addEventListener("click", async () => {
    const images = current.images.filter((_, i) => i !== Number(button.dataset.removeImage));
    await saveText(root, {images});
  }));
  root.querySelectorAll("[data-remove-pending]").forEach(button => button.addEventListener("click", () => {
    pendingImages.splice(Number(button.dataset.removePending), 1);
    renderStage(root);
  }));
  root.querySelector("#save-now")?.addEventListener("click", async () => {
    const paths = [];
    for (const pending of pendingImages) {
      const data = new FormData();
      data.append("file", pending.file);
      paths.push((await api.uploadImage(data)).path);
    }
    pendingImages = [];
    await saveText(root, {images: [...current.images, ...paths]});
  });

  /* snapshot: flush pending images first, then freeze */
  root.querySelector("#snapshot-now")?.addEventListener("click", async () => {
    try {
      const paths = [];
      for (const pending of pendingImages) {
        const data = new FormData();
        data.append("file", pending.file);
        paths.push((await api.uploadImage(data)).path);
      }
      pendingImages = [];
      if (paths.length) current = await api.now.update({...collectState(), images: [...current.images, ...paths]});
      await api.now.createSnapshot();
      toast(c.snapshotDone);
    } catch (reason) { toast(reason.message, true); }
  });
}

/* ---------- snapshots ---------- */

function renderSnapshots(root) {
  api.now.snapshots().then(snapshots => {
    const list = root.querySelector("#snapshot-list");
    if (!list) return;
    list.innerHTML = snapshots.map(s => `
      <article class="card snapshot-card" data-view="${s.id}">
        <div class="snapshot-head"><strong>${escapeHtml(s.stageTitle || "现在。")}</strong>
          <small>${date(s.createdAt)} · ${escapeHtml(s.theme || "")}</small></div>
        ${s.favoriteQuote?`<p class="snapshot-quote">“${escapeHtml(s.favoriteQuote)}”</p>`:""}
        <div class="row-actions snapshot-meta">
          ${s.favoriteSongs.length?`<span class="badge">${c.songsLabel(s.favoriteSongs.length)}</span>`:""}
          ${s.images.slice(0,3).map(img=>`<img class="snapshot-thumb" src="${escapeHtml(img)}" alt="">`).join("")}
          ${s.currentDreams.map(d=>`<span class="badge goal-chip">${escapeHtml(d.snapshotTitle)}</span>`).join("")}
        </div>
        <div class="row-actions"><button class="text-link" data-view="${s.id}">${c.snapshotView}</button>
        <button class="text-link danger-link" data-del="${s.id}">${c.snapshotDelete}</button></div>
      </article>`).join("")
      || empty(c.snapshotsEmpty);
    list.querySelectorAll("[data-view]").forEach(button => button.addEventListener("click", async () => {
      viewingSnapshot = await api.now.snapshot(button.dataset.view);
      renderSnapshot(root, viewingSnapshot);
    }));
    list.querySelectorAll("[data-del]").forEach(button => button.addEventListener("click", async () => {
      if (!confirm(c.snapshotDeleteConfirm)) return;
      await api.now.removeSnapshot(button.dataset.del);
      now(root);
    }));
  }).catch(reason => toast(reason.message, true));
}

function renderSnapshot(root, snapshot) {

  const itemList = items => items.map(item => `<li>${escapeHtml(item.title)}${item.subtitle?`<small> · ${escapeHtml(item.subtitle)}</small>`:""}</li>`).join("");
  root.innerHTML = `<div class="direction-page"><section class="panel"><div class="section-head"><div><div class="eyebrow">${c.readonlyTitle}</div>
    <h1>${escapeHtml(snapshot.stageTitle || "现在。")}</h1><p>${escapeHtml(snapshot.theme || "")}</p></div>
    <button class="text-link" id="back-now">${c.backToCurrent}</button></div>
    <small>${date(snapshot.createdAt)}</small>
    ${snapshot.favoriteSongs.length?`<div id="snapshot-playlist-board"></div>`:""}
    ${snapshot.favoriteQuote?`<p class="snapshot-quote big">“${escapeHtml(snapshot.favoriteQuote)}”</p>`:""}
    <div class="snapshot-grid">
      ${[["currentGames",snapshot.currentGames],["currentAnime",snapshot.currentAnime],["currentBooks",snapshot.currentBooks]]
        .map(([key, items])=>items.length?`<div><h3>${c.lists[key]}</h3><ul>${itemList(items)}</ul></div>`:"").join("")}
      ${snapshot.currentDreams.length?`<div><h3>${c.currentDreams}</h3><ul>${snapshot.currentDreams.map(d=>`<li>${escapeHtml(d.snapshotTitle)}</li>`).join("")}</ul></div>`:""}
      ${snapshot.currentGoals.length?`<div><h3>${c.currentGoals}</h3><ul>${snapshot.currentGoals.map(g=>`<li>${escapeHtml(g.snapshotTitle)}</li>`).join("")}</ul></div>`:""}
    </div>
    ${snapshot.content?`<p class="snapshot-content">${escapeHtml(snapshot.content)}</p>`:""}
    ${snapshot.images.length?`<div class="now-gallery">${snapshot.images.map(img=>`<div class="gallery-item"><img src="${escapeHtml(img)}" alt=""></div>`).join("")}</div>`:""}
  </section></div>`;
  const boardEl = root.querySelector("#snapshot-playlist-board");
  if (boardEl) renderBoard(boardEl, {
    state: snapshot, editable: false,
    onSongsChange: () => {}, onBackgroundChange: () => {}
  });
  root.querySelector("#back-now").addEventListener("click", () => { viewingSnapshot = null; now(root); });
}
