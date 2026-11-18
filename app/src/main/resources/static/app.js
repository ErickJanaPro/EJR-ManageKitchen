document.addEventListener("DOMContentLoaded", () => {
    fetch('/api/customer/menu')
        .then(response => response.json())
        .then(plats => {
            const container = document.getElementById('menu-container');
            container.innerHTML = ''; // On vide le texte de chargement

            plats.forEach(plat => {
                const card = document.createElement('div');
                card.className = 'menu-card';
                card.innerHTML = `
                    <div style="font-size: 3rem;">${plat.imageUrl}</div>
                    <h3>${plat.nom}</h3>
                    <p>${plat.description}</p>
                    <p class="price">${plat.prix.toFixed(2)} €</p>
                    <button>Ajouter au panier</button>
                `;
                container.appendChild(card);
            });
        })
        .catch(error => {
            console.error('Erreur lors du chargement du menu:', error);
            document.getElementById('menu-container').innerHTML = '<p>Désolé, le menu est indisponible pour le moment.</p>';
        });
});