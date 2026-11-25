const RESTAURANT = { lat: 48.8566, lng: 2.3522, name: 'EJR Kitchen — Paris Centre', radiusMeters: 15000 };
const DEFAULT_MAP_STATUS = "Zone de livraison : 15 km autour d'EJR Kitchen (Paris Centre).";
const TRACKING_POLL_MS = 5000;

const state = {
    menu: [],
    cart: new Map(),
    orders: new Map(),
    notifications: [],
    token: localStorage.getItem('ejr_token'),
    user: JSON.parse(localStorage.getItem('ejr_user') || 'null'),
    registerMode: false,
    stomp: null,
    selectedLocation: { address: '15 Rue de Rivoli, Paris', lat: 48.8556, lng: 2.3580 },
    locationValid: true,
    map: null,
    clientMarker: null,
    courierMarker: null,
    routeLine: null,
    trackedOrderId: null,
    trackingInterval: null,
    feedbackTimer: null
};

const $ = sel => document.querySelector(sel);
const esc = s => String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

async function api(path, options = {}) {
    const headers = { 'Content-Type': 'application/json', ...options.headers };
    if (state.token) headers.Authorization = `Bearer ${state.token}`;

    const res = await fetch(path, { ...options, headers });
    const body = res.status === 204 ? null : await res.json().catch(() => null);
    if (!res.ok) {
        const err = new Error(body?.message || `Erreur HTTP ${res.status}`);
        err.status = res.status;
        throw err;
    }
    return body;
}

function setOrderFeedback(message, autoClearMs = 0) {
    clearTimeout(state.feedbackTimer);
    $('#order-feedback').textContent = message;
    if (message && autoClearMs > 0) {
        state.feedbackTimer = setTimeout(() => {
            $('#order-feedback').textContent = '';
        }, autoClearMs);
    }
}

function updateOrderButtonState() {
    const hasItems = state.cart.size > 0;
    const isDelivery = $('#fulfillment-type')?.value === 'LIVRAISON';
    $('#order-btn').disabled = !hasItems || (isDelivery && !state.locationValid);
}

// ---------- Carte Leaflet & Suivi (F8) ----------
function stopTrackingPolling() {
    if (state.trackingInterval) {
        clearInterval(state.trackingInterval);
        state.trackingInterval = null;
    }
}

function startTrackingPolling(orderId) {
    stopTrackingPolling();
    state.trackingInterval = setInterval(() => {
        if (state.trackedOrderId !== orderId || !state.token) {
            stopTrackingPolling();
            return;
        }
        trackDelivery(orderId, { scroll: false, recenter: false });
    }, TRACKING_POLL_MS);
}

function clearMapTracking() {
    stopTrackingPolling();
    state.trackedOrderId = null;
    if (state.courierMarker && state.map) {
        state.map.removeLayer(state.courierMarker);
        state.courierMarker = null;
    }
    $('#map-status-text').textContent = DEFAULT_MAP_STATUS;
}

function drawRouteOnMap(lat, lng, address) {
    if (!state.map) return;

    if (state.clientMarker) state.clientMarker.setLatLng([lat, lng]);
    else state.clientMarker = L.marker([lat, lng]).addTo(state.map);
    state.clientMarker.bindPopup(`Destination : ${esc(address)}`);

    const points = [[RESTAURANT.lat, RESTAURANT.lng], [lat, lng]];
    if (state.routeLine) state.routeLine.setLatLngs(points);
    else state.routeLine = L.polyline(points, { color: '#e67e22', dashArray: '6, 6' }).addTo(state.map);
}

function initMap() {
    if (typeof L === 'undefined') return;
    state.map = L.map('delivery-map').setView([RESTAURANT.lat, RESTAURANT.lng], 13);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap'
    }).addTo(state.map);

    L.marker([RESTAURANT.lat, RESTAURANT.lng]).addTo(state.map)
        .bindPopup(`<strong>${esc(RESTAURANT.name)}</strong><br>Point de départ`).openPopup();

    L.circle([RESTAURANT.lat, RESTAURANT.lng], {
        radius: RESTAURANT.radiusMeters,
        color: '#1f3a5f',
        fillOpacity: 0.06
    }).addTo(state.map);

    updateClientPin(state.selectedLocation.lat, state.selectedLocation.lng, state.selectedLocation.address);

    state.map.on('click', e => {
        clearMapTracking();
        setOrderFeedback('');
        const label = `Point sélectionné (${e.latlng.lat.toFixed(4)}, ${e.latlng.lng.toFixed(4)})`;
        $('#delivery-address').value = label;
        $('#geocode-btn').disabled = true;
        updateClientPin(e.latlng.lat, e.latlng.lng, label, true);
    });
}

function updateClientPin(lat, lng, address, fromMapClick = false) {
    state.selectedLocation = { address, lat, lng };
    drawRouteOnMap(lat, lng, address);

    if (!state.map) return;
    const rawKm = state.map.distance([RESTAURANT.lat, RESTAURANT.lng], [lat, lng]) / 1000;
    const kmRounded = Math.round(rawKm * 10) / 10;
    const maxKm = RESTAURANT.radiusMeters / 1000;
    const inZone = kmRounded <= maxKm;
    state.locationValid = inZone;

    const mins = Math.max(15, Math.round(10 + kmRounded * 3.5));
    const prefix = fromMapClick ? 'Point enregistré depuis la carte (bouton « Localiser » inutile) — ' : '';
    const infoEl = $('#delivery-zone-info');
    infoEl.textContent = inZone
        ? `${prefix}Distance : ${kmRounded.toFixed(1)} km — Temps estimé : ~${mins} min`
        : `${prefix}Hors zone (${kmRounded.toFixed(1)} km > ${maxKm} km max) — Rapprochez le point pour valider la commande.`;
    infoEl.className = inZone ? 'muted' : 'error';

    updateOrderButtonState();
}

async function geocodeAddress() {
    const query = $('#delivery-address').value.trim();
    if (!query) return;
    clearMapTracking();
    const infoEl = $('#delivery-zone-info');
    infoEl.className = 'muted';
    infoEl.textContent = 'Recherche de l’adresse...';
    try {
        const res = await fetch(`https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${encodeURIComponent(query)}`);
        const data = await res.json();
        if (!data.length) {
            infoEl.textContent = 'Adresse introuvable : cliquez directement sur la carte.';
            return;
        }
        const lat = Number(data[0].lat);
        const lng = Number(data[0].lon);
        updateClientPin(lat, lng, data[0].display_name);
        state.map?.setView([lat, lng], 14);
    } catch {
        infoEl.textContent = 'Géocodage indisponible : cliquez sur la carte pour placer votre point.';
    }
}

window.trackDelivery = async function(orderId, { scroll = true, recenter = true } = {}) {
    try {
        const d = await api(`/api/customer/deliveries/${orderId}`);
        state.trackedOrderId = orderId;

        if (recenter) {
            drawRouteOnMap(d.clientLatitude, d.clientLongitude, d.address);
        }

        if (state.map) {
            if (state.courierMarker) state.courierMarker.setLatLng([d.currentLatitude, d.currentLongitude]);
            else state.courierMarker = L.circleMarker([d.currentLatitude, d.currentLongitude], {
                radius: 9, color: '#166534', fillColor: '#22c55e', fillOpacity: 1
            }).addTo(state.map);

            state.courierMarker.bindPopup(`Livreur — Commande #${d.orderId} (${d.progressPercent}%)`);
            if (recenter) {
                state.courierMarker.openPopup();
                state.map.setView([d.currentLatitude, d.currentLongitude], 14);
            }
        }

        $('#map-status-text').textContent =
            `Commande #${d.orderId} : ${d.status} (${d.progressPercent}% — ~${d.estimatedMinutes} min, ${d.distanceKm} km)`;

        if (d.status === 'EN_COURS') {
            if (!state.trackingInterval) startTrackingPolling(orderId);
        } else {
            stopTrackingPolling();
        }

        if (scroll) location.hash = '#carte';
    } catch (err) {
        stopTrackingPolling();
        if (recenter) $('#map-status-text').textContent = err.message;
    }
};

// ---------- Menu ----------
async function loadMenu() {
    try {
        state.menu = await api('/api/customer/menu');
        renderMenu();
    } catch (err) {
        $('#menu-container').innerHTML = `<p class="error">Impossible de charger le menu : ${esc(err.message)}</p>`;
    }
}

function renderMenu() {
    $('#menu-container').innerHTML = state.menu.map(plat => `
        <article class="menu-card ${plat.disponible ? '' : 'unavailable'}">
            <div>
                <div style="font-size: 2.8rem;">${esc(plat.imageUrl)}</div>
                <h3>${esc(plat.nom)}</h3>
                <p class="muted">${esc(plat.description)}</p>
            </div>
            <div>
                <p class="price">${Number(plat.prix).toFixed(2)} €</p>
                <button ${plat.disponible ? '' : 'disabled'} onclick="addToCart(${plat.id})">
                    ${plat.disponible ? 'Ajouter au panier' : 'Indisponible'}
                </button>
            </div>
        </article>
    `).join('');
}

// ---------- Panier ----------
window.addToCart = function(dishId) {
    const plat = state.menu.find(p => p.id === dishId);
    if (!plat || !plat.disponible) return;
    setOrderFeedback('');
    const entry = state.cart.get(dishId);
    if (entry) entry.quantity += 1;
    else state.cart.set(dishId, { plat, quantity: 1 });
    renderCart();
};

window.updateQty = function(dishId, delta) {
    const entry = state.cart.get(dishId);
    if (!entry) return;
    setOrderFeedback('');
    entry.quantity += delta;
    if (entry.quantity <= 0) state.cart.delete(dishId);
    renderCart();
};

function renderCart() {
    const entries = [...state.cart.values()];
    const count = entries.reduce((s, e) => s + e.quantity, 0);
    const total = entries.reduce((s, e) => s + Number(e.plat.prix) * e.quantity, 0);

    $('#cart-count').textContent = count;
    $('#cart-total').textContent = `${total.toFixed(2)} €`;
    updateOrderButtonState();

    if (entries.length === 0) {
        $('#cart-items').innerHTML = '<p class="muted">Votre panier est vide.</p>';
        return;
    }

    $('#cart-items').innerHTML = entries.map(({ plat, quantity }) => `
        <div class="cart-row">
            <span><strong>${esc(plat.nom)}</strong> (${Number(plat.prix).toFixed(2)} €)</span>
            <div class="row">
                <button class="small secondary" onclick="updateQty(${plat.id}, -1)">-</button>
                <span>${quantity}</span>
                <button class="small secondary" onclick="updateQty(${plat.id}, 1)">+</button>
            </div>
        </div>
    `).join('');
}

async function submitOrder() {
    setOrderFeedback('');
    if (!state.token) {
        $('#auth-panel').classList.remove('hidden');
        $('#auth-error').textContent = 'Connectez-vous pour valider votre commande.';
        return;
    }

    const fulfillmentType = $('#fulfillment-type').value;
    if (fulfillmentType === 'LIVRAISON' && !state.locationValid) {
        setOrderFeedback('Erreur : adresse hors zone de livraison (15 km max).');
        return;
    }

    const payload = {
        fulfillmentType,
        items: [...state.cart.values()].map(({ plat, quantity }) => ({ dishId: plat.id, quantity }))
    };

    try {
        const order = await api('/api/orders', { method: 'POST', body: JSON.stringify(payload) });
        if (fulfillmentType === 'LIVRAISON') {
            const addr = $('#delivery-address').value.trim() || state.selectedLocation.address;
            await api('/api/customer/deliveries', {
                method: 'POST',
                body: JSON.stringify({
                    orderId: order.id,
                    address: addr,
                    latitude: state.selectedLocation.lat,
                    longitude: state.selectedLocation.lng
                })
            });
            await trackDelivery(order.id);
        }
        state.cart.clear();
        state.orders.set(order.id, order);
        renderCart();
        renderOrders();
        setOrderFeedback(`Commande #${order.id} envoyée en cuisine !`, 6000);
    } catch (err) {
        setOrderFeedback(`Erreur : ${err.message}`);
    }
}

// ---------- Authentification ----------
function updateAuthUI() {
    const btn = $('#auth-toggle-btn');
    if (state.user && state.token) {
        btn.textContent = `${state.user.fullName} (Déconnexion)`;
        $('#auth-panel').classList.add('hidden');
    } else {
        btn.textContent = 'Connexion';
    }
}

async function handleAuthSubmit(e) {
    e.preventDefault();
    $('#auth-error').textContent = '';
    const endpoint = state.registerMode ? '/api/auth/register' : '/api/auth/login';
    const body = state.registerMode
        ? { email: $('#auth-email').value, password: $('#auth-password').value, fullName: $('#auth-fullname').value }
        : { email: $('#auth-email').value, password: $('#auth-password').value };

    try {
        const auth = await api(endpoint, { method: 'POST', body: JSON.stringify(body) });
        state.token = auth.accessToken;
        state.user = auth.user;
        localStorage.setItem('ejr_token', state.token);
        localStorage.setItem('ejr_user', JSON.stringify(state.user));
        updateAuthUI();
        await initAuthenticatedSession();
    } catch (err) {
        $('#auth-error').textContent = err.message;
    }
}

function logout() {
    if (state.stomp) state.stomp.deactivate();
    state.token = null;
    state.user = null;
    state.orders.clear();
    state.notifications = [];
    localStorage.removeItem('ejr_token');
    localStorage.removeItem('ejr_user');
    setOrderFeedback('');
    clearMapTracking();
    setWsStatus(false);
    updateAuthUI();
    renderOrders();
    renderNotifications();
}

// ---------- Commandes, Notifications & WebSocket STOMP ----------
async function initAuthenticatedSession() {
    if (!state.token) return;
    try {
        const [orders, notifs] = await Promise.all([
            api('/api/orders/me'),
            api('/api/notifications/me')
        ]);
        state.orders = new Map(orders.map(o => [o.id, o]));
        state.notifications = notifs;
        renderOrders();
        renderNotifications();
        connectWebSocket();
    } catch (err) {
        if (err.status === 401) logout();
    }
}

function connectWebSocket() {
    if (state.stomp) state.stomp.deactivate();
    const scheme = location.protocol === 'https:' ? 'wss' : 'ws';
    state.stomp = new StompJs.Client({
        brokerURL: `${scheme}://${location.host}/ws`,
        connectHeaders: { Authorization: `Bearer ${state.token}` },
        reconnectDelay: 3000,
        onConnect: () => {
            setWsStatus(true);
            state.stomp.subscribe('/user/queue/orders', frame => {
                const msg = JSON.parse(frame.body);
                state.orders.set(msg.order.id, msg.order);
                renderOrders();
                if (msg.order.fulfillmentType === 'LIVRAISON'
                    && (msg.order.status === 'PRETE' || msg.order.status === 'LIVREE')) {
                    trackDelivery(msg.order.id);
                }
            });
            state.stomp.subscribe('/user/queue/notifications', frame => {
                const notif = JSON.parse(frame.body);
                state.notifications.unshift(notif);
                renderNotifications();
            });
        },
        onWebSocketClose: () => setWsStatus(false),
        onStompError: () => setWsStatus(false)
    });
    state.stomp.activate();
}

function setWsStatus(connected) {
    const badge = $('#ws-badge');
    badge.className = `badge ${connected ? 'on' : 'off'}`;
    badge.textContent = connected ? 'Temps réel actif' : 'Hors ligne';
}

window.markNotifRead = async function(id) {
    await api(`/api/notifications/${id}/read`, { method: 'PATCH' });
    state.notifications = state.notifications.map(n => n.id === id ? { ...n, read: true } : n);
    renderNotifications();
};

function renderNotifications() {
    const unread = state.notifications.filter(n => !n.read);
    $('#notifications-box').innerHTML = unread.map(n => `
        <div class="notif">
            <span>🔔 ${esc(n.message)}</span>
            <button class="small secondary" onclick="markNotifRead(${n.id})">OK</button>
        </div>
    `).join('');
}

function renderOrders() {
    if (!state.token) {
        $('#orders-list').innerHTML = '<p class="muted">Connectez-vous pour voir et suivre vos commandes.</p>';
        return;
    }
    const list = [...state.orders.values()].sort((a, b) => b.id - a.id);
    if (list.length === 0) {
        $('#orders-list').innerHTML = '<p class="muted">Aucune commande pour le moment.</p>';
        return;
    }
    $('#orders-list').innerHTML = list.map(o => `
        <article class="order-card">
            <div class="row" style="justify-content: space-between;">
                <strong>Commande #${o.id} (${esc(o.fulfillmentType)})</strong>
                <span class="status-pill ${esc(o.status)}">${esc(o.status)}</span>
            </div>
            <ul>${o.items.map(i => `<li>${i.quantity} × ${esc(i.dishName)} — ${Number(i.lineTotal).toFixed(2)} €</li>`).join('')}</ul>
            <div class="row" style="justify-content: space-between;">
                <strong>Total : ${Number(o.total).toFixed(2)} €</strong>
                ${o.fulfillmentType === 'LIVRAISON'
                    ? `<button class="small secondary" onclick="trackDelivery(${o.id})">Suivre sur la carte</button>`
                    : ''}
            </div>
        </article>
    `).join('');
}

// ---------- Initialisation ----------
document.addEventListener('DOMContentLoaded', () => {
    $('#auth-toggle-btn').addEventListener('click', () => {
        if (state.token) logout();
        else $('#auth-panel').classList.toggle('hidden');
    });

    $('#auth-switch').addEventListener('click', () => {
        state.registerMode = !state.registerMode;
        $('#auth-title').textContent = state.registerMode ? 'Inscription client' : 'Connexion client';
        $('#auth-submit').textContent = state.registerMode ? 'Créer le compte' : 'Se connecter';
        $('#auth-switch').textContent = state.registerMode ? 'Déjà un compte ?' : 'Créer un compte';
        $('#auth-fullname').classList.toggle('hidden', !state.registerMode);
        $('#auth-fullname').required = state.registerMode;
    });

    $('#delivery-address').addEventListener('input', () => {
        $('#geocode-btn').disabled = false;
    });

    $('#fulfillment-type').addEventListener('change', e => {
        setOrderFeedback('');
        const isDelivery = e.target.value === 'LIVRAISON';
        $('#delivery-address-box').classList.toggle('hidden', !isDelivery);
        if (isDelivery) {
            clearMapTracking();
            $('#delivery-address').value = state.selectedLocation.address;
            $('#geocode-btn').disabled = false;
            setTimeout(() => state.map?.invalidateSize(), 50);
        }
        updateOrderButtonState();
    });

    $('#geocode-btn').addEventListener('click', geocodeAddress);
    $('#auth-form').addEventListener('submit', handleAuthSubmit);
    $('#order-btn').addEventListener('click', submitOrder);

    initMap();
    updateAuthUI();
    loadMenu();
    if (state.token) initAuthenticatedSession();
});