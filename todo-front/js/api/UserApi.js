const apiHost = window.location.hostname === "127.0.0.1" ? "127.0.0.1" : "localhost";
const API_URL = `http://${apiHost}:9001/api`;

export async function apiRequest(path, { method = "GET", body } = {}) {
  method = method.toUpperCase();
  const headers = {};

  // Lấy CSRF token trước mỗi request thay đổi dữ liệu.
  if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
    const csrf = await apiRequest("/users/csrf");
    headers[csrf.headerName] = csrf.token;
  }

  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  const response = await fetch(`${API_URL}${path}`, {
    method,
    headers,
    credentials: "include",
    cache: "no-store",
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  // Register trả text, login trả JSON, logout không có body.
  const text = await response.text();
  const contentType = response.headers.get("content-type") ?? "";
  const data = text
    ? contentType.includes("json") ? JSON.parse(text) : text
    : null;

  if (!response.ok) {
    const message =
      data?.detail ||
      data?.message ||
      (typeof data === "string" ? data : null) ||
      `Request thất bại: HTTP ${response.status}`;

    const error = new Error(message);
    error.status = response.status;
    throw error;
  }

  return data;
}

export const userApi = {
  register(username, password) {
    return apiRequest("/users/register", {
      method: "POST",
      body: { username, password },
    });
  },

  login(username, password) {
    return apiRequest("/users/login", {
      method: "POST",
      body: { username, password },
    });
  }
};
