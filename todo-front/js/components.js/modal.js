export function setupModals() {
    const dialogs = document.querySelectorAll("dialog");
    const status = document.querySelector("#page-status");
    const openers = new Map();

    document.querySelectorAll("[data-modal-target]").forEach((button) => {
        button.addEventListener("click", () => {
            const dialog = document.getElementById(button.dataset.modalTarget);
            dialogs.forEach((other) => {
                if (other.open && other !== dialog) other.close();
            });
            openers.set(dialog, button);
            if (!dialog.open) dialog.showModal();
            document.body.classList.add("modal-open");
        });
    });

    dialogs.forEach((dialog) => {
        dialog.addEventListener("keydown", (event) => {
            if (event.key === "Escape") {
                event.preventDefault();
                dialog.close();
            }
        });
        dialog.querySelectorAll("[data-close-modal]").forEach((button) => {
            button.addEventListener("click", () => dialog.close());
        });
        dialog.addEventListener("close", () => {
            const anotherOpen = Boolean(document.querySelector("dialog[open]"));
            document.body.classList.toggle("modal-open", anotherOpen);
            if (anotherOpen) return;
            const opener = openers.get(dialog);
            // Khi chuyển từ modal task sang category, nút mở cũ có thể đã bị ẩn.
            const target = opener?.getClientRects().length
                ? opener
                : document.querySelector(`main [data-modal-target="${dialog.id}"]`);
            target?.focus();
        });
        dialog.addEventListener("created", (event) => {
            status.textContent = event.detail.message;
            if (dialog.open) dialog.close();
        });
    });
}
