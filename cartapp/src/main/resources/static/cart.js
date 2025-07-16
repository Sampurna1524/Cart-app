let cartId = null;

const cartItems = document.getElementById("cart-items");
const cartTotal = document.getElementById("cart-total");
const checkoutBtn = document.getElementById("checkout-btn");
const clearCartBtn = document.getElementById("clear-cart-btn");

// Toast Utility
function showToast(message) {
  const container = document.getElementById("toast-container");
  const toast = document.createElement("div");
  toast.className = "toast";
  toast.innerText = message;
  container.appendChild(toast);
  setTimeout(() => toast.remove(), 3000);
}

// Dark Mode
if (localStorage.getItem("darkMode") === "true") {
  document.body.classList.add("dark");
}
document.getElementById("dark-toggle").onclick = () => {
  document.body.classList.toggle("dark");
  localStorage.setItem("darkMode", document.body.classList.contains("dark"));
};

// Init Cart
async function initCart() {
  cartId = localStorage.getItem("cartId");

  if (!cartId) {
    const res = await fetch("/cart", { method: "POST" });
    const cart = await res.json();
    cartId = cart.id;
    localStorage.setItem("cartId", cartId);
  }

  // Only on index.html: loadProducts()
  if (typeof loadProducts === "function") {
    await loadProducts();
  }

  await loadCart();
}


async function loadCart() {
  const res = await fetch(`/cart/${cartId}`);
  const cart = await res.json();

  cartItems.innerHTML = "";
  if (!cart.items || cart.items.length === 0) {
    cartItems.innerHTML = "<p>Your cart is empty.</p>";
    cartTotal.textContent = "Total: ₹0";
    return;
  }

  cart.items.forEach(item => {
    const div = document.createElement("div");
    div.className = "cart-item";
    div.innerHTML = `
      <h4>${item.product.name}</h4>
      <span>₹${item.product.price} × 
        <input type="number" min="1" value="${item.quantity}" 
          onchange="updateQuantity(${item.product.id}, this.value)">
      </span>
      <button onclick="removeItem(${item.product.id})">❌</button>
    `;
    cartItems.appendChild(div);
  });

  const totalRes = await fetch(`/cart/${cartId}/total`);
  const total = await totalRes.text();
  cartTotal.textContent = `Total: ₹${parseFloat(total).toFixed(2)}`;
}

async function updateQuantity(productId, qty) {
  await fetch(`/cart/${cartId}/update?productId=${productId}&quantity=${qty}`, {
    method: "PUT"
  });
  await loadCart();
  showToast("📝 Quantity updated!");
}

async function removeItem(productId) {
  await fetch(`/cart/${cartId}/remove?productId=${productId}`, {
    method: "DELETE"
  });
  await loadCart();
  showToast("🗑️ Product removed!");
}

clearCartBtn.addEventListener("click", async () => {
  await fetch(`/cart/${cartId}/clear`, { method: "DELETE" });
  await loadCart();
  showToast("🧹 Cart cleared!");
});

checkoutBtn.addEventListener("click", async () => {
  await fetch(`/cart/${cartId}/checkout`, { method: "POST" });
  await loadCart();
  showToast("✅ Order placed successfully!");
});

window.addEventListener("DOMContentLoaded", initCart);
