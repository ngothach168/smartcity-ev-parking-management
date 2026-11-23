(function () {
    'use strict';

// ==================== TIỆN ÍCH ====================
    const $ = id => document.getElementById(id);
    const e = v => escapeHtml(String(v ?? ''));
    const pick = (r, ...ks) => { for (const k of ks) if (r[k] !== undefined && r[k] !== null) return r[k]; return ''; };
    const send = (method, url, body) => apiFetch(url, { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
    const vn = d => d ? String(d).slice(0, 10).split('-').reverse().join('/') : '—';
    const dt = v => v ? String(v).replace('T', ' ').slice(0, 19) : '---';
    const TODAY = new Date(Date.now() - new Date().getTimezoneOffset() * 6e4).toISOString().slice(0, 10);
    const MONTH_START = TODAY.slice(0, 8) + '01';

    const IN   = 'w-full px-3 py-2 text-xs border border-slate-300 rounded-lg bg-white';
    const BTN  = 'px-5 py-2 text-xs font-semibold bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg transition';
    const BTN2 = 'px-3 py-1.5 text-xs font-semibold bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg transition';
    const NAV_ON  = 'w-full text-left px-3 py-2.5 rounded-lg font-semibold bg-emerald-600 text-white transition';
    const NAV_OFF = 'w-full text-left px-3 py-2.5 rounded-lg font-medium text-slate-300 hover:bg-slate-800 hover:text-white transition';

    const badge = s => `<span class="px-2 py-0.5 text-[11px] font-bold rounded-full ${s === 'CHARGING' ? 'bg-emerald-100 text-emerald-800' : 'bg-blue-100 text-blue-800'}">${e(s)}</span>`;
    const stat = s => `<span class="px-2 py-0.5 text-[11px] font-bold rounded-full ${s === 'AVAILABLE' ? 'bg-emerald-100 text-emerald-800' : s === 'IN_USE' ? 'bg-blue-100 text-blue-800' : 'bg-amber-100 text-amber-800'}">${e(s)}</span>`;
    const mono = v => `<span class="font-mono">${e(v)}</span>`;
    const pl = v => (v === '' || v == null) ? '---' : `<code class="block max-w-xs whitespace-pre-wrap break-all text-[11px] text-slate-600">${e(typeof v === 'object' ? JSON.stringify(v) : v)}</code>`;

    const API = {
        daily: '/api/admin/reports/daily-revenue',
        summary: '/api/admin/reports/revenue-summary',
        devStatus: '/api/admin/reports/device-status',
        locations: '/api/locations',
        locSave: '/api/admin/locations',
        spotsOf: id => `/api/locations/${id}/parking-spots`,
        chargersOf: id => `/api/locations/${id}/ev-chargers`,
        spots: '/api/admin/parking-spots',
        chargers: '/api/admin/ev-chargers',
        tariffs: '/api/tariffs',
        tariffSchedule: '/api/admin/tariffs/schedule',
        // Đường dẫn từng tab Kiểm toán. Backend đặt tên không theo 1 quy luật chung nên khai báo riêng từng cái.
        audit: {
            activity: '/api/admin/reports/admin-activity',        // đã xác nhận chạy được
            tariffs: '/api/admin/reports/audit/tariffs',          // CHƯA xác nhận, cần đối chiếu với backend
            locations: '/api/admin/reports/audit/locations',      // CHƯA xác nhận
            devices: '/api/admin/reports/audit/devices'           // CHƯA xác nhận
        }
    };

    // Chống lồng trang: nếu admin.html bị nạp bên trong iframe thì nhảy ra ngoài, không dựng thêm 1 lớp admin nữa.
    if (window.self !== window.top) { window.top.location.replace(window.location.href); return; }

    const me = requireAuth('ADMIN');
    if (!me) return;
    $('sidebarAdminName').textContent = me.fullName || 'Quản trị viên';
    $('sidebarAdminPhone').textContent = me.phoneNumber || `Admin #${me.id}`;

    const AUTO = {};      // tab nào tự tải khi mở
    let locs = null;      // cache danh sách khu vực

    async function loadLocations() {
        if (!locs) locs = await apiFetch(API.locations);
        return locs;
    }

// Lỗi từ apiFetch đã được api.js báo toast; chỉ báo thêm lỗi JS thật, không nuốt im lặng.
    async function guard(fn) {
        try { return await fn(); }
        catch (err) {
            console.error(err);
            if (err instanceof TypeError || err instanceof ReferenceError || err instanceof RangeError) showToast('Lỗi hiển thị: ' + err.message, 'error');
        }
    }

// ==================== KHUNG KẾT QUẢ ====================
// head: thêm ">" đầu chuỗi để canh phải. rows/total: mảng ô (HTML đã escape).
    function card(r) {
        const R = h => h[0] === '>';
        const th = r.head.map(h => `<th class="px-3 py-3 ${R(h) ? 'text-right' : ''}">${e(R(h) ? h.slice(1) : h)}</th>`).join('');
        const tr = (cells, cls) => `<tr class="${cls}">${cells.map((x, i) => `<td class="px-3 py-2.5 ${R(r.head[i]) ? 'text-right' : ''}">${x}</td>`).join('')}</tr>`;
        const body = r.rows.length
            ? r.rows.map(c => tr(c, 'hover:bg-slate-50')).join('') + (r.total ? tr(r.total, 'bg-slate-900 text-white font-bold') : '')
            : `<tr><td colspan="${r.head.length}" class="text-center py-8 text-slate-400">${e(r.empty || 'Không có dữ liệu.')}</td></tr>`;
        const kpi = (r.kpis || []).length
            ? `<div class="p-4 bg-slate-50 border-b border-slate-200 grid grid-cols-2 ${r.kpis.length === 3 ? 'lg:grid-cols-3' : 'lg:grid-cols-4'} gap-3">${r.kpis.map(k => `
            <div class="bg-white rounded-xl border border-slate-200 p-3">
                <div class="text-[11px] text-slate-500 font-medium">${e(k.label)}</div>
                <div class="text-base font-bold mt-0.5 ${k.cls || 'text-slate-900'}">${e(k.value)}</div>
            </div>`).join('')}</div>` : '';
        return `
        <div class="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
            <div class="px-5 py-4 border-b border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                    <h2 class="font-bold text-sm text-slate-900">${e(r.title)}</h2>
                    ${r.sub ? `<p class="text-xs text-slate-500 mt-0.5">${e(r.sub)}</p>` : ''}
                </div>
                <span class="text-xs font-semibold px-2.5 py-1 rounded-full bg-emerald-100 text-emerald-800 self-start sm:self-auto">${e(r.badge || '')}</span>
            </div>
            ${kpi}
            <div class="overflow-x-auto">
                <table class="w-full text-xs text-left">
                    <thead class="bg-slate-50 border-b border-slate-200 uppercase text-slate-500"><tr>${th}</tr></thead>
                    <tbody class="divide-y divide-slate-100">${body}</tbody>
                </table>
            </div>
        </div>`;
    }

// ==================== DỰNG 1 TAB: tiêu đề + form lọc riêng + khung kết quả riêng ====================
    function fieldHtml(key, f) {
        const id = `f-${key}-${f.id}`, req = f.required ? 'required' : '';
        const ctl = f.type === 'select'
            ? `<select id="${id}" ${req} class="${IN}">${(f.options || []).map(o => `<option value="${e(o[0])}">${e(o[1])}</option>`).join('')}</select>`
            : `<input id="${id}" type="${f.type || 'text'}" ${f.type === 'number' ? 'step="any"' : ''} value="${e(f.value ?? '')}" placeholder="${e(f.ph || '')}" ${req} class="${IN}">`;
        return `<div><label class="block text-xs font-semibold text-slate-600 mb-1">${e(f.label)}</label>${ctl}</div>`;
    }

// o: { title, desc, fields, btn, action, auto, run(f) -> dữ liệu cho card() hoặc null }
// Không có btn => đổi bộ lọc là tự chạy lại. Trả về hàm chạy lại tab.
    function mount(key, o) {
        const fields = o.fields || [];
        $('pane-' + key).innerHTML = `
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
                <h1 class="text-xl font-bold text-slate-900">${e(o.title)}</h1>
                ${o.desc ? `<p class="text-xs text-slate-500 mt-0.5">${e(o.desc)}</p>` : ''}
            </div>
            ${o.action || ''}
        </div>
        ${fields.length ? `
        <form id="form-${key}" class="bg-white rounded-2xl shadow-sm border border-slate-200 p-5 space-y-4">
            <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">${fields.map(f => fieldHtml(key, f)).join('')}</div>
            ${o.btn ? `<div class="flex justify-end"><button id="btn-${key}" type="submit" class="${BTN}">${e(o.btn)}</button></div>` : ''}
        </form>` : ''}
        <div id="out-${key}"></div>`;

        const exec = () => guard(async () => {
            const f = {};
            fields.forEach(x => { f[x.id] = $(`f-${key}-${x.id}`).value; });
            const b = $('btn-' + key), txt = b ? b.textContent : '';
            if (b) { b.disabled = true; b.textContent = 'Đang tải...'; }
            try {
                const res = await o.run(f);
                if (res) $('out-' + key).innerHTML = card(res);
            } finally {
                if (b) { b.disabled = false; b.textContent = txt; }
            }
        });
        const form = $('form-' + key);
        if (form) form.addEventListener('submit', ev => { ev.preventDefault(); exec(); });
        if (form && !o.btn) form.addEventListener('input', exec);
        if (o.auto) AUTO[key] = exec;
        return exec;
    }

    const checkDates = f => {
        if (f.start > f.end) { showToast('Ngày bắt đầu không được lớn hơn ngày kết thúc.', 'error'); return false; }
        return true;
    };

// ==================== TAB ĐẶT CHỖ: nhúng trang customer.html ====================
// customer.html tự có tab "Phiên của tôi", "Bảng giá" nên admin chỉ cần 1 tab này.
// Trong khung nhúng: ẩn nút "Trang Quản trị" (tránh admin.html lồng trong admin.html) và ẩn khối tên + SĐT + Đăng xuất
// ở góc trên phải (admin đã có sẵn ở sidebar bên trái).
    function tabFrame(key, url) {
        $('pane-' + key).innerHTML =
            `<div class="flex justify-end mb-2"><button type="button" id="back-${key}" class="${BTN2}">← Quay lại trang đầu</button></div>
            <iframe id="frame-${key}" title="${e(key)}" class="w-full bg-white rounded-2xl shadow-sm border border-slate-200" style="height: calc(100vh - 7rem); min-height: 480px;"></iframe>`;
        const fr = $('frame-' + key);
        $('back-' + key).addEventListener('click', () => fr.setAttribute('src', url));

        const hide = el => el && el.style.setProperty('display', 'none', 'important');

        // Ẩn khối thông tin tài khoản trong customer.html: nút Đăng xuất + (tên & SĐT admin)
        function hideUserBox(d) {
            d.querySelectorAll('a, button').forEach(el => {
                const goesAdmin = /admin\.html/i.test(el.getAttribute('href') || '') || /admin\.html/i.test(el.getAttribute('onclick') || '');
                if (goesAdmin || /Trang Quản trị/i.test(el.textContent)) hide(el);
                if (/Đăng xuất/i.test(el.textContent) || /logout/i.test(el.getAttribute('onclick') || '')) hide(el);
            });
            const phone = me.phoneNumber, name = me.fullName;
            if (!phone || !d.body) return;
            const all = [...d.body.querySelectorAll('*')];
            const phoneEl = all.find(el => el.children.length === 0 && el.textContent.includes(phone));
            if (!phoneEl) return;
            // phần tử nhỏ nhất chứa cả SĐT lẫn tên
            let box = phoneEl;
            while (name && !box.textContent.includes(name) && box.parentElement && box.parentElement !== d.body) box = box.parentElement;
            const expected = (name || '').length + phone.length;
            if (box.textContent.trim().length <= expected + 40) {
                hide(box);                                      // đúng là khối tên + SĐT nhỏ gọn
            } else {                                            // quá lớn (dính cả menu): chỉ ẩn riêng chữ SĐT và chữ tên
                hide(phoneEl);
                if (name) hide(all.find(el => el.children.length === 0 && el.textContent.trim() === name));
            }
        }

        fr.addEventListener('load', () => {
            try {
                // Khung bị chuyển khỏi customer.html (đăng xuất hoặc hết phiên) thì đưa cả trang sang đó, không để lồng trong admin
                const w = fr.contentWindow;
                if (w.location.href === 'about:blank') return;
                if (w.location.pathname !== new URL(url, window.location.origin).pathname) {
                    window.top.location.replace(w.location.href);
                    return;
                }
                const d = fr.contentDocument;
                if (!d || !d.body) return;
                hideUserBox(d);
                // customer.html vẽ lại header sau khi tải xong thì ẩn lại
                new MutationObserver(() => hideUserBox(d)).observe(d.body, { childList: true, subtree: true, characterData: true });
            } catch (err) { console.warn('Không chỉnh được khung nhúng:', err); }
        });
        AUTO[key] = () => { fr.setAttribute('src', url); };      // mỗi lần mở tab là nạp lại trang từ đầu
    }
    const tabSearch = () => tabFrame('search', '/customer.html');

// ==================== BÁO CÁO ====================
    function tabRevenue() {
        mount('revenue', {
            title: 'Doanh thu theo dịch vụ', btn: 'Xuất thống kê',
            fields: [
                { id: 'service', label: 'Loại dịch vụ', type: 'select', options: [['', 'Cả hai (Đỗ xe & Sạc điện)'], ['PARKING', 'Chỉ Đỗ xe (PARKING)'], ['CHARGING', 'Chỉ Sạc điện (CHARGING)']] },
                { id: 'start', label: 'Từ ngày', type: 'date', value: MONTH_START, required: true },
                { id: 'end', label: 'Đến ngày', type: 'date', value: TODAY, required: true }
            ],
            async run(f) {
                if (!checkDates(f)) return null;
                const q = new URLSearchParams({ startDate: f.start, endDate: f.end });
                if (f.service) q.set('serviceType', f.service);
                const raw = await apiFetch(`${API.daily}?${q}`);
                const g = new Map();
                raw.forEach(r => {
                    const w = { key: String(r.revenueDate).slice(0, 10), label: vn(r.revenueDate) };
                    const loc = r.locationName || r.locationCode || `Trạm #${r.locationId}`;
                    const svc = r.serviceType || '';
                    const k = `${w.key}|${loc}|${svc}`;
                    const a = g.get(k) || { w, loc, svc, n: 0, rev: 0 };
                    a.n += Number(r.totalSessions ?? r.sessionCount ?? 0);
                    a.rev += Number(r.totalRevenue ?? 0);
                    g.set(k, a);
                });
                const list = [...g.values()].sort((a, b) => b.w.key.localeCompare(a.w.key) || a.loc.localeCompare(b.loc));
                const sumN = list.reduce((s, x) => s + x.n, 0), sumR = list.reduce((s, x) => s + x.rev, 0);
                const svcText = f.service === 'PARKING' ? 'Đỗ xe' : f.service === 'CHARGING' ? 'Sạc điện' : 'Cả hai dịch vụ';
                return {
                    title: `Doanh thu theo ngày — ${svcText}`, sub: `Từ ${vn(f.start)} đến ${vn(f.end)}`, badge: `${list.length} dòng`,
                    kpis: [
                        { label: 'Tổng doanh thu', value: formatMoney(sumR), cls: 'text-emerald-700' },
                        { label: 'Tổng số phiên tính tiền', value: String(sumN) },
                        { label: 'Phạm vi dịch vụ', value: svcText }
                    ],
                    head: ['Ngày', 'Khu vực', 'Dịch vụ', '>Số phiên', '>Doanh thu'],
                    rows: list.map(x => [mono(x.w.label), `<span class="font-medium">${e(x.loc)}</span>`, badge(x.svc), mono(x.n), `<b class="text-emerald-700">${formatMoney(x.rev)}</b>`]),
                    total: ['TỔNG CỘNG', '', '', String(sumN), formatMoney(sumR)],
                    empty: 'Không có dữ liệu doanh thu trong khoảng đã chọn.'
                };
            }
        });
    }

    function tabSummary() {
        mount('summary', {
            title: 'Doanh thu theo khu vực', btn: 'Xuất thống kê',
            fields: [
                { id: 'loc', label: 'Khu vực', type: 'select', options: [['', 'Tất cả khu vực']] },
                { id: 'start', label: 'Từ ngày', type: 'date', value: MONTH_START, required: true },
                { id: 'end', label: 'Đến ngày', type: 'date', value: TODAY, required: true }
            ],
            async run(f) {
                if (!checkDates(f)) return null;
                const q = new URLSearchParams({ startDate: f.start, endDate: f.end });
                if (f.loc) q.set('locationId', f.loc);
                const all = await apiFetch(`${API.summary}?${q}`);
                const t = all.find(r => r.locationCode === 'TỔNG');
                const rows = all.filter(r => r.locationCode !== 'TỔNG');
                const cells = (r, tot) => [
                    tot ? 'TỔNG' : mono(r.locationCode), tot ? 'Toàn hệ thống' : e(r.locationName),
                    mono(r.completedCount ?? 0), mono(r.expiredCount ?? 0), mono(r.cancelledWithFeeCount ?? 0), mono(r.cancelledFreeCount ?? 0),
                    formatMoney(r.serviceRevenue), formatMoney(r.penaltyRevenue),
                    tot ? formatMoney(r.totalRevenue) : `<b class="text-emerald-700">${formatMoney(r.totalRevenue)}</b>`
                ];
                return {
                    title: 'Doanh thu & phí phạt theo khu vực', sub: `Từ ${vn(f.start)} đến ${vn(f.end)}`, badge: `${rows.length} khu vực`,
                    kpis: t ? [
                        { label: 'Tổng doanh thu thực thu', value: formatMoney(t.totalRevenue), cls: 'text-emerald-700' },
                        { label: 'Doanh thu dịch vụ', value: formatMoney(t.serviceRevenue), cls: 'text-blue-700' },
                        { label: 'Doanh thu phí phạt giữ chỗ', value: formatMoney(t.penaltyRevenue), cls: 'text-amber-700' }
                    ] : [],
                    head: ['Mã trạm', 'Tên khu vực', '>Hoàn tất', '>Quá hạn', '>Hủy mất cọc', '>Hủy miễn phí', '>DT dịch vụ', '>DT phí phạt', '>Tổng doanh thu'],
                    rows: rows.map(r => cells(r, false)), total: (t && !f.loc) ? cells(t, true) : null,   // chọn 1 khu vực thì dòng TỔNG trùng dòng đó nên ẩn
                    empty: 'Không có dữ liệu đối soát trong khoảng ngày này.'
                };
            }
        });
        // Nạp danh sách khu vực vào ô chọn mỗi khi mở tab (giữ lại lựa chọn đang có)
        AUTO.summary = () => guard(async () => {
            const list = await loadLocations();
            const sel = $('f-summary-loc'), prev = sel.value;
            sel.innerHTML = '<option value="">Tất cả khu vực</option>' + list.map(l => `<option value="${Number(l.id)}">${e(l.locationCode)} — ${e(l.name)}</option>`).join('');
            sel.value = prev;
        });
    }

    function tabDevStatus() {
        mount('devstatus', {
            title: 'Thống kê thiết bị', btn: 'Xuất thống kê',
            fields: [{ id: 'type', label: 'Loại thiết bị', type: 'select', options: [['ALL', 'Cả hai (Ô đỗ xe & Trụ sạc)'], ['PARKING', 'Chỉ Ô đỗ xe'], ['CHARGING', 'Chỉ Trụ sạc điện']] }],
            async run(f) {
                const data = await apiFetch(API.devStatus);
                const sp = f.type !== 'CHARGING', ch = f.type !== 'PARKING';
                const keys = [
                    ...(sp ? [['availableSpots', 'Ô đỗ trống'], ['inUseSpots', 'Ô đỗ đang dùng'], ['maintenanceSpots', 'Ô đỗ bảo trì']] : []),
                    ...(ch ? [['availableChargers', 'Trụ sạc trống'], ['inUseChargers', 'Trụ sạc đang dùng'], ['maintenanceChargers', 'Trụ sạc bảo trì']] : [])
                ];
                const clr = ['text-emerald-700', 'text-blue-700', 'text-amber-700'];
                const n = (r, k) => Number(r[k] || 0);
                const maint = r => keys.reduce((s, [k], i) => s + (i % 3 === 2 ? n(r, k) : 0), 0);
                const all = data.slice().sort((a, b) => maint(b) - maint(a));      // nhiều bảo trì xếp trước
                const sum = k => all.reduce((s, r) => s + n(r, k), 0);
                const rowTotal = r => keys.reduce((s, [k]) => s + n(r, k), 0);
                const byState = i => keys.filter((_, j) => j % 3 === i).reduce((s, [k]) => s + sum(k), 0);
                const grand = all.reduce((s, r) => s + rowTotal(r), 0);
                return {
                    title: 'Tình trạng thiết bị theo khu vực', badge: `${all.length} khu vực`,
                    kpis: [
                        { label: 'Tổng thiết bị', value: String(grand) },
                        { label: 'Đang trống', value: String(byState(0)), cls: 'text-emerald-700' },
                        { label: 'Đang sử dụng', value: String(byState(1)), cls: 'text-blue-700' },
                        { label: 'Đang bảo trì', value: String(byState(2)), cls: 'text-amber-700' }
                    ],
                    head: ['Mã trạm', 'Tên khu vực', ...keys.map(k => '>' + k[1]), '>Tổng'],
                    rows: all.map(r => [`<b class="font-mono">${e(r.locationCode)}</b>`, e(r.locationName),
                        ...keys.map(([k], i) => `<b class="font-mono ${clr[i % 3]}">${n(r, k)}</b>`), `<b>${rowTotal(r)}</b>`]),
                    total: ['TỔNG', 'Toàn hệ thống', ...keys.map(([k]) => String(sum(k))), String(grand)],
                    empty: 'Không có dữ liệu thiết bị.'
                };
            }
        });
    }

// ==================== MODAL DÙNG CHUNG ====================
    let onSave = null;
    function openForm(title, fields, save) {
        const opts = (list, val) => list.map(o => `<option value="${e(o[0])}" ${String(o[0]).toUpperCase() === String(val ?? '').toUpperCase() ? 'selected' : ''}>${e(o[1])}</option>`).join('');
        const initial = id => (fields.find(x => x.id === id) || {}).value;
        const listOf = f => f.optionsBy ? (f.optionsBy[initial(f.dependsOn)] || Object.values(f.optionsBy)[0]) : f.options;
        $('modalTitle').textContent = title;
        $('modalBody').innerHTML = fields.map(f => `
        <div><label class="block font-semibold text-slate-700 mb-1">${e(f.label)}</label>
        ${f.type === 'select'
            ? `<select id="m_${f.id}" class="${IN}">${opts(listOf(f), f.value)}</select>`
            : `<input id="m_${f.id}" type="${f.type || 'text'}" ${f.step ? `step="${f.step}"` : ''} ${f.pattern ? `pattern="${f.pattern}" title="${e(f.title || '')}"` : ''} value="${e(f.value ?? '')}" required class="${IN} ${f.upper ? 'uppercase font-mono' : ''}">`}</div>`).join('');
        // Ô chọn phụ thuộc: đổi "Loại thiết bị" thì danh sách "Phân loại" đổi theo
        fields.filter(f => f.optionsBy).forEach(f => {
            const src = $('m_' + f.dependsOn), tgt = $('m_' + f.id);
            src.addEventListener('change', () => { tgt.innerHTML = opts(f.optionsBy[src.value] || [], ''); });
        });
        onSave = async () => {
            const d = {};
            fields.forEach(f => { const v = $('m_' + f.id).value; d[f.id] = f.upper ? v.toUpperCase() : v; });
            await save(d);
            closeModal();
        };
        $('modal').classList.remove('hidden');
    }
    function closeModal() { $('modal').classList.add('hidden'); }
    $('modalForm').addEventListener('submit', ev => { ev.preventDefault(); guard(() => onSave()); });

// ==================== CẤU HÌNH ====================
    function tabLocations() {
        const exec = mount('locations', {
            title: 'Khu vực', auto: true,
            action: `<button type="button" onclick="addLocation()" class="${BTN} self-start sm:self-auto">+ Thêm khu vực</button>`,
            fields: [{ id: 'kw', label: 'Tìm theo mã hoặc tên khu vực', ph: 'Ví dụ: LOC-01' }],
            async run(f) {
                const all = await loadLocations();
                const k = f.kw.trim().toLowerCase();
                const list = all.filter(l => !k || `${l.locationCode} ${l.name}`.toLowerCase().includes(k));
                return {
                    title: 'Danh sách khu vực', badge: `${list.length} khu vực`,
                    head: ['Mã khu vực', 'Tên & địa chỉ', 'Tọa độ (Lat, Lng)', '>Thao tác'],
                    rows: list.map(l => [mono(l.locationCode),
                        `<div class="font-semibold">${e(l.name)}</div><div class="text-slate-500">${e(l.address)}</div>`,
                        mono(`${l.latitude}, ${l.longitude}`),
                        `<button type="button" onclick="editLocation(${Number(l.id)})" class="${BTN2}">Chỉnh sửa</button>`]),
                    empty: 'Không tìm thấy khu vực nào.'
                };
            }
        });
        AUTO.locations = () => { locs = null; return exec(); };      // mở tab là tải mới
        const form = id => {
            const l = (locs || []).find(x => x.id === id) || {};
            openForm(id ? 'Chỉnh sửa khu vực' : 'Thêm khu vực mới', [
                { id: 'locationCode', label: 'Mã khu vực', value: l.locationCode, upper: true },
                { id: 'name', label: 'Tên khu vực / Trạm', value: l.name },
                { id: 'address', label: 'Địa chỉ chi tiết', value: l.address },
                { id: 'latitude', label: 'Vĩ độ', type: 'number', step: 'any', value: l.latitude },
                { id: 'longitude', label: 'Kinh độ', type: 'number', step: 'any', value: l.longitude }
            ], async d => {
                await send(id ? 'PUT' : 'POST', id ? `${API.locSave}/${id}` : API.locSave, { ...d, latitude: +d.latitude, longitude: +d.longitude });
                showToast('Đã lưu khu vực.', 'success');
                locs = null; exec();
            });
        };
        window.addLocation = () => form(0);
        window.editLocation = id => form(id);
    }

    function tabDevices() {
        let rows = [], last = null;
        const KINDS = {
            PARKING: {
                label: 'Ô đỗ xe', codeLabel: 'Mã ô đỗ', subLabel: 'Loại phương tiện', url: API.spots, codeKey: 'spotCode', subKey: 'vehicleType',
                subs: [['4_WHEELER', '4_WHEELER (Xe 4 bánh)'], ['2_WHEELER', '2_WHEELER (Xe 2 bánh)']]
            },
            CHARGING: {
                label: 'Trụ sạc điện', codeLabel: 'Mã trụ sạc', subLabel: 'Chuẩn cổng sạc', url: API.chargers, codeKey: 'chargerCode', subKey: 'portType',
                subs: [['TYPE_2', 'TYPE_2 (Sạc chuẩn AC)'], ['CCS2', 'CCS2 (Sạc nhanh DC)']]
            }
        };
        const KIND_OPTIONS = [['PARKING', 'Ô đỗ xe (parking-spot)'], ['CHARGING', 'Trụ sạc điện (ev-charger)']];

        // Chọn khu vực hoặc loại thiết bị là hiện danh sách ngay (không có nút bấm)
        const exec = mount('devices', {
            title: 'Thiết bị (ô đỗ & trụ sạc)',
            action: `<button type="button" onclick="addDevice()" class="${BTN} self-start sm:self-auto">+ Thêm thiết bị</button>`,
            fields: [
                { id: 'loc', label: 'Khu vực', type: 'select', options: [['', '-- Chọn khu vực --']] },
                { id: 'type', label: 'Loại thiết bị', type: 'select', options: KIND_OPTIONS }
            ],
            async run(f) {
                if (!f.loc) { $('out-devices').innerHTML = ''; return null; }
                const k = KINDS[f.type];
                rows = await apiFetch(f.type === 'PARKING' ? API.spotsOf(f.loc) : API.chargersOf(f.loc));
                last = { loc: f.loc, type: f.type };
                const loc = (locs || []).find(l => String(l.id) === f.loc);
                return {
                    title: `${k.label} tại ${loc ? loc.name : 'khu vực đã chọn'}`,
                    badge: `${rows.length} thiết bị`,
                    head: ['ID', 'Mã thiết bị', 'Phân loại', 'Trạng thái', '>Thao tác'],
                    rows: rows.map(d => {
                        const busy = d.status === 'IN_USE';
                        return [mono(d.id), `<b class="font-mono">${e(d[k.codeKey])}</b>`, e(d[k.subKey]), stat(d.status),
                            `<button type="button" ${busy ? 'disabled' : ''} onclick="editDevice(${Number(d.id)})" class="${BTN2} ${busy ? 'opacity-40 cursor-not-allowed' : ''}">Chỉnh sửa</button>`];
                    }),
                    empty: 'Khu vực này chưa có thiết bị nào thuộc loại đã chọn.'
                };
            }
        });

        // Mở tab: nạp danh sách khu vực, chọn sẵn khu vực đầu tiên và hiện luôn
        AUTO.devices = () => guard(async () => {
            const list = await loadLocations();
            const sel = $('f-devices-loc'), prev = sel.value;
            sel.innerHTML = '<option value="">-- Chọn khu vực --</option>' + list.map(l => `<option value="${Number(l.id)}">${e(l.locationCode)} — ${e(l.name)}</option>`).join('');
            sel.value = prev || (list.length ? String(list[0].id) : '');
            await exec();
        });

        // THÊM: chọn khu vực -> loại thiết bị -> phân loại (danh sách cố định theo loại) -> gõ mã
        const addForm = () => {
            if (!locs || !locs.length) return showToast('Chưa có khu vực nào để thêm thiết bị.', 'warning');
            openForm('Thêm thiết bị', [
                { id: 'loc', label: 'Khu vực', type: 'select', value: $('f-devices-loc').value, options: locs.map(l => [l.id, `${l.locationCode} — ${l.name}`]) },
                { id: 'kind', label: 'Loại thiết bị', type: 'select', value: $('f-devices-type').value, options: KIND_OPTIONS },
                { id: 'sub', label: 'Phân loại (theo loại thiết bị)', type: 'select', dependsOn: 'kind', optionsBy: { PARKING: KINDS.PARKING.subs, CHARGING: KINDS.CHARGING.subs } },
                { id: 'code', label: 'Mã thiết bị', upper: true }
            ], async v => {
                const k = KINDS[v.kind];
                await send('POST', k.url, { locationId: +v.loc, [k.codeKey]: v.code, [k.subKey]: v.sub });
                showToast('Đã thêm thiết bị.', 'success');
                $('f-devices-loc').value = v.loc;       // chuyển bộ lọc sang đúng khu vực/loại vừa thêm để thấy ngay
                $('f-devices-type').value = v.kind;
                exec();
            });
        };

        // SỬA: loại thiết bị cố định theo dòng đang xem; chỉ đổi mã, phân loại, trạng thái
        const editForm = id => {
            const d = rows.find(x => x.id === id);
            if (!d || !last) return;
            const k = KINDS[last.type];
            openForm(`Chỉnh sửa ${k.label.toLowerCase()}`, [
                { id: 'code', label: k.codeLabel, value: d[k.codeKey], upper: true },
                { id: 'sub', label: k.subLabel, type: 'select', value: d[k.subKey], options: k.subs },
                { id: 'status', label: 'Trạng thái', type: 'select', value: d.status, options: [['AVAILABLE', 'AVAILABLE (Trống)'], ['MAINTENANCE', 'MAINTENANCE (Bảo trì)']] }
            ], async v => {
                await send('PUT', `${k.url}/${id}`, { locationId: +last.loc, [k.codeKey]: v.code, [k.subKey]: v.sub, status: v.status });
                showToast('Đã lưu thiết bị.', 'success');
                exec();
            });
        };
        window.addDevice = addForm;
        window.editDevice = editForm;
    }

    function tabTariffs() {
        const TIME_RE = '^([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$', TIME_HINT = 'Nhập đúng định dạng 24h HH:mm:ss (ví dụ: 08:00:00)';
        const DAY_END = 86399; // 23:59:59
        const sec = t => { const [h, m, s] = String(t).split(':').map(Number); return h * 3600 + m * 60 + s; };
        const hms = x => [Math.floor(x / 3600), Math.floor(x % 3600 / 60), x % 60].map(v => String(v).padStart(2, '0')).join(':');
        const same = (a, b) => a.s === b.s && a.e === b.e && a.price === b.price && a.mult === b.mult;

        // Đặt khoảng [ns, ne) cùng giá vào danh sách (idx = khung đang sửa, -1 = thêm mới).
        // Khung khác bị đè thì bị cắt hoặc xóa; phần giờ bỏ trống do thu hẹp khung thì khung kề bên tự lấp lại.
        function applySlot(list, idx, ns, ne, price, mult) {
            const next = [];
            list.forEach((t, k) => {
                if (k === idx) return;
                if (t.e <= ns || t.s >= ne) { next.push({ ...t }); return; }
                if (t.s < ns) next.push({ ...t, e: ns });
                if (t.e > ne) next.push({ ...t, s: ne });
            });
            const mine = { s: ns, e: ne, price, mult };
            next.push(mine);
            next.sort((a, b) => a.s - b.s);
            // Lấp mọi chỗ trống còn lại: bên phải khung vừa đặt thì khung sau kéo lùi lại, các chỗ khác thì khung trước kéo dài ra.
            for (let k = 1; k < next.length; k++) {
                if (next[k].s > next[k - 1].e) {
                    if (next[k - 1] === mine) next[k].s = mine.e;
                    else next[k - 1].e = next[k].s;
                }
            }
            next[0].s = 0;
            next[next.length - 1].e = DAY_END;
            return next;
        }

        // Bộ khung hợp lệ: bắt đầu 00:00:00, nối liền nhau, kết thúc 23:59:59.
        function coverageError(list) {
            if (!list.length) return 'Chưa có khung giá nào.';
            if (list[0].s !== 0) return 'Khung đầu tiên phải bắt đầu từ 00:00:00.';
            for (let i = 0; i < list.length; i++) {
                if (list[i].e <= list[i].s) return `Khung ${i + 1} có giờ kết thúc không lớn hơn giờ bắt đầu.`;
                if (i && list[i].s !== list[i - 1].e) return `Khung ${i} và khung ${i + 1} không liền nhau.`;
            }
            if (list[list.length - 1].e !== DAY_END) return 'Khung cuối cùng phải kết thúc lúc 23:59:59.';
            return '';
        }

        // Kiểm tra dữ liệu nhập trong form (khớp ràng buộc backend: giá tối đa 8 số nguyên + 2 thập phân, hệ số 0.01 - 9.99).
        function inputError(ns, ne, price, mult) {
            if (!(ns < ne)) return 'Giờ kết thúc phải lớn hơn giờ bắt đầu.';
            if (ne > DAY_END) return 'Giờ kết thúc tối đa là 23:59:59.';
            if (!(Number(price) > 0)) return 'Giá cơ bản phải lớn hơn 0.';
            if (!/^\d{1,8}(\.\d{1,2})?$/.test(String(price))) return 'Giá cơ bản không hợp lệ (tối đa 8 chữ số nguyên và 2 chữ số thập phân).';
            if (!/^\d(\.\d{1,2})?$/.test(String(mult)) || Number(mult) < 0.01) return 'Hệ số giá phải từ 0.01 đến 9.99.';
            return '';
        }
        let draft = null, draftType = '', shown = '';   // draft != null nghĩa là đang ở màn hình sửa cả bộ

        const exec = mount('tariffs', {
            title: 'Biểu giá', auto: true,
            action: `<div class="flex gap-2 self-start sm:self-auto">
                <button type="button" onclick="editTariffSet('PARKING')" class="${BTN}">Chỉnh sửa bảng giá PARKING</button>
                <button type="button" onclick="editTariffSet('CHARGING')" class="${BTN}">Chỉnh sửa bảng giá CHARGING</button>
            </div>`,
            fields: [{ id: 'service', label: 'Loại dịch vụ', type: 'select', options: [['', 'Cả hai (PARKING & CHARGING)'], ['PARKING', 'Chỉ Đỗ xe (PARKING)'], ['CHARGING', 'Chỉ Sạc điện (CHARGING)']] }],
            async run(f) {
                if (draft) {
                    if (f.service === shown) return null;   // mở lại tab: giữ nguyên màn hình đang sửa
                    if (!confirm('Bạn đang sửa bảng giá chưa lưu. Bỏ chỉnh sửa và đổi bộ lọc?')) { $('f-tariffs-service').value = shown; return null; }
                    draft = null;
                }
                shown = f.service;
                const kinds = f.service ? [f.service] : ['PARKING', 'CHARGING'];
                const parts = await Promise.all(kinds.map(k => apiFetch(`${API.tariffs}?serviceType=${k.toLowerCase()}`)));
                const rows = parts.flat().sort((a, b) => String(a.serviceType).localeCompare(String(b.serviceType)) || String(a.startTime).localeCompare(String(b.startTime)));
                return {
                    title: 'Biểu giá hiện tại', badge: `${rows.length} khung giá`,
                    head: ['ID', 'Dịch vụ', 'Khung giờ', '>Giá cơ bản', '>Hệ số', '>Đơn giá áp dụng'],
                    rows: rows.map(t => [mono(t.id), badge(t.serviceType), mono(`${t.startTime} - ${t.endTime}`), formatMoney(t.basePrice), mono(t.multiplier),
                        `<b class="text-emerald-700">${formatMoney(t.basePrice * t.multiplier)}</b>`]),
                    empty: 'Chưa có biểu giá nào.'
                };
            }
        });

        // ---------- Màn hình sửa cả bộ ----------
        const out = () => $('out-tariffs');
        const startOf = i => i === 0 ? 0 : draft[i - 1].e;   // giờ bắt đầu luôn = giờ kết thúc của khung trước
        const parseTime = v => { v = String(v || '').trim(); if (!v) return NaN; if (/^\d{2}:\d{2}$/.test(v)) v += ':00'; return /^\d{2}:\d{2}:\d{2}$/.test(v) ? sec(v) : NaN; };

        function validate() {
            for (let i = 0; i < draft.length; i++) {
                const t = draft[i], s = startOf(i);
                if (!Number.isFinite(t.e)) return `Khung ${i + 1}: giờ kết thúc không hợp lệ.`;
                const err = inputError(s, t.e, t.price, t.mult);
                if (err) return `Khung ${i + 1}: ${err}`;
            }
            if (draft[draft.length - 1].e !== DAY_END) return 'Khung cuối cùng phải kết thúc lúc 23:59:59.';
            return '';
        }
        function showValidation() {
            const msg = validate();
            $('tariff-err').textContent = msg;
            $('tariff-save').disabled = !!msg;
        }
        function renderEditor() {
            const n = draft.length;
            const rowsHtml = draft.map((t, i) => `
                <tr>
                    <td class="px-3 py-2.5">${i + 1}</td>
                    <td class="px-3 py-2.5" data-start="${i}">${mono(hms(startOf(i)))}</td>
                    <td class="px-3 py-2.5">${i === n - 1
                ? mono(hms(DAY_END))
                : `<input type="time" step="1" data-i="${i}" data-f="end" value="${e(hms(t.e))}" class="${IN}">`}</td>
                    <td class="px-3 py-2.5"><input type="number" min="0" step="1" data-i="${i}" data-f="price" value="${e(t.price)}" class="${IN} text-right"></td>
                    <td class="px-3 py-2.5"><input type="number" min="0.01" max="9.99" step="0.01" data-i="${i}" data-f="mult" value="${e(t.mult)}" class="${IN} text-right"></td>
                    <td class="px-3 py-2.5 text-right"><button type="button" data-act="del" data-i="${i}" class="${BTN2}" ${n === 1 ? 'disabled' : ''}>Xóa khung</button></td>
                </tr>`).join('');
            out().innerHTML = `
            <div class="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
                <div class="px-5 py-4 border-b border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                    <div>
                        <h2 class="font-bold text-sm text-slate-900">Chỉnh sửa bảng giá ${e(draftType)}</h2>
                        <p class="text-xs text-slate-500 mt-0.5">Giờ bắt đầu tự nối theo khung trước, khung đầu từ 00:00:00 và khung cuối đến 23:59:59. Bấm "Lưu cả bảng giá" sẽ thay thế toàn bộ bảng giá của ${e(draftType)}.</p>
                    </div>
                    <div class="flex items-center gap-2 self-start sm:self-auto">
                        <button type="button" data-act="cancel" class="${BTN2}">← Quay lại</button>
                        <span class="text-xs font-semibold px-2.5 py-1 rounded-full bg-emerald-100 text-emerald-800">${n} khung giá</span>
                        <button type="button" data-act="add" class="${BTN2}">+ Thêm khung giá</button>
                    </div>
                </div>
                <div class="overflow-x-auto">
                    <table class="w-full text-xs text-left">
                        <thead class="bg-slate-50 border-b border-slate-200 uppercase text-slate-500"><tr>
                            <th class="px-3 py-3">#</th><th class="px-3 py-3">Giờ bắt đầu</th><th class="px-3 py-3">Giờ kết thúc</th>
                            <th class="px-3 py-3 text-right">Giá cơ bản (VNĐ)</th><th class="px-3 py-3 text-right">Hệ số</th><th class="px-3 py-3 text-right">Thao tác</th>
                        </tr></thead>
                        <tbody class="divide-y divide-slate-100">${rowsHtml}</tbody>
                    </table>
                </div>
                <div id="tariff-err" class="px-5 pt-3 text-xs font-semibold text-red-600"></div>
                <div class="px-5 py-4 flex justify-end gap-2">
                    <button type="button" data-act="cancel" class="${BTN2}">Hủy</button>
                    <button type="button" id="tariff-save" data-act="save" class="${BTN} disabled:opacity-40 disabled:cursor-not-allowed">Lưu cả bảng giá</button>
                </div>
            </div>`;
            showValidation();
        }

        // Gõ vào ô: chỉ cập nhật bản nháp, không dựng lại bảng (tránh mất con trỏ). Đổi giờ kết thúc thì ô "Giờ bắt đầu" của khung sau đổi theo.
        out().addEventListener('input', ev => {
            const t = ev.target;
            if (!draft || !t.dataset || t.dataset.f === undefined) return;
            const i = Number(t.dataset.i), f = t.dataset.f;
            if (f === 'end') {
                draft[i].e = parseTime(t.value);
                out().querySelectorAll('[data-start]').forEach(td => { td.innerHTML = mono(hms(startOf(Number(td.dataset.start)))); });
            } else draft[i][f] = t.value;
            showValidation();
        });

        out().addEventListener('click', ev => {
            const b = ev.target.closest('[data-act]');
            if (!b || !draft) return;
            const act = b.dataset.act, i = Number(b.dataset.i);
            if (act === 'cancel') { draft = null; exec(); return; }
            if (act === 'del') {
                if (draft.length === 1) return;
                const wasLast = i === draft.length - 1;
                draft.splice(i, 1);                       // khung sau tự nuốt khoảng giờ vừa bỏ (giờ bắt đầu luôn nối theo khung trước)
                if (wasLast) draft[draft.length - 1].e = DAY_END;
                renderEditor();
                return;
            }
            if (act === 'add') { addSlot(); return; }
            if (act === 'save') {
                if (validate()) return;
                b.disabled = true;
                guard(async () => {
                    try {
                        await send('PUT', API.tariffSchedule, {
                            serviceType: draftType,
                            slots: draft.map((t, k) => ({ startTime: hms(startOf(k)), endTime: hms(t.e), basePrice: Number(t.price), multiplier: Number(t.mult) }))
                        });
                        showToast(`Đã cập nhật bảng giá ${draftType}.`, 'success');
                        draft = null;
                        exec();
                    } finally { b.disabled = false; }
                });
            }
        });

        // Thêm khung mới: các khung kề bên tự điều chỉnh (bị cắt / tách / nuốt) để bộ khung vẫn phủ kín cả ngày.
        function addSlot() {
            const msg = validate();
            if (msg) { showToast('Hãy sửa lỗi trong bảng trước khi thêm khung: ' + msg, 'error'); return; }
            openForm(`Thêm khung giá ${draftType}`, [
                { id: 'start', label: 'Giờ bắt đầu (HH:mm:ss)', value: '', pattern: TIME_RE, title: TIME_HINT },
                { id: 'end', label: 'Giờ kết thúc (HH:mm:ss)', value: '', pattern: TIME_RE, title: TIME_HINT },
                { id: 'price', label: 'Giá cơ bản (VNĐ)', type: 'number', step: '1', value: '' },
                { id: 'mult', label: 'Hệ số khung giờ', type: 'number', step: '0.01', value: 1 }
            ], async d => {
                const ns = sec(d.start), ne = sec(d.end);
                const err = inputError(ns, ne, d.price, d.mult);
                if (err) { showToast(err, 'error'); throw new Error(err); }   // ném lỗi để cửa sổ không bị đóng
                draft.forEach((t, k) => { t.s = startOf(k); });
                draft = applySlot(draft, -1, ns, ne, Number(d.price), Number(d.mult));
                renderEditor();
            });
        }

        window.editTariffSet = type => guard(async () => {
            if (draft && !confirm('Bạn đang sửa bảng giá chưa lưu. Bỏ chỉnh sửa và mở lại?')) return;
            const cur = await apiFetch(`${API.tariffs}?serviceType=${type.toLowerCase()}`);
            draftType = type;
            draft = cur.length
                ? cur.map(t => ({ s: sec(t.startTime), e: sec(t.endTime), price: Number(t.basePrice), mult: Number(t.multiplier) })).sort((a, b) => a.s - b.s)
                : [{ s: 0, e: DAY_END, price: '', mult: 1 }];   // chưa có bảng giá: bắt đầu bằng 1 khung phủ cả ngày
            renderEditor();
        });
    }

// ==================== KIỂM TOÁN ====================
// cols: 'a|b' = thử lần lượt các tên field; hậu tố '*' = JSON dạng code; '#' = thời gian
    function tabAudit(key, label, heads, cols) {
        mount('audit-' + key, {
            title: label, auto: true,
            async run() {
                const all = await apiFetch(API.audit[key]);
                const isTotal = r => String(pick(r, 'adminId', 'id')) === 'TỔNG';
                const cell = r => cols.map(c => {
                    const flag = /[*#]$/.test(c) ? c.slice(-1) : '';
                    const v = pick(r, ...c.replace(/[*#]$/, '').split('|'));
                    return flag === '*' ? pl(v) : flag === '#' ? e(dt(v)) : e(v === '' ? '---' : v);
                });
                const total = all.find(isTotal);
                const rows = all.filter(r => !isTotal(r));
                return { title: label, badge: `${rows.length} dòng`, head: heads, rows: rows.map(cell), total: total ? cell(total) : null, empty: 'Chưa có bản ghi nào.' };
            }
        });
    }

// ==================== DANH SÁCH TAB & ĐIỀU HƯỚNG ====================
    const TABS = [
        ['search', '', '→ Tìm trạm & Đặt chỗ', tabSearch],
        ['revenue', 'Báo cáo', 'Doanh thu theo dịch vụ', tabRevenue],
        ['summary', 'Báo cáo', 'Doanh thu theo khu vực', tabSummary],
        ['devstatus', 'Báo cáo', 'Thống kê thiết bị', tabDevStatus],
        ['locations', 'Cấu hình', 'Khu vực', tabLocations],
        ['devices', 'Cấu hình', 'Thiết bị', tabDevices],
        ['tariffs', 'Cấu hình', 'Biểu giá', tabTariffs],
        ['audit-activity', 'Kiểm toán', 'Tần suất thao tác Admin', () => tabAudit('activity', 'Tần suất thao tác Admin',
            ['Mã Admin', 'Họ tên Admin', '>Thao tác bảng giá', '>Thao tác hạ tầng', '>Tổng thao tác'],
            ['adminId|id', 'adminName|fullName', 'tariffActions', 'infraActions', 'totalActions'])],
        ['audit-tariffs', 'Kiểm toán', 'Lịch sử Biểu giá', () => tabAudit('tariffs', 'Lịch sử Biểu giá',
            ['Thời gian', 'Admin thao tác', 'Loại dịch vụ', 'Khung giờ', 'Hành động', 'Giá trị cũ', 'Giá trị mới'],
            ['createdAt#', 'adminName', 'serviceType', 'timeSlot', 'actionType', 'oldPayload*', 'newPayload*'])],
        ['audit-locations', 'Kiểm toán', 'Lịch sử Khu vực', () => tabAudit('locations', 'Lịch sử Khu vực',
            ['Thời gian', 'Admin thao tác', 'Mã trạm', 'Tên trạm', 'Hành động', 'Giá trị cũ', 'Giá trị mới'],
            ['createdAt#', 'adminName', 'locationCode', 'locationName', 'actionType', 'oldPayload*', 'newPayload*'])],
        ['audit-devices', 'Kiểm toán', 'Lịch sử Thiết bị', () => tabAudit('devices', 'Lịch sử Thiết bị',
            ['Thời gian', 'Admin thao tác', 'Mã trạm', 'Tên trạm', 'Loại thiết bị', 'Mã thiết bị', 'Phân loại', 'Hành động', 'Giá trị cũ', 'Giá trị mới'],
            ['createdAt#', 'adminName', 'locationCode', 'locationName', 'deviceTable', 'deviceCode', 'subType', 'actionType', 'oldPayload*', 'newPayload*'])]
    ];

    function build() {
        let nav = '', last = null;
        TABS.forEach(([key, group, label]) => {
            if (group !== last) {
                if (group) nav += `<div class="px-3 pt-4 pb-1 mt-3 border-t border-slate-800 text-[10px] font-bold uppercase tracking-wider text-slate-500">${e(group)}</div>`;
                last = group;
            }
            nav += `<button type="button" id="nav-${key}" onclick="adminGo('${key}')" class="${NAV_OFF}">${e(label)}</button>`;
            const sec = document.createElement('section');
            sec.id = 'pane-' + key;
            sec.className = 'hidden space-y-5';
            $('main').appendChild(sec);
        });
        $('navItems').innerHTML = nav;
        TABS.forEach(t => t[3]());
    }

    window.adminGo = key => {
        TABS.forEach(([k]) => {
            const on = k === key;
            $('pane-' + k).classList.toggle('hidden', !on);
            $('nav-' + k).className = on ? NAV_ON : NAV_OFF;
        });
        const nav = $('sidebarNav');
        if (window.innerWidth < 1024) { nav.classList.add('hidden'); nav.classList.remove('flex'); }
        if (AUTO[key]) AUTO[key]();
    };
    window.toggleMobileSidebar = () => { const n = $('sidebarNav'); n.classList.toggle('hidden'); n.classList.toggle('flex'); };
    window.closeModal = closeModal;

    build();
    window.adminGo('search');
})();