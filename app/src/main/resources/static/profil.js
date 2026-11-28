document.addEventListener('DOMContentLoaded', async () => {
    EJR.initLayout('profil');

    const $ = id => document.getElementById(id);
    let registerMode = false;

    const ALLOWED_REDIRECT = /^(index|commander|suivi|reserver|profil)\.html(\?[a-zA-Z0-9_=&%-]*)?$/;

    function getSafeRedirect() {
        const raw = new URLSearchParams(location.search).get('redirect');
        if (!raw) return null;
        return ALLOWED_REDIRECT.test(raw) ? raw : null;
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

    function renderAccount() {
        const user = EJR.getUser();
        const isLoggedIn = Boolean(EJR.getToken() && user);

        $('auth-view').classList.toggle('hidden', isLoggedIn);
        $('account-view').classList.toggle('hidden', !isLoggedIn);

        if (!isLoggedIn) return;

        const name = user.fullName || user.email || 'Client';
        $('profile-avatar').textContent = name.trim().charAt(0).toUpperCase();
        $('profile-name').textContent = name;
        $('profile-email').textContent = user.email || '';

        const prefs = EJR.getDeliveryPrefs();
        $('profile-mode-pill').textContent = prefs.fulfillmentType === 'LIVRAISON' ? 'Livraison' : 'À emporter';
        $('saved-address').value = prefs.address || '';
        $('saved-instructions').value = prefs.instructions || '';
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