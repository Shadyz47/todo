import { createCategory } from "../api/CategoryApi.js";

export function setupCategoryForm(onCreated) {
    const form = document.querySelector("#category-form");
    const nameInput = form.elements.namedItem("name");
    const button = form.querySelector('button[type="submit"]');

    const error = form.querySelector(".error");
    const success = form.querySelector(".success");

    nameInput.addEventListener("input", () => {
        error.textContent = "";
        success.textContent = "";
        nameInput.removeAttribute("aria-invalid");
    });

    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        if (button.disabled) return;
        
        error.textContent = "";
        success.textContent = "";
        
        const name = nameInput.value.trim();

        if (!name) {
            error.textContent = "Vui lòng nhập tên category.";
            nameInput.setAttribute("aria-invalid", "true");
            nameInput.focus();
            return;
        }

        button.disabled = true;

        //tạo category
        try {
            await createCategory({ name });
        } catch (failure) {
            error.textContent = failure.status === 401
                ? "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
                : failure.status === 409
                    ? "Tên category đã tồn tại."
                    : failure.message;
            button.disabled = false;
            return;
        }

        form.reset();
        success.textContent = "Đã tạo category.";
        
        // tao thanh cong -> get lai danh sach
        try {
            const refreshed = await onCreated(name);
            if (refreshed !== false) {
                form.dispatchEvent(new CustomEvent("created", {
                    bubbles: true,
                    detail: { message: "Đã tạo category. Bạn có thể chọn category này khi tạo task." },
                }));
            }
        } finally {
            button.disabled = false;
        }
    });
}
