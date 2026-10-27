import { updateTaskCompletion, deleteTask } from "../api/TaskApi.js";

export function createTaskItem(task, onDeleted = () => {}, onCompletionChanged = () => {}) {
  const item = document.createElement("li");

  const heading = document.createElement("div");
  heading.className = "task-row-heading";
  const title = document.createElement("strong");
  title.className = "task-title";

  const description = document.createElement("p");

  const details = document.createElement("small");

  const categoryName = document.createElement("p");

  const label = document.createElement("label");
  label.className = "task-completion";

  const checkbox = document.createElement("input");
  checkbox.type = "checkbox";
  checkbox.setAttribute("aria-label", `Đã hoàn thành: ${task.title}`);

  const createAt = document.createElement("p");
  createAt.textContent = `Ngày tạo: ${new Date(task.createdAt).toLocaleString()}`;

  const error = document.createElement("p");
  error.className = "error";
  error.setAttribute("role", "alert");

  const deleteButton = document.createElement("button");
  deleteButton.type = "button";
  deleteButton.className = "delete-button";
  deleteButton.setAttribute("aria-label", `Xóa task: ${task.title}`);
  deleteButton.textContent = "Xóa";

  const updateButton = document.createElement("button");
  updateButton.type = "button";
  updateButton.className = "update-button";
  updateButton.setAttribute("aria-label", `Cập nhật task: ${task.title}`);
  updateButton.textContent = "Cập nhật";

  const priorities = { HIGH: "Cao", MEDIUM: "Trung bình", LOW: "Thấp" };

  title.textContent = task.title;
  description.textContent = task.description;
  categoryName.textContent = task.categoryName ? `Danh mục: ${task.categoryName}` : "Không có danh mục";
  let completed = Boolean(task.completed);
  item.dataset.completed = String(completed);

  updateButton.addEventListener("click", async () => {
    const form = document.querySelector("#task-form");
    const submitButton = form.querySelector("button[type=submit]");

    form.dataset.editingId = String(task.id);

    form.elements.namedItem("title").value = task.title;
    form.elements.namedItem("description").value = task.description;
    form.elements.namedItem("categoryId").value = task.categoryId ?? "";
    form.elements.namedItem("taskPriority").value =
      task.taskPriority ?? "MEDIUM";

    document.querySelector("#task-modal-heading").textContent = "Cập nhật task";
    document.querySelector("#task-modal").showModal();
    submitButton.textContent = "Cập nhật";
    document.body.classList.add("modal-open");
  });

  deleteButton.addEventListener("click", async () => {
    const confirmed = window.confirm(
      `Bạn có chắc muốn xóa task "${task.title}" không?`,
    );

    if (!confirmed) return;
    deleteButton.disabled = true;
    item.setAttribute("aria-busy", "true");
    error.textContent = "";

    // Gọi API xóa task
    try {
      await deleteTask(task.id);
      item.remove();
      onDeleted();
    } catch (failure) {
      error.textContent =
        failure.status === 401
          ? "Vui lòng đăng nhập lại."
          : "Không thể xóa task. Vui lòng thử lại.";
    } finally {
      deleteButton.disabled = false;
      item.removeAttribute("aria-busy");
    }
  });

  //update checkbox da hoan thanh chua
  function renderCompletion(value) {
    checkbox.checked = value;
    item.classList.toggle("task-completed", value);
    details.textContent = `Ưu tiên: ${priorities[task.taskPriority] ?? task.taskPriority} · ${
      value ? "Đã hoàn thành" : "Chưa hoàn thành"
    }`;
  }

  checkbox.addEventListener("change", async () => {
    const nextCompleted = checkbox.checked;
    checkbox.disabled = true;

    item.setAttribute("aria-busy", "true");
    error.textContent = "";

    renderCompletion(nextCompleted);

    try {
      const saved = await updateTaskCompletion(task.id, nextCompleted);

      completed = saved.completed;
      task.completed = completed;

      item.dataset.completed = String(completed);

      renderCompletion(completed);
      onCompletionChanged();
      
    } catch (failure) {
      renderCompletion(completed);
      error.textContent =
        failure.status === 401
          ? "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
          : "Chưa lưu được trạng thái task. Vui lòng thử lại.";
    } finally {
      checkbox.disabled = false;
      item.removeAttribute("aria-busy");
    }
  });

  renderCompletion(completed);

  label.append(checkbox, document.createTextNode("Đã hoàn thành"));

  heading.append(title, label);

  item.append(heading, description, details,categoryName, createAt, updateButton, deleteButton, error);
  return item;
}
