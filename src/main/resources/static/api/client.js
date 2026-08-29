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
