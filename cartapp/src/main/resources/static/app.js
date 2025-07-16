const productList = document.getElementById("product-list");
const searchInput = document.getElementById("search-input");
const categorySelect = document.getElementById("category-select");
const sortSelect = document.getElementById("sort-select");
const cartItems = document.getElementById("cart-items");
const cartTotal = document.getElementById("cart-total");
const checkoutBtn = document.getElementById("checkout-btn");
const clearCartBtn = document.getElementById("clear-cart-btn");

let cartId = null;
let allProducts = [];
let currentPage = 0;
let totalPages = 1;
const pageSize = 10;
let activeCategory = "all"; // 🆕 Track selected category


const isCartPage = cartItems && cartTotal;

function authHeaders() {
  const token = sessionStorage.getItem("jwt");
  return token ? { Authorization: `Bearer ${token}` } : {};
}

// ========== Toast ==========
function showToast(message) {
  let container = document.getElementById("toast-container");
  if (!container) {
    container = document.createElement("div");
    container.id = "toast-container";
    document.body.appendChild(container);
  }
  const toast = document.createElement("div");
  toast.className = "toast";
  toast.innerText = message;
  container.appendChild(toast);
  setTimeout(() => toast.remove(), 3000);
}

// ========== Cart Count Badge ==========
function updateCartCountBadge() {
  const badge = document.getElementById("cart-count");
  if (!badge) {
    console.warn("⚠️ 'cart-count' element not found in DOM.");
    return;
  }

  const currentCartId = cartId || sessionStorage.getItem("cartId");
  if (!currentCartId) {
    console.warn("⚠️ No cart ID found. Skipping badge update.");
    badge.textContent = "0";
    return;
  }

  fetch(`/cart/${currentCartId}`, { headers: authHeaders() })
    .then(res => {
      if (!res.ok) throw new Error("Failed to fetch cart");
      return res.json();
    })
    .then(cart => {
      const count = cart.items?.reduce((sum, item) => sum + item.quantity, 0) || 0;
      badge.textContent = count;
      console.log("🟢 Cart badge updated:", count);
    })
    .catch(err => {
      console.error("❌ Failed to update cart badge:", err);
    });
}

// ========== Dark Mode ==========
function toggleDarkMode() {
  document.body.classList.toggle("dark");
  localStorage.setItem("darkMode", document.body.classList.contains("dark"));
}
if (localStorage.getItem("darkMode") === "true") document.body.classList.add("dark");

const toggleBtn = document.createElement("button");
toggleBtn.id = "dark-toggle";
toggleBtn.title = "Toggle Dark Mode";
toggleBtn.innerText = "🌓";
toggleBtn.onclick = toggleDarkMode;
document.body.appendChild(toggleBtn);

// ========== Init ==========
async function initCart() {
  const token = sessionStorage.getItem("jwt");
let user = null;
try {
  user = JSON.parse(sessionStorage.getItem("user"));
} catch (e) {
  console.warn("Invalid user JSON:", e);
}

  
  if (!token || !user) {
  console.warn("User not logged in. Redirecting...");
  window.location.href = "/login.html";
  return;
}


  // Try getting existing cart from backend
  try {
    const res = await fetch(`/cart/user/${user.id}`, {
      headers: authHeaders()
    });

    if (res.ok) {
      const cart = await res.json();
      cartId = cart.id;
      sessionStorage.setItem("cartId", cartId);
      console.log("✅ Existing cart loaded:", cartId);
    } else if (res.status === 404) {
  // If no cart exists, create one
  const createRes = await fetch(`/cart?userId=${user.id}`, {
    method: "POST",
    headers: authHeaders()
  });
  if (!createRes.ok) throw new Error("Failed to create cart");
  const newCart = await createRes.json();
  cartId = newCart.id;
  sessionStorage.setItem("cartId", cartId);
  console.log("🆕 New cart created:", cartId);
}
 else {
      throw new Error("Failed to fetch cart");
    }
  } catch (e) {
    console.error("❌ Cart init failed:", e);
    return;
  }

  if (typeof loadCategories === "function") await loadCategories();
  if (typeof loadProducts === "function") await loadProducts();

  if (isCartPage) await loadCart();

  updateCartCountBadge();
}


// ========== Products ==========
async function loadProducts(page = 0) {
  const query = searchInput?.value?.trim() || "";
  const category = categorySelect?.value || "all";
  const sort = sortSelect?.value || "";

  let url = `/products?page=${page}&size=${pageSize}`;

  if (category !== "all") url += `&category=${encodeURIComponent(category)}`;
  if (query) url += `&search=${encodeURIComponent(query)}`;
  if (sort) url += `&sort=${encodeURIComponent(sort)}`;

  try {
    const res = await fetch(url, {
      headers: { ...authHeaders(), Accept: "application/json" },
    });
    if (!res.ok) throw new Error(await res.text());

    const data = await res.json();
    allProducts = data.content;
    totalPages = data.totalPages;
    currentPage = data.number;

    displayProducts(allProducts); // ✅ no need to apply filters on frontend now
    updatePaginationControls();
  } catch (e) {
    console.error("❌ Error loading products:", e);
    productList.innerHTML = "<p>⚠️ Failed to load products.</p>";
  }
}


function updatePaginationControls() {
  const info = document.getElementById("page-info");
  const prev = document.getElementById("prev-page");
  const next = document.getElementById("next-page");

  if (info) info.textContent = `Page ${currentPage + 1} of ${totalPages}`;
  if (prev) prev.disabled = currentPage === 0;
  if (next) next.disabled = currentPage >= totalPages - 1;
}

document.getElementById("prev-page")?.addEventListener("click", () => {
  if (currentPage > 0) loadProducts(currentPage - 1);
});

document.getElementById("next-page")?.addEventListener("click", () => {
  if (currentPage < totalPages - 1) loadProducts(currentPage + 1);
});


// ========== Load Categories from Backend ==========
async function loadCategories() {
  try {
    const res = await fetch("/products/categories", {
      headers: { ...authHeaders(), Accept: "application/json" },
    });
    if (!res.ok) throw new Error("Failed to load categories");
    const categories = await res.json();

    if (categorySelect) {
      categorySelect.innerHTML = `<option value="all">📂 All Categories</option>`;
      categories.forEach((cat) => {
        const option = document.createElement("option");
        option.value = cat.toLowerCase();
        option.textContent = cat;
        categorySelect.appendChild(option);
      });

      // Restore from localStorage
      const savedCategory = localStorage.getItem("activeCategory");
      if (savedCategory) {
        categorySelect.value = savedCategory;
        activeCategory = savedCategory;
      }
    }
  } catch (e) {
    console.error("❌ Failed to load categories:", e);
  }
}


function applyFilters() {
  localStorage.setItem("activeCategory", categorySelect?.value || "all");
  loadProducts(0); // Always load page 0 when filter/search/sort changes
}


function displayProducts(products) {
  if (!productList) {
    console.warn("⚠️ No #product-list element found.");
    return;
  }

  productList.innerHTML = "";
  if (products.length === 0) {
    productList.innerHTML = "<p>No products found.</p>";
    return;
  }

  products.forEach((p) => {
    const card = document.createElement("div");
    card.className = "product-card";
    const img = p.imageUrl || "https://via.placeholder.com/200";

    const disabled = p.quantity === 0;
    const quantityInputId = `quantity-${p.id}`;

    card.innerHTML = `
      <a href="/product.html?id=${p.id}" class="product-link">
        <img src="${img}" alt="${p.name}" class="product-image" />
        <h3>${p.name}</h3>
        <p class="category-tag">📁 ${p.category || "Uncategorized"}</p>
        <p>₹${p.price.toFixed(2)}</p>
      </a>

      <div class="product-quantity-wrapper">
        <label for="${quantityInputId}">Quantity:</label>
        <input type="number" id="${quantityInputId}" class="quantity-input" min="1" max="${p.quantity}" value="1" ${disabled ? "disabled" : ""}>
        <small class="stock-info">${p.quantity > 0 ? `🧾 ${p.quantity} left` : "❌ Out of stock"}</small>
      </div>

      <div class="product-actions">
        <button ${disabled ? "disabled" : ""} onclick="handleAddToCart(${p.id}); event.stopPropagation();">Add to Cart</button>
        <button onclick="toggleWishlist(${p.id}); event.stopPropagation();" class="wishlist-btn">💖</button>
      </div>
    `;
    productList.appendChild(card);
  });
}


// ========== Cart ==========
async function handleAddToCart(productId) {
  const token = sessionStorage.getItem("jwt");
  if (!token) {
    alert("🔒 Please login to add to cart.");
    window.location.href = "/login.html";
    return;
  }

  const qtyInput = document.getElementById(`quantity-${productId}`);
  let quantity = parseInt(qtyInput?.value || "1");

  if (isNaN(quantity) || quantity < 1) quantity = 1;
  if (qtyInput && quantity > parseInt(qtyInput.max)) {
    showToast("⚠️ Not enough stock");
    return;
  }

  await addToCart(productId, quantity);
}


async function addToCart(productId, quantity) {
  try {
    await fetch(`/cart/${cartId}/add?productId=${productId}&quantity=${quantity}`, {
      method: "POST",
      headers: authHeaders(),
    });
    if (isCartPage) await loadCart();
    showToast("🛒 Added to cart");
    updateCartCountBadge();
  } catch (e) {
    console.error("❌ Add to cart failed:", e);
    showToast("⚠️ Could not add to cart");
  }
}


async function loadCart() {
  try {
    const res = await fetch(`/cart/${cartId}`, { headers: authHeaders() });
    const cart = await res.json();
    cartItems.innerHTML = "";

    if (!cart.items || cart.items.length === 0) {
      cartItems.innerHTML = "<p>Your cart is empty.</p>";
      cartTotal.textContent = "Total: ₹0";
      return;
    }

    cart.items.forEach((item) => {
      const div = document.createElement("div");
      div.className = "cart-item";
      div.innerHTML = `
        <h4>${item.product.name}</h4>
        <span>₹${item.product.price} × 
          <input type="number" min="1" value="${item.quantity}" onchange="updateQuantity(${item.product.id}, this.value)">
        </span>
        <button onclick="removeItem(${item.product.id})">❌</button>
      `;
      cartItems.appendChild(div);
    });

    const totalRes = await fetch(`/cart/${cartId}/total`, { headers: authHeaders() });
    const total = await totalRes.text();
    cartTotal.textContent = `Total: ₹${parseFloat(total).toFixed(2)}`;
  } catch (e) {
    console.error("❌ Load cart failed:", e);
  }
}

async function updateQuantity(productId, qty) {
  try {
    await fetch(`/cart/${cartId}/update?productId=${productId}&quantity=${qty}`, {
      method: "PUT",
      headers: authHeaders(),
    });
    if (isCartPage) await loadCart();
    showToast("Quantity updated");
    updateCartCountBadge();
  } catch (e) {
    console.error("❌ Update quantity failed:", e);
  }
}

async function removeItem(productId) {
  try {
    await fetch(`/cart/${cartId}/remove?productId=${productId}`, {
      method: "DELETE",
      headers: authHeaders(),
    });
    if (isCartPage) await loadCart();
    showToast("Item removed");
    updateCartCountBadge();
  } catch (e) {
    console.error("❌ Remove failed:", e);
  }
}

if (isCartPage) {
  clearCartBtn?.addEventListener("click", async () => {
    await fetch(`/cart/${cartId}/clear`, {
      method: "DELETE",
      headers: authHeaders(),
    });
    await loadCart();
    showToast("Cart cleared");
    updateCartCountBadge();
  });

  checkoutBtn?.addEventListener("click", async () => {
  const user = JSON.parse(sessionStorage.getItem("user"));
  if (!user) {
    alert("User not logged in");
    return;
  }

  const addressLine = document.getElementById("address-line")?.value;
  const city = document.getElementById("city")?.value;
  const state = document.getElementById("state")?.value;
  const zip = document.getElementById("zipcode")?.value;
  const paymentMethod = document.querySelector('input[name="payment"]:checked')?.value;

  if (!addressLine || !city || !state || !zip || !paymentMethod) {
    alert("❌ Please fill all address fields and select a payment method.");
    return;
  }

  const shippingAddress = `${addressLine}, ${city}, ${state} - ${zip}`;

  try {
    const res = await fetch(`/orders/place?cartId=${cartId}&userId=${user.id}&shippingAddress=${encodeURIComponent(shippingAddress)}&paymentMethod=${paymentMethod}`, {
      method: "POST",
      headers: authHeaders(),
    });

    const result = await res.text();

    if (!res.ok) {
      alert("❌ Checkout failed: " + result);
      return;
    }

    showToast("✅ " + result);
    await loadCart();
    updateCartCountBadge();
  } catch (e) {
    console.error("❌ Checkout error:", e);
    showToast("⚠️ Failed to place order");
  }
});

}

// ========== Event Listeners ==========
searchInput?.addEventListener("input", applyFilters);
categorySelect?.addEventListener("change", () => {
  activeCategory = categorySelect.value;
  localStorage.setItem("activeCategory", activeCategory);
  applyFilters();
});
sortSelect?.addEventListener("change", applyFilters);


// ========== Start ==========
window.addEventListener("DOMContentLoaded", () => {
  initCart();
  updateCartCountBadge(); // ✅ fallback trigger
});

// ========== Wishlist ==========
function toggleWishlist(productId) {
  let wishlist = JSON.parse(localStorage.getItem("wishlist") || "[]");
  if (wishlist.includes(productId)) {
    wishlist = wishlist.filter((id) => id !== productId);
    showToast("💔 Removed from wishlist");
  } else {
    wishlist.push(productId);
    showToast("💖 Added to wishlist");
  }
  localStorage.setItem("wishlist", JSON.stringify(wishlist));
}



