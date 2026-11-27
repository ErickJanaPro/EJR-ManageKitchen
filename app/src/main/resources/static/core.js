(() => {
    const STORAGE = {
        TOKEN: 'ejr_token',
        USER: 'ejr_user',
        CART: 'ejr_cart',
        DELIVERY: 'ejr_delivery_prefs'
    };

    const RESTAURANT = {
        lat: 48.8566,
        lng: 2.3522,
        name: 'EJR Kitchen — Paris Centre',
        address: '12 Rue des Archives, 75004 Paris',
        radiusMeters: 15000
    };

    const DISH_PHOTOS = [
        { match: 'burger', url: 'https://images.unsplash.com/photo-1550547660-d9450f859349?auto=format&fit=crop&w=800&q=80' },
        { match: 'salade', url: 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=800&q=80' },
        { match: 'tiramisu', url: 'https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?auto=format&fit=crop&w=800&q=80' },
        { match: 'risotto', url: 'https://images.unsplash.com/photo-1476124369491-e7addf5db371?auto=format&fit=crop&w=800&q=80' }
    ];
    const FALLBACK_DISH_PHOTO = 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=800&q=80';

    const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));

    const formatPrice = n => `${Number(n || 0).toFixed(2).replace('.', ',')} €`;

    function getDishPhoto(dish) {
        if (dish?.imageUrl && (/^https?:\/\//i.test(dish.imageUrl) || dish.imageUrl.startsWith('/'))) {
            return dish.imageUrl;
        }
        const name = String(dish?.nom || '').toLowerCase();
        const found = DISH_PHOTOS.find(p => name.includes(p.match));
        return found ? found.url : FALLBACK_DISH_PHOTO;
    }

    // ---------- Auth ----------
    function getToken() {
        return localStorage.getItem(STORAGE.TOKEN);
    }

    function getUser() {
        try {
            return JSON.parse(localStorage.getItem(STORAGE.USER) || 'null');
        } catch {
            return null;
        }
    }

    function setSession(token, user) {
        localStorage.setItem(STORAGE.TOKEN, token);
        localStorage.setItem(STORAGE.USER, JSON.stringify(user));
        updateHeaderAuth();
    }

    function clearSession() {
        localStorage.removeItem(STORAGE.TOKEN);
        localStorage.removeItem(STORAGE.USER);
        updateHeaderAuth();
    }

    // ---------- API ----------
    async function api(path, options = {}) {
        const headers = { 'Content-Type': 'application/json', ...options.headers };
        const token = getToken();
        if (token) headers.Authorization = `Bearer ${token}`;

        const res = await fetch(path, { ...options, headers });
        const body = res.status === 204 ? null : await res.json().catch(() => null);
        if (!res.ok) {
            const err = new Error(body?.message || `Erreur HTTP ${res.status}`);
            err.status = res.status;
            throw err;
        }
        return body;
    }

    // ---------- Panier partagé (localStorage) ----------
    function loadCart() {
        try {
            const parsed = JSON.parse(localStorage.getItem(STORAGE.CART) || '[]');
            return Array.isArray(parsed) ? parsed : [];
        } catch {
            return [];
        }
    }

    function saveCart(items) {
        localStorage.setItem(STORAGE.CART, JSON.stringify(items));
        renderCartDrawer();
        window.dispatchEvent(new CustomEvent('ejr:cart-changed', { detail: items }));
    }

    function addToCart(dish) {
        if (!dish || !dish.disponible) return;
        const items = loadCart();
        const photo = getDishPhoto(dish);
        const existing = items.find(i => i.id === dish.id);
        if (existing) {
            existing.quantity += 1;
            if (!existing.photo) existing.photo = photo;
        } else {
            items.push({
                id: dish.id,
                nom: dish.nom,
                prix: Number(dish.prix),
                photo,
                quantity: 1
            });
        }
        saveCart(items);
        openCart();
    }

    function updateCartQty(dishId, delta) {
        const items = loadCart();
        const idx = items.findIndex(i => i.id === dishId);
        if (idx === -1) return;
        items[idx].quantity += delta;
        if (items[idx].quantity <= 0) items.splice(idx, 1);
        saveCart(items);
    }

    function clearCart() {
        saveCart([]);
    }

    // ---------- Préférences de livraison partagées ----------
    function getDeliveryPrefs() {
        try {
            const saved = JSON.parse(localStorage.getItem(STORAGE.DELIVERY) || 'null');
            return saved || {
                fulfillmentType: 'RETRAIT',
                address: '15 Rue de Rivoli, Paris',
                lat: 48.8556,
                lng: 2.3580,
                instructions: ''
            };
        } catch {
            return { fulfillmentType: 'RETRAIT', address: '15 Rue de Rivoli, Paris', lat: 48.8556, lng: 2.3580, instructions: '' };
        }
    }

    function saveDeliveryPrefs(prefs) {
        localStorage.setItem(STORAGE.DELIVERY, JSON.stringify({ ...getDeliveryPrefs(), ...prefs }));
    }

    // ---------- Toast ----------
    let toastTimer = null;
    function toast(message) {
        let el = document.getElementById('ejr-toast');
        if (!el) {
            el = document.createElement('div');
            el.id = 'ejr-toast';
            el.className = 'toast';
            document.body.appendChild(el);
        }
        el.textContent = message;
        el.classList.add('visible');
        clearTimeout(toastTimer);
        toastTimer = setTimeout(() => el.classList.remove('visible'), 2800);
    }

    // ---------- Layout commun (Header, Footer, Tiroir Panier) ----------
    function initLayout(activePage) {
        const headerTarget = document.getElementById('site-header');
        if (headerTarget) {
            headerTarget.className = 'site-header';
            headerTarget.innerHTML = `
                <div class="header-inner">
                    <a href="index.html" class="brand">ejr kitchen</a>
                    <nav class="nav-links" aria-label="Navigation principale">
                        <a href="commander.html" class="${activePage === 'commander' ? 'active' : ''}">La Carte</a>
                        <a href="reserver.html" class="${activePage === 'reserver' ? 'active' : ''}">Réserver</a>
                        <a href="suivi.html" class="${activePage === 'suivi' ? 'active' : ''}">Suivi</a>
                    </nav>
                    <div class="header-actions">
                        <span id="nav-ws-dot" class="ws-dot" title="Statut temps réel"></span>
                        <a id="nav-account-link" href="profil.html" class="account-link">Connexion</a>
                        <button type="button" id="open-cart-btn" class="cart-trigger" aria-label="Ouvrir le panier">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z"></path>
                                <path d="M3 6h18"></path>
                                <path d="M16 10a4 4 0 0 1-8 0"></path>
                            </svg>
                            <span id="nav-cart-count" class="cart-badge">0</span>
                        </button>
                    </div>
                </div>
            `;
        }

        const footerTarget = document.getElementById('site-footer');
        if (footerTarget) {
            footerTarget.className = 'site-footer';
            footerTarget.innerHTML = `
                <div class="container">
                    <div class="footer-grid">
                        <div>
                            <div class="brand" style="margin-bottom:0.6rem;">ejr kitchen</div>
                            <p style="margin:0; max-width:36ch;">Cuisine fraîche de saison à Paris Centre, commande en direct et suivi cuisine temps réel.</p>
                            <p style="margin:0.8rem 0 0;">${esc(RESTAURANT.address)} · Lun–Dim 11h30–22h30</p>
                        </div>
                        <div>
                            <h4>Découvrir</h4>
                            <ul class="footer-links">
                                <li><a href="commander.html">La Carte & Commande</a></li>
                                <li><a href="reserver.html">Réserver une table</a></li>
                                <li><a href="suivi.html">Suivi de commande</a></li>
                                <li><a href="profil.html">Mon compte</a></li>
                            </ul>
                        </div>
                        <div>
                            <h4>Espace équipe</h4>
                            <ul class="footer-links">
                                <li><a href="/kds/">Écran Cuisine (KDS)</a></li>
                                <li><a href="/swagger-ui.html">Documentation API</a></li>
                            </ul>
                        </div>
                    </div>
                    <div class="footer-bottom">
                        <span>© 2026 EJR-ManageKitchen — Domaine Customer</span>
                        <span>Zone de livraison : 15 km autour de Paris Centre</span>
                    </div>
                </div>
            `;
        }

        mountCartDrawer(activePage);
        updateHeaderAuth();
        renderCartDrawer();
    }

    function updateHeaderAuth() {
        const link = document.getElementById('nav-account-link');
        if (!link) return;
        const user = getUser();
        link.textContent = (user && getToken()) ? user.fullName : 'Connexion';
    }

    function mountCartDrawer(activePage) {
        if (document.getElementById('ejr-cart-drawer')) return;
        const wrapper = document.createElement('div');
        wrapper.innerHTML = `
            <div id="ejr-drawer-backdrop" class="drawer-backdrop"></div>
            <aside id="ejr-cart-drawer" class="cart-drawer" aria-label="Votre panier">
                <div class="drawer-header">
                    <h2>Votre panier</h2>
                    <button type="button" id="close-cart-btn" class="icon-close" aria-label="Fermer">&times;</button>
                </div>
                <div id="drawer-items" class="drawer-body"></div>
                <div class="drawer-footer">
                    <div class="drawer-total">
                        <span>Total</span>
                        <strong id="drawer-total-price">0,00 €</strong>
                    </div>
                    <button type="button" id="drawer-checkout-btn" class="btn btn-primary" disabled>
                        ${activePage === 'commander' ? 'Valider la commande' : 'Finaliser sur La Carte'}
                    </button>
                </div>
            </aside>
        `;
        document.body.appendChild(wrapper);

        document.getElementById('open-cart-btn')?.addEventListener('click', openCart);
        document.getElementById('close-cart-btn')?.addEventListener('click', closeCart);
        document.getElementById('ejr-drawer-backdrop')?.addEventListener('click', closeCart);

        document.getElementById('drawer-items')?.addEventListener('click', e => {
            const btn = e.target.closest('button[data-qty-delta]');
            if (!btn) return;
            updateCartQty(Number(btn.dataset.dishId), Number(btn.dataset.qtyDelta));
        });

        document.getElementById('drawer-checkout-btn')?.addEventListener('click', () => {
            if (activePage === 'commander') {
                window.dispatchEvent(new CustomEvent('ejr:checkout-requested'));
            } else {
                location.href = 'commander.html?checkout=1';
            }
        });
    }

    function openCart() {
        document.getElementById('ejr-drawer-backdrop')?.classList.add('open');
        document.getElementById('ejr-cart-drawer')?.classList.add('open');
    }

    function closeCart() {
        document.getElementById('ejr-drawer-backdrop')?.classList.remove('open');
        document.getElementById('ejr-cart-drawer')?.classList.remove('open');
    }

    function renderCartDrawer() {
        const items = loadCart();
        const count = items.reduce((s, i) => s + i.quantity, 0);
        const total = items.reduce((s, i) => s + i.prix * i.quantity, 0);

        const badge = document.getElementById('nav-cart-count');
        if (badge) badge.textContent = String(count);

        const totalEl = document.getElementById('drawer-total-price');
        if (totalEl) totalEl.textContent = formatPrice(total);

        const checkoutBtn = document.getElementById('drawer-checkout-btn');
        if (checkoutBtn) checkoutBtn.disabled = items.length === 0;

        const body = document.getElementById('drawer-items');
        if (!body) return;

        if (items.length === 0) {
            body.innerHTML = '<p style="color:var(--muted);">Votre panier est vide. Parcourez notre carte pour ajouter des plats.</p>';
            return;
        }

        body.innerHTML = items.map(item => {
            const photoUrl = item.photo || getDishPhoto(item);
            return `
                <div class="drawer-item">
                    <div class="drawer-item-left">
                        <img class="drawer-item-thumb" src="${esc(photoUrl)}" alt="${esc(item.nom)}" loading="lazy">
                        <div class="drawer-item-info">
                            <strong>${esc(item.nom)}</strong>
                            <span>${formatPrice(item.prix)}</span>
                        </div>
                    </div>
                    <div class="qty-stepper">
                        <button type="button" data-dish-id="${Number(item.id)}" data-qty-delta="-1" aria-label="Retirer">−</button>
                        <span>${Number(item.quantity)}</span>
                        <button type="button" data-dish-id="${Number(item.id)}" data-qty-delta="1" aria-label="Ajouter">+</button>
                    </div>
                </div>
            `;
        }).join('');
    }

    window.EJR = {
        RESTAURANT,
        esc,
        formatPrice,
        getDishPhoto,
        api,
        getToken,
        getUser,
        setSession,
        clearSession,
        loadCart,
        addToCart,
        updateCartQty,
        clearCart,
        getDeliveryPrefs,
        saveDeliveryPrefs,
        toast,
        initLayout,
        openCart,
        closeCart
    };
})();