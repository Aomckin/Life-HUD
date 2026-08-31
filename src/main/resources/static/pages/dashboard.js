import { api } from "../api/client.js?v=1.0.0";
import { confirmDialog, empty, error, escapeHtml, toast } from "../components/ui.js?v=1.0.0";

const clock=value=>value?new Intl.DateTimeFormat("zh-CN",{hour:"2-digit",minute:"2-digit"}).format(new Date(value)):"—";
const minutes=value=>value>=60?`${Math.floor(value/60)}h ${value%60}min`:`${value||0} min`;
const dateText=value=>new Intl.DateTimeFormat("zh-CN",{month:"long",day:"numeric",weekday:"long"}).format(new Date(`${value}T12:00:00`));
const link=(path,label)=>`<a href="${path}" data-link>${label}</a>`;

export async function dashboard(root){
  root.innerHTML='<div class="dashboard dashboard-v2 dashboard-hud"><section class="hud-loading">正在校准今日状态舱…</section></div>';
  try{render(root,await api.dashboard.summary());}catch(reason){root.querySelector(".dashboard-hud").innerHTML=error(reason.message);toast(reason.message,true);}
}

function render(root,data){
  const s=data.status,check=s.checkIn,active=s.activeFocus,life=data.life;
  const meal=life.meals?.[0],dream=data.companions?.dream||data.dreams.active?.[0];
  const selectedRitual=data.companions?.ritual,ritualDone=data.rituals.completedToday?.find(v=>v.ritualId===(selectedRitual?.id||data.selections?.ritualId))||(!selectedRitual&&data.rituals.completedToday?.[0]),ritual=selectedRitual||ritualDone||data.rituals.available?.[0];
  const media=data.companions?.media?{title:data.companions.media.title,detail:data.companions.media.detail}:recentMedia(data.media,data.selections);
  root.querySelector(".dashboard-hud").innerHTML=`
    <section class="hud-cockpit">
      <div class="hud-cockpit-copy"><div class="hud-kicker"><span class="hud-live-dot"></span> 此刻 · NOW <time>${clock(data.generatedAt)}</time></div>
        <h1>${escapeHtml(data.headline)}</h1>
        <p class="hud-date">${dateText(data.date)}</p>
        <div class="hud-checkin">${check?`<span>心情 <b>${check.mood}</b></span><span>精力 <b>${check.energy}</b></span><span>疲劳 <b>${check.fatigue}</b></span><span>专注欲望 <b>${check.focusDesire}</b></span>`:link("/life","今天还没有记录状态 →")}</div>
        ${active?`<a class="hud-active-focus" href="/focus" data-link><span>FOCUS ACTIVE</span><strong>${escapeHtml(active.title)}</strong><small>${minutes(active.effectiveMinutes)} · 返回专注 →</small></a>`:""}
      </div>
      <div class="hud-status" aria-label="成长状态">
        ${readout("Energy",s.energy,"energy")}${readout("Level",`Lv.${s.level}`,"level")}${readout("EXP",s.exp,"exp")}${readout("Title",s.title||"尚未装备","title")}
      </div>
    </section>

    <section class="hud-today-strip" aria-label="今日指标">
      <div class="hud-strip-label"><span>TODAY</span><strong>今日脉搏</strong></div>
      ${metric("Focus",active?"进行中":minutes(data.focus.effectiveMinutes),"/focus","◎")}
      ${metric("Task",`${data.tasks.completed} / ${data.tasks.completed+data.tasks.remaining}`,"/tasks","✓")}
      ${metric("Sleep",life.sleep?minutes(life.sleep.durationMinutes):"暂无记录","/life","☾")}
      ${metric("Meal",meal?`${meal.mealType} · ${clock(meal.time)}`:"暂无记录","/life","◌")}
    </section>

    <section class="hud-companions">
      <a class="hud-companion hud-dream" href="/dreams" data-link><span class="hud-symbol">✦</span><div><small>DREAM · 航向</small><strong>${escapeHtml(dream?.title||"等待一个方向")}</strong><p>${escapeHtml(dream?.meaning||"把远方放进今天")}</p></div><i>→</i></a>
      <a class="hud-companion hud-ritual" href="/rituals" data-link><span class="hud-symbol">◈</span><div><small>RITUAL · 仪式</small><strong>${escapeHtml(ritual?(ritual.ritualName||ritual.name):"尚未进入仪式")}</strong><p>${ritualDone?`今日完成 · ${clock(ritualDone.completedAt)}`:"为状态留一个入口"}</p></div><i>→</i></a>
      <a class="hud-companion hud-media" href="/media" data-link><span class="hud-symbol">▶</span><div><small>MEDIA · 陪伴</small><strong>${escapeHtml(media.title)}</strong><p>${escapeHtml(media.detail)}</p></div><i>→</i></a>
    </section>

    <section class="hud-timeline"><header><div><small>FOOTPRINTS</small><h2>今天的足迹</h2></div>${link("/journal","完整 Journal →")}</header><div class="hud-timeline-list">${timeline(data.timeline)}</div></section>

    <details class="hud-copy-manager">
      <summary><span>今日文案</span><small>管理状态舱随机标题</small></summary>
      <div class="hud-copy-manager-body">
        <form class="hud-copy-form"><input name="text" maxlength="80" placeholder="写一句想在今天看见的话" required><button class="button button-primary" type="submit">添加</button></form>
        <div class="hud-copy-list">${data.headlines?.length?data.headlines.map(v=>`<div><span>${escapeHtml(v.text)}</span>${v.id.startsWith("default-")?`<small class="badge">内置</small>`:`<button class="text-link danger-link" type="button" data-remove-headline="${v.id}">删除</button>`}</div>`).join(""):`<p class="hud-copy-empty">还没有可用文案。</p>`}</div>
      </div>
    </details>`;
  bindHeadlineManager(root);
}

function bindHeadlineManager(root){
  root.querySelector(".hud-copy-form")?.addEventListener("submit",async event=>{event.preventDefault();const input=event.currentTarget.elements.text;try{await api.dashboard.addHeadline(input.value);toast("今日文案已添加");dashboard(root);}catch(reason){toast(reason.message,true);}});
  root.querySelectorAll("[data-remove-headline]").forEach(button=>button.addEventListener("click",async()=>{if(!(await confirmDialog("删除这句今日文案？")))return;try{await api.dashboard.removeHeadline(button.dataset.removeHeadline);toast("今日文案已删除");dashboard(root);}catch(reason){toast(reason.message,true);}}));
}

const readout=(label,value,kind)=>`<div class="hud-readout hud-${kind}"><span>${label}</span><strong>${escapeHtml(String(value))}</strong></div>`;
const metric=(label,value,path,symbol)=>`<a class="hud-metric" href="${path}" data-link><span class="hud-metric-symbol">${symbol}</span><small>${label}</small><strong>${value}</strong></a>`;
function recentMedia(media,selection){const chosen=selection?.mediaType==="GAME"?media.playingGames?.find(v=>v.id===selection.mediaId):selection?.mediaType==="ANIME"?media.watchingAnime?.find(v=>v.id===selection.mediaId):media.items?.find(v=>v.id===selection?.mediaId);if(chosen)return{title:chosen.title,detail:"正在陪伴"};const game=media.gameSessions?.[0],anime=media.animeSessions?.[0];if(game){const item=media.playingGames?.find(v=>v.id===game.gameId);return{title:item?.title||"最近一次游戏",detail:`游玩 ${minutes(game.durationMinutes)}`};}if(anime){const item=media.watchingAnime?.find(v=>v.id===anime.animeId);return{title:item?.title||"最近一次观看",detail:`看到 EP${anime.episodeEnd}`};}const current=media.playingGames?.[0]||media.watchingAnime?.[0]||media.items?.[0];return current?{title:current.title,detail:"正在陪伴"}:{title:"最近没有媒体 Session",detail:"留一点空白也很好"};}
function timeline(items){return items?.length?items.map(v=>`<article class="hud-event"><time>${clock(v.occurredAt)}</time><span></span><div><strong>${escapeHtml(v.title)}</strong><p>${escapeHtml(v.summary||v.type)}</p></div></article>`).join(""):empty("今天还没有留下足迹。");}
