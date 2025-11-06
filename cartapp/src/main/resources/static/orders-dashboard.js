console.log("📊 Order Dashboard booting…");

let categoryChart = null;
let orderTrendChart = null;

// Get backend URL automatically (http://localhost:8080)
function getBaseUrl() {
  return window.location.origin;
}

// ✅ Get userId stored during login (login.html stores userId)
function getUserIdFromSession() {
  const userId = sessionStorage.getItem("userId");
  return userId && userId !== "undefined" ? Number(userId) : null;
}

// ✅ Try /users/me if userId missing (uses JWT)
async function getUserIdViaMe() {
  const token = sessionStorage.getItem("jwt"); // ✅ FIXED

  if (!token) return null;

  try {
    const res = await fetch(`${getBaseUrl()}/users/me`, {
      headers: { Authorization: `Bearer ${token}` }
    });

    if (!res.ok) return null;

    const data = await res.json();
    sessionStorage.setItem("userId", data.id);
    return data.id;
  } catch {
    return null;
  }
}

// ✅ Initialize dashboard
async function initDashboard() {
  let userId = getUserIdFromSession();

  if (!userId) {
    userId = await getUserIdViaMe();
  }

  if (!userId) {
    alert("⚠️ Please login first to view insights.");
    return; // ✅ Don't redirect. Let user manually click Login
  }

  document.querySelectorAll(".filter-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      document.querySelector(".filter-btn.active")?.classList.remove("active");
      btn.classList.add("active");
      loadAnalytics(userId, btn.dataset.filter);
    });
  });

  await loadAnalytics(userId, "all");
}

// ✅ Fetch analytics
async function loadAnalytics(userId, filter = "all") {
  const token = sessionStorage.getItem("jwt"); // ✅ FIXED

  const url = `${getBaseUrl()}/orders/analysis?userId=${encodeURIComponent(
    userId
  )}&filter=${encodeURIComponent(filter)}`;

  try {
    const res = await fetch(url, {
      headers: { Authorization: `Bearer ${token}` },
    });

    if (!res.ok) {
      console.error("Analytics HTTP error:", res.status);
      throw new Error(`HTTP ${res.status}`);
    }

    const data = await res.json();
    console.log("✅ Analytics loaded:", data);

    setTotals(data.totalOrders ?? 0, data.totalSpent ?? 0);
    renderCategoryChart(data.categories || {});
    renderOrderTrendChart(data.categories || {});
  } catch (err) {
    console.error("❌ Failed to load analytics:", err);
    alert("⚠️ Failed to load analytics. Try logging in again.");
  }
}

// ✅ Update totals UI
function setTotals(totalOrders, totalSpent) {
  document.getElementById("totalOrders").textContent = totalOrders;
  document.getElementById("totalSpent").textContent = `₹${totalSpent.toFixed(2)}`;
}

// ✅ Donut chart
function renderCategoryChart(categories) {
  const ctx = document.getElementById("categoryChart");
  if (!ctx) return;

  if (categoryChart) categoryChart.destroy();

  categoryChart = new Chart(ctx, {
    type: "doughnut",
    data: {
      labels: Object.keys(categories),
      datasets: [
        {
          data: Object.values(categories),
          hoverOffset: 12,
        }
      ]
    }
  });
}

// ✅ Bar chart
function renderOrderTrendChart(categories) {
  const ctx = document.getElementById("orderTrendChart");
  if (!ctx) return;

  if (orderTrendChart) orderTrendChart.destroy();

  orderTrendChart = new Chart(ctx, {
    type: "bar",
    data: {
      labels: Object.keys(categories),
      datasets: [
        {
          label: "Orders per Category",
          data: Object.values(categories)
        }
      ]
    }
  });
}

// 🚀 Go!
initDashboard();
