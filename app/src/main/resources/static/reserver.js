document.addEventListener('DOMContentLoaded', () => {
    EJR.initLayout('reserver');

    const $ = id => document.getElementById(id);

    const LUNCH_SLOTS = ['12:00', '12:45', '13:30'];
    const DINNER_SLOTS = ['19:30', '20:30', '21:30'];
    const MIN_GUESTS = 1;
    const MAX_GUESTS = 12;

    const state = {
        date: todayIso(),
        guests: 2,
        selectedSlot: '19:30'
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

    // ---------- Sélecteurs couverts & créneaux ----------
    function renderGuests() {
        $('guests-label').textContent = state.guests > 1
            ? `${state.guests} personnes`
            : '1 personne';
        $('guests-minus').disabled = state.guests <= MIN_GUESTS;
        $('guests-plus').disabled = state.guests >= MAX_GUESTS;
    }

    function renderSlotGroup(containerId, slots) {
        $(containerId).innerHTML = slots.map(slot => {
            const active = state.selectedSlot === slot;
            return `
                <button type="button"
                        class="slot-btn ${active ? 'active' : ''}"
                        data-slot="${EJR.esc(slot)}"
                        aria-pressed="${String(active)}">
                    <strong>${EJR.esc(slot)}</strong>
                    <span>Disponible</span>
                </button>
            `;
        }).join('');
    }

    function renderAllSlots() {
        renderSlotGroup('slots-lunch', LUNCH_SLOTS);
        renderSlotGroup('slots-dinner', DINNER_SLOTS);
    }

    // ---------- Initialisation formulaire ----------
    const dateInput = $('res-date');
    dateInput.min = state.date;
    dateInput.value = state.date;

    dateInput.addEventListener('change', () => {
        state.date = dateInput.value || todayIso();
        $('res-feedback').classList.add('hidden');
    });

    $('guests-minus').addEventListener('click', () => {
        if (state.guests > MIN_GUESTS) {
            state.guests -= 1;
            renderGuests();
        }
    });

    $('guests-plus').addEventListener('click', () => {
        if (state.guests < MAX_GUESTS) {
            state.guests += 1;
            renderGuests();
        }
    });

    document.addEventListener('click', e => {
        const slotBtn = e.target.closest('button[data-slot]');
        if (!slotBtn) return;
        state.selectedSlot = slotBtn.dataset.slot;
        renderAllSlots();
    });

    $('reservation-form').addEventListener('submit', e => {
        e.preventDefault();

        if (!EJR.getToken()) {
            EJR.toast('Connectez-vous pour réserver une table');
            setTimeout(() => {
                location.href = 'profil.html?redirect=reserver.html';
            }, 650);
            return;
        }

        const feedback = $('res-feedback');
        const guestText = state.guests > 1 ? `${state.guests} couverts` : '1 couvert';
        const user = EJR.getUser();
        const clientLabel = user?.fullName ? ` au nom de ${user.fullName}` : '';

        feedback.className = 'reserve-feedback success';
        feedback.innerHTML = `
            <strong>Pré-validation enregistrée${EJR.esc(clientLabel)}</strong><br>
            ${EJR.esc(formatDateFr(state.date))} à <strong>${EJR.esc(state.selectedSlot)}</strong> (${EJR.esc(guestText)}).
            <div style="margin-top:0.4rem; font-size:0.82rem; opacity:0.85;">
                L'enregistrement en base et le calcul des places restantes seront activés dès le ticket <em>Réservation de table</em> (<code>/api/customer/reservations</code>).
            </div>
        `;
        feedback.classList.remove('hidden');
    });

    initRestaurantMap();
    renderGuests();
    renderAllSlots();
});