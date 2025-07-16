document.addEventListener("DOMContentLoaded", () => {
  const productList = document.getElementById("admin-product-list");
  const addForm = document.getElementById("upload-form");
  const nameInput = document.querySelector('input[name="name"]');
  const categoryInput = document.querySelector('input[name="category"]');
  const priceInput = document.querySelector('input[name="price"]');
  const quantityInput = document.querySelector('input[name="quantity"]'); // ✅ NEW
  const imageInput = document.getElementById("product-image");
  const imagePreview = document.getElementById("image-preview");
  const toggleArchivedBtn = document.getElementById("toggle-archived");

  const token = sessionStorage.getItem("jwt");
  let isArchivedMode = false;

  function authHeaders() {
    return token ? { Authorization: `Bearer ${token}` } : {};
  }

  function showToast(msg) {
    const container = document.getElementById("toast-container");
    const toast = document.createElement("div");
    toast.className = "toast";
    toast.innerText = msg;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 2500);
  }

  async function loadProducts() {
    const url = isArchivedMode ? "/products/archived" : "/products";
    try {
      const res = await fetch(url, { headers: authHeaders() });
      const data = await res.json();
      const products = Array.isArray(data) ? data : data.content;

      productList.innerHTML = "";
      products.sort((a, b) => a.name.localeCompare(b.name));

      products.forEach(product => {
        const isArchived = !product.visible;
        const card = document.createElement("div");
        card.className = "product-card";
        card.innerHTML = `
          ${product.imageUrl ? `<img src="${product.imageUrl}" alt="${product.name}" class="product-image"/>` : ''}
          <div class="product-content">
            <h3>${product.name}</h3>
            <p>₹${product.price.toFixed(2)}</p>
            <p>📦 ${product.category || "Uncategorized"}</p>
            <p>🧮 Quantity: ${product.quantity}</p>

            ${
              isArchived
                ? `<button onclick="unarchiveProduct(${product.id})" class="btn primary">♻️ Unarchive</button>`
                : `<button onclick="archiveProduct(${product.id})" class="btn primary">📥 Archive</button>`
            }

            <div style="margin: 8px 0;">
              <input type="number" id="restock-${product.id}" min="1" placeholder="Restock amount" style="padding:6px; width: 65%; margin-bottom: 6px;" />
              <button onclick="restockProduct(${product.id})" class="btn primary">➕ Restock</button>
            </div>

            <button onclick="deleteProduct(${product.id})" class="btn danger">❌ Delete</button>
          </div>
        `;
        productList.appendChild(card);
      });
    } catch (err) {
      console.error("Error loading products", err);
      showToast("⚠️ Failed to load products");
    }
  }

  toggleArchivedBtn?.addEventListener("click", () => {
    isArchivedMode = !isArchivedMode;
    toggleArchivedBtn.textContent = isArchivedMode ? "📦 Show Active" : "📦 Show Archived";
    loadProducts();
  });

  imageInput.addEventListener("change", () => {
    const file = imageInput.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = () => {
        imagePreview.src = reader.result;
        imagePreview.style.display = "block";
      };
      reader.readAsDataURL(file);
    } else {
      imagePreview.src = "";
      imagePreview.style.display = "none";
    }
  });

  addForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    const name = nameInput.value.trim();
    const category = categoryInput.value.trim();
    const price = parseFloat(priceInput.value);
    const quantity = parseInt(quantityInput.value); // ✅ NEW
    const file = imageInput.files[0];

    if (!name || !category || isNaN(price) || isNaN(quantity)) {
      showToast("⚠️ Please fill all required fields.");
      return;
    }

    let imageUrl = "";

    if (file) {
      const formData = new FormData();
      formData.append("file", file);

      try {
        const uploadRes = await fetch("/products/upload", {
          method: "POST",
          headers: authHeaders(),
          body: formData,
        });

        if (uploadRes.ok) {
          imageUrl = await uploadRes.text();
        } else {
          showToast("❌ Failed to upload image.");
          return;
        }
      } catch (err) {
        console.error("Upload error", err);
        showToast("⚠️ Error uploading image.");
        return;
      }
    }

    const productData = { name, category, price, quantity, imageUrl }; // ✅ quantity included

    try {
      const res = await fetch("/products", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...authHeaders(),
        },
        body: JSON.stringify(productData),
      });

      if (!res.ok) {
        showToast("❌ Failed to add product");
        return;
      }

      showToast("✅ Product added");

      // Reset form
      nameInput.value = "";
      categoryInput.value = "";
      priceInput.value = "";
      quantityInput.value = ""; // ✅ Reset quantity
      imageInput.value = "";
      imagePreview.src = "";
      imagePreview.style.display = "none";

      await loadProducts();
    } catch (err) {
      console.error("Add product error", err);
      showToast("⚠️ Error adding product.");
    }
  });

  window.deleteProduct = async function (productId) {
    if (!confirm("Are you sure you want to delete this product?")) return;

    try {
      const res = await fetch(`/products/${productId}`, {
        method: "DELETE",
        headers: authHeaders(),
      });

      if (!res.ok) {
        showToast("❌ Failed to delete product");
        return;
      }

      showToast("🗑️ Product deleted");
      await loadProducts();
    } catch (err) {
      console.error("Delete error", err);
      showToast("⚠️ Error deleting product.");
    }
  };

  window.archiveProduct = async function (id) {
    try {
      const res = await fetch(`/products/${id}/archive`, {
        method: "PUT",
        headers: authHeaders(),
      });
      if (res.ok) {
        showToast("📥 Product archived");
        await loadProducts();
      } else {
        showToast("❌ Failed to archive");
      }
    } catch (err) {
      console.error(err);
      showToast("⚠️ Error archiving product");
    }
  };

  window.unarchiveProduct = async function (id) {
    try {
      const res = await fetch(`/products/${id}/unarchive`, {
        method: "PUT",
        headers: authHeaders(),
      });
      if (res.ok) {
        showToast("♻️ Product unarchived");
        await loadProducts();
      } else {
        showToast("❌ Failed to unarchive");
      }
    } catch (err) {
      console.error(err);
      showToast("⚠️ Error unarchiving product");
    }
  };

  window.restockProduct = async function (id) {
    const input = document.getElementById(`restock-${id}`);
    const amount = parseInt(input.value);
    if (!amount || amount <= 0) {
      showToast("⚠️ Enter valid restock amount");
      return;
    }

    try {
      const res = await fetch(`/products/${id}/restock?amount=${amount}`, {
        method: "PUT",
        headers: authHeaders(),
      });
      if (res.ok) {
        showToast(`➕ Restocked by ${amount}`);
        await loadProducts();
      } else {
        showToast("❌ Failed to restock");
      }
    } catch (err) {
      console.error(err);
      showToast("⚠️ Error restocking product");
    }
  };

  loadProducts();
});
