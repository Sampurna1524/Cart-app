// === Floating Chatbot Button & Window ===

// Create floating button
const chatButton = document.createElement("button");
chatButton.id = "chatbot-btn";
chatButton.innerHTML = "💬";
document.body.appendChild(chatButton);

// Create floating chat window (hidden by default)
const chatWindow = document.createElement("div");
chatWindow.id = "chatbot-window";
chatWindow.style.display = "none";

chatWindow.innerHTML = `
  <div id="chat-header">
    <span>SmartCart Assistant 🤖</span>
    <button id="chat-close">✖</button>
  </div>
  <div id="chat-body" class="chat-body"></div>
  <div id="chat-input-area">
    <input type="text" id="chat-input" placeholder="Ask me anything... (e.g., show my order history)" />
    <button id="chat-send">➤</button>
  </div>
`;
document.body.appendChild(chatWindow);

// === Styling ===
const style = document.createElement("style");
style.textContent = `
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
  }
  #chatbot-btn:hover { background-color: #005fcc; }

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
    display: none;
    overflow: hidden;
    z-index: 9999;
    font-family: "Poppins", sans-serif;
  }

  #chat-header {
    background: #0078ff;
    color: white;
    padding: 10px;
    display: flex;
    justify-content: space-between;
    align-items: center;
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

  .product-link {
    color: #0078ff;
    font-weight: 600;
    text-decoration: underline;
  }
`;
document.head.appendChild(style);

// === DOM refs ===
const chatBody = chatWindow.querySelector("#chat-body");
const chatInput = chatWindow.querySelector("#chat-input");
const chatSend = chatWindow.querySelector("#chat-send");
const chatClose = chatWindow.querySelector("#chat-close");

// === Show/Hide ===
chatButton.addEventListener("click", () => {
  chatWindow.style.display = chatWindow.style.display === "none" ? "flex" : "none";
  chatInput.focus();
});
chatClose.addEventListener("click", () => (chatWindow.style.display = "none"));

// ✅ Get Logged-in User Session
function getUserSession() {
  try {
    const user = JSON.parse(sessionStorage.getItem("user"));
    const token = sessionStorage.getItem("jwt"); // <-- FIXED KEY

    return {
      userId: user?.id ?? null,
      token: token ?? null
    };
  } catch {
    return { userId: null, token: null };
  }
}

// ✅ Clean & format bot messages (links)
function formatResponse(text) {
  text = text.replace(/\*/g, "").replace(/\n/g, "<br>");
  return text;
}

// === Send Message ===
chatSend.addEventListener("click", sendMessage);
chatInput.addEventListener("keypress", (e) => e.key === "Enter" && sendMessage());

async function sendMessage() {
  const message = chatInput.value.trim();
  if (!message) return;

  appendMessage("user", message);
  chatInput.value = "";

  appendMessage("bot", "⏳ Thinking...");

  const { userId, token } = getUserSession();

  try {
    const response = await fetch("http://localhost:8080/api/chat/ask", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(token && { Authorization: `Bearer ${token}` })
      },
      body: JSON.stringify({ message, userId }) // ✅ SEND USER ID TO BACKEND
    });

    const data = await response.json();
    const lastBotMsg = chatBody.querySelector(".bot-msg:last-child");

    lastBotMsg.innerHTML = data.response
      ? formatResponse(data.response)
      : "⚠️ No response from server.";

  } catch {
    const lastBotMsg = chatBody.querySelector(".bot-msg:last-child");
    lastBotMsg.textContent = "⚠️ Error contacting AI.";
  }
}

// === Append messages to UI ===
function appendMessage(sender, text) {
  const msg = document.createElement("div");
  msg.className = `chat-msg ${sender === "user" ? "user-msg" : "bot-msg"}`;
  msg.innerHTML = text;
  chatBody.appendChild(msg);
  chatBody.scrollTop = chatBody.scrollHeight;
}
