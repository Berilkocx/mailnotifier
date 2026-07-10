/* MailNotifier — Ortak JavaScript */

// ── Badge ──────────────────────────────────────────────────────────────────
function updateBadge(count) {
    const badge = document.getElementById('notification-badge');
    if (!badge) return;
    if (count > 0) {
        badge.textContent = count > 99 ? '99+' : count;
        badge.classList.remove('d-none');
    } else {
        badge.classList.add('d-none');
    }
}

function incrementBadge() {
    const badge = document.getElementById('notification-badge');
    if (!badge) return;
    const current = parseInt(badge.textContent) || 0;
    updateBadge(current + 1);
}

// ── Toast ──────────────────────────────────────────────────────────────────
function showToast(message) {
    let container = document.getElementById('mn-toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'mn-toast-container';
        container.style.cssText = 'position:fixed;top:80px;right:20px;z-index:9999;display:flex;flex-direction:column;gap:10px;max-width:380px;';
        document.body.appendChild(container);
    }
    const item = document.createElement('div');
    item.style.cssText = 'background:white;border-radius:10px;border-left:4px solid #0d6efd;box-shadow:0 4px 20px rgba(0,0,0,.15);padding:14px 16px;font-size:14px;color:#374151;line-height:1.5;animation:mnSlideIn .3s ease;cursor:pointer;display:flex;align-items:flex-start;gap:10px;';
    item.innerHTML =
        '<span style="font-size:18px;flex-shrink:0">📬</span>' +
        '<div style="flex:1">' + message + '</div>' +
        '<button style="background:none;border:none;cursor:pointer;color:#9ca3af;font-size:18px;padding:0;line-height:1" onclick="this.parentElement.remove()">×</button>';
    container.prepend(item);
    const timer = setTimeout(() => {
        item.style.animation = 'mnFadeOut .3s ease forwards';
        setTimeout(() => item.remove(), 300);
    }, 5000);
    item.querySelector('button').addEventListener('click', () => clearTimeout(timer));
}

// CSS animasyonları (inject once)
(function injectToastStyles() {
    if (document.getElementById('mn-toast-styles')) return;
    const style = document.createElement('style');
    style.id = 'mn-toast-styles';
    style.textContent = '@keyframes mnSlideIn{from{opacity:0;transform:translateX(40px)}to{opacity:1;transform:translateX(0)}}@keyframes mnFadeOut{from{opacity:1;transform:translateX(0)}to{opacity:0;transform:translateX(40px)}}';
    document.head.appendChild(style);
})();

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
    if (diff < 3600)     return Math.floor(diff / 60) + ' dakika önce';
    if (diff < 86400)    return Math.floor(diff / 3600) + ' saat önce';
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
    if (level === 'HIGH')   return 'badge bg-success';
    if (level === 'MEDIUM') return 'badge bg-warning text-dark';
    return 'badge bg-secondary';
}

function confidenceLabel(level) {
    if (level === 'HIGH')   return 'Yüksek';
    if (level === 'MEDIUM') return 'Orta';
    return 'Düşük';
}
