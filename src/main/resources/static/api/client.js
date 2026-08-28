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

export const api = {
  state: () => request("/state"),
  events: (limit = 8) => request(`/api/life-events?limit=${limit}`),
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
