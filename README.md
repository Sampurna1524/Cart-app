# 🛒 SmartCart - Modern E-Commerce Web Application

SmartCart is a full-stack e-commerce web application that simulates a modern online shopping platform. It provides a smooth and intuitive user experience for browsing products, managing a shopping cart, handling orders, and includes a robust admin panel for managing products.

## 🚀 Project Overview

SmartCart offers both **customer-facing features** and **admin functionalities**, built using **Spring Boot** (Java) for the backend and **HTML/CSS/JavaScript** for the frontend. It includes essential e-commerce capabilities like product filtering, search, authentication, wishlist, and stock management — along with advanced additions like product archiving, JWT authentication, and a simulated self-checkout.

---

## 🧩 Tech Stack

### 🔧 Backend
- Java 17
- Spring Boot
- Spring Data JPA (Hibernate)
- Spring Security (with JWT)
- H2 Database (in-memory, for development)
- Maven

### 🎨 Frontend
- HTML5, CSS3, JavaScript (Vanilla)
- Responsive design (mobile-friendly)
- Toast notifications
- Pagination & filtering logic
- Camera-based barcode scanner (optional)

---

## 💡 Features

### 👤 User Features
- 🔐 User authentication (login, signup)
- 🛍️ Browse all products
- 🔎 Search, filter by category, and sort by price
- ❤️ Wishlist products
- 🛒 Add to cart, update quantity, and checkout
- 📦 Place orders with shipping address and payment method
- 🧾 View order history
- 🌑 Dark mode toggle
- 👤 Profile page with user info and picture

### 💡 Extras
- 🌙 Dark mode toggle
- 🔁 Persistent cart & wishlist
- 📱 Responsive design
- 📦 Product stock tracking (Low stock warning, Out-of-stock disabling)
- 🔐 JWT-based role-based security
- 📸 User profile picture upload
- 🧾 Self-checkout (coming soon)

### 🛠️ Admin Features
- 📋 View all products (with pagination)
- ➕ Add new products with image and stock
- ✏️ Edit or delete existing products
- 🔴 Archive or unarchive products
- 📦 Restock inventory
- 👁️ View archived products list with unarchive option
- 🧮 See stock status (low, out of stock)

### 🔐 Security
- JWT-based user authentication
- Role-based access (admin/user)
- Secure endpoints for admin functionality

### 🧪 Experimental Features (Optional)
- 📷 Self-checkout simulation using barcode scanning
- 📊 Admin analytics dashboard (planned)

---

## 📁 Project Structure

```plaintext
smartcart/
├── backend/
│   ├── src/main/java/com/shoppingcart/cartapp/
│   │   ├── controller/
│   │   ├── model/
│   │   ├── repository/
│   │   ├── service/
│   │   └── config/
│   ├── src/main/resources/
│   │   ├── static/             # Frontend HTML, JS, CSS
│   │   ├── uploads/            # Product/user images
│   │   └── application.properties
│   └── pom.xml
└── README.md
```

## 🧱 Tech Stack (Summary)

| Layer        | Technology                 |
|--------------|-----------------------------|
| Frontend     | HTML, CSS, JavaScript       |
| Backend      | Spring Boot (Java)          |
| Database     | H2 (dev) |
| Security     | Spring Security + JWT       |
| File Upload  | Multipart file handling     |
| Deployment   | Locally  |
