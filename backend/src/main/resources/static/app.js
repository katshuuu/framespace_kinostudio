'use strict';

/**
 * Access token хранится только в памяти вкладки. Refresh token браузер получает
 * в HttpOnly/SameSite cookie.
 */
const state = {
    accessToken: null,
    user: null,
    locations: [],
    clientOptions: [],
    clients: { page: 0, size: 25, totalPages: 0, query: '', content: [] },
    requests: { page: 0, size: 25, totalPages: 0, query: '', content: [] },
    selectedRequestIds: new Set(),
    activeRequestForNotes: null,
    refreshPromise: null
};

const $ = (id) => document.getElementById(id);
const escapeHtml = (value) => String(value ?? '').replace(/[&<>'"]/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[ch]));
const roles = () => new Set((state.user?.roles || []).map(String));
const isAdmin = () => roles().has('ADMIN');
const canEdit = () => roles().has('OPERATOR') || isAdmin();

function roleLabel(value) {
    return ({
        ADMIN: 'Администратор',
        OPERATOR: 'Оператор',
        VIEWER: 'Просмотр'
    })[value] || value;
}

function fieldLabel(value) {
    return ({
        externalId: 'внешний ID',
        title: 'название',
        description: 'описание',
        clientId: 'клиент',
        clientName: 'клиент',
        clientEmail: 'email клиента',
        clientPhone: 'телефон клиента',
        fullName: 'ФИО / название',
        email: 'email',
        phone: 'телефон',
        shootingDate: 'дата съёмки',
        locationId: 'локация',
        status: 'статус',
        budget: 'бюджет',
        durationHours: 'длительность (ч)',
        urgent: 'срочно',
        tags: 'теги',
        version: 'версия'
    })[value] || value;
}

function lowerFirst(value) {
    const text = String(value ?? '');
    return text ? text.charAt(0).toLocaleLowerCase('ru-RU') + text.slice(1) : text;
}

function normalizeAfterColon(value) {
    return String(value ?? '').replace(/:\s*([А-ЯЁA-Z])/g,
        (_, letter) => `: ${letter.toLocaleLowerCase('ru-RU')}`);
}

function formatRussianPhone(value) {
    let digits = String(value ?? '').replace(/\D/g, '');

    if (digits.startsWith('8')) {
        digits = `7${digits.slice(1)}`;
    }

    if (digits.startsWith('7')) {
        digits = digits.slice(1);
    }

    digits = digits.slice(0, 10);

    let result = '+7-';
    if (!digits.length) return result;

    result += digits.slice(0, 3);
    if (digits.length > 3) result += `-${digits.slice(3, 6)}`;
    if (digits.length > 6) result += `-${digits.slice(6, 8)}`;
    if (digits.length > 8) result += `-${digits.slice(8, 10)}`;

    return result;
}

function bindPhoneMask(input) {
    if (!input) return;

    input.addEventListener('focus', () => {
        if (!input.value) input.value = '+7-';
    });

    input.addEventListener('keydown', event => {
        const allowedKeys = new Set([
            'Backspace', 'Delete', 'ArrowLeft', 'ArrowRight',
            'Home', 'End', 'Tab', 'Enter'
        ]);

        if (event.ctrlKey || event.metaKey || allowedKeys.has(event.key) || /^\d$/.test(event.key)) {
            return;
        }

        event.preventDefault();
    });

    input.addEventListener('input', () => {
        input.value = formatRussianPhone(input.value);
    });

    input.addEventListener('blur', () => {
        if (input.value === '+7-') input.value = '';
    });
}

function toast(message, error = false) {
    const node = $('toast');
    node.textContent = message;
    node.className = `toast show${error ? ' error' : ''}`;
    clearTimeout(toast.timer);
    toast.timer = setTimeout(() => node.className = 'toast', 3500);
}

function showFormError(form, message) {
    const box = form.querySelector('[data-form-error]');
    if (!box) {
        toast(message, true);
        return;
    }
    box.textContent = message;
    box.hidden = false;
}

function clearFormError(form) {
    const box = form.querySelector('[data-form-error]');
    if (!box) return;
    box.textContent = '';
    box.hidden = true;
}

async function parseError(response) {
    try {
        const body = await response.json();
        const fields = body.validationErrors
            ? Object.entries(body.validationErrors)
                .map(([key, value]) => `${fieldLabel(key)}: ${lowerFirst(value)}`)
                .join('; ')
            : '';

        const message = `${body.message || response.statusText}${fields ? ` — ${fields}` : ''}${body.correlationId ? ` [${body.correlationId}]` : ''}`;
        return normalizeAfterColon(message);
    } catch (_) {
        return `${response.status} ${response.statusText}`;
    }
}

async function api(path, options = {}, retry = true) {
    const headers = new Headers(options.headers || {});
    if (state.accessToken) headers.set('Authorization', `Bearer ${state.accessToken}`);
    if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
    const response = await fetch(`/api${path}`, {...options, headers, credentials: 'include'});
    if (response.status === 401 && retry) {
        const refreshed = await refreshAccessToken();
        if (refreshed) return api(path, options, false);
    }
    if (!response.ok) throw new Error(await parseError(response));
    if (response.status === 204) return null;
    const type = response.headers.get('content-type') || '';
    return type.includes('application/json') ? response.json() : response;
}

async function refreshAccessToken() {
    if (!state.refreshPromise) {
        state.refreshPromise = fetch('/api/auth/refresh', {
            method: 'POST', credentials: 'include', headers: {'Content-Type':'application/json'}, body: '{}'
        }).then(async response => {
            if (!response.ok) return false;
            const result = await response.json();
            state.accessToken = result.accessToken;
            state.user = result.user;
            return true;
        }).finally(() => state.refreshPromise = null);
    }
    return state.refreshPromise;
}

async function boot() {
    bindEvents();
    if (await refreshAccessToken()) {
        await enterApplication();
    } else {
        showLogin();
    }
}

function showLogin() {
    $('app-view').hidden = true;
    $('login-view').hidden = false;
    state.accessToken = null;
    state.user = null;
}

async function enterApplication() {
    $('login-view').hidden = true;
    $('app-view').hidden = false;
    applyRoleVisibility();
    $('current-user').innerHTML = `<strong>${escapeHtml(state.user.displayName)}</strong><br>${escapeHtml(state.user.email)}<br>${escapeHtml(state.user.roles.map(roleLabel).join(', '))}`;

    if (state.user.passwordChangeRequired) {
        toast('Для продолжения необходимо сменить временный пароль', true);
        const changed = await openPasswordDialog(true);
        if (!changed) return;
    }

    try {
        await Promise.all([loadLocations(), loadClientOptions(), loadDashboard()]);
    } catch (error) {
        console.error('[FrameSpace] Initial data loading failed', error);
        toast(`Вход выполнен, но часть данных не загрузилась: ${error.message}`, true);
    }
}

function applyRoleVisibility() {
    document.querySelectorAll('.admin-only').forEach(node => node.hidden = !isAdmin());
    document.querySelectorAll('.operator-only').forEach(node => node.hidden = !canEdit());
}

function switchTab(name) {
    document.querySelectorAll('.tab').forEach(tab => tab.classList.toggle('active', tab.id === `tab-${name}`));
    document.querySelectorAll('.nav-item').forEach(button => button.classList.toggle('active', button.dataset.tab === name));
    const titles = {
        dashboard: ['Аналитика', 'Сводная картина по заявкам на съёмку'],
        requests: ['Заявки', 'Умный поиск, карточки и массовые операции'],
        clients: ['Клиенты', 'Заказчики съёмок'],
        locations: ['Локации', 'Справочник павильонов и площадок'],
        users: ['Пользователи', 'Роли, блокировка и временные пароли'],
        audit: ['Журнал аудита', 'История действий пользователей']
    };
    [$('page-title').textContent, $('page-subtitle').textContent] = titles[name];
    if (name === 'requests') loadRequests();
    if (name === 'clients') loadClients();
    if (name === 'locations') renderLocations();
    if (name === 'users' && isAdmin()) loadUsers();
    if (name === 'audit' && isAdmin()) loadAudit();
    if (name === 'dashboard') loadDashboard();
}

async function loadDashboard() {
    try {
        const data = await api('/analytics/summary');
        const metrics = [
            ['Всего', data.total],
            ['Активные', data.active],
            ['В архиве', data.archived],
            ['Срочные', data.withUrgent],
            ['Средний бюджет', formatBudget(data.averageBudget)]
        ];
        $('metric-grid').innerHTML = metrics.map(([name, value]) =>
            `<article class="metric card"><span class="muted">${name}</span><strong>${value}</strong></article>`).join('');
        renderBars('chart-locations', data.byLocation);
        renderBars('chart-status', data.byStatus);
        renderBars('chart-priority', data.byPriority);
    } catch (error) { toast(error.message, true); }
}

function formatBudget(value) {
    if (value == null || value === '') return '—';
    return new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 2 }).format(Number(value));
}

function renderBars(id, values) {
    const max = Math.max(1, ...values.map(item => item.count));

    const displayName = value => {
        if (id === 'chart-status') return statusLabel(value);
        if (id === 'chart-priority') return priorityLabel(value);
        return value;
    };

    $(id).innerHTML = values.length
        ? values.map(item => `<div class="bar-row"><span>${escapeHtml(displayName(item.name))}</span><progress value="${item.count}" max="${max}"></progress><strong>${item.count}</strong></div>`).join('')
        : '<p class="muted">Нет данных</p>';
}

async function loadLocations() {
    try {
        state.locations = await api('/locations?activeOnly=false');
        renderLocations();
    } catch (error) { toast(error.message, true); }
}

function renderLocations() {
    const body = $('locations-body');
    if (!body) return;
    body.innerHTML = state.locations.map(location => `<tr>
        <td><strong>${escapeHtml(location.code)}</strong></td>
        <td>${escapeHtml(location.name || '—')}</td>
        <td>${escapeHtml(locationTypeLabel(location.type))}</td>
        <td>${escapeHtml(location.address || '—')}</td>
        <td>${location.capacity ?? '—'}</td>
        <td><span class="badge ${location.active ? 'success' : ''}">${location.active ? 'Да' : 'Нет'}</span></td>
        <td class="admin-only"><div class="actions">
            <button class="secondary" data-location-edit="${location.id}">Изменить</button>
            <button class="secondary" data-location-toggle="${location.id}">${location.active ? 'Отключить' : 'Включить'}</button>
        </div></td>
    </tr>`).join('');
    applyRoleVisibility();
}

async function loadRequests(page = state.requests.page) {
    try {
        const q = encodeURIComponent(state.requests.query);
        const result = await api(`/requests?q=${q}&page=${page}&size=${state.requests.size}&sort=title,asc`);
        state.requests = {...state.requests, ...result};
        state.selectedRequestIds.clear();
        renderRequests();
    } catch (error) { toast(error.message, true); }
}

function renderRequests() {
    $('requests-body').innerHTML = state.requests.content.map(request => `<tr class="${request.archived ? 'archived' : ''}">
        <td class="operator-only"><input type="checkbox" data-request-select="${request.id}"></td>
        <td><strong>${escapeHtml(request.title)}</strong><br><small class="muted">${escapeHtml(request.externalId || '')}</small></td>
        <td>${escapeHtml(request.clientName)}</td>
        <td>${escapeHtml(request.locationCode || '—')}</td>
        <td>${escapeHtml(statusLabel(request.status))}</td>
        <td>${formatBudget(request.budget)}</td>
        <td>${request.urgent ? '<span class="badge warning">Да</span>' : 'Нет'}</td>
        <td><span class="badge risk-${request.priorityLevel}">${escapeHtml(priorityLabel(request.priorityLevel))}</span></td>
        <td>${request.tags.map(tag => `<span class="badge">${escapeHtml(tag)}</span>`).join(' ') || '—'}</td>
        <td><div class="actions">
            <button class="secondary" data-request-view="${request.id}">Карточка</button>
            ${canEdit() ? `<button class="secondary" data-request-edit="${request.id}">Изменить</button><button class="secondary" data-request-notes="${request.id}">Заметки</button><button class="danger-outline" data-request-archive="${request.id}">${request.archived ? 'Восстановить' : 'В архив'}</button>` : ''}
        </div></td>
    </tr>`).join('') || '<tr><td colspan="10" class="muted">Заявки не найдены</td></tr>';
    $('requests-page').textContent = `Страница ${state.requests.page + 1} из ${Math.max(1, state.requests.totalPages)} · записей ${state.requests.totalElements}`;
    $('requests-prev').disabled = state.requests.first;
    $('requests-next').disabled = state.requests.last;
    $('select-all-requests').checked = false;
    applyRoleVisibility();
}

function requestForm(request = null, readOnly = false) {
    const locationOptions = ['<option value="">Без локации</option>',
        ...state.locations.filter(l => l.active || l.id === request?.locationId)
            .map(l => `<option value="${l.id}" ${l.id === request?.locationId ? 'selected' : ''}>${escapeHtml(l.code)}</option>`)
    ].join('');
    const clients = [...state.clientOptions];
    if (request && !clients.some(c => c.id === request.clientId)) {
        clients.push({ id: request.clientId, fullName: request.clientName, email: request.clientEmail });
    }
    const clientOptions = ['<option value="">Выберите клиента</option>',
        ...clients.map(c => `<option value="${c.id}" ${c.id === request?.clientId ? 'selected' : ''}>${escapeHtml(c.fullName)} · ${escapeHtml(c.email)}</option>`)
    ].join('');
    const statuses = request ? [request.status, ...request.nextStatuses] : ['NEW'];
    return `<div class="dialog-head"><h3>${request ? (readOnly ? 'Карточка заявки' : 'Изменение заявки') : 'Новая заявка'}</h3><button type="button" data-close-dialog="entity-dialog" class="icon-button">×</button></div>
    <div class="form-grid">
      <label>External ID<input name="externalId" maxlength="100" value="${escapeHtml(request?.externalId || '')}" ${readOnly ? 'disabled' : ''}></label>
      <label>Название<input name="title" required maxlength="200" value="${escapeHtml(request?.title || '')}" ${readOnly ? 'disabled' : ''}></label>
      <label class="span-2">Описание<textarea name="description" required maxlength="4000" rows="4" ${readOnly ? 'disabled' : ''}>${escapeHtml(request?.description || '')}</textarea></label>
      <label class="span-2">Клиент<select name="clientId" required ${readOnly ? 'disabled' : ''}>${clientOptions}</select>
        ${readOnly && request ? `<small class="muted">${escapeHtml(request.clientPhone || '')}</small>` : ''}
        ${readOnly ? '' : '<small class="muted">Нет нужного клиента? Добавьте его на вкладке «Клиенты».</small>'}</label>
      <label>Дата съёмки<input name="shootingDate" type="date" required value="${request?.shootingDate || ''}" ${readOnly ? 'disabled' : ''}></label>
      <label>Локация<select name="locationId" ${readOnly ? 'disabled' : ''}>${locationOptions}</select></label>
      <label>Статус<select name="status" ${readOnly ? 'disabled' : ''}>${statusOptions(request?.status, statuses)}</select></label>
      <label>Бюджет<input name="budget" type="number" min="0" max="99999999.99" step="0.01" inputmode="decimal" placeholder="0.00" value="${request?.budget ?? ''}" ${readOnly ? 'disabled' : ''}></label>
      <label>Длительность (ч)<input name="durationHours" type="number" min="1" step="1" value="${request?.durationHours ?? ''}" ${readOnly ? 'disabled' : ''}></label>
      <label class="inline"><input name="urgent" type="checkbox" ${request?.urgent ? 'checked' : ''} ${readOnly ? 'disabled' : ''}> Срочная заявка</label>
      <label class="span-2">Теги через запятую<input name="tags" maxlength="1000" value="${escapeHtml((request?.tags || []).join(', '))}" ${readOnly ? 'disabled' : ''}></label>
    </div>
    ${readOnly ? '' : '<div class="form-message error" data-form-error hidden></div><button class="primary" type="submit">Сохранить</button>'}`;
}

async function openRequest(id = null, readOnly = false) {
    const request = id ? await api(`/requests/${id}`) : null;
    const form = $('entity-form');
    form.innerHTML = requestForm(request, readOnly);

    form.onsubmit = readOnly ? null : async event => {
        event.preventDefault();
        clearFormError(form);

        if (!form.reportValidity()) return;

        const data = new FormData(form);
        const budgetRaw = empty(data.get('budget'));
        const durationRaw = empty(data.get('durationHours'));
        const payload = {
            externalId: empty(data.get('externalId')),
            title: data.get('title'),
            description: data.get('description'),
            clientId: data.get('clientId'),
            shootingDate: data.get('shootingDate'),
            locationId: empty(data.get('locationId')),
            status: data.get('status'),
            budget: budgetRaw == null ? null : Number(budgetRaw),
            durationHours: durationRaw == null ? null : Number(durationRaw),
            urgent: data.get('urgent') === 'on',
            tags: String(data.get('tags') || '').split(',').map(v => v.trim()).filter(Boolean),
            version: request?.version ?? null
        };
        try {
            await api(id ? `/requests/${id}` : '/requests', { method: id ? 'PUT' : 'POST', body: JSON.stringify(payload) });
            $('entity-dialog').close();
            toast('Заявка сохранена');
            await Promise.all([loadRequests(), loadDashboard()]);
        } catch (error) {
            showFormError(form, error.message);
        }
    };
    $('entity-dialog').showModal();
}

async function archiveRequest(id) {
    const request = state.requests.content.find(item => item.id === id);
    const action = request?.archived ? 'восстановить' : 'переместить в архив';
    if (!confirm(`Действительно ${action} заявку?`)) return;
    try {
        await api(request.archived ? `/requests/${id}/restore` : `/requests/${id}`, { method: request.archived ? 'POST' : 'DELETE' });
        toast('Операция выполнена');
        await Promise.all([loadRequests(), loadDashboard()]);
    } catch (error) { toast(error.message, true); }
}

async function openNotes(requestId) {
    state.activeRequestForNotes = requestId;
    await renderNotes();
    $('notes-dialog').showModal();
}

async function renderNotes() {
    try {
        const list = await api(`/requests/${state.activeRequestForNotes}/notes`);
        $('notes-list').innerHTML = list.map(note => `<article class="note"><div class="note-meta"><span>${escapeHtml(note.authorName)} · ${formatDate(note.createdAt)}</span><span>${note.pinned ? '📌' : ''}</span></div><p>${escapeHtml(note.body).replace(/\n/g, '<br>')}</p><div class="actions"><button class="danger-outline" data-note-delete="${note.id}">Удалить</button></div></article>`).join('') || '<p class="muted">Заметок пока нет</p>';
    } catch (error) { toast(error.message, true); }
}

async function bulkAction() {
    if (!state.selectedRequestIds.size) return toast('Выберите хотя бы одну заявку', true);
    const action = prompt('Действие: ARCHIVE, RESTORE, SET_STATUS, ADD_TAG, REMOVE_TAG', 'ARCHIVE');
    if (!action) return;
    const payload = { ids: [...state.selectedRequestIds], action: action.toUpperCase(), status: null, tag: null };
    if (payload.action === 'SET_STATUS') payload.status = prompt('Статус: APPROVED, IN_PROGRESS, COMPLETED, REJECTED, CANCELLED. Заявки, для которых переход недопустим, попадут в отчёт об ошибках.', 'APPROVED');
    if (payload.action === 'ADD_TAG' || payload.action === 'REMOVE_TAG') payload.tag = prompt('Тег');
    try {
        const result = await api('/requests/bulk', { method: 'POST', body: JSON.stringify(payload) });
        toast(`Успешно: ${result.succeeded}; ошибок: ${result.failures.length}`, result.failures.length > 0);
        if (result.failures.length) console.warn('[FrameSpace] Bulk failures', result.failures);
        await Promise.all([loadRequests(), loadDashboard()]);
    } catch (error) { toast(error.message, true); }
}

async function exportCsv() {
    try {
        const response = await api(`/requests/export?q=${encodeURIComponent(state.requests.query)}`);
        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `requests-${new Date().toISOString().slice(0, 10)}.csv`;
        link.click();
        URL.revokeObjectURL(url);
    } catch (error) { toast(error.message, true); }
}

async function loadClientOptions() {
    const page = await api('/clients?page=0&size=100&sort=name,asc');
    state.clientOptions = page.content;
}

async function loadClients(page = state.clients.page) {
    try {
        const q = encodeURIComponent(state.clients.query);
        const result = await api(`/clients?q=${q}&page=${page}&size=${state.clients.size}&sort=name,asc`);
        state.clients = {...state.clients, ...result};
        renderClients();
    } catch (error) { toast(error.message, true); }
}

function renderClients() {
    $('clients-body').innerHTML = state.clients.content.map(client => `<tr>
        <td><strong>${escapeHtml(client.fullName)}</strong></td>
        <td>${escapeHtml(client.email)}</td>
        <td>${escapeHtml(client.phone)}</td>
        <td>${client.requestCount}</td>
        <td class="operator-only"><div class="actions">
            <button class="secondary" data-client-edit="${client.id}">Изменить</button>
            ${isAdmin() ? `<button class="danger-outline" data-client-delete="${client.id}" ${client.requestCount ? 'disabled title="У клиента есть заявки"' : ''}>Удалить</button>` : ''}
        </div></td>
    </tr>`).join('') || '<tr><td colspan="5" class="muted">Клиенты не найдены</td></tr>';
    $('clients-page').textContent = `Страница ${state.clients.page + 1} из ${Math.max(1, state.clients.totalPages)} · записей ${state.clients.totalElements}`;
    $('clients-prev').disabled = state.clients.first;
    $('clients-next').disabled = state.clients.last;
    applyRoleVisibility();
}

function clientForm(client = null) {
    return `<div class="dialog-head"><h3>${client ? 'Изменение клиента' : 'Новый клиент'}</h3><button type="button" data-close-dialog="entity-dialog" class="icon-button">×</button></div>
    <label>ФИО или название организации<input name="fullName" required maxlength="150" value="${escapeHtml(client?.fullName || '')}"></label>
    <label>Email<input name="email" type="email" required maxlength="254" value="${escapeHtml(client?.email || '')}"></label>
    <label>Телефон<input name="phone" type="tel" required inputmode="numeric" autocomplete="tel" maxlength="16" pattern="\\+7-[0-9]{3}-[0-9]{3}-[0-9]{2}-[0-9]{2}" title="Формат: +7-999-999-99-99" placeholder="+7-999-999-99-99" value="${escapeHtml(client?.phone || '')}"></label>
    <div class="form-message error" data-form-error hidden></div>
    <button class="primary" type="submit">Сохранить</button>`;
}

async function openClient(id = null) {
    const client = id ? state.clients.content.find(c => c.id === id) : null;
    const form = $('entity-form');
    form.innerHTML = clientForm(client);
    bindPhoneMask(form.elements.phone);
    form.onsubmit = async event => {
        event.preventDefault();
        clearFormError(form);
        if (!form.reportValidity()) return;
        const data = new FormData(form);
        const payload = {
            fullName: data.get('fullName'),
            email: data.get('email'),
            phone: data.get('phone'),
            version: client?.version ?? null
        };
        try {
            await api(id ? `/clients/${id}` : '/clients', { method: id ? 'PUT' : 'POST', body: JSON.stringify(payload) });
            $('entity-dialog').close();
            toast('Клиент сохранён');
            await Promise.all([loadClients(), loadClientOptions()]);
        } catch (error) {
            showFormError(form, error.message);
        }
    };
    $('entity-dialog').showModal();
}

async function deleteClient(id) {
    const client = state.clients.content.find(c => c.id === id);
    if (!confirm(`Удалить клиента «${client?.fullName}»?`)) return;
    try {
        await api(`/clients/${id}`, { method: 'DELETE' });
        toast('Клиент удалён');
        await Promise.all([loadClients(), loadClientOptions()]);
    } catch (error) { toast(error.message, true); }
}

function locationForm(location = null) {
    return `<div class="dialog-head"><h3>${location ? 'Изменение' : 'Новая'} локация</h3><button type="button" data-close-dialog="entity-dialog" class="icon-button">×</button></div>
    <label>Код<input name="code" required maxlength="50" value="${escapeHtml(location?.code || '')}"></label>
    <label>Название<input name="name" maxlength="200" value="${escapeHtml(location?.name || '')}"></label>
    <label>Тип<select name="type">${['PAVILION', 'STUDIO', 'OUTDOOR', 'OFFICE'].map(type =>
        `<option value="${type}" ${type === (location?.type || 'PAVILION') ? 'selected' : ''}>${locationTypeLabel(type)}</option>`).join('')}</select></label>
    <label>Адрес<input name="address" maxlength="500" value="${escapeHtml(location?.address || '')}"></label>
    <label>Вместимость<input name="capacity" type="number" min="1" step="1" value="${location?.capacity ?? ''}"></label>
    <label class="inline"><input name="active" type="checkbox" ${location?.active !== false ? 'checked' : ''}> Активна</label>
    <button class="primary">Сохранить</button>`;
}

async function openLocation(id = null) {
    const location = id ? state.locations.find(l => l.id === id) : null;
    const form = $('entity-form');
    form.innerHTML = locationForm(location);
    form.onsubmit = async event => {
        event.preventDefault();
        const data = new FormData(form);
        const capacityRaw = empty(data.get('capacity'));
        const payload = {
            code: data.get('code'),
            name: empty(data.get('name')),
            address: empty(data.get('address')),
            capacity: capacityRaw == null ? null : Number(capacityRaw),
            type: data.get('type'),
            active: data.get('active') === 'on',
            version: location?.version ?? null
        };
        try {
            await api(id ? `/locations/${id}` : '/locations', { method: id ? 'PUT' : 'POST', body: JSON.stringify(payload) });
            $('entity-dialog').close();
            toast('Локация сохранена');
            await loadLocations();
        } catch (error) { toast(error.message, true); }
    };
    $('entity-dialog').showModal();
}

async function toggleLocation(id) {
    const location = state.locations.find(l => l.id === id);
    try {
        await api(`/locations/${id}/active?active=${!location.active}&version=${location.version}`, { method: 'PATCH' });
        await loadLocations();
    } catch (error) { toast(error.message, true); }
}

async function loadUsers() {
    try {
        const page = await api('/admin/users?page=0&size=100&sort=email,asc');
        $('users-body').innerHTML = page.content.map(user => `<tr><td>${escapeHtml(user.email)}</td><td>${escapeHtml(user.displayName)}</td><td>${user.roles.map(r => `<span class="badge">${escapeHtml(roleLabel(r))}</span>`).join(' ')}</td><td>${user.enabled ? 'Да' : 'Нет'}</td><td>${user.passwordChangeRequired ? 'Да' : 'Нет'}</td><td><div class="actions"><button class="secondary" data-user-edit="${user.id}">Изменить</button><button class="secondary" data-user-reset="${user.id}">Сбросить пароль</button></div></td></tr>`).join('');
        state.users = page.content;
    } catch (error) { toast(error.message, true); }
}

function userForm(user = null) {
    const checked = role => user?.roles.includes(role) || (!user && role === 'VIEWER');
    return `<div class="dialog-head"><h3>${user ? 'Изменение' : 'Новый'} пользователь</h3><button type="button" data-close-dialog="entity-dialog" class="icon-button">×</button></div>
    <label>Email<input name="email" type="email" required maxlength="254" value="${escapeHtml(user?.email || '')}"></label>
    <label>Отображаемое имя<input name="displayName" required maxlength="160" value="${escapeHtml(user?.displayName || '')}"></label>
    ${user ? '' : '<label>Временный пароль<input name="temporaryPassword" type="password" required minlength="12" maxlength="128"></label>'}
    <fieldset><legend>Роли</legend>${['VIEWER', 'OPERATOR', 'ADMIN'].map(r => `<label class="inline"><input type="checkbox" name="roles" value="${r}" ${checked(r) ? 'checked' : ''}> ${escapeHtml(roleLabel(r))}</label>`).join('')}</fieldset>
    ${user ? `<label class="inline"><input type="checkbox" name="enabled" ${user.enabled ? 'checked' : ''}> Активен</label>` : ''}
    <button class="primary">Сохранить</button>`;
}

async function openUser(id = null) {
    const user = id ? state.users.find(u => u.id === id) : null;
    const form = $('entity-form');
    form.innerHTML = userForm(user);
    form.onsubmit = async event => {
        event.preventDefault();
        const data = new FormData(form);
        const payload = {
            email: data.get('email'),
            displayName: data.get('displayName'),
            roles: data.getAll('roles'),
            ...(user ? { enabled: data.get('enabled') === 'on', version: user.version } : { temporaryPassword: data.get('temporaryPassword') })
        };
        try {
            await api(id ? `/admin/users/${id}` : '/admin/users', { method: id ? 'PUT' : 'POST', body: JSON.stringify(payload) });
            $('entity-dialog').close();
            toast('Пользователь сохранён');
            await loadUsers();
        } catch (error) { toast(error.message, true); }
    };
    $('entity-dialog').showModal();
}

async function resetUserPassword(id) {
    const password = prompt('Новый временный пароль (не менее 12 символов, регистры, цифра и спецсимвол)');
    if (!password) return;
    try {
        await api(`/admin/users/${id}/reset-password`, { method: 'POST', body: JSON.stringify({ temporaryPassword: password }) });
        toast('Пароль сброшен');
        await loadUsers();
    } catch (error) { toast(error.message, true); }
}

async function loadAudit() {
    try {
        const q = encodeURIComponent($('audit-search').value.trim());
        const page = await api(`/admin/audit?q=${q}&page=0&size=100&sort=occurred,desc`);
        $('audit-body').innerHTML = page.content.map(item => `<tr><td>${formatDate(item.occurredAt)}</td><td>${escapeHtml(item.actorEmail || 'system')}</td><td>${escapeHtml(item.eventType)}</td><td>${escapeHtml(item.entityType || '—')}</td><td>${escapeHtml(item.summary)}</td><td><code>${escapeHtml(item.correlationId || '—')}</code></td></tr>`).join('');
    } catch (error) { toast(error.message, true); }
}

function openPasswordDialog(required = false) {
    return new Promise(resolve => {
        const dialog = $('entity-dialog');
        const form = $('entity-form');

        const currentPasswordField = required
            ? ''
            : '<label>Текущий пароль<input name="current" type="password" required maxlength="128" autocomplete="current-password"></label>';

        form.innerHTML = `<div class="dialog-head"><h3>${required ? 'Установка постоянного пароля' : 'Смена пароля'}</h3>${required ? '' : '<button type="button" data-close-dialog="entity-dialog" class="icon-button">×</button>'}</div>
            ${currentPasswordField}
            <label>Новый пароль<input name="next" type="password" required minlength="12" maxlength="128" autocomplete="new-password"></label>
            <label>Повторите новый пароль<input name="confirm" type="password" required minlength="12" maxlength="128" autocomplete="new-password"></label>
            <p class="muted">Минимум 12 символов: верхний/нижний регистр, цифра и специальный символ.</p>
            <button class="primary">${required ? 'Установить пароль' : 'Изменить'}</button>`;

        let completed = false;

        const onClose = () => {
            dialog.removeEventListener('close', onClose);
            if (!completed) resolve(false);
        };
        dialog.addEventListener('close', onClose);

        form.onsubmit = async event => {
            event.preventDefault();

            const data = new FormData(form);
            const next = String(data.get('next') || '');
            const confirm = String(data.get('confirm') || '');

            if (next !== confirm) {
                toast('Новые пароли не совпадают', true);
                return;
            }

            try {
                if (required) {
                    await api('/auth/change-initial-password', {
                        method: 'POST',
                        body: JSON.stringify({ newPassword: next })
                    });

                    const refreshed = await refreshAccessToken();
                    if (!refreshed) {
                        completed = true;
                        dialog.close();
                        showLogin();
                        toast('Пароль изменён, но сессию не удалось обновить. Войдите новым паролем.', true);
                        resolve(false);
                        return;
                    }

                    state.user.passwordChangeRequired = false;
                    completed = true;
                    dialog.close();
                    toast('Постоянный пароль установлен');
                    resolve(true);
                    return;
                }

                await api('/auth/change-password', {
                    method: 'POST',
                    body: JSON.stringify({
                        currentPassword: data.get('current'),
                        newPassword: next
                    })
                });

                completed = true;
                dialog.close();
                toast('Пароль изменён. Выполните вход заново.');
                resolve(true);
                await logout();
            } catch (error) {
                toast(error.message, true);
            }
        };

        dialog.showModal();
    });
}

async function logout() {
    try {
        await fetch('/api/auth/logout', { method: 'POST', credentials: 'include', headers: { 'Content-Type': 'application/json' }, body: '{}' });
    } catch (_) {}
    showLogin();
}

function bindEvents() {
    $('login-form').addEventListener('submit', async event => {
        event.preventDefault();

        const form = $('login-form');
        const button = form.querySelector('button[type="submit"]');
        const errorNode = $('login-error');

        errorNode.hidden = true;
        button.disabled = true;
        const originalCaption = button.textContent;
        button.textContent = 'Входим…';

        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    email: $('login-email').value,
                    password: $('login-password').value,
                    clientType: 'WEB'
                })
            });

            if (!response.ok) {
                throw new Error(await parseError(response));
            }

            const result = await response.json();
            state.accessToken = result.accessToken;
            state.user = result.user;
            $('login-password').value = '';

            await enterApplication();
        } catch (error) {
            console.error('[FrameSpace] Login flow failed', error);
            showLogin();
            errorNode.textContent = error.message || 'Не удалось выполнить вход';
            errorNode.hidden = false;
        } finally {
            button.disabled = false;
            button.textContent = originalCaption;
        }
    });

    $('navigation').addEventListener('click', event => {
        const button = event.target.closest('[data-tab]');
        if (button && !button.hidden) switchTab(button.dataset.tab);
    });
    $('logout').addEventListener('click', logout);
    $('change-password').addEventListener('click', () => openPasswordDialog(false));
    $('search-requests').addEventListener('click', () => {
        state.requests.query = $('request-search').value.trim();
        state.requests.page = 0;
        loadRequests(0);
    });
    $('request-search').addEventListener('keydown', event => { if (event.key === 'Enter') $('search-requests').click(); });
    $('requests-prev').addEventListener('click', () => loadRequests(state.requests.page - 1));
    $('requests-next').addEventListener('click', () => loadRequests(state.requests.page + 1));
    $('add-request').addEventListener('click', () => openRequest());
    $('bulk-action').addEventListener('click', bulkAction);
    $('export-csv').addEventListener('click', exportCsv);
    $('import-csv').addEventListener('click', () => { $('csv-result').textContent = ''; $('csv-apply').disabled = true; $('csv-dialog').showModal(); });
    $('search-clients').addEventListener('click', () => {
        state.clients.query = $('client-search').value.trim();
        loadClients(0);
    });
    $('client-search').addEventListener('keydown', event => { if (event.key === 'Enter') $('search-clients').click(); });
    $('clients-prev').addEventListener('click', () => loadClients(state.clients.page - 1));
    $('clients-next').addEventListener('click', () => loadClients(state.clients.page + 1));
    $('add-client').addEventListener('click', () => openClient());
    $('clients-body').addEventListener('click', event => {
        const edit = event.target.closest('[data-client-edit]');
        const remove = event.target.closest('[data-client-delete]');
        if (edit) openClient(edit.dataset.clientEdit);
        if (remove) deleteClient(remove.dataset.clientDelete);
    });
    $('add-location').addEventListener('click', () => openLocation());
    $('add-user').addEventListener('click', () => openUser());
    $('search-audit').addEventListener('click', loadAudit);
    $('requests-body').addEventListener('click', event => handleRequestTableClick(event));
    $('requests-body').addEventListener('change', event => {
        const checkbox = event.target.closest('[data-request-select]');
        if (checkbox) {
            checkbox.checked
                ? state.selectedRequestIds.add(checkbox.dataset.requestSelect)
                : state.selectedRequestIds.delete(checkbox.dataset.requestSelect);
        }
    });
    $('select-all-requests').addEventListener('change', event => {
        document.querySelectorAll('[data-request-select]').forEach(cb => {
            cb.checked = event.target.checked;
            event.target.checked
                ? state.selectedRequestIds.add(cb.dataset.requestSelect)
                : state.selectedRequestIds.delete(cb.dataset.requestSelect);
        });
    });
    $('locations-body').addEventListener('click', event => {
        const edit = event.target.closest('[data-location-edit]');
        const toggle = event.target.closest('[data-location-toggle]');
        if (edit) openLocation(edit.dataset.locationEdit);
        if (toggle) toggleLocation(toggle.dataset.locationToggle);
    });
    $('users-body').addEventListener('click', event => {
        const edit = event.target.closest('[data-user-edit]');
        const reset = event.target.closest('[data-user-reset]');
        if (edit) openUser(edit.dataset.userEdit);
        if (reset) resetUserPassword(reset.dataset.userReset);
    });
    $('note-form').addEventListener('submit', async event => {
        event.preventDefault();
        try {
            await api(`/requests/${state.activeRequestForNotes}/notes`, {
                method: 'POST',
                body: JSON.stringify({ body: $('note-body').value, pinned: $('note-pinned').checked, version: null })
            });
            $('note-body').value = '';
            $('note-pinned').checked = false;
            await renderNotes();
        } catch (error) { toast(error.message, true); }
    });
    $('notes-list').addEventListener('click', async event => {
        const button = event.target.closest('[data-note-delete]');
        if (button && confirm('Удалить заметку?')) {
            try {
                await api(`/requests/${state.activeRequestForNotes}/notes/${button.dataset.noteDelete}`, { method: 'DELETE' });
                await renderNotes();
            } catch (error) { toast(error.message, true); }
        }
    });
    $('csv-preview').addEventListener('click', () => runCsv(false));
    $('csv-apply').addEventListener('click', () => runCsv(true));
    document.body.addEventListener('click', event => {
        const close = event.target.closest('[data-close-dialog]');
        if (close) $(close.dataset.closeDialog).close();
    });
}

function handleRequestTableClick(event) {
    const view = event.target.closest('[data-request-view]');
    const edit = event.target.closest('[data-request-edit]');
    const notes = event.target.closest('[data-request-notes]');
    const archive = event.target.closest('[data-request-archive]');
    if (view) openRequest(view.dataset.requestView, true);
    if (edit) openRequest(edit.dataset.requestEdit, false);
    if (notes) openNotes(notes.dataset.requestNotes);
    if (archive) archiveRequest(archive.dataset.requestArchive);
}

async function runCsv(apply) {
    const file = $('csv-file').files[0];
    if (!file) return toast('Выберите CSV-файл', true);
    const form = new FormData();
    form.append('file', file);
    try {
        const result = await api(`/requests/import/${apply ? 'apply' : 'preview'}?mode=${$('csv-mode').value}`, { method: 'POST', body: form });
        $('csv-result').textContent = JSON.stringify(result, null, 2);
        $('csv-apply').disabled = apply ? true : result.invalidRows > 0;
        if (apply) {
            toast('Импорт завершён');
            await Promise.all([loadRequests(), loadDashboard()]);
        }
    } catch (error) {
        toast(error.message, true);
        $('csv-result').textContent = error.message;
    }
}

function statusOptions(selected = 'NEW', allowed = ['NEW', 'APPROVED', 'IN_PROGRESS', 'COMPLETED', 'REJECTED', 'CANCELLED']) {
    return allowed
        .map(v => `<option value="${v}" ${v === selected ? 'selected' : ''}>${statusLabel(v)}</option>`)
        .join('');
}

function statusLabel(value) {
    return ({
        NEW: 'Новая',
        APPROVED: 'Утверждена',
        IN_PROGRESS: 'В работе',
        COMPLETED: 'Завершена',
        REJECTED: 'Отклонена',
        CANCELLED: 'Отменена'
    })[value] || value;
}

function locationTypeLabel(value) {
    return ({
        PAVILION: 'Павильон',
        STUDIO: 'Студия',
        OUTDOOR: 'Натура',
        OFFICE: 'Офис'
    })[value] || value || '—';
}

function priorityLabel(value) {
    return ({
        NOT_APPLICABLE: 'Не применяется',
        LOW: 'Низкий',
        MEDIUM: 'Средний',
        HIGH: 'Высокий',
        CRITICAL: 'Критический'
    })[value] || value;
}

function formatDate(value) {
    return value ? new Intl.DateTimeFormat('ru-RU', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) : '—';
}

function empty(value) {
    const normalized = String(value ?? '').trim();
    return normalized || null;
}

boot().catch(error => {
    console.error('[FrameSpace] Boot failed', error);
    showLogin();
    toast(error.message || 'Не удалось инициализировать приложение', true);
});
