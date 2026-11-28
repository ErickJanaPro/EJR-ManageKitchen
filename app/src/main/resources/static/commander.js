document.addEventListener('DOMContentLoaded', async () => {
    EJR.initLayout('commander');

    const MAX_RADIUS_KM = EJR.RESTAURANT.radiusMeters / 1000;

    const state = {
        dishes: [],
        activeCategory: 'ALL',
        prefs: EJR.getDeliveryPrefs(),
        draftMode: 'RETRAIT',
        draftLocation: null,
        draftValid: true,
        miniMap: null,
        pinMarker: null,
        routeLine: null,
        submitting: false
    };

    const $ = id => document.getElementById(id);

    // ---------- Catégorisation (compatible mock actuel & futur menu F3) ----------
    function inferCategory(dish) {
        if (dish.categorie) return dish.categorie;
        const name = String(dish.nom || '').toLowerCase();
        if (name.includes('salade') || name.includes('soupe') || name.includes('entrée')) return 'Entrées & Salades';
        if (name.includes('tiramisu') || name.includes('gâteau') || name.includes('tarte') || name.includes('mousse') || name.includes('dessert')) return 'Desserts';
        return 'Plats';
    }

    function computeDistanceKm(lat1, lon1, lat2, lon2) {
        const R = 6371;
        const dLat = (lat2 - lat1) * Math.PI / 180;
        const dLon = (lon2 - lon1) * Math.PI / 180;
        const a = Math.sin(dLat / 2) ** 2
            + Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLon / 2) ** 2;
        return Math.round(R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) * 10) / 10;
    }

    function estimateMinutes(km) {
        return Math.max(15, Math.round(10 + km * 3.5));
    }

    // ---------- Rendu du menu & des catégories ----------
    function renderCategoryTabs() {
        const categories = [...new Set(state.dishes.map(inferCategory))];
        const tabs = [
            { id: 'ALL', label: 'Toute la carte' },
            ...categories.map(c => ({ id: c, label: c }))
        ];

        $('category-tabs').innerHTML = tabs.map(t => `
            <button type="button"
                    class="cat-tab ${state.activeCategory === t.id ? 'active' : ''}"
                    data-category="${EJR.esc(t.id)}">
                ${EJR.esc(t.label)}
            </button>
        `).join('');
    }

    function renderMenu() {
        const container = $('menu-sections');
        if (state.dishes.length === 0) {
            container.innerHTML = '<p style="color:var(--muted); padding:2rem 0;">Aucun plat disponible pour le moment.</p>';
            return;
        }

        const groups = new Map();
        for (const dish of state.dishes) {
            const cat = inferCategory(dish);
            if (state.activeCategory !== 'ALL' && cat !== state.activeCategory) continue;
            if (!groups.has(cat)) groups.set(cat, []);
            groups.get(cat).push(dish);
        }

        container.innerHTML = [...groups.entries()].map(([cat, list]) => `
            <section class="menu-section">
                <h2 class="menu-section-title">${EJR.esc(cat)}</h2>
                <div class="bao-grid">
                    ${list.map(plat => `
                        <article class="bao-card ${plat.disponible ? '' : 'unavailable'}">
                            <div class="bao-card-info">
                                <h3 class="bao-card-title">${EJR.esc(plat.nom)}</h3>
                                <p class="bao-card-desc">${EJR.esc(plat.description)}</p>
                                <div class="bao-card-bottom">
                                    <span class="bao-card-price">${EJR.formatPrice(plat.prix)}</span>
                                    <button type="button"
                                            class="btn-add-mini"
                                            data-add-dish="${Number(plat.id)}"
                                            ${plat.disponible ? '' : 'disabled'}>
                                        ${plat.disponible ? '+ Ajouter' : 'Indisponible'}
                                    </button>
                                </div>
                            </div>
                            <img class="bao-card-img"
                                 src="${EJR.esc(EJR.getDishPhoto(plat))}"
                                 alt="${EJR.esc(plat.nom)}"
                                 loading="lazy">
                        </article>
                    `).join('')}
                </div>
            </section>
        `).join('');
    }

    // ---------- Barre inférieure & indicateurs de mode ----------
    function setBottomBarCollapsed(collapsed) {
        $('order-bottom-bar')?.classList.toggle('hidden', collapsed);
        $('expand-bottom-bar-btn')?.classList.toggle('hidden', !collapsed);
        document.body.classList.toggle('bar-collapsed', collapsed);
        sessionStorage.setItem('ejr_bar_collapsed', collapsed ? '1' : '0');
    }


    function syncModeIndicators() {
        const isDelivery = state.prefs.fulfillmentType === 'LIVRAISON';
        const km = computeDistanceKm(EJR.RESTAURANT.lat, EJR.RESTAURANT.lng, state.prefs.lat, state.prefs.lng);
        const icon = isDelivery ? '🛵' : '🛍';
        const fullLabel = isDelivery
            ? `Livraison — ${state.prefs.address}`
            : 'À emporter — Paris Marais';

        $('change-mode-top-btn').textContent = isDelivery
            ? 'Livraison · Modifier'
            : 'À emporter · Modifier';

        $('bottom-mode-icon').textContent = icon;
        $('fab-mode-icon').textContent = icon;

        const labelEl = $('bottom-mode-label');
        labelEl.textContent = fullLabel;
        labelEl.title = fullLabel;

        $('expand-bottom-bar-btn').title = `${fullLabel} — Cliquer pour afficher le bandeau`;

        $('bottom-eta-label').textContent = isDelivery
            ? `~${estimateMinutes(km)} min · ${km.toFixed(1)} km`
            : 'Dès que possible (~15 min)';
    }

    function syncBottomCart() {
        const items = EJR.loadCart();
        const count = items.reduce((s, i) => s + i.quantity, 0);
        const total = items.reduce((s, i) => s + i.prix * i.quantity, 0);
        $('bottom-cart-count').textContent = String(count);
        $('bottom-cart-total').textContent = EJR.formatPrice(total);

        const fabBadge = $('fab-cart-badge');
        fabBadge.textContent = String(count);
        fabBadge.classList.toggle('hidden', count === 0);
    }

    // ---------- Modale Bao Family + Mini-carte Leaflet ----------
    function ensureMiniMap() {
        if (state.miniMap || typeof L === 'undefined') return;
        state.miniMap = L.map('modal-mini-map').setView([EJR.RESTAURANT.lat, EJR.RESTAURANT.lng], 12);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; OpenStreetMap'
        }).addTo(state.miniMap);

        L.marker([EJR.RESTAURANT.lat, EJR.RESTAURANT.lng])
            .addTo(state.miniMap)
            .bindPopup(`<strong>${EJR.esc(EJR.RESTAURANT.name)}</strong>`);

        L.circle([EJR.RESTAURANT.lat, EJR.RESTAURANT.lng], {
            radius: EJR.RESTAURANT.radiusMeters,
            color: '#c84b31',
            fillOpacity: 0.07
        }).addTo(state.miniMap);

        state.miniMap.on('click', e => {
            const label = `Point sélectionné (${e.latlng.lat.toFixed(4)}, ${e.latlng.lng.toFixed(4)})`;
            $('modal-address').value = label;
            setDraftLocation(e.latlng.lat, e.latlng.lng, label, true);
        });
    }

    function setDraftLocation(lat, lng, address, fromMapClick = false) {
        state.draftLocation = { lat, lng, address };
        const km = computeDistanceKm(EJR.RESTAURANT.lat, EJR.RESTAURANT.lng, lat, lng);
        const inZone = km <= MAX_RADIUS_KM;
        state.draftValid = inZone;

        if (state.miniMap) {
            if (state.pinMarker) state.pinMarker.setLatLng([lat, lng]);
            else state.pinMarker = L.marker([lat, lng]).addTo(state.miniMap);
            state.pinMarker.bindPopup(EJR.esc(address));

            const pts = [[EJR.RESTAURANT.lat, EJR.RESTAURANT.lng], [lat, lng]];
            if (state.routeLine) state.routeLine.setLatLngs(pts);
            else state.routeLine = L.polyline(pts, { color: '#c84b31', dashArray: '6, 6' }).addTo(state.miniMap);
        }

        const statusEl = $('modal-zone-status');
        const note = fromMapClick ? 'Point enregistré sur la carte · ' : '';
        if (inZone) {
            statusEl.textContent = `${note}Zone OK : ${km.toFixed(1)} km (~${estimateMinutes(km)} min)`;
            statusEl.className = 'zone-status valid';
        } else {
            statusEl.textContent = `Hors zone (${km.toFixed(1)} km > ${MAX_RADIUS_KM} km max) — rapprochez votre adresse.`;
            statusEl.className = 'zone-status invalid';
        }
        updateModalValidateBtn();
    }

    function updateModalValidateBtn() {
        $('modal-validate-btn').disabled = state.draftMode === 'LIVRAISON' && !state.draftValid;
    }

    function switchModalMode(mode) {
        state.draftMode = mode;
        $('tab-retrait').classList.toggle('active', mode === 'RETRAIT');
        $('tab-livraison').classList.toggle('active', mode === 'LIVRAISON');
        $('modal-retrait-box').classList.toggle('hidden', mode !== 'RETRAIT');
        $('modal-livraison-box').classList.toggle('hidden', mode !== 'LIVRAISON');

        if (mode === 'LIVRAISON') {
            ensureMiniMap();
            setTimeout(() => {
                state.miniMap?.invalidateSize();
                if (state.draftLocation) {
                    setDraftLocation(state.draftLocation.lat, state.draftLocation.lng, state.draftLocation.address);
                    state.miniMap?.setView([state.draftLocation.lat, state.draftLocation.lng], 13);
                }
            }, 60);
        }
        updateModalValidateBtn();
    }

    function openModeModal() {
        state.draftMode = state.prefs.fulfillmentType;
        state.draftLocation = {
            lat: state.prefs.lat,
            lng: state.prefs.lng,
            address: state.prefs.address
        };
        $('modal-address').value = state.prefs.address || '';
        $('modal-instructions').value = state.prefs.instructions || '';
        $('order-mode-modal').classList.remove('hidden');
        switchModalMode(state.draftMode);
    }

    function closeModeModal() {
        $('order-mode-modal').classList.add('hidden');
    }

    async function geocodeInputAddress() {
        const query = $('modal-address').value.trim();
        if (!query) return;
        const statusEl = $('modal-zone-status');
        statusEl.className = 'zone-status';
        statusEl.textContent = 'Recherche de votre adresse...';
        try {
            const res = await fetch(`https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${encodeURIComponent(query)}`);
            const data = await res.json();
            if (!data.length) {
                statusEl.textContent = 'Adresse introuvable — cliquez directement sur la carte.';
                return;
            }
            const lat = Number(data[0].lat);
            const lng = Number(data[0].lon);
            $('modal-address').value = data[0].display_name;
            setDraftLocation(lat, lng, data[0].display_name);
            state.miniMap?.setView([lat, lng], 14);
        } catch {
            statusEl.textContent = 'Géocodage indisponible — cliquez directement sur la carte.';
        }
    }

    function geolocateBrowser() {
        const statusEl = $('modal-zone-status');
        if (!navigator.geolocation) {
            statusEl.className = 'zone-status invalid';
            statusEl.textContent = 'Géolocalisation non prise en charge par ce navigateur.';
            return;
        }
        const btn = $('modal-geolocate-btn');
        btn.disabled = true;
        btn.textContent = 'Localisation en cours...';
        statusEl.className = 'zone-status';
        statusEl.textContent = 'Récupération de votre position GPS...';

        navigator.geolocation.getCurrentPosition(async pos => {
            const lat = pos.coords.latitude;
            const lng = pos.coords.longitude;
            let label = `Ma position (${lat.toFixed(4)}, ${lng.toFixed(4)})`;
            try {
                const res = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}`);
                const data = await res.json();
                if (data?.display_name) label = data.display_name;
            } catch {
                // On garde les coordonnées si le géocodage inverse échoue
            }
            $('modal-address').value = label;
            setDraftLocation(lat, lng, label);
            state.miniMap?.setView([lat, lng], 14);
            btn.disabled = false;
            btn.textContent = '⌖ Me géolocaliser';
        }, () => {
            statusEl.className = 'zone-status invalid';
            statusEl.textContent = 'Autorisation refusée — saisissez votre adresse ou cliquez sur la carte.';
            btn.disabled = false;
            btn.textContent = '⌖ Me géolocaliser';
        }, { enableHighAccuracy: true, timeout: 8000 });
    }

    // ---------- Validation de commande (POST /api/orders + /api/customer/deliveries) ----------
    async function submitCheckout() {
        if (state.submitting) return;
        const items = EJR.loadCart();
        if (items.length === 0) return;

        if (!EJR.getToken()) {
            EJR.closeCart();
            EJR.toast('Connectez-vous pour valider votre commande');
            setTimeout(() => {
                location.href = 'profil.html?redirect=commander.html%3Fcheckout%3D1';
            }, 700);
            return;
        }

        const isDelivery = state.prefs.fulfillmentType === 'LIVRAISON';
        if (isDelivery) {
            const km = computeDistanceKm(EJR.RESTAURANT.lat, EJR.RESTAURANT.lng, state.prefs.lat, state.prefs.lng);
            if (km > MAX_RADIUS_KM) {
                EJR.closeCart();
                openModeModal();
                EJR.toast('Adresse hors zone de livraison (15 km max)');
                return;
            }
        }

        state.submitting = true;
        const btn = $('drawer-checkout-btn');
        if (btn) {
            btn.disabled = true;
            btn.textContent = 'Envoi en cuisine...';
        }

        try {
            const order = await EJR.api('/api/orders', {
                method: 'POST',
                body: JSON.stringify({
                    fulfillmentType: state.prefs.fulfillmentType,
                    items: items.map(i => ({ dishId: i.id, quantity: i.quantity }))
                })
            });

            if (isDelivery) {
                const fullAddress = state.prefs.instructions
                    ? `${state.prefs.address} — ${state.prefs.instructions}`.slice(0, 255)
                    : state.prefs.address.slice(0, 255);

                await EJR.api('/api/customer/deliveries', {
                    method: 'POST',
                    body: JSON.stringify({
                        orderId: order.id,
                        address: fullAddress,
                        latitude: state.prefs.lat,
                        longitude: state.prefs.lng
                    })
                });
            }

            EJR.clearCart();
            EJR.closeCart();
            $('order-confirm-title').textContent = `Commande #${order.id} envoyée en cuisine !`;
            $('order-confirm-sub').textContent = isDelivery
                ? `Livraison prévue vers ${state.prefs.address} (${EJR.formatPrice(order.total)}).`
                : `Retrait sur place au 12 Rue des Archives (${EJR.formatPrice(order.total)}).`;
            $('order-confirm-link').href = `suivi.html?orderId=${order.id}`;
            $('order-confirm-banner').classList.remove('hidden');
            window.scrollTo({ top: 0, behavior: 'smooth' });
        } catch (err) {
            if (err.status === 401) {
                EJR.clearSession();
                EJR.toast('Session expirée — reconnectez-vous');
                location.href = 'profil.html?redirect=commander.html%3Fcheckout%3D1';
            } else {
                EJR.toast(`Erreur : ${err.message}`);
            }
        } finally {
            state.submitting = false;
            if (btn) {
                btn.textContent = 'Valider la commande';
                btn.disabled = EJR.loadCart().length === 0;
            }
        }
    }

    // ---------- Événements ----------
    $('category-tabs').addEventListener('click', e => {
        const tab = e.target.closest('button[data-category]');
        if (!tab) return;
        state.activeCategory = tab.dataset.category;
        renderCategoryTabs();
        renderMenu();
    });

    $('collapse-bottom-bar-btn').addEventListener('click', () => setBottomBarCollapsed(true));
    $('expand-bottom-bar-btn').addEventListener('click', () => setBottomBarCollapsed(false));

    $('menu-sections').addEventListener('click', e => {
        const btn = e.target.closest('button[data-add-dish]');
        if (!btn) return;
        const dish = state.dishes.find(p => p.id === Number(btn.dataset.addDish));
        if (dish) EJR.addToCart(dish);
    });

    $('change-mode-top-btn').addEventListener('click', openModeModal);
    $('bottom-mode-btn').addEventListener('click', openModeModal);
    $('bottom-cart-btn').addEventListener('click', () => EJR.openCart());
    $('close-modal-btn').addEventListener('click', closeModeModal);
    $('order-mode-modal').addEventListener('click', e => {
        if (e.target === $('order-mode-modal')) closeModeModal();
    });

    $('tab-retrait').addEventListener('click', () => switchModalMode('RETRAIT'));
    $('tab-livraison').addEventListener('click', () => switchModalMode('LIVRAISON'));
    $('modal-search-btn').addEventListener('click', geocodeInputAddress);
    $('modal-address').addEventListener('keydown', e => {
        if (e.key === 'Enter') {
            e.preventDefault();
            geocodeInputAddress();
        }
    });
    $('modal-geolocate-btn').addEventListener('click', geolocateBrowser);

    $('modal-validate-btn').addEventListener('click', () => {
        const nextPrefs = {
            fulfillmentType: state.draftMode,
            address: ($('modal-address').value.trim() || state.draftLocation?.address || state.prefs.address),
            lat: state.draftLocation?.lat ?? state.prefs.lat,
            lng: state.draftLocation?.lng ?? state.prefs.lng,
            instructions: $('modal-instructions').value.trim()
        };
        state.prefs = nextPrefs;
        EJR.saveDeliveryPrefs(nextPrefs);
        syncModeIndicators();
        closeModeModal();
        EJR.toast(nextPrefs.fulfillmentType === 'LIVRAISON'
            ? 'Mode Livraison enregistré'
            : 'Mode À emporter enregistré');
    });

    window.addEventListener('ejr:cart-changed', syncBottomCart);
    window.addEventListener('ejr:checkout-requested', submitCheckout);

    // ---------- Chargement initial ----------
    syncModeIndicators();
    syncBottomCart();

    setBottomBarCollapsed(sessionStorage.getItem('ejr_bar_collapsed') === '1');

    try {
        state.dishes = await EJR.api('/api/customer/menu');
        renderCategoryTabs();
        renderMenu();
    } catch (err) {
        $('menu-sections').innerHTML = `<p style="color:var(--danger); padding:2rem 0;">Impossible de charger la carte : ${EJR.esc(err.message)}</p>`;
    }

    const params = new URLSearchParams(location.search);
    if (params.get('checkout') === '1') {
        EJR.openCart();
    } else if (!sessionStorage.getItem('ejr_mode_prompted')) {
        sessionStorage.setItem('ejr_mode_prompted', '1');
        openModeModal();
    }
});