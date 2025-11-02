// === Floating Chatbot Button & Window ===

// Create floating button
const chatButton = document.createElement("button");
chatButton.id = "chatbot-btn";
chatButton.innerHTML = "💬";
document.body.appendChild(chatButton);

// Create floating chat window (hidden by default)
const chatWindow = document.createElement("div");
chatWindow.id = "chatbot-window";
chatWindow.style.display = "none"; // ✅ Hidden by default

chatWindow.innerHTML = `
  <div id="chat-header">
    <span>SmartCart Assistant 🤖</span>
    <button id="chat-close">✖</button>
  </div>
  <div id="chat-body" class="chat-body"></div>
  <div id="chat-input-area">
    <input type="text" id="chat-input" placeholder="Ask me anything..." />
    <button id="chat-send">➤</button>
  </div>
`;
document.body.appendChild(chatWindow);

// === Styling ===
const style = document.createElement("style");
style.textContent = `
  /* Floating Chat Button */
  #chatbot-btn {
    position: fixed;
    bottom: 25px;
    right: 25px;
    width: 60px;
    height: 60px;
    border-radius: 50%;
    background-color: #0078ff;
    color: white;
    font-size: 26px;
    border: none;
    cursor: pointer;
    box-shadow: 0 4px 10px rgba(0,0,0,0.3);
    z-index: 9998;
    transition: background-color 0.3s;
  }
  #chatbot-btn:hover { background-color: #005fcc; }

  /* Chat Window */
  #chatbot-window {
    position: fixed;
    bottom: 100px;
    right: 25px;
    width: 320px;
    height: 420px;
    background: white;
    border-radius: 16px;
    box-shadow: 0 6px 15px rgba(0,0,0,0.25);
    flex-direction: column;
    overflow: hidden;
    z-index: 9999;
    display: none;
    font-family: "Poppins", sans-serif;
  }

  #chat-header {
    background: #0078ff;
    color: white;
    padding: 10px;
    font-weight: bold;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  #chat-close {
    background: transparent;
    color: white;
    border: none;
    font-size: 16px;
    cursor: pointer;
  }

  .chat-body {
    flex: 1;
    padding: 10px;
    overflow-y: auto;
    background: #f5f7fb;
    display: flex;
    flex-direction: column;
  }

  .chat-msg {
    margin: 8px 0;
    padding: 8px 10px;
    border-radius: 12px;
    max-width: 80%;
    word-wrap: break-word;
  }

  .user-msg {
    background: #0078ff;
    color: white;
    align-self: flex-end;
  }

  .bot-msg {
    background: #e5e5ea;
    color: #333;
    align-self: flex-start;
  }

  #chat-input-area {
    display: flex;
    padding: 8px;
    background: #f0f0f0;
  }

  #chat-input {
    flex: 1;
    border: none;
    border-radius: 8px;
    padding: 8px;
    outline: none;
  }

  #chat-send {
    margin-left: 6px;
    background: #0078ff;
    border: none;
    color: white;
    border-radius: 8px;
    padding: 8px 12px;
    cursor: pointer;
  }

  /* Product link formatting */
  .product-link {
    color: #0078ff;
    font-weight: 500;
    text-decoration: underline;
  }
  .product-link:hover {
    color: #0056b3;
  }

  /* Smooth open animation */
  @keyframes slideUp {
    from { opacity: 0; transform: translateY(20px); }
    to { opacity: 1; transform: translateY(0); }
  }
`;
document.head.appendChild(style);

// === Query DOM elements after creation ===
const chatBody = chatWindow.querySelector("#chat-body");
const chatInput = chatWindow.querySelector("#chat-input");
const chatSend = chatWindow.querySelector("#chat-send");
const chatClose = chatWindow.querySelector("#chat-close");

// === Show / Hide chat ===
chatButton.addEventListener("click", () => {
  if (chatWindow.style.display === "none") {
    chatWindow.style.display = "flex";
    chatWindow.style.animation = "slideUp 0.3s ease-out";
    chatInput.focus();
  } else {
    chatWindow.style.display = "none";
  }
});

chatClose.addEventListener("click", () => {
  chatWindow.style.display = "none";
});

// === Chat send logic ===
chatSend.addEventListener("click", sendMessage);
chatInput.addEventListener("keypress", (e) => {
  if (e.key === "Enter") sendMessage();
});

// ✅ Format Gemini responses
function formatResponse(text) {
  // Remove asterisks and clean bullet points
  text = text.replace(/\*/g, "").replace(/\s*-\s*/g, "<br>• ");

  // Add newlines for readability
  text = text.replace(/\n/g, "<br>");

  // Convert [id:11] → clickable link
  text = text.replace(/\[id:(\d+)\]/g, (match, id) => {
    return `<a href="http://localhost:8080/product.html?id=${id}" target="_blank" class="product-link">View Product</a>`;
  });

  return text;
}

async function sendMessage() {
  const message = chatInput.value.trim();
  if (!message) return;

  appendMessage("user", message);
  chatInput.value = "";

  appendMessage("bot", "⏳ Thinking...");

  try {
    const response = await fetch("http://localhost:8080/api/chat/ask", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ message }),
    });

    const data = await response.json();
    const lastBotMsg = chatBody.querySelector(".bot-msg:last-child");

    // ✅ Apply formatting
    const formatted = data.response
      ? formatResponse(data.response)
      : "⚠️ No response from server.";

    lastBotMsg.innerHTML = formatted;
  } catch (err) {
    const lastBotMsg = chatBody.querySelector(".bot-msg:last-child");
    lastBotMsg.textContent = "⚠️ Error contacting AI.";
  }
}

function appendMessage(sender, text) {
  const msg = document.createElement("div");
  msg.className = `chat-msg ${sender === "user" ? "user-msg" : "bot-msg"}`;
  msg.innerHTML = text; // ✅ Allows HTML for formatted text
  chatBody.appendChild(msg);
  chatBody.scrollTop = chatBody.scrollHeight;
}
