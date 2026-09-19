/* MailNotifier — Ortak JavaScript */

// ── Badge & Zil ────────────────────────────────────────────────────────────
function updateBadge(count) {
    document.querySelectorAll('.js-notif-badge').forEach(badge => {
        if (count > 0) {
            badge.textContent = count > 99 ? '99+' : count;
            badge.classList.remove('d-none');
        } else {
            badge.classList.add('d-none');
        }
    });
}

function currentBadgeCount() {
    const badge = document.querySelector('.js-notif-badge');
    return badge ? (parseInt(badge.textContent) || 0) : 0;
}

function incrementBadge() {
    updateBadge(currentBadgeCount() + 1);
    document.querySelectorAll('.js-notif-badge').forEach(badge => {
        badge.classList.remove('pop');
        void badge.offsetWidth;
        badge.classList.add('pop');
    });
    ringBell();
}

// Yeni bildirim gelince zil sallanır
function ringBell() {
    document.querySelectorAll('.js-bell').forEach(bell => {
        bell.classList.remove('bell-ring');
        void bell.offsetWidth;
        bell.classList.add('bell-ring');
        bell.addEventListener('animationend', () => bell.classList.remove('bell-ring'), { once: true });
    });
}

// ── Toast ──────────────────────────────────────────────────────────────────
function showToast(message) {
    let container = document.getElementById('mn-toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'mn-toast-container';
        container.style.cssText = 'position:fixed;top:84px;right:20px;z-index:1100;display:flex;flex-direction:column;gap:10px;max-width:380px;';
        document.body.appendChild(container);
    }
    const item = document.createElement('div');
    item.className = 'mn-toast';

    const icon = document.createElement('span');
    icon.style.cssText = 'font-size:18px;flex-shrink:0';
    icon.textContent = '📬';

    // Mesaj mail içeriğinden türeyebildiği için HTML olarak değil, düz metin olarak basılır
    const text = document.createElement('div');
    text.style.flex = '1';
    text.textContent = message;

    const close = document.createElement('button');
    close.textContent = '×';
    close.addEventListener('click', () => item.remove());

    item.append(icon, text, close);
    container.prepend(item);
    const timer = setTimeout(() => {
        item.style.animation = 'mnFadeOut .3s ease forwards';
        setTimeout(() => item.remove(), 300);
    }, 6000);
    close.addEventListener('click', () => clearTimeout(timer));
}

// ── Tarayıcı Bildirimi ─────────────────────────────────────────────────────
function requestBrowserPermission() {
    if ('Notification' in window && Notification.permission === 'default') {
        Notification.requestPermission();
    }
}

function showBrowserNotification(message) {
    if ('Notification' in window && Notification.permission === 'granted') {
        new Notification('MailNotifier', { body: message, icon: '/favicon.ico' });
    }
}

// ── WebSocket ──────────────────────────────────────────────────────────────
let _stompClient = null;

function initWebSocket(userId) {
    if (!userId || userId === 'unknown') return;
    _stompClient = new StompJs.Client({
        webSocketFactory: () => new SockJS('/ws'),
        reconnectDelay: 5000,
        onConnect: () => {
            _stompClient.subscribe('/topic/notifications/' + userId, (frame) => {
                const notification = JSON.parse(frame.body);
                showToast(notification.message);
                showBrowserNotification(notification.message);
                incrementBadge();
            });
        }
    });
    _stompClient.activate();
}

// ── API Yardımcısı ─────────────────────────────────────────────────────────
async function apiCall(url, options = {}) {
    const res = await fetch(url, {
        headers: { 'Content-Type': 'application/json', ...options.headers },
        ...options
    });
    return res.json();
}

// ── Türkçe Relative Tarih ─────────────────────────────────────────────────
function relativeTime(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return dateStr;
    const diff = Math.floor((Date.now() - date.getTime()) / 1000);
    if (diff < 60)       return 'Az önce';
    if (diff < 3600)     return Math.floor(diff / 60) + ' dk önce';
    if (diff < 86400)    return Math.floor(diff / 3600) + ' sa önce';
    const days = Math.floor(diff / 86400);
    if (days === 1)      return 'Dün';
    if (days < 7)        return days + ' gün önce';
    return date.toLocaleDateString('tr-TR', { day: 'numeric', month: 'long', year: 'numeric' });
}

// data-time niteliği olan tüm elementlere relative time uygula
function applyRelativeTimes() {
    document.querySelectorAll('[data-time]').forEach(el => {
        const t = el.getAttribute('data-time');
        if (t) {
            el.textContent = relativeTime(t);
            el.title = new Date(t).toLocaleString('tr-TR');
        }
    });
}

// ── Confidence Badge Sınıfı ────────────────────────────────────────────────
function confidenceBadgeClass(level) {
    if (level === 'HIGH')   return 'badge badge-high';
    if (level === 'MEDIUM') return 'badge badge-medium';
    return 'badge badge-low';
}

function confidenceLabel(level) {
    if (level === 'HIGH')   return 'Yüksek';
    if (level === 'MEDIUM') return 'Orta';
    return 'Düşük';
}
