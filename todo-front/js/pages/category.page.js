import { getAllCategories } from "../api/CategoryApi.js";
import { setupCategoryForm } from "../components.js/category-form.js";

export function setupCategoryPage(onCategoriesLoaded) {
    const list = document.querySelector("#category-list");
    const error = document.querySelector("#category-list-error");
    const button = document.querySelector("#reload-categories");
    let loading = false;

    async function loadCategories(selectedName) {
        const request = (async () => {
            button.disabled = true;
            error.textContent = "";
            try {
                const categories = await getAllCategories();
                list.dataset.loaded = "true";
                list.replaceChildren();
                if (!categories.length) {
                    const item = document.createElement("li");
                    item.textContent = "Chưa có category. Hãy tạo category đầu tiên.";
                    list.append(item);
                }

                categories.forEach((category) => {
                    const item = document.createElement("li");
                    item.textContent = category.name;
                    list.append(item);
                });
                
                onCategoriesLoaded(categories, selectedName);
                return true;
            } catch (failure) {
                if (!list.dataset.loaded) {
                    list.replaceChildren();
                    document.querySelector("#task-hint").textContent =
                        "Chưa tải được category. Bạn vẫn có thể tạo task không có category.";
                }
                error.textContent = failure.status === 401
                    ? "Bạn cần đăng nhập để xem category."
                    : "Chưa tải được danh sách category. Bấm tải lại để thử lại.";
                return false;
            } finally {
                button.disabled = false;
            }
        })();
        loading = request;
        try {
            return await request;
        } finally {
            loading = false;
        }
    }

    setupCategoryForm(loadCategories);
    button.addEventListener("click", () => loadCategories());
    return { loadCategories };
}
