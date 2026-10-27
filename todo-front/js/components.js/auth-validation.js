export function setupAuthValidation(form) {

    const fields = ["username", "password"];
    const formError = form.querySelector("#form-error");

    function setFieldError(name, message) {
        const input = form.elements.namedItem(name);
        
        form.querySelector(`#${name}-error`).textContent = message;
        input.setAttribute("aria-invalid", message ? "true" : "false");
    }

    // sua thi mat loi bao 
    fields.forEach((name) => {
        form.elements.namedItem(name).addEventListener("input", () => {
            setFieldError(name, "");
            formError.textContent = "";
        });
    });

    return {
        validate() {
            formError.textContent = "";
            const username = form.elements.namedItem("username").value.trim();
            const password = form.elements.namedItem("password").value;
            const errors = {
                username: !username
                    ? "Vui lòng nhập tên đăng nhập."
                    : username.length < 3 || username.length > 100
                        ? "Tên đăng nhập phải có từ 3 đến 100 ký tự."
                        : "",
                password: !password.trim()
                    ? "Vui lòng nhập mật khẩu."
                    : password.length < 6
                        ? "Mật khẩu phải có ít nhất 6 ký tự."
                        : "",
            };

            //  hiển thị error ra giao diện 
            fields.forEach((name) => setFieldError(name, errors[name]));
            const firstInvalid = fields.find((name) => errors[name]);
            
            if (firstInvalid) {
                form.elements.namedItem(firstInvalid).focus();
                return null;
            }

            return { username, password };
        },
        showError(message) {
            formError.textContent = message;
        },
    };
}
