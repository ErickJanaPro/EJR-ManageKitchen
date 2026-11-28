document.addEventListener('DOMContentLoaded', () => {
    EJR.initLayout('reserver');

    const $ = id => document.getElementById(id);

    const MIN_GUESTS = 1;
    const MAX_GUESTS = 12;

    const state = {
        date: todayIso(),
        guests: 2,
        slots: [],
        selectedSlot: null,
        loadingSlots: false,
        submitting: false
    };

    function todayIso() {
        const now = new Date();
        const y = now.getFullYear();
        const m = String(now.getMonth() + 1).padStart(2, '0');
        const d = String(now.getDate()).padStart(2, '0');
        return `${y}-${m}-${d}`;
    }

    function formatDateFr(iso) {
        const [y, m, d] = iso.split('-').map(Number);
        if (!y || !m || !d) return iso;
        return new Date(y, m - 1, d).toLocaleDateString('fr-FR', {
            weekday: 'long',
            day: 'numeric',
            month: 'long'
        });
    }

    function isSlotSelectable(slot) {
        return Boolean(slot && slot.available && slot.remainingSeats >= state.guests);
    }

    // ---------- Carte Leaflet ----------
    function initRestaurantMap() {
        if (typeof L === 'undefined') return;
        const map = L.map('restaurant-map').setView([EJR.RESTAURANT.lat, EJR.RESTAURANT.lng], 15);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; OpenStreetMap'
        }).addTo(map);

        L.marker([EJR.RESTAURANT.lat, EJR.RESTAURANT.lng])
            .addTo(map)
            .bindPopup(`<strong>${EJR.esc(EJR.RESTAURANT.name)}</strong><br>${EJR.esc(EJR.RESTAURANT.address)}`)
            .openPopup();
    }

    // ---------- Disponibilités & rendu des créneaux ----------
    function renderGuests() {
        $('guests-label').textContent = state.guests > 1
            ? `${state.guests} personnes`
            : '1 personne';
        $('guests-minus').disabled = state.guests <= MIN_GUESTS;
        $('guests-plus').disabled = state.guests >= MAX_GUESTS;
    }

    function syncSelectedSlot() {
        const current = state.slots.find(s => s.timeSlot === state.selectedSlot);
        if (!isSlotSelectable(current)) {
            const firstValid = state.slots.find(isSlotSelectable);
            state.selectedSlot = firstValid ? firstValid.timeSlot : null;
        }
        $('res-submit-btn').disabled = state.loadingSlots || state.submitting || !state.selectedSlot;
    }

    function slotSubLabel(slot) {
        if (!slot.available) {
            return slot.remainingSeats === 0 ? 'Complet' : 'Passé';
        }
        if (slot.remainingSeats < state.guests) {
            return `${slot.remainingSeats} pl. (trop juste)`;
        }
        return `${slot.remainingSeats} place${slot.remainingSeats > 1 ? 's' : ''}`;
    }

    function renderSlotGroup(containerId, service) {
        const list = state.slots.filter(s => s.service === service);
        if (list.length === 0) {
            $(containerId).innerHTML = '<span style="color:var(--muted); font-size:0.86rem;">Aucun créneau.</span>';
            return;
        }

        $(containerId).innerHTML = list.map(slot => {
            const selectable = isSlotSelectable(slot);
            const active = selectable && state.selectedSlot === slot.timeSlot;
            return `
                <button type="button"
                        class="slot-btn ${active ? 'active' : ''}"
                        data-slot="${EJR.esc(slot.timeSlot)}"
                        aria-pressed="${String(active)}"
                        ${selectable ? '' : 'disabled'}>
                    <strong>${EJR.esc(slot.timeSlot)}</strong>
                    <span>${EJR.esc(slotSubLabel(slot))}</span>
                </button>
            `;
        }).join('');
    }

    function renderAllSlots() {
        syncSelectedSlot();
        renderSlotGroup('slots-lunch', 'DEJEUNER');
        renderSlotGroup('slots-dinner', 'DINER');
    }

    async function loadAvailability() {
        state.loadingSlots = true;
        $('res-submit-btn').disabled = true;
        $('slots-lunch').innerHTML = '<span style="color:var(--muted); font-size:0.86rem;">Chargement...</span>';
        $('slots-dinner').innerHTML = '<span style="color:var(--muted); font-size:0.86rem;">Chargement...</span>';

        try {
            state.slots = await EJR.api(`/api/customer/reservations/availability?date=${encodeURIComponent(state.date)}`);
        } catch (err) {
            state.slots = [];
            showFeedback(`Impossible de charger les disponibilités : ${err.message}`, 'error');
        } finally {
            state.loadingSlots = false;
            renderAllSlots();
        }
    }

    function showFeedback(htmlOrText, kind = 'success', isHtml = false) {
        const el = $('res-feedback');
        el.className = `reserve-feedback ${kind}`;
        if (isHtml) el.innerHTML = htmlOrText;
        else el.textContent = htmlOrText;
        el.classList.remove('hidden');
    }

    // ---------- Événements ----------
    const dateInput = $('res-date');
    dateInput.min = state.date;
    dateInput.value = state.date;

    dateInput.addEventListener('change', () => {
        state.date = dateInput.value || todayIso();
        $('res-feedback').classList.add('hidden');
        loadAvailability();
    });

    $('guests-minus').addEventListener('click', () => {
        if (state.guests > MIN_GUESTS) {
            state.guests -= 1;
            renderGuests();
            renderAllSlots();
        }
    });

    $('guests-plus').addEventListener('click', () => {
        if (state.guests < MAX_GUESTS) {
            state.guests += 1;
            renderGuests();
            renderAllSlots();
        }
    });

    document.addEventListener('click', e => {
        const slotBtn = e.target.closest('button[data-slot]');
        if (!slotBtn || slotBtn.disabled) return;
        state.selectedSlot = slotBtn.dataset.slot;
        renderAllSlots();
    });

    $('reservation-form').addEventListener('submit', async e => {
        e.preventDefault();
        if (state.submitting) return;

        if (!EJR.getToken()) {
            EJR.toast('Connectez-vous pour confirmer votre réservation');
            setTimeout(() => {
                location.href = 'profil.html?redirect=reserver.html';
            }, 650);
            return;
        }

        if (!state.selectedSlot) {
            showFeedback('Aucun créneau disponible pour ce nombre de couverts à cette date.', 'error');
            return;
        }

        state.submitting = true;
        const btn = $('res-submit-btn');
        btn.disabled = true;
        btn.textContent = 'Confirmation en cours...';

        const user = EJR.getUser();
        const payload = {
            reservationDate: state.date,
            timeSlot: state.selectedSlot,
            guestsCount: state.guests,
            clientName: user?.fullName || null,
            notes: $('res-notes').value.trim() || null
        };

        try {
            const created = await EJR.api('/api/customer/reservations', {
                method: 'POST',
                body: JSON.stringify(payload)
            });

            const guestText = created.guestsCount > 1
                ? `${created.guestsCount} couverts`
                : '1 couvert';

            showFeedback(`
                <strong>Réservation #${Number(created.id)} confirmée !</strong><br>
                ${EJR.esc(formatDateFr(created.reservationDate))} à <strong>${EJR.esc(created.timeSlot)}</strong> (${EJR.esc(guestText)}).
                <div style="margin-top:0.65rem;">
                    <a href="profil.html" class="editorial-link">Gérer mes réservations →</a>
                </div>
            `, 'success', true);

            $('res-notes').value = '';
            EJR.toast('Table réservée !');
            await loadAvailability();
        } catch (err) {
            if (err.status === 401) {
                EJR.clearSession();
                EJR.toast('Session expirée — reconnectez-vous');
                location.href = 'profil.html?redirect=reserver.html';
                return;
            }
            showFeedback(err.message || 'Impossible de finaliser la réservation.', 'error');
            await loadAvailability();
        } finally {
            state.submitting = false;
            btn.textContent = 'Confirmer ma réservation';
            syncSelectedSlot();
        }
    });

    // ---------- Initialisation ----------
    initRestaurantMap();
    renderGuests();
    loadAvailability();
});