import { createTask, updateTask } from "../api/TaskApi.js";

export function setupTaskForm(onCreated) {
  const form = document.querySelector("#task-form");
  const fields = form.querySelector("fieldset");
  const categorySelect = form.elements.namedItem("categoryId");
  const button = form.querySelector('button[type="submit"]');

  const error = form.querySelector(".error");
  const success = form.querySelector(".success");
  const hint = document.querySelector("#task-hint");

  form.addEventListener("input", () => {
    error.textContent = "";
    success.textContent = "";
  });

  form.addEventListener("submit", async (event) => {
    event.preventDefault();

    if (fields.disabled || button.disabled) return;

    error.textContent = "";
    success.textContent = "";

    const title = form.elements.namedItem("title").value.trim();
    const description = form.elements.namedItem("description").value.trim();
    const categoryId =
      categorySelect.value === "" ? null : Number(categorySelect.value);
    const taskPriority = form.elements.namedItem("taskPriority").value;

    if (!title || !description) {
      error.textContent = "Vui lòng nhập tiêu đề và mô tả.";
      form.elements.namedItem(!title ? "title" : "description").focus();
      return;
    }

    button.disabled = true;
    try {
      const payload = { title, description, categoryId, taskPriority };
      const editingId = form.dataset.editingId;

      if (editingId) {
        await updateTask(editingId, payload);
      } else {
        await createTask(payload);
      }
    } catch (failure) {
      error.textContent =
        failure.status === 401
          ? "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
          : failure.message;
      button.disabled = false;
      return;
    }

    delete form.dataset.editingId;
    form.reset();
    categorySelect.value = categoryId === null ? "" : String(categoryId);
    success.textContent = "Đã tạo task.";

    //get lai danh sach
    try {
      const refreshed = await onCreated();
      form.dispatchEvent(
        new CustomEvent("created", {
          bubbles: true,
          detail: {
            message:
              refreshed === false
                ? "Đã tạo task. Bấm tải lại để cập nhật danh sách."
                : "Đã tạo task và cập nhật danh sách.",
          },
        }),
      );
    } finally {
      button.disabled = false;
    }
  });

  return {
    setCategories(categories, selectedName) {
      const previousId = categorySelect.value;
      categorySelect.replaceChildren(new Option("Không có category", ""));

      categories.forEach((category) => {
        categorySelect.add(new Option(category.name, String(category.id)));
      });

      const selected =
        categories.find((category) => category.name === selectedName) ??
        categories.find((category) => String(category.id) === previousId);

      if (selected) categorySelect.value = String(selected.id);
      fields.disabled = false;
      hint.textContent =
        "Category không bắt buộc. Bạn có thể tạo task mà không chọn category.";
    },
  };
}
