import { api } from "../api/client.js?v=0.5.0";
import { nowCopy as c } from "../content/copy.js?v=0.5.0";
import { empty, error, escapeHtml, toast } from "../components/ui.js";

const date = value => value ? new Intl.DateTimeFormat("zh-CN", {year:"numeric",month:"short",day:"numeric"}).format(new Date(value)) : "";
let viewingSnapshot = null;
let current = null;
let dreamsList = [];
let goalsList = [];

const listKeys = Object.keys(c.lists);

export async function now(root) {
  root.innerHTML = '<section class="panel">正在读取「现在。」…</section>';
  try {
    [current, dreamsList] = await Promise.all([api.now.current(), api.dreams.all()]);
    const details = await Promise.all(dreamsList.map(d => api.dreams.detail(d.id)));
    goalsList = details.flatMap(d => d.goals.map(g => ({...g.goal, dreamTitle: d.dream.title})));
    if (viewingSnapshot) return renderSnapshot(root, viewingSnapshot);
    renderCurrent(root);
  } catch (reason) { root.innerHTML = error(reason.message); }
}

function renderCurrent(root) {
  const itemList = key => `
    <div class="now-list" data-list="${key}">
      ${current[key].map((item, index) => itemRow(key, item, index)).join("")}
      <button class="text-link" data-add="${key}">${c.addItem}</button>
    </div>`;
  const checkboxList = (key, items) => `
    <div class="now-checks" data-checks="${key}">
      ${items.map(item => `<label class="check-field"><input type="checkbox" data-${key}="${item.id}" ${current[key].includes(item.id)?"checked":""}> ${escapeHtml(item.title)}${item.dreamTitle?`<small> · ${escapeHtml(item.dreamTitle)}</small>`:""}</label>`).join("")||empty("还没有可关联的条目。")}
    </div>`;
  root.innerHTML = `<div class="direction-page"><section class="panel"><div class="section-head"><div><div class="eyebrow">v0.5 · Now</div><h2>此刻的我</h2></div>
    <button class="button button-primary" id="save-now">${c.save}</button></div>
    <form id="now-form">
      <div class="milestone-form-grid">
        <label class="field"><span>${c.stageTitleLabel}</span><input id="now-stage" maxlength="60" value="${escapeHtml(current.stageTitle)}" placeholder="${c.stageTitlePlaceholder}"></label>
        <label class="field"><span>${c.themeLabel}</span><input id="now-theme" maxlength="120" value="${escapeHtml(current.theme)}" placeholder="${c.themePlaceholder}"></label>
      </div>
      ${listKeys.map(key => `<div class="now-section"><h3>${c.lists[key]}</h3>${itemList(key)}</div>`).join("")}
      <div class="now-section"><h3>${c.currentDreams}</h3>${checkboxList("currentDreamIds", dreamsList.filter(d=>d.status==="ACTIVE"))}</div>
      <div class="now-section"><h3>${c.currentGoals}</h3>${checkboxList("currentGoalIds", goalsList)}</div>
      <label class="field"><span>${c.quoteLabel}</span><input id="now-quote" maxlength="200" value="${escapeHtml(current.favoriteQuote)}"></label>
      <label class="field"><span>${c.contentLabel}</span><textarea id="now-content" rows="5">${escapeHtml(current.content)}</textarea></label>
      <label class="field"><span>${c.images}</span><input id="now-images" type="file" accept="image/*" multiple></label>
      <small>${current.images.map(img=>escapeHtml(img)).join(" · ")}</small>
      <div><button class="button button-primary" type="submit">${c.save}</button>
      <button class="button button-secondary" type="button" id="snapshot-now">${c.snapshotButton}</button></div>
    </form>
  </section>
  <section class="panel"><div class="section-head"><h2>${c.snapshotsTitle}</h2></div>
    <div id="snapshot-list"></div>
  </section></div>`;
  renderSnapshots(root);

  const collectList = key => [...root.querySelectorAll(`[data-list="${key}"] .now-item`)].map(row => ({
    title: row.querySelector(".item-title").value.trim(),
    subtitle: row.querySelector(".item-subtitle").value.trim(),
    note: row.querySelector(".item-note").value.trim()
  })).filter(item => item.title);
  const collectChecks = key => [...root.querySelectorAll(`[data-checks="${key}"] input:checked`)].map(input => input.dataset[key === "currentDreamIds" ? "currentdreamids" : "currentgoalids"]);

  root.querySelectorAll("[data-add]").forEach(button => button.addEventListener("click", () => {
    const key = button.dataset.add;
    button.insertAdjacentHTML("beforebegin", itemRow(key, {title:"",subtitle:"",note:""}, -1));
  }));
  root.querySelector("#now-form").addEventListener("click", event => {
    const remove = event.target.closest(".item-remove");
    if (remove) remove.closest(".now-item").remove();
  });
  root.querySelector("#now-form").addEventListener("submit", async event => {
    event.preventDefault();
    try {
      const uploads = await uploadImages(root);
      current = await api.now.update({
        stageTitle: root.querySelector("#now-stage").value.trim(),
        theme: root.querySelector("#now-theme").value.trim(),
        favoriteSongs: collectList("favoriteSongs"), currentGames: collectList("currentGames"),
        currentAnime: collectList("currentAnime"), currentBooks: collectList("currentBooks"),
        currentDreamIds: collectChecks("currentDreamIds"), currentGoalIds: collectChecks("currentGoalIds"),
        favoriteQuote: root.querySelector("#now-quote").value.trim(),
        images: [...current.images, ...uploads],
        content: root.querySelector("#now-content").value
      });
      toast(c.saved);
    } catch (reason) { toast(reason.message, true); }
  });
  const collectForm = () => ({
    stageTitle: root.querySelector("#now-stage").value.trim(),
    theme: root.querySelector("#now-theme").value.trim(),
    favoriteSongs: collectList("favoriteSongs"), currentGames: collectList("currentGames"),
    currentAnime: collectList("currentAnime"), currentBooks: collectList("currentBooks"),
    currentDreamIds: collectChecks("currentDreamIds"), currentGoalIds: collectChecks("currentGoalIds"),
    favoriteQuote: root.querySelector("#now-quote").value.trim(),
    images: current.images, content: root.querySelector("#now-content").value
  });
  root.querySelector("#snapshot-now").addEventListener("click", async () => {
    try {
      const uploads = await uploadImages(root);
      const state = collectForm();
      if (uploads.length) state.images = [...state.images, ...uploads];
      await api.now.update(state);
      await api.now.createSnapshot();
      toast(c.snapshotDone);
      now(root);
    } catch (reason) { toast(reason.message, true); }
  });
  async function uploadImages(root){
    const files = [...root.querySelector("#now-images").files];
    const paths = [];
    for (const file of files) {
      const data = new FormData();
      data.append("file", file);
      paths.push((await api.uploadImage(data)).path);
    }
    return paths;
  }
}

function itemRow(key, item) {
  return `<div class="now-item">
    <input class="item-title" placeholder="${c.itemTitle}" maxlength="60" value="${escapeHtml(item.title||"")}">
    <input class="item-subtitle" placeholder="${c.itemSubtitle}" maxlength="80" value="${escapeHtml(item.subtitle||"")}">
    <input class="item-note" placeholder="${c.itemNote}" maxlength="120" value="${escapeHtml(item.note||"")}">
    <button class="text-link danger-link item-remove" type="button">✕</button></div>`;
}

function renderSnapshots(root) {
  api.now.snapshots().then(snapshots => {
    const list = root.querySelector("#snapshot-list");
    if (!list) return;
    list.innerHTML = snapshots.map(s => `
      <div class="snapshot-card"><div class="snapshot-head"><strong>${escapeHtml(s.stageTitle || "现在。")}</strong>
      <small>${date(s.createdAt)} · ${escapeHtml(s.theme || "")}</small></div>
      ${s.favoriteQuote?`<p class="snapshot-quote">${escapeHtml(s.favoriteQuote)}</p>`:""}
      <div class="row-actions"><button class="text-link" data-view="${s.id}">${c.snapshotView}</button>
      <button class="text-link danger-link" data-del="${s.id}">${c.snapshotDelete}</button></div></div>`).join("")
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
  root.innerHTML = `<div class="direction-page"><section class="panel"><div class="section-head"><div><div class="eyebrow">${c.readonlyTitle}</div><h2>${escapeHtml(snapshot.stageTitle || "现在。")}</h2></div>
    <button class="text-link" id="back-now">${c.backToCurrent}</button></div>
    <small>${date(snapshot.createdAt)} · ${escapeHtml(snapshot.theme || "")}</small>
    ${snapshot.favoriteQuote?`<p class="snapshot-quote big">“${escapeHtml(snapshot.favoriteQuote)}”</p>`:""}
    <div class="snapshot-grid">
      ${[["favoriteSongs",snapshot.favoriteSongs],["currentGames",snapshot.currentGames],["currentAnime",snapshot.currentAnime],["currentBooks",snapshot.currentBooks]]
        .map(([key, items])=>items.length?`<div><h3>${c.lists[key]}</h3><ul>${itemList(items)}</ul></div>`:"").join("")}
      ${snapshot.currentDreams.length?`<div><h3>${c.currentDreams}</h3><ul>${snapshot.currentDreams.map(d=>`<li>${escapeHtml(d.snapshotTitle)}</li>`).join("")}</ul></div>`:""}
      ${snapshot.currentGoals.length?`<div><h3>${c.currentGoals}</h3><ul>${snapshot.currentGoals.map(g=>`<li>${escapeHtml(g.snapshotTitle)}</li>`).join("")}</ul></div>`:""}
    </div>
    ${snapshot.content?`<p class="snapshot-content">${escapeHtml(snapshot.content)}</p>`:""}
    ${snapshot.images.map(img=>`<img class="dream-cover" src="${escapeHtml(img)}" alt="">`).join("")}
  </section></div>`;
  root.querySelector("#back-now").addEventListener("click", () => { viewingSnapshot = null; now(root); });
}
