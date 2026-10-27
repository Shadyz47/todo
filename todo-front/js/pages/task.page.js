import { getAllTasks } from "../api/TaskApi.js";
import { setupCategoryPage } from "./category.page.js";
import { setupTaskForm } from "../components.js/task-form.js";
import { createTaskItem } from "../components.js/task-item.js";
import { setupModals } from "../components.js/modal.js";

setupModals();

const list = document.querySelector("#task-list");

const error = document.querySelector("#task-list-error");
const reloadButton = document.querySelector("#reload-tasks");

const priorityFilters = document.querySelector("#filter-tasks");
const searchInput = document.querySelector("#search-tasks");
const sortSelect = document.querySelector("#sort-tasks");
const completionFilter = document.querySelector("#completion-filter");
const clearFiltersButton = document.querySelector("#clear-task-filters");

let tasks = [];
let latestRequest = 0;
let searchTimer;

const emptyState = document.createElement("li");

function applyFilters() {
  const priorities = Array.from(
    priorityFilters.querySelectorAll('input[name="priority"]:checked'),
    (checkbox) => checkbox.value,
  );

  let visibleCount = 0;     // so luong task hien thi sau khi loc
  list.querySelectorAll("[data-task-priority]").forEach((item) => {
    item.hidden =
      (priorities.length > 0 &&
        !priorities.includes(item.dataset.taskPriority)) ||
      (completionFilter.value !== "" &&
        item.dataset.completed !== completionFilter.value);
    if (!item.hidden) visibleCount++;
  });

  emptyState.hidden = visibleCount > 0;

  emptyState.textContent =
    priorities.length ||
    searchInput.value.trim() ||
    completionFilter.value !== ""
      ? "Không có task phù hợp với bộ lọc."
      : "Chưa có task.";
}

function renderTasks() {
  list.replaceChildren();

  tasks.forEach((task) => {
    const item = createTaskItem(task, applyFilters, applyFilters);
    item.dataset.taskPriority = task.taskPriority ?? "";
    list.append(item);
  });

  list.append(emptyState);
  applyFilters();
}

async function loadTasks() {
  clearTimeout(searchTimer);
  const requestId = ++latestRequest;
  reloadButton.disabled = true;
  list.setAttribute("aria-busy", "true");
  error.textContent = "";
  try {
    const fetchedTasks = await getAllTasks(
      searchInput.value.trim(),
      undefined,
      undefined,
      sortSelect.value,
      completionFilter.value === ""
        ? undefined
        : completionFilter.value === "true",
      document.querySelector('input[name="sort-order"]:checked').value,
    );

    if (requestId !== latestRequest) return false;

    tasks = fetchedTasks;
    list.dataset.loaded = "true";
    renderTasks();
    return true;

  } catch (failure) {
    if (requestId !== latestRequest) return false;
    if (!list.dataset.loaded) list.replaceChildren();
    error.textContent =
      failure.status === 401
        ? "Bạn cần đăng nhập để xem task."
        : "Chưa tải được danh sách task. Bấm tải lại để thử lại.";
    return false;
    
  } finally {
    if (requestId === latestRequest) {
      reloadButton.disabled = false;
      list.removeAttribute("aria-busy");
    }
  }
}

const taskForm = setupTaskForm(loadTasks);
const categoryPage = setupCategoryPage(taskForm.setCategories);
reloadButton.addEventListener("click", loadTasks);

priorityFilters.addEventListener("change", () => {
  if (list.dataset.loaded) applyFilters();
});

sortSelect.addEventListener("change", loadTasks);

completionFilter.addEventListener("change", loadTasks);

searchInput.addEventListener("input", () => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(loadTasks, 300);
});

document.querySelectorAll('input[name="sort-order"]').forEach((radio) => {
  radio.addEventListener("change", loadTasks);
});

clearFiltersButton.addEventListener("click", () => {
  document.querySelector('input[name="sort-order"][value="desc"]').checked =
    true;
  priorityFilters
    .querySelectorAll('input[name="priority"]')
    .forEach((checkbox) => {
      checkbox.checked = false;
    });
  searchInput.value = "";
  sortSelect.value = "priority";
  completionFilter.value = "";
  loadTasks();
});

await categoryPage.loadCategories(); // tai lai category
await loadTasks(); // tai lai task
