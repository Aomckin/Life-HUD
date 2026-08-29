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
  dreams: {
    all: () => request("/api/dreams"),
    detail: id => request(`/api/dreams/${id}`),
    create: body => post("/api/dreams", body),
    update: (id, body) => put(`/api/dreams/${id}`, body),
    archive: id => remove(`/api/dreams/${id}`),
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
    unlink: (source, taskId) => remove(`/api/task-directions/${source}/${taskId}`)
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
    pause: id => post(`/api/focus/${id}/pause`),
    resume: id => post(`/api/focus/${id}/resume`),
    complete: (id, note = "") => post(`/api/focus/${id}/complete`, {note}),
    interrupt: (id, note = "") => post(`/api/focus/${id}/interrupt`, {note})
  }
};
