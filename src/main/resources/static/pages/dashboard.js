import { api } from "../api/client.js?v=0.6.1";
import { modeLabels, growthCopy } from "../content/copy.js?v=0.6.1";
import { empty, error, escapeHtml, toast } from "../components/ui.js";

const formatDate = value => new Intl.DateTimeFormat("zh-CN", {
  month: "long",
  day: "numeric",
  weekday: "short",
  hour: "2-digit",
  minute: "2-digit"
}).format(value ? new Date(value) : new Date());

const progress = state => {
  const values = String(state.exp_text || "").match(/(\d+)\s*\/\s*(\d+)/);
  return values ? Math.min(100, Math.round(Number(values[1]) / Number(values[2]) * 100)) : 0;
};

const energyProgress = state => {
  const values = String(state.energy_text || "").match(/(\d+)\s*\/\s*(\d+)/);
  return values ? Math.min(100, Math.round(Number(values[1]) / Number(values[2]) * 100)) : 0;
};

export async function dashboard(root) {
  root.innerHTML = [
    '<div class="dashboard">',
    '<section class="panel hero">',
    '<div class="hero-copy"><div class="eyebrow" id="today-date">今天</div><div class="hero-time" id="today-time"></div><h1>把今天过得清澈一点。</h1><p>这里保留正在发生的生活：你的能量、进度、待完成的事，以及最近留下的痕迹。</p></div>',
    '<div class="hero-actions"><div class="hero-status"><span>此刻的状态</span><strong id="hero-energy">正在读取…</strong><span id="hero-title">Life HUD</span></div><button class="button button-primary" data-go="/focus">开始一次专注</button><button class="button button-secondary" data-go="/tasks">看看今天的任务</button></div>',
    '</section>',
    '<section class="metric-grid" id="metrics"><div class="card metric">正在同步今天的状态…</div></section>',
    '<section class="dashboard-grid">',
    '<section class="panel content-card"><div class="section-head"><h2>今天的任务</h2><a href="/tasks" data-link>查看全部</a></div><div id="today-tasks"></div></section>',
    '<section class="panel content-card"><div class="section-head"><h2>最近发生</h2><span class="badge">LifeEvent</span></div><div id="recent-events"></div></section>',
    '</section></div>'
  ].join("");

  root.querySelector("#today-date").textContent = formatDate();
  root.querySelector("#today-time").textContent = new Intl.DateTimeFormat("zh-CN", {
    hour: "2-digit",
    minute: "2-digit"
  }).format(new Date());

  root.querySelectorAll("[data-go]").forEach(button => {
    button.addEventListener("click", () => window.navigate(button.dataset.go));
  });

  try {
    const [state, events, currentFocus, focusToday, growth] = await Promise.all([
      api.state(), api.events(), api.focus.current(), api.focus.today(), api.growth.overview()
    ]);
    renderHero(root, state, currentFocus, growth);
    renderMetrics(root, state, currentFocus, focusToday, growth);
    renderTasks(root, state);
    renderEvents(root, events);
  } catch (reason) {
    root.querySelector("#metrics").innerHTML = error(reason.message);
    root.querySelector("#today-tasks").innerHTML = error("任务暂时无法读取");
    root.querySelector("#recent-events").innerHTML = error("事件暂时无法读取");
    toast(reason.message, true);
  }
}

const MODE_LABELS = modeLabels;
const formatFocusTime = seconds => {
  const total = Math.max(0, Math.floor(seconds || 0));
  if (total > 0 && total < 60) return "<1min";
  const hours = Math.floor(total / 3600);
  const minutes = Math.floor(total % 3600 / 60);
  return hours ? `${hours}h ${minutes}min` : `${minutes}min`;
};

function renderHero(root, state, currentFocus, growth) {
  root.querySelector("#hero-energy").textContent = currentFocus
    ? `${MODE_LABELS[currentFocus.mode]} · ${currentFocus.title}`
    : `Energy ${growth.energy} / ${growth.energyMax}`;
  root.querySelector("#hero-title").textContent = currentFocus
    ? `${formatFocusTime(currentFocus.actualSeconds)} · ${currentFocus.status === "PAUSED" ? "已暂停" : "专注中"}`
    : (growth.currentTitle || "今天由你决定");
  if (currentFocus) root.querySelector('[data-go="/focus"]').textContent = "返回当前 Focus";
}

function renderMetrics(root, state, currentFocus, focusToday, growth) {
  const energy = `${growth.energy} / ${growth.energyMax}`;
  const level = `Lv.${growth.level}`;
  const exp = `${growth.currentExp} / ${growth.requiredExp}`;
  const title = escapeHtml(growth.currentTitle || "称号仍在积累");
  const expProgress = Math.min(100, Math.round(growth.currentExp / growth.requiredExp * 100));
  const currentEnergy = Math.min(100, Math.round(growth.energy / growth.energyMax * 100));
  const energyFlow = (growth.todayEnergyEarn || growth.todayEnergySpend)
    ? `今日 +${growth.todayEnergyEarn || 0} / −${growth.todayEnergySpend || 0}`
    : growthCopy.energyFlowEmpty;
  const expNote = growth.todayExpDelta ? `今日 +${growth.todayExpDelta} · 长期积累` : growthCopy.expAccumulating;

  root.querySelector("#metrics").innerHTML = [
    '<article class="card card-hover metric metric-energy growth-link" data-growth="overview"><div class="metric-label">能量 · Energy</div><div class="metric-value">' + energy + '</div><div class="progress" aria-label="当前能量"><span style="width:' + currentEnergy + '%"></span></div><div class="metric-status">' + energyFlow + '</div></article>',
    '<article class="card card-hover metric metric-level growth-link" data-growth="overview"><div class="metric-label">等级 · Level</div><div class="metric-value">' + level + '</div><div class="metric-note">距下一阶段 ' + growth.expToNext + ' EXP · ' + title + '</div></article>',
    '<article class="card card-hover metric metric-exp growth-link" data-growth="overview"><div class="metric-label">经验 · EXP</div><div class="metric-value">' + exp + '</div><div class="progress" aria-label="当前经验进度"><span style="width:' + expProgress + '%"></span></div><div class="metric-note">' + expNote + '</div></article>',
    currentFocus
      ? '<article class="card card-hover metric metric-focus is-active"><div class="metric-label">正在 Focus</div><div class="metric-value metric-focus-title" title="' + escapeHtml(currentFocus.title) + '">' + escapeHtml(currentFocus.title) + '</div><div class="metric-note">' + escapeHtml(`${MODE_LABELS[currentFocus.mode]} · ${formatFocusTime(currentFocus.actualSeconds)} · ${currentFocus.status === "PAUSED" ? "已暂停" : "专注中"}`) + '</div><button class="metric-focus-action" type="button">返回 Focus →</button></article>'
      : '<article class="card card-hover metric metric-focus"><div class="metric-label">专注 · Focus</div><div class="metric-value">' + formatFocusTime(focusToday.totalSeconds) + '</div><div class="metric-note">' + (focusToday.sessionCount ? `今日 ${focusToday.sessionCount} 次 Session` : "今天还没有留下专注时间") + '</div><button class="metric-focus-action" type="button">开始 Focus →</button></article>'
  ].join("");
  root.querySelector(".metric-focus-action").addEventListener("click", () => window.navigate("/focus"));
  root.querySelectorAll("[data-growth]").forEach(card => card.addEventListener("click", () => window.navigate(`/growth#${card.dataset.growth}`)));
}

function renderTasks(root, state) {
  const tasks = Array.isArray(state.active_task_views) ? state.active_task_views : [];
  const taskList = tasks.slice(0, 4).map(task => [
    '<div class="task-item"><div><strong>',
    escapeHtml(task.name || task.id || "任务"),
    '</strong><div class="event-meta">',
    escapeHtml(task.detail_text || task.reward_text || ""),
    '</div></div><button class="button button-secondary" data-task="',
    escapeHtml(JSON.stringify(task.command_payload || {})),
    '" ',
    task.button_state === "normal" ? "" : "disabled",
    '>',
    escapeHtml(task.button_text || "完成"),
    '</button></div>'
  ].join("")).join("");

  root.querySelector("#today-tasks").innerHTML = taskList
    ? '<div class="task-list">' + taskList + '</div>'
    : empty("今天还没有任务，留一点空白也很好。");

  root.querySelectorAll("[data-task]").forEach(button => {
    button.addEventListener("click", async () => {
      button.disabled = true;
      try {
        const beforeGrowth = await api.growth.overview();
        const result = await api.command("COMPLETE_DAILY_TASK", JSON.parse(button.dataset.task));
        const afterGrowth = await api.growth.overview();
        const exp = afterGrowth.totalExp - beforeGrowth.totalExp;
        const energy = afterGrowth.energy - beforeGrowth.energy;
        toast(`${result.message || "已完成"}${exp ? ` · EXP +${exp}` : ""}${energy ? ` · Energy ${energy > 0 ? "+" : ""}${energy}` : ""}`);
        window.navigate("/dashboard", true);
      } catch (reason) {
        toast(reason.message, true);
        button.disabled = false;
      }
    });
  });
}

function renderEvents(root, events) {
  const eventList = events.map(event => [
    '<div class="event-item"><span class="event-dot"></span><div class="event-main"><strong>',
    escapeHtml(event.title),
    '</strong><div class="event-meta">',
    escapeHtml(event.content || event.type),
    ' · ',
    formatDate(event.occurredAt),
    '</div></div></div>'
  ].join("")).join("");

  root.querySelector("#recent-events").innerHTML = eventList
    ? '<div class="event-list">' + eventList + '</div>'
    : empty("今天还没有记录。完成一次任务或专注后，它会出现在这里。");
}
