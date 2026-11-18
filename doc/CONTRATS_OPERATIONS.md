# Contrats du domaine `operations` (Dev 2 — Erick)

Ce document décrit ce que le domaine **Commandes & Opérations** fournit aux deux autres domaines et ce qu'il attend d'eux. Toute modification d'un contrat est annoncée à l'équipe **avant** d'être fusionnée.

---

## 1. Authentification (pour tout le monde)

| Méthode | Route | Accès | Rôle |
|---|---|---|---|
| POST | `/api/auth/register` | public | Inscription d'un **client** |
| POST | `/api/auth/login` | public | Renvoie `{ accessToken, tokenType, expiresIn, user }` |
| GET | `/api/users/me` | connecté | Profil de l'utilisateur connecté |
| GET/POST/PUT/DELETE | `/api/users[/{id}]` | ADMIN | Gestion des comptes (création des cuisiniers…) |

Le front envoie l'en-tête `Authorization: Bearer <accessToken>` sur chaque requête.

**Côté Java, pour connaître l'utilisateur connecté :**
```java
// Dans un contrôleur
public Foo bar(@AuthenticationPrincipal AuthenticatedUser user) { user.id(); user.role(); }

// Dans un service
AuthenticatedUser user = CurrentUser.get();
```
Les rôles (`Role.CLIENT`, `Role.CUISINIER`, `Role.ADMIN`) se trouvent dans `com.ejr.managekitchen.common.security`. Pour protéger une route : `@PreAuthorize("hasRole('ADMIN')")`.

> **Joël** : le profil client (allergies, régime, préférences) appartient au domaine `customer`. Il se rattache au compte par `userId` = `AuthenticatedUser.id()`.

---

## 2. Ce que Rayane (supply chain) doit fournir

Les commandes ont besoin de deux interfaces, définies dans `com.ejr.managekitchen.operations.order.port` :

```java
public interface MenuCatalogPort {
    Optional<DishSnapshot> findDish(Long dishId);   // id, nom, prix, disponible ?
}

public interface StockPort {
    void consumeIngredientsFor(List<OrderLine> lines); // OrderLine = (dishId, quantity)
}
```

- **Implémentation** : une classe `@Component` dans `com.ejr.managekitchen.supplychain` (par ex. `supplychain/stock/`) qui implémente ces deux interfaces (une seule classe ou deux, au choix).
- **Stock insuffisant** : lever `InsufficientStockException("Stock insuffisant : tomates")`. La commande est alors annulée, rien n'est enregistré et le client reçoit une erreur 409.
- **Transaction** : `consumeIngredientsFor` est appelée **dans la transaction de la commande**. Il ne faut pas ouvrir de transaction `REQUIRES_NEW`, sinon le stock serait décrémenté même si la commande échoue.
- **Bascule** : une fois les implémentations prêtes, passer `app.stubs.supplychain: false` dans `application.yml`. Le simulateur `StubSupplyChainAdapter` est alors désactivé.

**Lecture disponible pour F4 Finances :**
```java
@Autowired OrderQueryService orders;
BigDecimal ca = orders.revenueBetween(debut, fin); // commandes LIVREE ou RECUPEREE
```

---

## 3. Ce que Joël (espace client) peut utiliser

### API REST
| Méthode | Route | Accès | Rôle |
|---|---|---|---|
| POST | `/api/orders` | CLIENT | Passer commande (voir corps ci-dessous) |
| GET | `/api/orders/me` | connecté | Mes commandes |
| GET | `/api/orders/{id}` | connecté | Détail (un client ne voit que les siennes) |
| GET | `/api/notifications/me` | connecté | Mes notifications |
| PATCH | `/api/notifications/{id}/read` | connecté | Marquer comme lue |

```json
POST /api/orders
{ "fulfillmentType": "RETRAIT", "items": [ { "dishId": 1, "quantity": 2 } ] }
```
`fulfillmentType` vaut `LIVRAISON` ou `RETRAIT`.

### WebSocket (STOMP)
- Connexion : `ws://localhost:8080/ws`, avec l'en-tête STOMP `Authorization: Bearer <jwt>` au CONNECT (exemple dans `static/kds/index.html`).
- `/user/queue/orders` : chaque création ou changement de statut des commandes du client connecté. Format : `{ type, previousStatus, order }`, où `type` vaut `ORDER_CREATED` ou `ORDER_STATUS_CHANGED`.
- `/user/queue/notifications` : notification « commande prête ».

### Événement Java (suivi de livraison)
```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onOrderChanged(OrderStatusChangedEvent event) {
    if (event.order().status() == OrderStatus.PRETE
        && event.order().fulfillmentType() == FulfillmentType.LIVRAISON) {
        // démarrer le suivi cartographique
    }
}
```

### Mode dégradé de l'IA (« Nos suggestions du chef »)
```java
List<DishSalesView> top = orderQueryService.bestSellers(5); // dishId, dishName, totalQuantity
```

---

## 4. Machine à états

```
NOUVELLE ──► EN_PREPARATION ──► PRETE ──► LIVREE     (fulfillmentType = LIVRAISON)
                                      └─► RECUPEREE  (fulfillmentType = RETRAIT)
```
- Seuls CUISINIER et ADMIN changent les statuts (`PATCH /api/orders/{id}/status` avec `{ "status": "PRETE" }`).
- Une transition interdite renvoie **409**, tout comme deux modifications simultanées de la même commande.
- Le passage à `PRETE` notifie le client par WebSocket et par e-mail. Pour l'instant, l'e-mail est simulé dans les logs.

## 5. Hypothèses à valider en équipe

1. Le statut `RECUPEREE` a été ajouté pour le retrait sur place, que le scénario du dossier de lancement mentionne.
2. Faute de rôle livreur, c'est le cuisinier (ou l'admin) qui passe une commande en `LIVREE` ou `RECUPEREE`.
3. Le chiffre d'affaires ne compte que les commandes remises au client (`LIVREE` ou `RECUPEREE`).
4. Le service d'e-mail réel reste à choisir : il faudra remplacer `LoggingEmailSender`.
