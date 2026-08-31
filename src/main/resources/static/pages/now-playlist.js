import { api } from "../api/client.js?v=1.0.0";
import { nowCopy as c } from "../content/copy.js?v=1.0.0";
import { confirmDialog, empty, escapeHtml, toast } from "../components/ui.js?v=1.0.0";

/**
 * PlaylistBoard — the horizontal immersive wall for the 「现在。」歌单.
 * Songs are hung on a background stage at persisted normalized positions;
 * playCount drives the size level. Layouts feel loose and spontaneous, while
 * remaining deterministic after they have been persisted.
 */

const minutes = seconds => {
  const total = Math.max(0, Math.floor(seconds || 0));
  return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, "0")}`;
};

/** playCount → visual size level; boundaries are product tuning knobs. */
export function sizeLevel(playCount) {
  if (playCount >= 1000) return "featured";
  if (playCount >= 300) return "xl";
  if (playCount >= 100) return "large";
  if (playCount >= 20) return "medium";
  return "small";
}
const LEVEL_RANK = {small: 0, medium: 1, large: 2, xl: 3, featured: 4};
/** Approximate card box per level (px at desktop), used for wall clamping. */
const LEVEL_BOX = {
  small:    {w: 200, h: 118},
  medium:   {w: 235, h: 138},
  large:    {w: 270, h: 158},
  xl:       {w: 305, h: 178},
  featured: {w: 345, h: 205}
};

/** Predefined hang points (normalized top-left + rank of the biggest level they host). */
const ANCHORS = [
  // Curated irregular points: generous gaps, uneven baselines, no visible grid.
  // Larger cards get the quieter central points; small labels can reach edges.
  {x:.28,y:.30,rank:4}, {x:.55,y:.49,rank:4},
  {x:.04,y:.38,rank:3}, {x:.61,y:.24,rank:3}, {x:.16,y:.65,rank:3}, {x:.70,y:.68,rank:3},
  {x:.07,y:.11,rank:2}, {x:.36,y:.08,rank:2}, {x:.72,y:.10,rank:2}, {x:.42,y:.72,rank:2},
  {x:.02,y:.77,rank:1}, {x:.23,y:.82,rank:1}, {x:.53,y:.84,rank:1}, {x:.79,y:.80,rank:1},
  {x:.18,y:.18,rank:0}, {x:.48,y:.19,rank:0}, {x:.84,y:.18,rank:0}, {x:.88,y:.35,rank:0},
  {x:.78,y:.47,rank:0}, {x:.34,y:.51,rank:0}, {x:.08,y:.55,rank:0}, {x:.88,y:.61,rank:0},
  {x:.59,y:.64,rank:0}, {x:.31,y:.68,rank:0}, {x:.91,y:.76,rank:0}, {x:.64,y:.88,rank:0}
];

/** Deterministic per-slot jitter (never Math.random) for the scattered feel. */
function seededUnit(seed) {
  const value = Math.sin(seed * 12.9898 + 78.233) * 43758.5453;
  return value - Math.floor(value);
}
function jitter(slot, salt, axis, spread) {
  return (seededUnit(slot * 97 + salt * 31 + axis * 17) * 2 - 1) * spread;
}
function rotationFor(slot, salt) {
  return Math.round((seededUnit(slot * 67 + salt * 43) * 8 - 4) * 10) / 10;
}

/** First free anchor whose rank fits the level, scanning from `salt` so a
 *  non-zero salt rotates the assignment and yields a visibly new arrangement. */
function pickAnchor(used, rank, salt) {
  const fitting = ANCHORS.map((anchor,index)=>({anchor,index}))
    .filter(({anchor,index})=>!used.has(index) && anchor.rank>=rank);
  const available = fitting.length ? fitting : ANCHORS.map((anchor,index)=>({anchor,index})).filter(({index})=>!used.has(index));
  if (!available.length) return -1;
  return available[Math.floor(seededUnit(salt) * available.length)].index;
}

/** Assign anchors to songs that have no wall position yet; deterministic and stable. */
export function ensureLayout(songs, salt = 0) {
  const placed = songs.filter(s => s.posX != null && s.posY != null);
  const missing = songs.filter(s => s.posX == null || s.posY == null);
  if (!missing.length) return songs;
  const used = new Set(placed.map(s => anchorIndexOf(s)));
  const order = [...missing].sort((a, b) =>
    (b.playCount - a.playCount) || (a.slot - b.slot));
  const out = songs.map(s => ({...s}));
  for (const song of order) {
    const level = sizeLevel(song.playCount);
    let chosen = pickAnchor(used, LEVEL_RANK[level], salt + song.slot * 101);
    if (chosen < 0) chosen = 0;
    used.add(chosen);
    const anchor = ANCHORS[chosen];
    const index = out.findIndex(s => s.slot === song.slot);
    out[index] = {...out[index],
      posX: Math.min(.96, Math.max(.01, anchor.x + jitter(song.slot, salt, 1, .035))),
      posY: Math.min(.90, Math.max(.06, anchor.y + jitter(song.slot, salt, 2, .026))),
      rotationDeg: rotationFor(song.slot, salt),
      zIndex: song.playCount > 0 ? 20 + song.slot : 10 + song.slot,
      _anchor: chosen
    };
  }
  return out.map(({_anchor, ...song}) => song);
}
function anchorIndexOf(song) {
  let best = 0, bestDist = Infinity;
  ANCHORS.forEach((a, i) => {
    if (song.posX == null) return;
    const dist = Math.abs(a.x - song.posX) + Math.abs(a.y - song.posY);
    if (dist < bestDist) { bestDist = dist; best = i; }
  });
  return best;
}

/** Escape hatch used after a playCount change: re-flow everything; the salt
 *  differs per invocation so the new arrangement is visibly different. */
export function relayout(songs, salt = 1) {
  return ensureLayout(songs.map(s => ({...s, posX: null, posY: null})), salt);
}

/**
 * Renders the board into `container`.
 * options: {state, editable, onSongsChange(songs), onBackgroundChange(pathOrNull), onEditRequest(song)}
 */
export function renderBoard(container, options) {
  const {state, editable, onSongsChange, onBackgroundChange} = options;
  const songs = editable ? ensureLayout(state.favoriteSongs) : state.favoriteSongs;
  const usedAnchors = new Set(songs.map(anchorIndexOf));
  const emptySlots = [];
  const taken = new Set(usedAnchors);
  const layoutSeed = songs.reduce((sum,song) => sum
    + Math.round((song.posX ?? 0) * 997)
    + Math.round((song.posY ?? 0) * 991), songs.length * 59);
  for (let slot = 1; slot <= 10; slot++) {
    if (songs.some(s => s.slot === slot)) continue;
    const chosen = pickAnchor(taken, 0, slot * 137 + layoutSeed);
    if (chosen < 0) break;
    taken.add(chosen);
    emptySlots.push({slot, anchor: chosen});
  }
  const featuredSlot = [...songs].sort((a, b) => (b.playCount - a.playCount) || (a.slot - b.slot))[0]?.slot;
  const decorations = ["tape", "pin", "clip"];

  const songCard = song => {
    const level = sizeLevel(song.playCount);
    const box = LEVEL_BOX[level];
    const rot = song.rotationDeg ?? 0;
    const style = `left:calc(${song.posX ?? .04} * (100% - ${box.w}px));top:calc(${song.posY ?? .05} * (100% - ${box.h}px));width:${box.w}px;--i:${song.slot};z-index:${song.zIndex ?? 10};transform:rotate(${rot}deg)`;
    const deco = decorations[(song.slot - 1) % 3];
    const featured = song.slot === featuredSlot && song.playCount > 0;
    return `<article class="board-song ps-${level} ${featured ? "featured" : ""}" style="${style}" data-edit-song="${song.slot}">
      <span class="board-deco ${deco}"></span>
      <div class="board-song-main">
        ${song.coverPath?`<img class="board-cover" src="${escapeHtml(song.coverPath)}" alt="">`
          :`<div class="board-cover board-cover-empty">♫</div>`}
        <div class="board-song-info">
          <strong>${escapeHtml(song.title)}</strong>
          <small>${escapeHtml(song.artist)}</small>
          <small>${minutes(song.durationSeconds)}${song.playCount>0?` · ${c.playedLabel(song.playCount)}`:""}</small>
        </div>
      </div>
      ${song.note?`<div class="board-note"><span>${escapeHtml(song.note)}</span></div>`:""}
      ${song.filePath?`<audio class="board-audio" controls preload="none" src="${escapeHtml(song.filePath)}"></audio>`:""}
      ${editable?`<div class="board-song-actions">
        <button class="text-link" data-replace-song="${song.slot}">${c.songReplace}</button>
        <button class="text-link danger-link" data-remove-song="${song.slot}">${c.songRemove}</button></div>`:""}
      ${editable?`<button class="board-more" data-edit-song="${song.slot}" title="编辑">⋯</button>`:""}
    </article>`;
  };
  const emptySlotCard = ({slot, anchor}) => {
    const a = ANCHORS[anchor];
    const x = Math.min(.94,Math.max(.02,a.x+jitter(slot,layoutSeed,3,.022)));
    const y = Math.min(.91,Math.max(.08,a.y+jitter(slot,layoutSeed,4,.018)));
    const style = `left:calc(${x} * (100% - 150px));top:calc(${y} * (100% - 64px));--i:${slot};transform:rotate(${rotationFor(slot,layoutSeed)*.45}deg)`;
    return `<button class="board-empty-slot" style="${style}" data-song-add="${slot}">
      <span>${String(slot).padStart(2,"0")}</span>${c.addSong}</button>`;
  };

  container.innerHTML = `<div class="playlist-board ${state.playlistBackgroundImage?"has-bg":""}" data-editable="${editable}">
    ${state.playlistBackgroundImage?`
      <div class="board-bg"><img src="${escapeHtml(state.playlistBackgroundImage)}" alt=""></div>
      <div class="board-veil"></div>`:""}
    <div class="board-head">
      <div class="board-title">
        <strong>${escapeHtml(state.playlistTitle || c.playlistTitle)}</strong>
        <small>${escapeHtml(state.playlistSubtitle || c.playlistHint)}</small>
      </div>
      ${editable?`<div class="row-actions board-bg-actions">
        <label class="text-link">${state.playlistBackgroundImage?c.bgReplace:c.bgAdd}
          <input type="file" id="board-bg-input" accept="image/*" hidden></label>
        ${state.playlistBackgroundImage?`<button class="text-link danger-link" id="board-bg-remove">${c.bgRemove}</button>`:""}
        ${editable?`<button class="text-link" id="board-relayout">${c.relayout}</button>`:""}
      </div>`:""}
    </div>
    <div class="board-wall">
      ${songs.map(songCard).join("")}
      ${editable?emptySlots.map(emptySlotCard).join(""):""}
    </div>
  </div>`;

  /* bindings */
  container.querySelectorAll("[data-song-add]").forEach(button => button.addEventListener("click", () => {
    const input = document.createElement("input");
    input.type = "file"; input.accept = ".mp3,.flac,audio/mpeg,audio/flac"; input.hidden = true;
    document.body.append(input);
    input.addEventListener("change", async () => {
      input.remove();
      if (!input.files[0]) return;
      const data = new FormData();
      data.append("file", input.files[0]);
      data.append("slot", button.dataset.songAdd);
      try {
        const state = await api.now.uploadSong(Number(button.dataset.songAdd), data);
        onSongsChange(state.favoriteSongs, {playlistBackgroundImage: state.playlistBackgroundImage,
          playlistTitle: state.playlistTitle, playlistSubtitle: state.playlistSubtitle});
        toast("歌曲已放入歌单");
      } catch (reason) { toast(reason.message, true); }
    });
    input.click();
  }));
  container.querySelectorAll("[data-remove-song]").forEach(button => button.addEventListener("click", async event => {
    event.stopPropagation();
    const song = songs.find(value => value.slot === Number(button.dataset.removeSong));
    if (!(await confirmDialog(`从「现在。」歌单取下「${song?.title || "这首歌"}」？`))) return;
    try {
      const state = await api.now.removeSong(Number(button.dataset.removeSong));
      onSongsChange(state.favoriteSongs, {playlistBackgroundImage: state.playlistBackgroundImage,
        playlistTitle: state.playlistTitle, playlistSubtitle: state.playlistSubtitle});
    } catch (reason) { toast(reason.message, true); }
  }));
  container.querySelectorAll("[data-replace-song]").forEach(button => button.addEventListener("click", event => {
    event.stopPropagation();
    const slot = Number(button.dataset.replaceSong);
    const input = document.createElement("input");
    input.type = "file"; input.accept = ".mp3,.flac,audio/mpeg,audio/flac"; input.hidden = true;
    document.body.append(input);
    input.addEventListener("change", async () => {
      input.remove();
      if (!input.files[0]) return;
      const data = new FormData();
      data.append("file", input.files[0]);
      data.append("slot", String(slot));
      try {
        const state = await api.now.uploadSong(slot, data);
        onSongsChange(state.favoriteSongs, {playlistBackgroundImage: state.playlistBackgroundImage,
          playlistTitle: state.playlistTitle, playlistSubtitle: state.playlistSubtitle});
        toast("歌曲已替换");
      } catch (reason) { toast(reason.message, true); }
    });
    input.click();
  }));
  container.querySelectorAll("[data-edit-song]").forEach(el => el.addEventListener("click", event => {
    event.stopPropagation();
    const song = songs.find(s => s.slot === Number(el.dataset.editSong));
    if (song) openSongEditor(container, song, options);
  }));
  {
    const input = container.querySelector("#board-bg-input");
    input?.addEventListener("change", async () => {
      if (!input.files[0]) return;
      const data = new FormData();
      data.append("file", input.files[0]);
      try {
        const state = await api.now.setBackground(data);
        onBackgroundChange(state);
      } catch (reason) { toast(reason.message, true); }
    });
    container.querySelector("#board-bg-remove")?.addEventListener("click", async () => {
      if (!(await confirmDialog("取下「现在。」歌单的舞台背景？"))) return;
      try { onBackgroundChange(await api.now.clearBackground()); }
      catch (reason) { toast(reason.message, true); }
    });
    container.querySelector("#board-relayout")?.addEventListener("click", async () => {
      const arranged = relayout(songs, Math.floor(Date.now() / 1000));
      try {
        let state = null;
        for (const song of arranged) {
          state = await api.now.updateSong(song.slot, {
            title: song.title, artist: song.artist, album: song.album,
            playCount: song.playCount, note: song.note,
            posX: song.posX, posY: song.posY, rotationDeg: song.rotationDeg, zIndex: song.zIndex
          });
        }
        if (state) onSongsChange(state.favoriteSongs, {playlistBackgroundImage: state.playlistBackgroundImage,
          playlistTitle: state.playlistTitle, playlistSubtitle: state.playlistSubtitle});
        toast("已重新排版");
      } catch (reason) { toast(reason.message, true); }
    });
  }
}

/* ---------- song editor modal ---------- */

function openSongEditor(container, song, options) {
  const {onSongsChange} = options;
  const overlay = document.createElement("div");
  overlay.className = "board-modal-overlay";
  overlay.innerHTML = `<div class="board-modal panel">
    <div class="section-head"><h2>${c.editorTitle}</h2><button class="text-link" data-close>✕</button></div>
    <div class="board-modal-body">
      <div class="board-modal-cover">
        ${song.coverPath?`<img src="${escapeHtml(song.coverPath)}" alt="">`:`<div class="board-cover board-cover-empty">♫</div>`}
        <label class="text-link">${c.songReplace}<input type="file" data-replace accept=".mp3,.flac,audio/mpeg,audio/flac" hidden></label>
      </div>
      <form class="board-modal-form">
        <label class="field"><span>${c.titleLabel || "歌名"}</span><input name="title" maxlength="80" value="${escapeHtml(song.title)}" required></label>
        <label class="field"><span>艺术家</span><input name="artist" maxlength="80" value="${escapeHtml(song.artist)}"></label>
        <label class="field"><span>专辑</span><input name="album" maxlength="80" value="${escapeHtml(song.album)}"></label>
        <div class="milestone-form-grid">
          <label class="field"><span>${c.playCountLabel}</span><input name="playCount" type="number" min="0" value="${song.playCount}"></label>
          <label class="field"><span>时长</span><input value="${minutes(song.durationSeconds)}" disabled></label>
        </div>
        <label class="field"><span>${c.noteEditorLabel}</span><textarea name="note" rows="3" maxlength="120"
          placeholder="${c.noteEditorPlaceholder}">${escapeHtml(song.note)}</textarea></label>
        <div class="row-actions">
          <button class="button button-primary" type="submit">${c.save || "保存当前状态"}</button>
          <button class="button button-ghost" type="button" data-close>${c.cancel || "取消"}</button>
        </div>
      </form>
    </div></div>`;
  document.body.append(overlay);
  const close = () => overlay.remove();
  overlay.querySelectorAll("[data-close]").forEach(b => b.addEventListener("click", close));
  overlay.addEventListener("click", event => { if (event.target === overlay) close(); });
  overlay.querySelector("[data-replace]")?.addEventListener("change", async event => {
    if (!event.target.files[0]) return;
    const data = new FormData();
    data.append("file", event.target.files[0]);
    data.append("slot", String(song.slot));
    try {
      const state = await api.now.uploadSong(song.slot, data);
      close();
      onSongsChange(state.favoriteSongs, {playlistBackgroundImage: state.playlistBackgroundImage,
        playlistTitle: state.playlistTitle, playlistSubtitle: state.playlistSubtitle});
      toast("歌曲已替换");
    } catch (reason) { toast(reason.message, true); }
  });
  overlay.querySelector("form").addEventListener("submit", async event => {
    event.preventDefault();
    const form = event.target;
    try {
      const state = await api.now.updateSong(song.slot, {
        title: form.title.value.trim(), artist: form.artist.value.trim(), album: form.album.value.trim(),
        playCount: Number(form.playCount.value) || 0, note: form.note.value,
        posX: song.posX, posY: song.posY, rotationDeg: song.rotationDeg, zIndex: song.zIndex
      });
      close();
      onSongsChange(state.favoriteSongs, {playlistBackgroundImage: state.playlistBackgroundImage,
        playlistTitle: state.playlistTitle, playlistSubtitle: state.playlistSubtitle});
    } catch (reason) { toast(reason.message, true); }
  });
}
