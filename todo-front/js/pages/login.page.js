import { userApi } from "../api/UserApi.js";
import { setupAuthValidation } from "../components.js/auth-validation.js";

const form = document.querySelector("form");
const submitButton = form.querySelector("button[type=submit]");
const validation = setupAuthValidation(form);

form.addEventListener("submit", async (event) => {
    event.preventDefault();

    if (submitButton.disabled) return;
    const values = validation.validate();
    if (!values) return;

    submitButton.disabled = true;

    try {
        await userApi.login(values.username, values.password);

        // Trình duyệt lưu cookie session do backend trả về.
        window.location.href = "./index.html";
    } catch (error) {
        validation.showError(
            error.status === 401
                ? "Sai tên đăng nhập hoặc mật khẩu."
                : error.message || "Không thể đăng nhập. Vui lòng thử lại."
        );
    } finally {
        submitButton.disabled = false;
    }
});
