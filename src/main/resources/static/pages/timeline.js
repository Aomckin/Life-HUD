import { api } from "../api/client.js?v=0.4.0";
import { empty, error, escapeHtml } from "../components/ui.js";

const IMPORTANT = new Set(["LEVEL_UP","ACHIEVEMENT_UNLOCKED","MILESTONE_CREATED","TITLE_UNLOCKED","TITLE_EQUIPPED"]);
const labels = {LEVEL_UP:"Level Up",ACHIEVEMENT_UNLOCKED:"Achievement",MILESTONE_CREATED:"Milestone",MILESTONE_UPDATED:"Milestone",TITLE_UNLOCKED:"Title",TITLE_EQUIPPED:"Title",FOCUS_FINISHED:"Focus",TASK_COMPLETED:"Task"};

export async function timeline(root) {
  root.innerHTML='<section class="panel timeline-page">正在读取生活轨迹…</section>';
  try {
    const events=await api.events(100);
    root.innerHTML=`<div class="timeline-page"><section class="panel timeline-intro"><div class="eyebrow">Journal · Timeline</div><h1>发生过的事，按时间留在这里。</h1><p>普通行为保持轻量，里程碑与重要成长节点会自然浮现。</p></section><section class="panel timeline-list">${events.length?events.map(event=>`<article class="timeline-event ${IMPORTANT.has(event.type)?"important":""} ${event.type.startsWith("MILESTONE")?"milestone":""}"><time>${new Intl.DateTimeFormat("zh-CN",{month:"short",day:"numeric",hour:"2-digit",minute:"2-digit"}).format(new Date(event.occurredAt))}</time><span class="timeline-dot"></span><div><small>${escapeHtml(labels[event.type]||event.sourceType||event.source||"LifeEvent")}</small><h3>${escapeHtml(event.title)}</h3><p>${escapeHtml(event.content||event.description||"")}</p></div></article>`).join(""):empty("还没有生活事件。下一次 Focus、任务或里程碑会从这里开始。")}</section></div>`;
  } catch(reason){root.innerHTML=`<div class="timeline-page">${error(reason.message)}</div>`;}
}
