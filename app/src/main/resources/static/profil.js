document.addEventListener('DOMContentLoaded', async () => {
    EJR.initLayout('profil');

    const $ = id => document.getElementById(id);
    let registerMode = false;
    let reservations = [];

    const ALLOWED_REDIRECT = /^(index|commander|suivi|reserver|profil)\.html(\?[a-zA-Z0-9_=&%-]*)?$/;

    function getSafeRedirect() {
        const raw = new URLSearchParams(location.search).get('redirect');
        if (!raw) return null;
        return ALLOWED_REDIRECT.test(raw) ? raw : null;
    }

    function formatDateFr(iso) {
        const [y, m, d] = String(iso || '').split('-').map(Number);
        if (!y || !m || !d) return iso || '';
        return new Date(y, m - 1, d).toLocaleDateString('fr-FR', {
            weekday: 'short',
            day: 'numeric',
            month: 'short'
        });
    }

    function setAuthMode(isRegister) {
        registerMode = isRegister;
        $('auth-tab-login').classList.toggle('active', !isRegister);
        $('auth-tab-register').classList.toggle('active', isRegister);
        $('fullname-group').classList.toggle('hidden', !isRegister);
        $('auth-fullname').required = isRegister;

        $('auth-heading').textContent = isRegister ? 'Créer votre compte' : 'Bon retour parmi nous';
        $('auth-sub').textContent = isRegister
            ? 'Inscrivez-vous en quelques secondes pour commander et suivre vos plats en temps réel.'
            : 'Connectez-vous pour commander, suivre vos livraisons en direct et gérer vos réservations.';
        $('auth-submit-btn').textContent = isRegister ? 'Créer mon compte' : 'Se connecter';
        $('auth-password').autocomplete = isRegister ? 'new-password' : 'current-password';
        hideError();
    }

    function showError(msg) {
        const el = $('auth-error');
        el.textContent = msg;
        el.classList.remove('hidden');
    }

    function hideError() {
        const el = $('auth-error');
        el.textContent = '';
        el.classList.add('hidden');
    }

    // ---------- Mes réservations de table ----------
    function renderReservations() {
        const listEl = $('my-reservations-list');
        const countEl = $('reservations-count');
        const activeCount = reservations.filter(r => r.status === 'CONFIRMEE').length;
        if (countEl) countEl.textContent = String(activeCount);

        if (!listEl) return;
        if (reservations.length === 0) {
            listEl.innerHTML = '<p style="color:var(--muted); font-size:0.9rem; margin:0;">Aucune table réservée pour le moment.</p>';
            return;
        }

        listEl.innerHTML = reservations.map(r => {
            const isCancelled = r.status === 'ANNULEE';
            const guestLabel = r.guestsCount > 1 ? `${r.guestsCount} couverts` : '1 couvert';
            const badgeClass = isCancelled ? 'ANNULEE' : 'LIVREE';
            const badgeText = isCancelled ? 'Annulée' : 'Confirmée';

            return `
                <div class="reservation-row ${isCancelled ? 'cancelled' : ''}">
                    <div class="reservation-row-info">
                        <strong>${EJR.esc(formatDateFr(r.reservationDate))} · ${EJR.esc(r.timeSlot)}</strong>
                        <span class="reservation-row-meta">
                            ${EJR.esc(guestLabel)}${r.notes ? ` · ${EJR.esc(r.notes)}` : ''}
                        </span>
                    </div>
                    <div class="reservation-row-actions">
                        <span class="status-badge ${badgeClass}">${badgeText}</span>
                        ${!isCancelled ? `
                            <button type="button"
                                    class="btn btn-outline btn-sm"
                                    data-cancel-reservation="${Number(r.id)}">
                                Annuler
                            </button>
                        ` : ''}
                    </div>
                </div>
            `;
        }).join('');
    }

    async function loadMyReservations() {
        if (!EJR.getToken()) return;
        try {
            reservations = await EJR.api('/api/customer/reservations/me');
            renderReservations();
        } catch (err) {
            const listEl = $('my-reservations-list');
            if (listEl) {
                listEl.innerHTML = `<p style="color:var(--danger); font-size:0.88rem; margin:0;">${EJR.esc(err.message)}</p>`;
            }
        }
    }

    function renderAccount() {
        const user = EJR.getUser();
        const isLoggedIn = Boolean(EJR.getToken() && user);

        $('auth-view').classList.toggle('hidden', isLoggedIn);
        $('account-view').classList.toggle('hidden', !isLoggedIn);

        if (!isLoggedIn) {
            reservations = [];
            return;
        }

        const name = user.fullName || user.email || 'Client';
        $('profile-avatar').textContent = name.trim().charAt(0).toUpperCase();
        $('profile-name').textContent = name;
        $('profile-email').textContent = user.email || '';

        const prefs = EJR.getDeliveryPrefs();
        $('profile-mode-pill').textContent = prefs.fulfillmentType === 'LIVRAISON' ? 'Livraison' : 'À emporter';
        $('saved-address').value = prefs.address || '';
        $('saved-instructions').value = prefs.instructions || '';

        loadMyReservations();
    }

    // ---------- Événements ----------
    $('auth-tab-login').addEventListener('click', () => setAuthMode(false));
    $('auth-tab-register').addEventListener('click', () => setAuthMode(true));

    $('auth-form').addEventListener('submit', async e => {
        e.preventDefault();
        hideError();

        const btn = $('auth-submit-btn');
        btn.disabled = true;

        const endpoint = registerMode ? '/api/auth/register' : '/api/auth/login';
        const payload = registerMode
            ? {
                email: $('auth-email').value.trim(),
                password: $('auth-password').value,
                fullName: $('auth-fullname').value.trim()
            }
            : {
                email: $('auth-email').value.trim(),
                password: $('auth-password').value
            };

        try {
            const auth = await EJR.api(endpoint, {
                method: 'POST',
                body: JSON.stringify(payload)
            });
            EJR.setSession(auth.accessToken, auth.user);
            EJR.toast(registerMode ? 'Compte créé ! Bienvenue.' : `Connecté en tant que ${auth.user.fullName}`);

            const redirect = getSafeRedirect();
            if (redirect) {
                location.assign(redirect);
                return;
            }
            renderAccount();
        } catch (err) {
            showError(err.message || 'Identifiants invalides.');
        } finally {
            btn.disabled = false;
        }
    });

    $('logout-btn').addEventListener('click', () => {
        EJR.clearSession();
        EJR.toast('Vous êtes déconnecté');
        renderAccount();
    });

    $('saved-address-form').addEventListener('submit', e => {
        e.preventDefault();
        EJR.saveDeliveryPrefs({
            address: $('saved-address').value.trim() || EJR.getDeliveryPrefs().address,
            instructions: $('saved-instructions').value.trim()
        });
        EJR.toast('Adresse enregistrée');
    });

    $('my-reservations-list')?.addEventListener('click', async e => {
        const btn = e.target.closest('button[data-cancel-reservation]');
        if (!btn) return;
        const id = Number(btn.dataset.cancelReservation);
        btn.disabled = true;
        try {
            const updated = await EJR.api(`/api/customer/reservations/${id}/cancel`, { method: 'PATCH' });
            reservations = reservations.map(r => r.id === id ? updated : r);
            renderReservations();
            EJR.toast('Réservation annulée');
        } catch (err) {
            btn.disabled = false;
            EJR.toast(err.message || 'Erreur lors de l\'annulation');
        }
    });

    // ---------- Vérification de session au chargement ----------
    if (EJR.getToken()) {
        try {
            const me = await EJR.api('/api/users/me');
            if (me && me.email) {
                EJR.setSession(EJR.getToken(), { ...EJR.getUser(), ...me });
            }
        } catch (err) {
            if (err.status === 401) {
                EJR.clearSession();
            }
        }
    }

    renderAccount();
});