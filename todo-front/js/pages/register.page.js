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
        await userApi.register(values.username, values.password);

        alert("Đăng ký thành công. Vui lòng đăng nhập lại.");
        window.location.href = "./login.html";
    } catch (error) {
        validation.showError(
            error.message || "Không thể đăng ký. Vui lòng thử lại."
        );
    } finally {
        submitButton.disabled = false;
    }
});
