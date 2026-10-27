import { apiRequest } from "./UserApi.js";

export async function getAllCategories() {
    return await apiRequest("/categories");
}

export async function createCategory(category) {
    return await apiRequest("/categories", { method: "POST", body: category });
}

export async function updateCategory(categoryId, category) {
    return await apiRequest(`/categories/${categoryId}`, { method: "PUT", body: category });
}

export async function deleteCategory(categoryId) {
    return await apiRequest(`/categories/${categoryId}`, { method: "DELETE" });
}
