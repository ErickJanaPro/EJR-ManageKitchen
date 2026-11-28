document.addEventListener('DOMContentLoaded', async () => {
    EJR.initLayout('suivi');

    const TRACKING_POLL_MS = 5000;
    const HISTORY_PAGE_SIZE = 5;
    const $ = id => document.getElementById(id);

    const state = {
        orders: new Map(),
        notifications: [],
        menu: [],
        activeTab: 'active',
        expandedHistoryIds: new Set(),
        historyLimit: HISTORY_PAGE_SIZE,
        stomp: null,
        map: null,
        clientMarker: null,
        courierMarker: null,
        routeLine: null,
        trackedOrderId: null,
        trackingInterval: null
    };

    const STATUS_LABELS = {
        NOUVELLE: 'Reçue',
        RECUE: 'Reçue',
        EN_ATTENTE: 'Reçue',
        EN_PREPARATION: 'En préparation',
        PRETE: 'Prête',
        LIVREE: 'Livrée',
        RECUPEREE: 'Récupérée',
        ANNULEE: 'Annulée'
    };

    const DELIVERY_LABELS = {
        EN_ATTENTE: 'En attente de départ',
        EN_COURS: 'Livreur en route',
        LIVREE: 'Livrée'
    };

    function statusStepIndex(status) {
        if (status === 'LIVREE' || status === 'RECUPEREE') return 3;
        if (status === 'PRETE') return 2;
        if (status === 'EN_PREPARATION') return 1;
        return 0;
    }

    function isActiveOrder(order) {
        return order.status !== 'LIVREE'
            && order.status !== 'RECUPEREE'
            && order.status !== 'ANNULEE';
    }

    function countOrderItems(order) {
        return (order.items || []).reduce((sum, i) => sum + Number(i.quantity || 0), 0);
    }

    function setActiveTab(tab) {
        state.activeTab = tab === 'history' ? 'history' : 'active';
        const isHistory = state.activeTab === 'history';

        $('tab-orders-active').classList.toggle('active', !isHistory);
        $('tab-orders-active').setAttribute('aria-selected', String(!isHistory));
        $('tab-orders-history').classList.toggle('active', isHistory);
        $('tab-orders-history').setAttribute('aria-selected', String(isHistory));

        $('panel-orders-active').classList.toggle('hidden', isHistory);
        $('panel-orders-history').classList.toggle('hidden', !isHistory);
    }

    // ---------- Carte Leaflet ----------
    function initMap() {
        if (typeof L === 'undefined') return;
        state.map = L.map('tracking-map').setView([EJR.RESTAURANT.lat, EJR.RESTAURANT.lng], 13);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; OpenStreetMap'
        }).addTo(state.map);

        L.circleMarker([EJR.RESTAURANT.lat, EJR.RESTAURANT.lng], {
            radius: 8,
            color: '#181818',
            fillColor: '#181818',
            fillOpacity: 1
        }).addTo(state.map).bindPopup(`<strong>${EJR.esc(EJR.RESTAURANT.name)}</strong><br>Point de départ`);

        L.circle([EJR.RESTAURANT.lat, EJR.RESTAURANT.lng], {
            radius: EJR.RESTAURANT.radiusMeters,
            color: '#c84b31',
            weight: 1.5,
            fillOpacity: 0.05
        }).addTo(state.map);
    }

    function stopTrackingPolling() {
        if (state.trackingInterval) {
            clearInterval(state.trackingInterval);
            state.trackingInterval = null;
        }
    }

    function startTrackingPolling(orderId) {
        stopTrackingPolling();
        state.trackingInterval = setInterval(() => {
            if (state.trackedOrderId !== orderId || !EJR.getToken()) {
                stopTrackingPolling();
                return;
            }
            trackDelivery(orderId, { recenter: false });
        }, TRACKING_POLL_MS);
    }

    async function trackDelivery(orderId, { recenter = true } = {}) {
        try {
            const d = await EJR.api(`/api/customer/deliveries/${orderId}`);
            state.trackedOrderId = orderId;
            renderOrders();

            $('map-order-title').textContent = `Livraison — Commande #${d.orderId}`;
            $('map-address-line').textContent = d.address;

            const badge = $('map-delivery-badge');
            badge.className = `status-badge ${d.status === 'LIVREE' ? 'LIVREE' : d.status === 'EN_COURS' ? 'PRETE' : ''}`;
            badge.textContent = DELIVERY_LABELS[d.status] || d.status;
            badge.classList.remove('hidden');

            $('map-progress-wrap').classList.remove('hidden');
            $('map-eta-text').textContent = d.status === 'LIVREE'
                ? `Livrée (${d.distanceKm} km)`
                : `~${d.estimatedMinutes} min · ${d.distanceKm} km`;
            $('map-percent-text').textContent = `${d.progressPercent} %`;

            const fill = $('map-progress-fill');
            fill.style.width = `${d.progressPercent}%`;
            fill.classList.toggle('done', d.status === 'LIVREE');

            if (state.map) {
                const dest = [d.clientLatitude, d.clientLongitude];
                const rest = [d.restaurantLatitude, d.restaurantLongitude];
                const cur = [d.currentLatitude, d.currentLongitude];

                if (state.clientMarker) state.clientMarker.setLatLng(dest);
                else state.clientMarker = L.circleMarker(dest, {
                    radius: 8, color: '#c84b31', fillColor: '#c84b31', fillOpacity: 0.9
                }).addTo(state.map);
                state.clientMarker.bindPopup(`<strong>Destination</strong><br>${EJR.esc(d.address)}`);

                if (state.routeLine) state.routeLine.setLatLngs([rest, dest]);
                else state.routeLine = L.polyline([rest, dest], {
                    color: '#c84b31', weight: 3, dashArray: '6, 6'
                }).addTo(state.map);

                if (d.status !== 'EN_ATTENTE') {
                    if (state.courierMarker) state.courierMarker.setLatLng(cur);
                    else state.courierMarker = L.circleMarker(cur, {
                        radius: 10, color: '#14532d', fillColor: '#16a34a', fillOpacity: 1, weight: 2
                    }).addTo(state.map);
                    state.courierMarker.bindPopup(`<strong>Livreur</strong> · Commande #${d.orderId} (${d.progressPercent} %)`);
                    if (recenter) state.courierMarker.openPopup();
                } else if (state.courierMarker) {
                    state.map.removeLayer(state.courierMarker);
                    state.courierMarker = null;
                }

                if (recenter) {
                    const bounds = L.latLngBounds([rest, dest]);
                    state.map.fitBounds(bounds, { padding: [55, 55], maxZoom: 15 });
                }
            }

            if (d.status === 'EN_COURS') {
                if (!state.trackingInterval) startTrackingPolling(orderId);
            } else {
                stopTrackingPolling();
            }
        } catch (err) {
            stopTrackingPolling();
            if (recenter) {
                EJR.toast(err.message || 'Suivi de livraison indisponible pour cette commande');
            }
        }
    }

    // ---------- Rendu Commandes & Notifications ----------
    function renderStepper(order) {
        const idx = statusStepIndex(order.status);
        const lastLabel = order.fulfillmentType === 'LIVRAISON' ? 'Livrée' : 'Retirée';
        const steps = ['Reçue', 'En cuisine', 'Prête', lastLabel];
        return `
            <div class="order-stepper" aria-label="Avancement de la commande">
                ${steps.map((label, i) => {
                    const cls = i < idx ? 'done' : i === idx ? 'current' : '';
                    return `
                        <div class="step-node ${cls}">
                            <span class="step-bar"></span>
                            <span>${EJR.esc(label)}</span>
                        </div>
                    `;
                }).join('')}
            </div>
        `;
    }

    function renderItemsList(order) {
        return `
            <ul class="order-items-list">
                ${(order.items || []).map(i => `
                    <li>
                        <span>${Number(i.quantity)} × ${EJR.esc(i.dishName)}</span>
                        <span>${EJR.formatPrice(i.lineTotal)}</span>
                    </li>
                `).join('')}
            </ul>
        `;
    }

    function renderActiveOrderCard(order) {
        const isSelected = state.trackedOrderId === order.id;
        const modeLabel = order.fulfillmentType === 'LIVRAISON' ? 'Livraison' : 'À emporter';
        const statusLabel = STATUS_LABELS[order.status] || order.status;

        return `
            <article class="order-track-card ${isSelected ? 'selected' : ''}">
                <div class="order-track-top">
                    <div>
                        <strong>Commande #${Number(order.id)}</strong>
                        <span class="order-meta"> · ${EJR.esc(modeLabel)}</span>
                    </div>
                    <span class="status-badge ${EJR.esc(order.status)}">${EJR.esc(statusLabel)}</span>
                </div>

                ${renderStepper(order)}
                ${renderItemsList(order)}

                <div class="order-track-footer">
                    <strong>Total : ${EJR.formatPrice(order.total)}</strong>
                    <div class="order-actions">
                        ${order.fulfillmentType === 'LIVRAISON' ? `
                            <button type="button" class="btn btn-outline btn-sm" data-track-order="${Number(order.id)}">
                                ${isSelected ? 'Suivie sur la carte' : 'Voir sur la carte'}
                            </button>
                        ` : ''}
                    </div>
                </div>
            </article>
        `;
    }

    function renderHistoryAccordion(order) {
        const isSelected = state.trackedOrderId === order.id;
        const isExpanded = state.expandedHistoryIds.has(order.id);
        const modeLabel = order.fulfillmentType === 'LIVRAISON' ? 'Livraison' : 'À emporter';
        const statusLabel = STATUS_LABELS[order.status] || order.status;
        const itemCount = countOrderItems(order);
        const itemLabel = itemCount > 1 ? `${itemCount} articles` : `${itemCount} article`;

        return `
            <article class="history-accordion ${isSelected ? 'selected' : ''} ${isExpanded ? 'expanded' : ''}">
                <button type="button"
                        class="history-summary-btn"
                        data-toggle-history="${Number(order.id)}"
                        aria-expanded="${String(isExpanded)}">
                    <div class="history-summary-main">
                        <strong>Commande #${Number(order.id)}</strong>
                        <span class="history-summary-meta">
                            ${EJR.esc(modeLabel)} · ${EJR.esc(itemLabel)} · ${EJR.formatPrice(order.total)}
                        </span>
                    </div>
                    <div class="history-summary-right">
                        <span class="status-badge ${EJR.esc(order.status)}">${EJR.esc(statusLabel)}</span>
                        <span class="history-chevron" aria-hidden="true">▾</span>
                    </div>
                </button>

                <div class="history-details ${isExpanded ? '' : 'hidden'}">
                    ${renderItemsList(order)}
                    <div class="order-track-footer">
                        <strong>Total : ${EJR.formatPrice(order.total)}</strong>
                        <div class="order-actions">
                            ${order.fulfillmentType === 'LIVRAISON' ? `
                                <button type="button" class="btn btn-outline btn-sm" data-track-order="${Number(order.id)}">
                                    ${isSelected ? 'Affichée sur la carte' : 'Voir le trajet'}
                                </button>
                            ` : ''}
                            <button type="button" class="btn btn-dark btn-sm" data-reorder="${Number(order.id)}">
                                Recommander
                            </button>
                        </div>
                    </div>
                </div>
            </article>
        `;
    }

    function renderOrders() {
        const all = [...state.orders.values()].sort((a, b) => b.id - a.id);
        const active = all.filter(isActiveOrder);
        const past = all.filter(o => !isActiveOrder(o));

        $('active-orders-count').textContent = String(active.length);
        $('past-orders-count').textContent = String(past.length);

        $('active-orders-list').innerHTML = active.length
            ? active.map(renderActiveOrderCard).join('')
            : '<p style="color:var(--muted); margin:0;">Aucune commande en cours. <a href="commander.html" class="editorial-link">Passer commande</a></p>';

        const visiblePast = past.slice(0, state.historyLimit);
        $('past-orders-list').innerHTML = visiblePast.length
            ? visiblePast.map(renderHistoryAccordion).join('')
            : '<p style="color:var(--muted); margin:0;">Aucune commande terminée pour le moment.</p>';

        const remaining = past.length - visiblePast.length;
        const loadMoreWrap = $('history-load-more-wrap');
        loadMoreWrap.classList.toggle('hidden', remaining <= 0);
        if (remaining > 0) {
            const nextBatch = Math.min(HISTORY_PAGE_SIZE, remaining);
            $('history-load-more-btn').textContent =
                `Afficher ${nextBatch} de plus (${remaining} restante${remaining > 1 ? 's' : ''})`;
        }
    }

    function renderNotifications() {
        const unread = state.notifications.filter(n => !n.read);
        $('notifs-section').classList.toggle('hidden', unread.length === 0);
        $('notifs-count').textContent = String(unread.length);

        $('notifs-list').innerHTML = unread.map(n => `
            <div class="notif-item">
                <span>🔔 ${EJR.esc(n.message)}</span>
                <button type="button" class="notif-dismiss" data-read-notif="${Number(n.id)}">OK</button>
            </div>
        `).join('');
    }

    function reorder(orderId) {
        const order = state.orders.get(orderId);
        if (!order || !order.items?.length) return;
        let added = 0;

        for (const item of order.items) {
            const dish = state.menu.find(d => d.id === item.dishId);
            if (!dish || !dish.disponible) continue;
            for (let q = 0; q < item.quantity; q++) {
                EJR.addToCart(dish);
                added++;
            }
        }

        if (added === 0) {
            EJR.toast('Les plats de cette commande ne sont plus disponibles aujourd\'hui.');
        }
    }

    // ---------- WebSocket STOMP ----------
    function setWsStatus(connected) {
        const pill = $('suivi-ws-pill');
        pill.classList.toggle('online', connected);
        $('suivi-ws-label').textContent = connected ? 'Temps réel actif' : 'Hors ligne';
        const navDot = $('nav-ws-dot');
        if (navDot) navDot.classList.toggle('online', connected);
    }

    function connectWebSocket() {
        if (typeof StompJs === 'undefined' || !EJR.getToken()) return;
        if (state.stomp) state.stomp.deactivate();

        const scheme = location.protocol === 'https:' ? 'wss' : 'ws';
        state.stomp = new StompJs.Client({
            brokerURL: `${scheme}://${location.host}/ws`,
            connectHeaders: { Authorization: `Bearer ${EJR.getToken()}` },
            reconnectDelay: 3000,
            onConnect: () => {
                setWsStatus(true);
                state.stomp.subscribe('/user/queue/orders', frame => {
                    const msg = JSON.parse(frame.body);
                    const order = msg.order;
                    if (!order) return;
                    state.orders.set(order.id, order);
                    renderOrders();
                    EJR.toast(`Commande #${order.id} : ${STATUS_LABELS[order.status] || order.status}`);

                    if (order.fulfillmentType === 'LIVRAISON'
                        && (order.status === 'PRETE' || order.status === 'LIVREE' || state.trackedOrderId === order.id)) {
                        trackDelivery(order.id, { recenter: true });
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

    // ---------- Délégation d'événements ----------
    document.addEventListener('click', async e => {
        const tabBtn = e.target.closest('button[data-orders-tab]');
        if (tabBtn) {
            setActiveTab(tabBtn.dataset.ordersTab);
            return;
        }

        const toggleBtn = e.target.closest('button[data-toggle-history]');
        if (toggleBtn) {
            const id = Number(toggleBtn.dataset.toggleHistory);
            if (state.expandedHistoryIds.has(id)) state.expandedHistoryIds.delete(id);
            else state.expandedHistoryIds.add(id);
            renderOrders();
            return;
        }

        if (e.target.closest('#history-load-more-btn')) {
            state.historyLimit += HISTORY_PAGE_SIZE;
            renderOrders();
            return;
        }

        const trackBtn = e.target.closest('button[data-track-order]');
        if (trackBtn) {
            await trackDelivery(Number(trackBtn.dataset.trackOrder), { recenter: true });
            return;
        }

        const reorderBtn = e.target.closest('button[data-reorder]');
        if (reorderBtn) {
            reorder(Number(reorderBtn.dataset.reorder));
            return;
        }

        const readBtn = e.target.closest('button[data-read-notif]');
        if (readBtn) {
            const id = Number(readBtn.dataset.readNotif);
            try {
                await EJR.api(`/api/notifications/${id}/read`, { method: 'PATCH' });
                state.notifications = state.notifications.map(n => n.id === id ? { ...n, read: true } : n);
                renderNotifications();
            } catch (err) {
                EJR.toast(err.message);
            }
        }
    });

    window.addEventListener('beforeunload', () => {
        stopTrackingPolling();
        if (state.stomp) state.stomp.deactivate();
    });

    // ---------- Initialisation ----------
    initMap();

    if (!EJR.getToken()) {
        $('suivi-guest-card').classList.remove('hidden');
        return;
    }

    $('suivi-auth-content').classList.remove('hidden');

    try {
        const [orders, notifs, menu] = await Promise.all([
            EJR.api('/api/orders/me'),
            EJR.api('/api/notifications/me'),
            EJR.api('/api/customer/menu').catch(() => [])
        ]);

        state.orders = new Map(orders.map(o => [o.id, o]));
        state.notifications = notifs;
        state.menu = menu;

        const requestedId = Number(new URLSearchParams(location.search).get('orderId'));
        const requestedOrder = requestedId ? state.orders.get(requestedId) : null;
        if (requestedOrder && !isActiveOrder(requestedOrder)) {
            state.expandedHistoryIds.add(requestedOrder.id);
            setActiveTab('history');
        } else {
            setActiveTab('active');
        }

        renderOrders();
        renderNotifications();
        connectWebSocket();

        if (requestedOrder?.fulfillmentType === 'LIVRAISON') {
            await trackDelivery(requestedOrder.id, { recenter: true });
        } else {
            const activeDelivery = [...state.orders.values()]
                .sort((a, b) => b.id - a.id)
                .find(o => o.fulfillmentType === 'LIVRAISON' && isActiveOrder(o));

            if (activeDelivery) {
                await trackDelivery(activeDelivery.id, { recenter: true });
            }
        }
    } catch (err) {
        if (err.status === 401) {
            EJR.clearSession();
            $('suivi-auth-content').classList.add('hidden');
            $('suivi-guest-card').classList.remove('hidden');
            EJR.toast('Session expirée — veuillez vous reconnecter');
        } else {
            $('active-orders-list').innerHTML = `<p style="color:var(--danger); margin:0;">Erreur de chargement : ${EJR.esc(err.message)}</p>`;
        }
    }
});