const STORAGE_KEY = 'ev_hub_current_user';

function getCurrentUser() {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        return raw ? JSON.parse(raw) : null;
    } catch {
        return null;
    }
}

function saveCurrentUser(userData) {
    const normalized = {
        id: userData.id ?? userData.userId,
        fullName: userData.fullName ?? userData.name ?? '',
        phoneNumber: userData.phoneNumber ?? '',
        role: (userData.role || 'CUSTOMER').toUpperCase()
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(normalized));
    return normalized;
}

function logout() {
    localStorage.removeItem(STORAGE_KEY);
    window.location.href = '/index.html';
}

function requireAuth(expectedRole) {
    const user = getCurrentUser();
    if (!user || !user.id) {
        window.location.href = '/index.html';
        return null;
    }
    if (expectedRole && user.role !== expectedRole) {
        window.location.href = user.role === 'ADMIN' ? '/admin.html' : '/customer.html';
        return null;
    }
    return user;
}

function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function formatMoney(value) {
    if (value === null || value === undefined || value === '') return '0 ₫';
    return Number(value).toLocaleString('vi-VN') + ' ₫';
}

function formatDateTime(isoStr) {
    if (!isoStr) return '—';
    const clean = String(isoStr).replace('T', ' ').slice(0, 19);
    const [datePart, timePart] = clean.split(' ');
    if (!datePart || !timePart) return clean;
    const [y, m, d] = datePart.split('-');
    return `${timePart} ${d}/${m}/${y}`;
}

function ensureToastContainer() {
    let box = document.getElementById('toast-container');
    if (!box) {
        box = document.createElement('div');
        box.id = 'toast-container';
        box.className = 'fixed top-4 right-4 z-[9999] flex flex-col gap-2 max-w-md w-full pointer-events-none px-4 sm:px-0';
        document.body.appendChild(box);
    }
    return box;
}

function showToast(message, type = 'success', errorCode = null) {
    const container = ensureToastContainer();
    const isErr = type === 'error';
    const isWarn = type === 'warning';

    const style = isErr
        ? 'bg-red-50 border-red-400 text-red-900'
        : isWarn
            ? 'bg-amber-50 border-amber-400 text-amber-900'
            : 'bg-emerald-50 border-emerald-400 text-emerald-900';

    const badge = errorCode
        ? `<span class="inline-block mr-1.5 px-1.5 py-0.5 text-xs font-mono font-bold rounded bg-red-600 text-white">${escapeHtml(errorCode)}</span>`
        : '';

    const el = document.createElement('div');
    el.className = `pointer-events-auto border-l-4 ${style} shadow-lg rounded-r-lg px-4 py-3 text-sm transition-all duration-300 flex items-start justify-between gap-3`;
    el.innerHTML = `
        <div class="leading-snug break-words">
            ${badge}<span>${escapeHtml(message)}</span>
        </div>
        <button type="button" class="text-xs font-bold opacity-60 hover:opacity-100 shrink-0">✕</button>
    `;

    el.querySelector('button').onclick = () => el.remove();
    container.appendChild(el);

    setTimeout(() => {
        el.classList.add('opacity-0', 'translate-x-4');
        setTimeout(() => el.remove(), 300);
    }, isErr ? 6000 : 3500);
}

async function apiFetch(path, options = {}) {
    const user = getCurrentUser();
    const headers = { ...(options.headers || {}) };

    if (options.body !== undefined && options.body !== null && !headers['Content-Type']) {
        headers['Content-Type'] = 'application/json';
    }

    if (user && user.id) {
        if (!headers['X-User-Id']) {
            headers['X-User-Id'] = String(user.id);
        }
        if (path.startsWith('/api/admin') && !headers['X-Admin-Id']) {
            headers['X-Admin-Id'] = String(user.id);
        }
    }

    const fetchOptions = {
        method: options.method || 'GET',
        headers
    };

    if (options.body !== undefined && options.body !== null) {
        fetchOptions.body = typeof options.body === 'string'
            ? options.body
            : JSON.stringify(options.body);
    }

    let response;
    try {
        response = await fetch(path, fetchOptions);
    } catch (networkErr) {
        const msg = 'Không kết nối được tới server Backend (kiểm tra Spring Boot đang chạy cổng 8080).';
        if (!options.silentError) showToast(msg, 'error', 'NETWORK');
        throw new Error(msg);
    }

    const rawText = await response.text();
    let data = null;
    if (rawText) {
        try {
            data = JSON.parse(rawText);
        } catch {
            data = { detailMessage: rawText };
        }
    }

    if (!response.ok) {
        const errorCode = data?.errorCode || `HTTP_${response.status}`;
        let detail = data?.detailMessage || data?.message || data?.error || `Yêu cầu thất bại (${response.status})`;
        if (data?.errors && typeof data.errors === 'object') {
            const firstFieldErr = Object.values(data.errors).join(' | ');
            if (firstFieldErr) detail = firstFieldErr;
        }
        if (!options.silentError) {
            showToast(detail, 'error', errorCode);
        }
        const err = new Error(detail);
        err.status = response.status;
        err.errorCode = errorCode;
        err.data = data;
        throw err;
    }

    return data;
}