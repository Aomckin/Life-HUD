import { icon } from "../components/ui.js";
export const routes = [
  {path:"/dashboard",name:"今日 · Today",icon:"today"},
  {path:"/focus",name:"专注 · Focus",icon:"focus"},
  {path:"/tasks",name:"任务 · Tasks",icon:"tasks"},
  {path:"/dreams",name:"梦想 · Dreams",icon:"dreams"},
  {path:"/rituals",name:"仪式 · Ritual",icon:"rituals"},
  {path:"/life",name:"生活 · Life",icon:"life"},
  {path:"/media",name:"媒体 · Media",icon:"media"},
  {path:"/journal",name:"日记 · Journal",icon:"journal"},
  {path:"/now",name:"「现在。」 · Now",icon:"now"},
  {path:"/growth",name:"成长 · Growth",icon:"growth"}
];
export function sidebar(active) {
  return `<aside class="sidebar"><a class="brand" href="/dashboard" data-link><span class="brand-mark">L</span><span class="brand-name">Life HUD</span></a><nav class="nav">${routes.map((route,index)=>`<a class="nav-link ${route.path===active?"active":""} ${index<5?"mobile-primary":""}" href="${route.path}" data-link title="${route.name}">${icon(route.icon)}<span>${route.name}</span></a>`).join("")}</nav><a class="nav-link nav-settings" href="/settings" data-link title="设置 · Settings">${icon("settings")}<span>设置 · Settings</span></a></aside>`;
}