import { api } from "../api/client.js?v=0.8.0";
import { empty, error, escapeHtml, toast } from "../components/ui.js";

const clock = value => value ? new Intl.DateTimeFormat("zh-CN", {hour:"2-digit",minute:"2-digit"}).format(new Date(value)) : "—";
const minutes = value => value >= 60 ? `${Math.floor(value / 60)}h ${value % 60}min` : `${value || 0} min`;
const dateText = value => new Intl.DateTimeFormat("zh-CN", {month:"long",day:"numeric",weekday:"long"}).format(new Date(`${value}T12:00:00`));
const link = (path, label) => `<a href="${path}" data-link>${label}</a>`;

export async function dashboard(root) {
  root.innerHTML = `<div class="dashboard dashboard-v2"><section class="panel cockpit-now"><div><div class="eyebrow">此刻 · NOW</div><div class="hero-time" id="today-clock"></div><h1 id="today-date">今天</h1><p>正在聚合今天的自己…</p></div></section><section id="dashboard-content"></section></div>`;
  try { render(root, await api.dashboard.summary()); }
  catch (reason) { root.querySelector("#dashboard-content").innerHTML=error(reason.message);toast(reason.message,true); }
}

function render(root, data) {
  const s=data.status, check=s.checkIn, active=s.activeFocus;
  root.querySelector("#today-date").textContent=dateText(data.date);
  root.querySelector("#today-clock").textContent=clock(data.generatedAt);
  root.querySelector(".cockpit-now p").innerHTML=check
    ? `心情 <strong>${check.mood}</strong> · 精力 <strong>${check.energy}</strong> · 疲劳 <strong>${check.fatigue}</strong> · 专注欲望 <strong>${check.focusDesire}</strong>`
    : link("/life","今天还没有记录状态 →");
  root.querySelector(".cockpit-now").insertAdjacentHTML("beforeend",`<div class="now-growth"><div><span>Energy</span><strong>${s.energy}</strong></div><div><span>Level</span><strong>Lv.${s.level}</strong></div><div><span>EXP</span><strong>${s.exp}</strong></div><div><span>Title</span><strong>${escapeHtml(s.title||"尚未装备")}</strong></div></div>`);

  const life=data.life, meal=life.meals?.[0], dream=data.dreams.active?.[0], ritual=data.rituals.completedToday?.[0]||data.rituals.available?.[0];
  const recentMedia=data.media.gameSessions?.[0]||data.media.animeSessions?.[0];
  root.querySelector("#dashboard-content").innerHTML=`
    <section class="today-layer"><div class="section-head"><div><div class="eyebrow">今天 · TODAY</div><h2>今日事实</h2></div></div><div class="fact-grid">
      ${fact("Focus",active?`进行中 · ${escapeHtml(active.title)}`:minutes(data.focus.effectiveMinutes),"/focus")}
      ${fact("Task",`${data.tasks.completed} / ${data.tasks.completed+data.tasks.remaining}`,"/tasks")}
      ${fact("Sleep",life.sleep?minutes(life.sleep.durationMinutes):"暂无记录","/life")}
      ${fact("Meal",meal?`${meal.mealType} · ${clock(meal.time)}`:"暂无记录","/life")}
    </div></section>
    <section class="direction-layer"><div class="section-head"><div><div class="eyebrow">方向与陪伴</div><h2>正在往哪里走</h2></div></div><div class="companion-grid">
      ${summary("✦ 当前 Dream",dream?.title||"还没有进行中的 Dream","/dreams")}
      ${summary("☀ Ritual",ritual?(ritual.ritualName||ritual.name):"今天还没有 Ritual","/rituals")}
      ${summary("🎮 最近陪伴",recentMedia?mediaLabel(recentMedia,data.media):"最近没有媒体 Session","/media")}
    </div></section>
    <section class="panel footprint-layer"><div class="section-head"><div><div class="eyebrow">今天的足迹</div><h2>Timeline</h2></div>${link("/journal","查看完整 Journal")}</div><div class="footprint-list">${timeline(data.timeline)}</div></section>`;
}

const fact=(label,value,path)=>`<a class="card fact-card" href="${path}" data-link><span>${label}</span><strong>${value}</strong><small>查看详情 →</small></a>`;
const summary=(label,value,path)=>`<a class="card companion-card" href="${path}" data-link><span>${label}</span><strong>${escapeHtml(value)}</strong></a>`;
function timeline(items){return items?.length?items.map(v=>`<article class="footprint"><time>${clock(v.occurredAt)}</time><span class="event-dot"></span><div><strong>${escapeHtml(v.title)}</strong><p>${escapeHtml(v.summary||v.type)}</p></div></article>`).join(""):empty("今天还没有留下足迹。");}
function mediaLabel(item,media){const game=media.playingGames?.find(v=>v.id===item.gameId);if(game)return `${game.title} · ${minutes(item.durationMinutes)}`;const anime=media.watchingAnime?.find(v=>v.id===item.animeId);return anime?`${anime.title} · EP${item.episodeEnd}`:"一段最近的陪伴";}
