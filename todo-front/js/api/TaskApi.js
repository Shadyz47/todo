import { apiRequest } from "./UserApi.js";

export async function getAllTasks(
  query = "",
  fromDate,
  toDate,
  sortBy = "priority",
  completed,
  order = "desc"
) {
  const params = new URLSearchParams({ query, sortBy, order });

  if (fromDate) params.set("fromDate", fromDate);
  if (toDate) params.set("toDate", toDate);

  if (typeof completed === "boolean") {
    params.set("completed", String(completed));
  }

  return await apiRequest(`/tasks?${params}`);
}

export async function createTask(task) {
  return await apiRequest("/tasks", { method: "POST", body: task });
}

export async function updateTask(taskId, task) {
  return await apiRequest(`/tasks/${taskId}`, { method: "PUT", body: task });
}

export async function deleteTask(taskId) {
  return await apiRequest(`/tasks/${taskId}`, { method: "DELETE" });
}

export async function updateTaskCompletion(taskId, completed) {
  return await apiRequest(`/tasks/${taskId}/completion`, {
    method: "PATCH",
    body: { completed },
  });
}
