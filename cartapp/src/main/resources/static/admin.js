document.addEventListener("DOMContentLoaded", () => {
  const productList = document.getElementById("admin-product-list");
  const addForm = document.getElementById("upload-form");
  const nameInput = document.querySelector('input[name="name"]');
  const categoryInput = document.querySelector('input[name="category"]');
  const priceInput = document.querySelector('input[name="price"]');
  const quantityInput = document.querySelector('input[name="quantity"]'); // ✅ NEW: for stock
  const imageInput = document.getElementById("product-image");
  const imagePreview = document.getElementById("image-preview");

  const token = sessionStorage.getItem("jwt");

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
    try {
      const res = await fetch("/products");
      const products = await res.json();

      productList.innerHTML = "";
      products.sort((a, b) => a.name.localeCompare(b.name));

      products.forEach(product => {
        const isLow = product.quantity <= 5;
        const isOut = product.quantity <= 0;

        const card = document.createElement("div");
        card.className = "product-card";
        card.innerHTML = `
  ${product.imageUrl ? `<img src="${product.imageUrl}" alt="${product.name}" class="product-image"/>` : ''}
  <div class="product-content">
    <h3>${product.name}</h3>
    <p>₹${product.price.toFixed(2)}</p>
    <p style="font-size: 0.9rem; color: #666;">📦 ${product.category || "Uncategorized"}</p>
    <p style="font-weight: bold; color: ${isOut ? 'gray' : isLow ? 'red' : 'green'};">
      ${isOut ? 'Out of Stock' : `Stock: ${product.quantity}`}
    </p>
    <button onclick="restockProduct(${product.id})" class="btn">➕ Restock</button>
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
    const quantity = parseInt(quantityInput.value); // ✅ NEW: quantity
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

    const productData = { name, category, price, quantity, imageUrl };

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

      nameInput.value = "";
      categoryInput.value = "";
      priceInput.value = "";
      quantityInput.value = "";
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
    window.restockProduct = async function (productId) {
    const amount = prompt("Enter quantity to add:");

    if (!amount || isNaN(amount) || parseInt(amount) <= 0) {
      showToast("⚠️ Invalid restock quantity.");
      return;
    }

    try {
      const res = await fetch(`/products/${productId}/restock?quantityToAdd=${parseInt(amount)}`, {
        method: "PATCH",
        headers: {
          ...authHeaders(),
        }
      });

      if (res.status === 403) {
        console.error("🔒 403 Forbidden - likely missing or invalid JWT token.");
        showToast("❌ Unauthorized. Please login again.");
        return;
      }

      if (!res.ok) {
        const errorMsg = await res.text();
        console.error("❌ Failed to restock:", errorMsg);
        showToast("❌ Failed to restock product.");
        return;
      }

      showToast("✅ Product restocked!");
      await loadProducts();
    } catch (err) {
      console.error("⚠️ Restock error:", err);
      showToast("⚠️ Error restocking product.");
    }
  };



  loadProducts();
});
