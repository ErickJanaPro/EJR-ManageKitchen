document.addEventListener('DOMContentLoaded', async () => {
    EJR.initLayout('home');

    const grid = document.getElementById('home-menu-grid');
    let dishes = [];

    try {
        dishes = await EJR.api('/api/customer/menu');
        grid.innerHTML = dishes.map(plat => `
            <article class="dish-card ${plat.disponible ? '' : 'unavailable'}">
                <img class="dish-photo" src="${EJR.esc(EJR.getDishPhoto(plat))}" alt="${EJR.esc(plat.nom)}" loading="lazy">
                <div class="dish-body">
                    <div>
                        <h3>${EJR.esc(plat.nom)}</h3>
                        <p>${EJR.esc(plat.description)}</p>
                    </div>
                    <div class="dish-footer">
                        <span class="dish-price">${EJR.formatPrice(plat.prix)}</span>
                        <button type="button"
                                class="btn-add-mini"
                                data-add-dish="${Number(plat.id)}"
                                ${plat.disponible ? '' : 'disabled'}>
                            ${plat.disponible ? '+ Ajouter' : 'Indisponible'}
                        </button>
                    </div>
                </div>
            </article>
        `).join('');
    } catch (err) {
        grid.innerHTML = `<p style="color:var(--danger);">Impossible de charger le menu : ${EJR.esc(err.message)}</p>`;
    }

    grid.addEventListener('click', e => {
        const btn = e.target.closest('button[data-add-dish]');
        if (!btn) return;
        const dish = dishes.find(p => p.id === Number(btn.dataset.addDish));
        if (dish) EJR.addToCart(dish);
    });
});