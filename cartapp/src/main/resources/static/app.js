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
const pageSize = 10; // Change as needed


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

  if (typeof loadProducts === "function") await loadProducts(0);

  if (isCartPage) await loadCart();

  updateCartCountBadge();
}


// ========== Products ==========
async function loadProducts(page = 0) {
  if (!productList) return; // 💡 Prevents running on pages without productList

  currentPage = page;
  const query = searchInput?.value?.trim().toLowerCase() || "";
  const category = categorySelect?.value || "";
  const sort = sortSelect?.value || "";

  let url = `/products?page=${page}&size=${pageSize}`;
  if (query) url += `&search=${encodeURIComponent(query)}`;
  if (category && category !== "all") url += `&category=${encodeURIComponent(category)}`;
  if (sort) url += `&sort=${encodeURIComponent(sort)}`;

  try {
    const res = await fetch(url, {
      headers: { ...authHeaders(), Accept: "application/json" },
    });

    if (!res.ok) throw new Error(await res.text());

    const { content, totalPages } = await res.json();
allProducts = content || [];
sessionStorage.setItem("allProducts", JSON.stringify(allProducts)); // 👈 ADD THIS


    displayProducts(content);
    renderPaginationControls(totalPages, page);
  } catch (e) {
    console.error("❌ Error loading products:", e);
    productList.innerHTML = "<p>⚠️ Failed to load products.</p>";
  }
}



async function populateCategories() {
  if (!categorySelect) return;

  try {
    const res = await fetch("/products/categories", {
      headers: { ...authHeaders(), Accept: "application/json" }
    });

    if (!res.ok) throw new Error(await res.text());
    const categories = await res.json();

    categorySelect.innerHTML = `<option value="all">📂 All Categories</option>`;
    categories.sort().forEach(cat => {
      const option = document.createElement("option");
      option.value = cat.toLowerCase();
      option.textContent = `📁 ${cat}`;
      categorySelect.appendChild(option);
    });
  } catch (e) {
    console.error("❌ Error loading categories from server:", e);
  }
}


function applyFilters() {
  loadProducts(0); // Re-fetch products from backend starting from page 0 with current filters
}



function displayProducts(products) {
  productList.innerHTML = "";
  if (products.length === 0) {
    productList.innerHTML = "<p>No products found.</p>";
    return;
  }

   const wishlist = getWishlist(); // ✅ Get latest wishlist

  products.forEach((p) => {
    const isWishlisted = wishlist.includes(p.id); // ✅

    const card = document.createElement("div");
    card.className = "product-card";
    const img = p.imageUrl || "https://via.placeholder.com/200";

    let stockMsg = "";
    if (p.quantity <= 0) {
      stockMsg = `<p style="color: gray;">Out of Stock</p>`;
    } else if (p.quantity <= 5) {
      stockMsg = `<p style="color: red;">Only ${p.quantity} left in stock!</p>`;
    }

    const quantityInput = p.quantity > 0
      ? `<label for="qty-${p.id}" style="font-size: 0.9rem;">Qty:</label>
         <input type="number" id="qty-${p.id}" min="1" max="${p.quantity}" value="1" 
                style="width: 100%; padding: 6px; font-size: 0.9rem; border-radius: 6px; border: 1px solid #ccc;" />`
      : "";

    const addToCartBtn = `
      <button
        onclick="handleAddToCart(${p.id}); event.stopPropagation();"
        ${p.quantity <= 0 ? "disabled class='out-of-stock-btn'" : ""}
        style="padding: 8px; font-size: 0.95rem; border: none; border-radius: 6px; font-weight: 600;"
      >
        Add to Cart
      </button>`;

    const wishlistBtn = `
      <div style="display: flex; justify-content: center; margin-top: 8px;">
        <button onclick="toggleWishlist(${p.id}); event.stopPropagation();" 
                class="wishlist-btn" 
                style="
                  padding: 4px 8px;
                  font-size: 1.2rem;
                  border-radius: 8px;
                  background-color: transparent;
                  border: none;
                  cursor: pointer;
                ">
          ${isWishlisted ? "💖" : "🤍"}
        </button>
      </div>`;

    card.innerHTML = `
      <a href="/product.html?id=${p.id}" class="product-link">
        <img src="${img}" alt="${p.name}" class="product-image" />
        <h3>${p.name}</h3>
        <p class="category-tag">📁 ${p.category || "Uncategorized"}</p>
        <p>₹${p.price.toFixed(2)}</p>
        ${stockMsg}
      </a>
      <div class="product-actions" style="display: flex; flex-direction: column; gap: 8px; margin-top: 10px;">
        ${quantityInput}
        ${addToCartBtn}
        ${wishlistBtn}
      </div>
    `;
    productList.appendChild(card);
  });
}

function renderPaginationControls(totalPages, currentPage) {
  const container = document.getElementById("pagination-controls");
  if (!container) return;
  container.innerHTML = "";

  for (let i = 0; i < totalPages; i++) {
    const btn = document.createElement("button");
    btn.innerText = i + 1;
    btn.style.padding = "8px 12px";
    btn.style.borderRadius = "8px";
    btn.style.border = "none";
    btn.style.cursor = "pointer";
    btn.style.margin = "0 4px";
    btn.style.background = i === currentPage ? "#333" : "#eee";
    btn.style.color = i === currentPage ? "#fff" : "#333";

    // 🔧 FIX: Call loadProducts(i) directly
    btn.onclick = () => {
      loadProducts(i);
    };

    container.appendChild(btn);
  }
}




// ========== Cart ==========
async function handleAddToCart(productId) {
  const token = sessionStorage.getItem("jwt");
  if (!token) {
    alert("🔒 Please login to add to cart.");
    window.location.href = "/login.html";
    return;
  }
  await addToCart(productId);
}

async function addToCart(productId) {
  try {
    const qtyInput = document.getElementById(`qty-${productId}`);
    const quantity = qtyInput ? parseInt(qtyInput.value) : 1;

    if (!quantity || quantity <= 0) {
      showToast("⚠️ Please enter a valid quantity");
      return;
    }

    await fetch(`/cart/${cartId}/add?productId=${productId}&quantity=${quantity}`, {
      method: "POST",
      headers: authHeaders(),
    });

    if (isCartPage) await loadCart();
    showToast(`🛒 Added ${quantity} item(s) to cart`);
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
categorySelect?.addEventListener("change", applyFilters);
sortSelect?.addEventListener("change", applyFilters);

// ========== Start ==========
window.addEventListener("DOMContentLoaded", () => {
  populateCategories();
  initCart();
  updateCartCountBadge();
  
  // 👉 Call wishlist rendering if on that page
  if (document.getElementById("wishlist-container")) {
    renderWishlist();
  }
});



// ========== Wishlist ==========
function toggleWishlist(productId) {
  let wishlist = getWishlist();
  if (wishlist.includes(productId)) {
    wishlist = wishlist.filter(id => id !== productId);
    showToast("💔 Removed from wishlist");
  } else {
    wishlist.push(productId);
    showToast("💖 Added to wishlist");
  }
  saveWishlist(wishlist);
  displayProducts(allProducts); // 🔁 Refresh UI to update heart icons
}

function getWishlist() {
  return JSON.parse(localStorage.getItem("wishlist") || "[]");
}

function saveWishlist(wishlist) {
  localStorage.setItem("wishlist", JSON.stringify(wishlist));
}

async function renderWishlist() {
  const wishlistContainer = document.getElementById("wishlist-container");
  if (!wishlistContainer) return;

  const wishlist = getWishlist();

  try {
    const res = await fetch(`/products/all`, {
      headers: authHeaders()
    });

    if (!res.ok) throw new Error("Failed to load all products");

    const allProducts = await res.json();
    const wishlistItems = allProducts.filter(p => wishlist.includes(p.id));
    wishlistContainer.innerHTML = "";

    if (wishlistItems.length === 0) {
      wishlistContainer.innerHTML = "<p>Your wishlist is empty.</p>";
      return;
    }

    wishlistItems.forEach((p) => {
      const img = p.imageUrl || "https://via.placeholder.com/200";
      const card = document.createElement("div");
      card.className = "wishlist-card";
      card.innerHTML = `
        <a href="/product.html?id=${p.id}" class="wishlist-link">
          <img src="${img}" alt="${p.name}" class="wishlist-image" />
          <h3>${p.name}</h3>
          <p>₹${p.price.toFixed(2)}</p>
        </a>
        <button onclick="toggleWishlist(${p.id})">Remove 💔</button>
      `;
      wishlistContainer.appendChild(card);
    });

  } catch (e) {
    console.error("❌ Failed to load wishlist:", e);
    wishlistContainer.innerHTML = "<p>⚠️ Could not load wishlist. Please try again.</p>";
  }
}

