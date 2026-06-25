// Application State
const state = {
  token: localStorage.getItem('jwt_token') || null,
  user: null,
  rooms: [],
  activeRoomId: null,
  stompClient: null,
  wsMsgSubscription: null,
  wsReactSubscription: null
};

// API Base URL (same host since we serve static files from Spring Boot)
const API_BASE = '';

/* ==========================================================================
   Toast Notification System
   ========================================================================== */
function showToast(message, type = 'success') {
  const container = document.getElementById('toast-container');
  if (!container) return;

  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `
    <span>${message}</span>
    <button class="toast-close">&times;</button>
  `;

  container.appendChild(toast);

  // Auto-remove after 4 seconds
  const timeoutId = setTimeout(() => {
    toast.remove();
  }, 4000);

  toast.querySelector('.toast-close').addEventListener('click', () => {
    clearTimeout(timeoutId);
    toast.remove();
  });
}

/* ==========================================================================
   API Helpers
   ========================================================================== */
async function apiFetch(url, options = {}) {
  const headers = new Headers(options.headers || {});
  
  if (state.token) {
    headers.set('Authorization', `Bearer ${state.token}`);
  }
  
  if (options.body && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(url, { ...options, headers });
  
  if (response.status === 401 || response.status === 403) {
    // If unauthorized, trigger logout if they were logged in
    if (state.token) {
      showToast('Session expired. Please log in again.', 'error');
      logout();
    }
    throw new Error('Unauthorized');
  }

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `API Error: ${response.status}`);
  }

  // Check if response is JSON, otherwise return text
  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    return await response.json();
  }
  return await response.text();
}

/* ==========================================================================
   Authentication
   ========================================================================== */
async function fetchUserProfile() {
  try {
    const userData = await apiFetch(`${API_BASE}/auth/me`);
    state.user = userData;
    updateAuthUI();
    return userData;
  } catch (error) {
    console.error('Failed to fetch user profile:', error);
    logout();
    return null;
  }
}

function updateAuthUI() {
  const navHome = document.getElementById('nav-home');
  const navChat = document.getElementById('nav-chat');
  const navProfile = document.getElementById('nav-profile');
  const navLogin = document.getElementById('nav-login');
  const navRegister = document.getElementById('nav-register');
  const navUserContainer = document.getElementById('nav-user-container');
  const navUserAvatar = document.getElementById('nav-user-avatar');
  const heroCtaBtn = document.getElementById('hero-cta-btn');

  if (state.user) {
    // Logged In
    if (navChat) navChat.style.display = 'block';
    if (navProfile) navProfile.style.display = 'block';
    if (navLogin) navLogin.style.display = 'none';
    if (navRegister) navRegister.style.display = 'none';
    if (navUserContainer) {
      navUserContainer.style.display = 'flex';
      navUserAvatar.textContent = state.user.name.charAt(0).toUpperCase();
      navUserAvatar.title = state.user.name;
    }
    if (heroCtaBtn) {
      heroCtaBtn.textContent = 'Go to Chat Dashboard';
      heroCtaBtn.href = '#chat';
    }
    
    // Fill profile info if visible
    const profileName = document.getElementById('profile-name');
    const profileEmail = document.getElementById('profile-email');
    const profileId = document.getElementById('profile-id');
    const profileAvatarChar = document.getElementById('profile-avatar-char');
    
    if (profileName) profileName.textContent = state.user.name;
    if (profileEmail) profileEmail.textContent = state.user.email;
    if (profileId) profileId.textContent = state.user.id;
    if (profileAvatarChar) profileAvatarChar.textContent = state.user.name.charAt(0).toUpperCase();
  } else {
    // Logged Out
    if (navChat) navChat.style.display = 'none';
    if (navProfile) navProfile.style.display = 'none';
    if (navLogin) navLogin.style.display = 'block';
    if (navRegister) navRegister.style.display = 'block';
    if (navUserContainer) navUserContainer.style.display = 'none';
    if (heroCtaBtn) {
      heroCtaBtn.textContent = 'Get Started';
      heroCtaBtn.href = '#register';
    }
  }
}

function logout() {
  // Disconnect Websocket
  disconnectWebSocket();
  
  // Clear State
  state.token = null;
  state.user = null;
  state.rooms = [];
  state.activeRoomId = null;
  localStorage.removeItem('jwt_token');
  
  updateAuthUI();
  window.location.hash = '#home';
  showToast('Logged out successfully.');
}

/* ==========================================================================
   STOMP WebSocket Connection
   ========================================================================== */
function connectWebSocket() {
  if (state.stompClient && state.stompClient.active) return;

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const brokerURL = `${protocol}//${window.location.host}/chat`;

  console.log('Connecting to WebSocket STOMP at:', brokerURL);

  state.stompClient = new StompJs.Client({
    brokerURL: brokerURL,
    connectHeaders: {
      Authorization: `Bearer ${state.token}`
    },
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    debug: function (str) {
      console.log('STOMP DEBUG:', str);
    }
  });

  state.stompClient.onConnect = (frame) => {
    console.log('STOMP Connected successfully.');
    // If there was an active room selected before reconnection, re-subscribe
    if (state.activeRoomId) {
      subscribeToRoom(state.activeRoomId);
    }
  };

  state.stompClient.onStompError = (frame) => {
    console.error('STOMP Broker error:', frame.headers['message']);
    console.error('STOMP Additional details:', frame.body);
    showToast('Real-time connection error. Retrying...', 'error');
  };

  state.stompClient.onWebSocketClose = () => {
    console.log('STOMP WebSocket Connection Closed.');
  };

  state.stompClient.activate();
}

function disconnectWebSocket() {
  if (state.wsMsgSubscription) {
    state.wsMsgSubscription.unsubscribe();
    state.wsMsgSubscription = null;
  }
  if (state.wsReactSubscription) {
    state.wsReactSubscription.unsubscribe();
    state.wsReactSubscription = null;
  }
  if (state.stompClient) {
    state.stompClient.deactivate();
    state.stompClient = null;
    console.log('STOMP connection deactivated.');
  }
}

function subscribeToRoom(roomId) {
  if (!state.stompClient || !state.stompClient.connected) {
    console.warn('STOMP client not connected. Delaying subscription.');
    return;
  }

  // Unsubscribe from previous subscriptions
  if (state.wsMsgSubscription) {
    state.wsMsgSubscription.unsubscribe();
    state.wsMsgSubscription = null;
  }
  if (state.wsReactSubscription) {
    state.wsReactSubscription.unsubscribe();
    state.wsReactSubscription = null;
  }

  const msgDestination = `/topic/room/${roomId}`;
  console.log('Subscribing to WebSocket messages:', msgDestination);
  state.wsMsgSubscription = state.stompClient.subscribe(msgDestination, (message) => {
    try {
      const msgBody = JSON.parse(message.body);
      console.log('WebSocket Message Received:', msgBody);
      
      if (state.activeRoomId === roomId) {
        appendMessage(msgBody);
        scrollChatToBottom();
      }
    } catch (e) {
      console.error('Error parsing WebSocket message:', e);
    }
  });

  const reactDestination = `/topic/room/${roomId}/reactions`;
  console.log('Subscribing to WebSocket reactions:', reactDestination);
  state.wsReactSubscription = state.stompClient.subscribe(reactDestination, (message) => {
    try {
      const reactEvent = JSON.parse(message.body);
      console.log('WebSocket Reaction Received:', reactEvent);
      
      if (state.activeRoomId === roomId) {
        handleIncomingReactionEvent(reactEvent);
      }
    } catch (e) {
      console.error('Error parsing WebSocket reaction:', e);
    }
  });
}

/* ==========================================================================
   Chat Dashboard & Rooms
   ========================================================================== */
async function loadRooms() {
  try {
    const rooms = await apiFetch(`${API_BASE}/rooms`);
    state.rooms = rooms;
    renderRoomsList();
  } catch (error) {
    console.error('Failed to load chat rooms:', error);
    showToast('Failed to load chat rooms.', 'error');
  }
}

function renderRoomsList() {
  const container = document.getElementById('chat-rooms-list');
  if (!container) return;

  if (state.rooms.length === 0) {
    container.innerHTML = `
      <div style="text-align: center; color: var(--text-dim); padding: 2rem; font-size: 0.9rem;">
        No rooms available. Create one to get started!
      </div>
    `;
    return;
  }

  container.innerHTML = '';
  state.rooms.forEach(room => {
    const isSelected = room.id === state.activeRoomId;
    const roomEl = document.createElement('div');
    roomEl.className = `room-item ${isSelected ? 'active' : ''}`;
    roomEl.dataset.roomId = room.id;
    
    // First letter of room name as icon
    const iconChar = room.roomName.charAt(0).toUpperCase();

    roomEl.innerHTML = `
      <div class="room-icon">${iconChar}</div>
      <div class="room-info">
        <div class="room-name">${escapeHTML(room.roomName)}</div>
        <div class="room-meta">Created by ${escapeHTML(room.creator?.name || 'Unknown')}</div>
      </div>
    `;

    roomEl.addEventListener('click', () => {
      selectRoom(room.id);
    });

    container.appendChild(roomEl);
  });
}

async function selectRoom(roomId) {
  if (state.activeRoomId === roomId) return;
  
  console.log('Selecting Room ID:', roomId);
  state.activeRoomId = roomId;

  // Highlight active room item in sidebar list
  document.querySelectorAll('.room-item').forEach(el => {
    el.classList.toggle('active', Number(el.dataset.roomId) === roomId);
  });

  // Switch chat panel from placeholder to window
  document.getElementById('chat-no-active-room').style.display = 'none';
  document.getElementById('chat-active-window').style.display = 'flex';

  const room = state.rooms.find(r => r.id === roomId);
  if (room) {
    document.getElementById('chat-active-room-name').textContent = room.roomName;
    const creatorSpan = document.getElementById('chat-active-room-creator');
    creatorSpan.textContent = room.creator?.name || 'Unknown';
    creatorSpan.style.cursor = 'pointer';
    creatorSpan.style.textDecoration = 'underline';
    
    // Clear old event listeners
    const newCreatorSpan = creatorSpan.cloneNode(true);
    creatorSpan.parentNode.replaceChild(newCreatorSpan, creatorSpan);
    newCreatorSpan.addEventListener('click', () => {
      if (room.creator) {
        showUserProfileModal(room.creator.id, room.creator.name, room.creator.email);
      }
    });
  }

  // Load message history from DB via REST
  const msgContainer = document.getElementById('chat-messages-container');
  msgContainer.innerHTML = '<div style="text-align: center; color: var(--text-dim); padding: 2rem;">Loading messages...</div>';

  try {
    const messages = await apiFetch(`${API_BASE}/rooms/${roomId}/messages`);
    msgContainer.innerHTML = '';
    messages.forEach(msg => appendMessage(msg));
    scrollChatToBottom();
  } catch (error) {
    console.error('Failed to load message history:', error);
    msgContainer.innerHTML = `
      <div style="text-align: center; color: var(--accent); padding: 2rem; font-size: 0.9rem;">
        Failed to load messages. Make sure you are a member of this room.
      </div>
    `;
    showToast('Failed to load room messages. Try adding yourself or joining.', 'error');
  }

  // Subscribe to real-time WebSocket channel for this room
  subscribeToRoom(roomId);
}

function appendMessage(msg) {
  const container = document.getElementById('chat-messages-container');
  if (!container) return;

  // Handle differences between REST MessageResponseDTO and WebSocket ChatMessageDTO
  const isSelf = msg.senderId === state.user.id || (msg.senderName === state.user.name && !msg.senderId);
  const senderName = msg.senderName || 'Anonymous';
  const senderEmail = msg.senderEmail || '';
  const content = msg.content || '';
  const msgId = msg.id;

  // Format Date
  let timeStr = '';
  if (msg.sentAt) {
    const date = new Date(msg.sentAt);
    timeStr = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  } else {
    timeStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  const msgWrapper = document.createElement('div');
  msgWrapper.className = `msg-wrapper ${isSelf ? 'self' : 'other'}`;
  if (msgId) {
    msgWrapper.dataset.msgId = msgId;
  }

  // Render emoji reaction options for picker
  const emojis = ['👍', '❤️', '😂', '😮', '😢', '😡'];
  const emojiButtonsHTML = msgId ? emojis.map(emoji => 
    `<button class="react-emoji-btn" data-emoji="${emoji}">${emoji}</button>`
  ).join('') : '';

  msgWrapper.innerHTML = `
    ${!isSelf ? `<span class="msg-sender" style="cursor: pointer; text-decoration: underline;" title="View Profile">${escapeHTML(senderName)}</span>` : ''}
    ${msgId ? `
    <div class="reaction-picker">
      ${emojiButtonsHTML}
    </div>
    ` : ''}
    <div class="msg-bubble">${escapeHTML(content)}</div>
    <div class="reactions-container" id="reactions-for-${msgId || 'temp'}"></div>
    <span class="msg-time">${timeStr}</span>
  `;

  container.appendChild(msgWrapper);

  if (msgId) {
    // Render initial reactions
    renderReactionsForMessage(msgId, msg.reactions || []);

    // Add click listeners to emoji buttons
    msgWrapper.querySelectorAll('.react-emoji-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const emoji = btn.dataset.emoji;
        toggleReaction(msgId, emoji);
      });
    });
  }

  // Click handler to show other user's profile
  const senderSpan = msgWrapper.querySelector('.msg-sender');
  if (senderSpan && !isSelf) {
    senderSpan.addEventListener('click', () => {
      showUserProfileModal(msg.senderId, senderName, senderEmail);
    });
  }
}

function renderReactionsForMessage(messageId, reactions) {
  const container = document.getElementById(`reactions-for-${messageId}`);
  if (!container) return;

  const msgWrapper = container.closest('.msg-wrapper');
  if (msgWrapper) {
    msgWrapper.dataset.reactions = JSON.stringify(reactions);
  }

  if (!reactions || reactions.length === 0) {
    container.innerHTML = '';
    return;
  }

  // Group reactions by type
  const groups = {};
  reactions.forEach(r => {
    if (!groups[r.reactionType]) {
      groups[r.reactionType] = [];
    }
    groups[r.reactionType].push({ userId: r.userId, userName: r.userName });
  });

  container.innerHTML = '';
  Object.entries(groups).forEach(([type, users]) => {
    const userReacted = users.some(u => u.userId === state.user.id);
    const userNames = users.map(u => u.userName).join(', ');

    const pill = document.createElement('div');
    pill.className = `reaction-pill ${userReacted ? 'user-reacted' : ''}`;
    pill.dataset.emoji = type;
    pill.dataset.tooltip = userNames;
    pill.innerHTML = `<span>${type}</span> <span>${users.length}</span>`;

    pill.addEventListener('click', (e) => {
      e.stopPropagation(); // Avoid triggering any bubble hover/click behavior
      toggleReaction(messageId, type);
    });

    container.appendChild(pill);
  });
}

async function toggleReaction(messageId, reactionType) {
  if (!state.activeRoomId) return;

  try {
    // 1. Post to REST API (toggles reaction in DB and returns ADDED or REMOVED)
    const action = await apiFetch(`${API_BASE}/rooms/${state.activeRoomId}/messages/${messageId}/react`, {
      method: 'POST',
      body: JSON.stringify({ reactionType })
    });

    // 2. Broadcast via WebSocket STOMP
    if (state.stompClient && state.stompClient.connected) {
      state.stompClient.publish({
        destination: `/app/room/${state.activeRoomId}/react`,
        body: JSON.stringify({
          messageId,
          reactionType
        })
      });
    }
  } catch (error) {
    console.error('Failed to toggle reaction:', error);
    showToast('Failed to update reaction.', 'error');
  }
}

function handleIncomingReactionEvent(event) {
  const msgWrapper = document.querySelector(`.msg-wrapper[data-msg-id="${event.messageId}"]`);
  if (!msgWrapper) return;

  let reactions = [];
  try {
    reactions = JSON.parse(msgWrapper.dataset.reactions || '[]');
  } catch (e) {
    console.error('Failed to parse reactions dataset:', e);
  }

  if (event.action === 'ADDED') {
    const exists = reactions.some(r => r.userId === event.senderId && r.reactionType === event.reactionType);
    if (!exists) {
      reactions.push({
        userId: event.senderId,
        userName: event.senderName,
        reactionType: event.reactionType
      });
    }
  } else if (event.action === 'REMOVED') {
    reactions = reactions.filter(r => !(r.userId === event.senderId && r.reactionType === event.reactionType));
  }

  renderReactionsForMessage(event.messageId, reactions);
}

function scrollChatToBottom() {
  const container = document.getElementById('chat-messages-container');
  if (container) {
    container.scrollTop = container.scrollHeight;
  }
}

async function handleSendMessage(e) {
  e.preventDefault();
  const input = document.getElementById('chat-message-input');
  if (!input || !state.activeRoomId) return;

  const content = input.value.trim();
  if (!content) return;

  input.value = '';

  try {
    // 1. Post to REST API (persists in DB and returns MessageResponseDTO with database ID)
    const savedMsg = await apiFetch(`${API_BASE}/rooms/${state.activeRoomId}/messages`, {
      method: 'POST',
      body: JSON.stringify({ content })
    });

    // 2. Broadcast via WebSocket STOMP so other clients receive it immediately
    if (state.stompClient && state.stompClient.connected) {
      state.stompClient.publish({
        destination: `/app/room/${state.activeRoomId}/send`,
        body: JSON.stringify({
          content
        })
      });
    } else {
      console.warn('STOMP client not connected. Live broadcast skipped, but message was saved.');
      // Fallback: manually fetch and render if WebSocket is down
      const messages = await apiFetch(`${API_BASE}/rooms/${state.activeRoomId}/messages`);
      const msgContainer = document.getElementById('chat-messages-container');
      msgContainer.innerHTML = '';
      messages.forEach(msg => appendMessage(msg));
      scrollChatToBottom();
    }
  } catch (error) {
    console.error('Failed to send message:', error);
    showToast('Failed to send message. Make sure you are in the room.', 'error');
  }
}

/* ==========================================================================
   Modals & User Listing
   ========================================================================== */
async function loadUsersForAddMember() {
  const listContainer = document.getElementById('modal-user-list');
  if (!listContainer) return;

  listContainer.innerHTML = '<div style="text-align: center; color: var(--text-dim); padding: 1rem;">Loading users...</div>';

  try {
    // Fetch all users
    const allUsers = await apiFetch(`${API_BASE}/auth/users`);
    
    // Fetch current room members to filter them out
    const currentMembers = await apiFetch(`${API_BASE}/rooms/${state.activeRoomId}/members`);
    const memberIds = new Set(currentMembers.map(m => m.user?.id).filter(Boolean));

    // Filter out users who are already members
    const eligibleUsers = allUsers.filter(u => u.id !== state.user.id && !memberIds.has(u.id));

    if (eligibleUsers.length === 0) {
      listContainer.innerHTML = '<div style="text-align: center; color: var(--text-dim); padding: 1rem; font-size: 0.9rem;">No other eligible users found to invite.</div>';
      return;
    }

    listContainer.innerHTML = '';
    eligibleUsers.forEach(u => {
      const item = document.createElement('div');
      item.className = 'user-select-item';
      item.innerHTML = `
        <div class="user-select-info">
          <span class="user-select-name">${escapeHTML(u.name)}</span>
          <span class="user-select-email">${escapeHTML(u.email)}</span>
        </div>
        <button class="btn btn-primary btn-add-user" data-user-id="${u.id}" style="padding: 0.4rem 0.8rem; font-size: 0.8rem;">Add</button>
      `;

      item.querySelector('.btn-add-user').addEventListener('click', async (e) => {
        const btn = e.target;
        const userId = Number(btn.dataset.userId);
        btn.disabled = true;
        btn.textContent = 'Adding...';

        try {
          await apiFetch(`${API_BASE}/rooms/${state.activeRoomId}/members`, {
            method: 'POST',
            body: JSON.stringify({ userId })
          });
          
          showToast(`Successfully added ${u.name} to the room.`);
          // Reload list to filter out the newly added user
          loadUsersForAddMember();
        } catch (error) {
          console.error('Failed to add member:', error);
          showToast('Failed to add member.', 'error');
          btn.disabled = false;
          btn.textContent = 'Add';
        }
      });

      listContainer.appendChild(item);
    });

  } catch (error) {
    console.error('Failed to load user list:', error);
    listContainer.innerHTML = '<div style="text-align: center; color: var(--accent); padding: 1rem;">Failed to load users.</div>';
  }
}

// Modal Toggle Helpers
function toggleModal(modalId, show) {
  const modal = document.getElementById(modalId);
  if (modal) {
    modal.classList.toggle('active', show);
  }
}

/* ==========================================================================
   SPA Router
   ========================================================================== */
function router() {
  const hash = window.location.hash || '#home';
  console.log('Routing to hash:', hash);

  // Define views and route checks
  const routes = {
    '#home': 'view-home',
    '#login': 'view-login',
    '#register': 'view-register',
    '#profile': 'view-profile',
    '#chat': 'view-chat'
  };

  const viewId = routes[hash];

  if (!viewId) {
    window.location.hash = '#home';
    return;
  }

  // Guard routes: if not logged in, redirect to #login for protected views
  const isProtected = ['#profile', '#chat'].includes(hash);
  if (isProtected && !state.token) {
    window.location.hash = '#login';
    showToast('Please login to access this page.', 'error');
    return;
  }

  // If logged in, prevent going to login/register
  const isGuestOnly = ['#login', '#register'].includes(hash);
  if (isGuestOnly && state.token) {
    window.location.hash = '#chat';
    return;
  }

  // Switch View Visibility
  document.querySelectorAll('.view').forEach(view => {
    view.classList.toggle('active', view.id === viewId);
  });

  // Update Nav Links Active Class
  document.querySelectorAll('.nav-link').forEach(link => {
    const href = link.getAttribute('href');
    link.classList.toggle('active', href === hash);
  });

  // Page-specific initializations
  if (hash === '#chat') {
    loadRooms();
    connectWebSocket();
  }
}

/* ==========================================================================
   Form Submissions & Event Listeners
   ========================================================================== */
async function handleLogin(e) {
  e.preventDefault();
  const emailInput = document.getElementById('login-email');
  const passwordInput = document.getElementById('login-password');
  
  const email = emailInput.value.trim();
  const password = passwordInput.value;

  try {
    const token = await apiFetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      body: JSON.stringify({ email, password })
    });

    if (token) {
      state.token = token;
      localStorage.setItem('jwt_token', token);
      showToast('Logged in successfully!');
      
      // Fetch user profile info
      await fetchUserProfile();
      
      // Redirect to chat dashboard
      window.location.hash = '#chat';
      
      // Clear form
      emailInput.value = '';
      passwordInput.value = '';
    }
  } catch (error) {
    console.error('Login error:', error);
    showToast(error.message || 'Login failed. Please check your credentials.', 'error');
  }
}

async function handleRegister(e) {
  e.preventDefault();
  const nameInput = document.getElementById('register-name');
  const emailInput = document.getElementById('register-email');
  const passwordInput = document.getElementById('register-password');
  const confirmPasswordInput = document.getElementById('register-confirm-password');

  const name = nameInput.value.trim();
  const email = emailInput.value.trim();
  const password = passwordInput.value;
  const confirmPassword = confirmPasswordInput.value;

  if (password !== confirmPassword) {
    showToast('Passwords do not match.', 'error');
    return;
  }

  try {
    await apiFetch(`${API_BASE}/auth/register`, {
      method: 'POST',
      body: JSON.stringify({ name, email, password })
    });

    showToast('Registration successful! You can now log in.');
    window.location.hash = '#login';

    // Clear form
    nameInput.value = '';
    emailInput.value = '';
    passwordInput.value = '';
    confirmPasswordInput.value = '';
  } catch (error) {
    console.error('Registration error:', error);
    showToast(error.message || 'Registration failed. Email may already be in use.', 'error');
  }
}

async function handleCreateRoom(e) {
  e.preventDefault();
  const roomNameInput = document.getElementById('new-room-name');
  const roomName = roomNameInput.value.trim();

  if (!roomName) return;

  try {
    const createdRoom = await apiFetch(`${API_BASE}/rooms`, {
      method: 'POST',
      body: JSON.stringify({ roomName })
    });

    showToast(`Room "${createdRoom.roomName}" created successfully!`);
    toggleModal('modal-create-room', false);
    roomNameInput.value = '';
    
    // Reload rooms list and automatically select it
    await loadRooms();
    if (createdRoom && createdRoom.id) {
      selectRoom(createdRoom.id);
    }
  } catch (error) {
    console.error('Failed to create room:', error);
    showToast('Failed to create room.', 'error');
  }
}

// Utility to escape HTML and prevent XSS
function escapeHTML(str) {
  if (!str) return '';
  return str.replace(/[&<>'"]/g, 
    tag => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      "'": '&#39;',
      '"': '&quot;'
    }[tag] || tag)
  );
}

/* ==========================================================================
   Initialization
   ========================================================================== */
document.addEventListener('DOMContentLoaded', async () => {
  // 1. Check for token and load profile
  if (state.token) {
    const user = await fetchUserProfile();
    if (user) {
      updateAuthUI();
      // If we are logged in, automatically connect WebSocket if we load into #chat
      if (window.location.hash === '#chat') {
        connectWebSocket();
      }
    }
  }

  // 2. Setup SPA Router
  window.addEventListener('hashchange', router);
  router(); // Run on first load

  // 3. Attach Event Listeners
  
  // Forms
  document.getElementById('form-login').addEventListener('submit', handleLogin);
  document.getElementById('form-register').addEventListener('submit', handleRegister);
  document.getElementById('form-create-room').addEventListener('submit', handleCreateRoom);
  document.getElementById('chat-send-form').addEventListener('submit', handleSendMessage);

  // Logout
  document.getElementById('btn-logout').addEventListener('click', logout);

  // Create Room Modal Toggles
  document.getElementById('btn-open-create-room').addEventListener('click', () => toggleModal('modal-create-room', true));
  document.getElementById('btn-close-create-room').addEventListener('click', () => toggleModal('modal-create-room', false));
  document.getElementById('btn-cancel-create-room').addEventListener('click', () => toggleModal('modal-create-room', false));

  // Add Member Modal Toggles
  document.getElementById('btn-open-add-member').addEventListener('click', () => {
    toggleModal('modal-add-member', true);
    loadUsersForAddMember();
  });
  document.getElementById('btn-close-add-member').addEventListener('click', () => toggleModal('modal-add-member', false));
  document.getElementById('btn-cancel-add-member').addEventListener('click', () => toggleModal('modal-add-member', false));

  // Close modals on clicking overlay background
  document.querySelectorAll('.modal-overlay').forEach(overlay => {
    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) {
        toggleModal(overlay.id, false);
      }
    });
  });

  // User Profile modal close hooks
  const btnCloseProfile = document.getElementById('btn-close-user-profile');
  if (btnCloseProfile) {
    btnCloseProfile.addEventListener('click', () => toggleModal('modal-user-profile', false));
  }
  const btnCloseProfileVal = document.getElementById('btn-close-view-profile-modal');
  if (btnCloseProfileVal) {
    btnCloseProfileVal.addEventListener('click', () => toggleModal('modal-user-profile', false));
  }
});

function showUserProfileModal(userId, name, email) {
  document.getElementById('modal-view-profile-name').textContent = name;
  document.getElementById('modal-view-profile-email').textContent = email || 'No email shared';
  document.getElementById('modal-view-profile-id').textContent = userId || '-';
  
  const avatarChar = name ? name.charAt(0).toUpperCase() : '?';
  document.getElementById('modal-view-profile-avatar').textContent = avatarChar;
  
  toggleModal('modal-user-profile', true);
}
