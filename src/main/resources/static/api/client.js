async function request(url, options) {
  const response = await fetch(url, options);
  const text = await response.text();
  let body = null;
  try { body = text ? JSON.parse(text) : null; }
  catch { throw new Error("服务器返回了无法识别的内容"); }
  if (!response.ok) {
    const versionMismatch = response.status === 404 && url.includes("/api/focus/")
      && (url.includes("/segments/") || url.endsWith("/manual"));
    throw new Error(versionMismatch
      ? "Focus 后端版本过旧，请重启 Life HUD 服务后刷新页面"
      : body?.detail || body?.message || `请求失败（${response.status}）`);
  }
  return body;
}

const post = (url, body) => request(url, {
  method: "POST",
  headers: {"Content-Type": "application/json"},
  body: body === undefined ? undefined : JSON.stringify(body)
});
const patch = (url, body) => request(url, {
  method: "PATCH", headers: {"Content-Type": "application/json"}, body: JSON.stringify(body)
});
const put = (url, body) => request(url, {
  method: "PUT", headers: {"Content-Type": "application/json"}, body: JSON.stringify(body)
});
const remove = url => request(url, {method: "DELETE"});

export const api = {
  state: () => request("/state"),
  events: (limit = 8) => request(`/api/life-events?limit=${limit}`),
  removeEvent: id => remove(`/api/life-events/${id}`),
  growth: {
    overview: () => request("/api/growth"),
    history: (limit = 30) => request(`/api/growth/history?limit=${limit}`),
    energyHistory: () => request("/api/growth/energy-history"),
    snapshots: (days = 7) => request(`/api/growth/snapshots?days=${days}`),
    recalculate: () => post("/api/growth/recalculate")
  },
  entertainment: {
    all: () => request("/api/entertainment"),
    create: body => post("/api/entertainment", body),
    update: (id, body) => patch(`/api/entertainment/${id}`, body),
    remove: id => remove(`/api/entertainment/${id}`)
  },
  media: {
    anime: {all: () => request("/api/media/anime"), detail: id => request(`/api/media/anime/${id}`), create: body => post("/api/media/anime", body), update: (id, body) => put(`/api/media/anime/${id}`, body), remove: id => remove(`/api/media/anime/${id}`)},
    animeSessions: {all: id => request(`/api/media/anime/${id}/sessions`), create: (id, body) => post(`/api/media/anime/${id}/sessions`, body), update: (id, body) => put(`/api/media/anime-sessions/${id}`, body), remove: id => remove(`/api/media/anime-sessions/${id}`)},
    games: {all: () => request("/api/media/games"), detail: id => request(`/api/media/games/${id}`), create: body => post("/api/media/games", body), update: (id, body) => put(`/api/media/games/${id}`, body), remove: id => remove(`/api/media/games/${id}`)},
    gameSessions: {all: id => request(`/api/media/games/${id}/sessions`), create: (id, body) => post(`/api/media/games/${id}/sessions`, body), update: (id, body) => put(`/api/media/game-sessions/${id}`, body), remove: id => remove(`/api/media/game-sessions/${id}`)},
    items: {all: type => request(`/api/media/items${type ? `?type=${type}` : ""}`), detail: id => request(`/api/media/items/${id}`), create: body => post("/api/media/items", body), update: (id, body) => put(`/api/media/items/${id}`, body), remove: id => remove(`/api/media/items/${id}`)}
  },
  dreams: {
    all: () => request("/api/dreams"),
    detail: id => request(`/api/dreams/${id}`),
    create: body => post("/api/dreams", body),
    update: (id, body) => put(`/api/dreams/${id}`, body),
    archive: id => remove(`/api/dreams/${id}`),
    purge: id => remove(`/api/dreams/${id}/purge`),
    complete: id => post(`/api/dreams/${id}/complete`),
    pause: id => post(`/api/dreams/${id}/pause`),
    resume: id => post(`/api/dreams/${id}/resume`),
    createGoal: (dreamId, body) => post(`/api/dreams/${dreamId}/goals`, body),
    updateGoal: (id, body) => put(`/api/goals/${id}`, body),
    completeGoal: id => post(`/api/goals/${id}/complete`),
    deleteGoal: id => remove(`/api/goals/${id}`),
    createMilestone: (goalId, body) => post(`/api/goals/${goalId}/milestones`, body),
    updateMilestone: (id, body) => put(`/api/dream-milestones/${id}`, body),
    completeMilestone: id => post(`/api/dream-milestones/${id}/complete`),
    deleteMilestone: id => remove(`/api/dream-milestones/${id}`)
  },
  rituals: {
    all: () => request("/api/rituals"),
    detail: id => request(`/api/rituals/${id}`),
    create: body => post("/api/rituals", body),
    update: (id, body) => put(`/api/rituals/${id}`, body),
    remove: id => remove(`/api/rituals/${id}`),
    enable: id => post(`/api/rituals/${id}/enable`),
    disable: id => post(`/api/rituals/${id}/disable`),
    start: id => post(`/api/rituals/${id}/start`),
    execution: id => request(`/api/ritual-executions/${id}`),
    step: (id, stepId, body) => post(`/api/ritual-executions/${id}/steps/${stepId}`, body),
    completeExecution: (id, note = "") => post(`/api/ritual-executions/${id}/complete`, {note}),
    cancelExecution: (id, note = "") => post(`/api/ritual-executions/${id}/cancel`, {note})
  },
  now: {
    current: () => request("/api/now"),
    update: body => put("/api/now", body),
    uploadSong: (slot, formData) => request(`/api/now/songs?slot=${slot}`, {method: "POST", body: formData}),
    updateSong: (slot, body) => put(`/api/now/songs/${slot}`, body),
    removeSong: slot => remove(`/api/now/songs/${slot}`),
    setBackground: formData => request("/api/now/background", {method: "POST", body: formData}),
    clearBackground: () => remove("/api/now/background"),
    snapshots: () => request("/api/now/snapshots"),
    snapshot: id => request(`/api/now/snapshots/${id}`),
    createSnapshot: () => post("/api/now/snapshots"),
    removeSnapshot: id => remove(`/api/now/snapshots/${id}`)
  },
  taskDirections: {
    all: () => request("/api/task-directions"),
    link: (source, taskId, body) => put(`/api/task-directions/${source}/${taskId}`, body),
    unlink: (source, taskId) => remove(`/api/task-directions/${source}/${taskId}`),
    complete: (source, taskId) => post(`/api/task-directions/${source}/${taskId}/complete`)
  },
  life: {
    sleep: {
      all: () => request("/api/life/sleep"),
      create: body => post("/api/life/sleep", body),
      update: (id, body) => put(`/api/life/sleep/${id}`, body),
      remove: id => remove(`/api/life/sleep/${id}`)
    },
    meals: {
      all: () => request("/api/life/meals"),
      create: body => post("/api/life/meals", body),
      update: (id, body) => put(`/api/life/meals/${id}`, body),
      remove: id => remove(`/api/life/meals/${id}`)
    },
    exercises: {
      all: () => request("/api/life/exercises"),
      create: body => post("/api/life/exercises", body),
      update: (id, body) => put(`/api/life/exercises/${id}`, body),
      remove: id => remove(`/api/life/exercises/${id}`)
    },
    checkIns: {
      all: () => request("/api/life/check-ins"),
      latest: () => request("/api/life/check-ins")
        .then(list => [...list].sort((a, b) => new Date(b.time) - new Date(a.time))[0] || null),
      create: body => post("/api/life/check-ins", body),
      update: (id, body) => put(`/api/life/check-ins/${id}`, body),
      remove: id => remove(`/api/life/check-ins/${id}`)
    },
    records: {
      all: () => request("/api/life/records"),
      create: body => post("/api/life/records", body),
      update: (id, body) => put(`/api/life/records/${id}`, body),
      remove: id => remove(`/api/life/records/${id}`)
    }
  },
  journal: {
    all: () => request("/api/journal"),
    create: body => post("/api/journal", body),
    update: (id, body) => put(`/api/journal/${id}`, body),
    remove: id => remove(`/api/journal/${id}`)
  },
  timeline: (params = {}) => {
    const query = Object.entries(params)
      .filter(([, value]) => value !== undefined && value !== null && value !== "")
      .map(([key, value]) => `${key}=${encodeURIComponent(value)}`)
      .join("&");
    return request(`/api/timeline${query ? "?" + query : ""}`);
  },
  taskPool: {
    all: () => request("/api/task-pool"),
    createDaily: body => post("/api/task-pool/daily", body),
    updateDaily: (id, body) => put(`/api/task-pool/daily/${id}`, body),
    deleteDaily: id => remove(`/api/task-pool/daily/${id}`),
    setDailyEnabled: (id, enabled) => post(`/api/task-pool/daily/${id}/enabled`, {enabled}),
    createSpecial: body => post("/api/task-pool/special", body),
    updateSpecial: (id, body) => put(`/api/task-pool/special/${id}`, body),
    deleteSpecial: id => remove(`/api/task-pool/special/${id}`),
    setSpecialEnabled: (id, enabled) => post(`/api/task-pool/special/${id}/enabled`, {enabled})
  },
  uploadImage: formData => request("/api/images", {method: "POST", body: formData}),
  achievements: () => request("/api/achievements"),
  milestones: {
    all: () => request("/api/milestones"),
    create: body => post("/api/milestones", body),
    update: (id, body) => patch(`/api/milestones/${id}`, body),
    remove: id => remove(`/api/milestones/${id}`)
  },
  titles: {
    all: () => request("/api/titles"),
    create: body => post("/api/titles", body),
    equip: id => post(`/api/titles/${id}/equip`),
    unequip: () => post("/api/titles/unequip"),
    remove: id => remove(`/api/titles/${id}`)
  },
  command: (type, payload = {}) => post("/command", {type, payload}),
  focus: {
    current: () => request("/api/focus/current"),
    today: () => request("/api/focus/today"),
    history: (limit = 30) => request(`/api/focus/history?limit=${limit}`),
    start: payload => post("/api/focus/start", payload),
    manual: payload => post("/api/focus/manual", payload),
    switchSegment: (id, payload) => post(`/api/focus/${id}/segments/switch`, payload),
    updateSegment: (id, segmentId, payload) => patch(`/api/focus/${id}/segments/${segmentId}`, payload),
    remove: id => remove(`/api/focus/${id}`),
    pause: id => post(`/api/focus/${id}/pause`),
    resume: id => post(`/api/focus/${id}/resume`),
    complete: (id, note = "") => post(`/api/focus/${id}/complete`, {note}),
    interrupt: (id, note = "") => post(`/api/focus/${id}/interrupt`, {note})
  }
};
