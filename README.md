# EJR-ManageKitchen — Plateforme SaaS de Restauration Connectée

[![CI](https://github.com/ErickJanaPro/EJR-ManageKitchen/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/ErickJanaPro/EJR-ManageKitchen/actions/workflows/ci.yml)
[![Version](https://img.shields.io/github/v/tag/ErickJanaPro/EJR-ManageKitchen?label=version&sort=semver)](https://github.com/ErickJanaPro/EJR-ManageKitchen/tags)
[![Licence](https://img.shields.io/github/license/ErickJanaPro/EJR-ManageKitchen?label=licence)](LICENSE)
[![Couverture](https://raw.githubusercontent.com/ErickJanaPro/EJR-ManageKitchen/badges/jacoco.svg)](https://github.com/ErickJanaPro/EJR-ManageKitchen/actions/workflows/ci.yml)
[![Branches](https://raw.githubusercontent.com/ErickJanaPro/EJR-ManageKitchen/badges/branches.svg)](https://github.com/ErickJanaPro/EJR-ManageKitchen/actions/workflows/ci.yml)
[![CodeQL](https://github.com/ErickJanaPro/EJR-ManageKitchen/actions/workflows/codeql.yml/badge.svg?branch=develop)](https://github.com/ErickJanaPro/EJR-ManageKitchen/actions/workflows/codeql.yml)
![Java](https://img.shields.io/badge/Java-17%2B-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F)

Projet de Master 1 MIAGE. Une plateforme qui relie l'**Espace Client (B2C)**, c'est-à-dire le menu, la commande, l'assistant IA et le suivi de livraison, à l'**Espace Entreprise (B2B)** : fournisseurs, stock, recettes, finances et écran de cuisine (KDS).

> Badges : **CI** = compilation + tests à chaque push et pull request ; **Version** = dernier tag Git ; **Couverture / Branches** = mesurées par JaCoCo dans la CI (publiées sur la branche `badges`) ; **CodeQL** = analyse statique de qualité et de sécurité du code.

---

## Équipe

| Domaine (package) | Développeur | Fonctionnalités |
|---|---|---|
| `com.ejr.managekitchen.supplychain` | MEKSEM Rayane | F1 Fournisseurs · F2 Stock · F3 Recettes · F4 Finances |
| `com.ejr.managekitchen.operations` | JANA GUANA Erick | F5 Utilisateurs & rôles · F6 Commandes & statuts · F7 Notifications |
| `com.ejr.managekitchen.customer` | KPINSO-FOLLY Joël | F8 Carte · F9 Recommandations · F10 Assistant IA |

## Stack

Java 17+ · Spring Boot 3.5 · Spring Security + JWT · Spring Data JPA · PostgreSQL · Flyway · WebSocket (STOMP) · OpenAPI/Swagger · JaCoCo · Gradle (Kotlin DSL) · GitHub Actions · CodeQL

---

## Installation

### Prérequis
- **JDK 17 ou plus récent** (`java -version`)
- **Docker** (pour la base PostgreSQL)
- Pas besoin d'installer Gradle : le wrapper `./gradlew` est fourni.

### 1. Variables d'environnement (facultatif)
```bash
cp .env.example .env   # puis modifier les valeurs si besoin
```
Toutes les variables ont une valeur par défaut qui fonctionne en local.

| Variable | Rôle | Défaut |
|---|---|---|
| `DB_URL` | URL JDBC | `jdbc:postgresql://localhost:5432/ejr_kitchen` |
| `DB_USER` / `DB_PASSWORD` | Identifiants de la base | `ejr` / `ejr` |
| `JWT_SECRET` | Clé de signature des jetons (32 caractères min.) | clé de développement |
| `CORS_ORIGINS` | Origines autorisées pour le front | `http://localhost:5173,http://localhost:8080` |

### 2. Base de données
```bash
docker compose up -d
```
Les tables sont créées automatiquement au démarrage par **Flyway** (`app/src/main/resources/db/migration`).

### 3. Lancement
```bash
./gradlew :app:bootRun
```

| URL | Contenu |
|---|---|
| http://localhost:8080 | Espace client (menu) |
| http://localhost:8080/swagger-ui.html | Documentation de l'API (bouton **Authorize** pour coller un JWT) |
| http://localhost:8080/kds/ | Écran de cuisine (KDS) temps réel |

### Comptes de test (profil `dev`, créés au premier démarrage)

| Rôle | E-mail | Mot de passe |
|---|---|---|
| Admin | `admin@ejr.local` | `password123` |
| Cuisinier | `cuisinier@ejr.local` | `password123` |
| Client | `client@ejr.local` | `password123` |

### E-mails de notification (Mailpit)

Par défaut (`EMAIL_MODE=log`), les e-mails sont seulement écrits dans les logs. Pour un envoi réel en local :

```bash
docker compose up -d                       # démarre aussi Mailpit
EMAIL_MODE=smtp ./gradlew :app:bootRun
```

Les e-mails envoyés (par exemple « Votre commande est prête ») s'affichent sur http://localhost:8025. Rien ne part réellement sur Internet.

### 4. Tests et couverture (en local)
```bash
./gradlew test
```
Les tests tournent sur une base H2 en mémoire, Docker n'est pas nécessaire. Le rapport de couverture JaCoCo se trouve dans `app/build/reports/jacoco/test/html/index.html`.

### 5. Intégration continue

À chaque push sur `main` / `develop` et à chaque pull request, GitHub Actions ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) :
1. compile le projet et lance tous les tests (`./gradlew build`) ;
2. mesure la couverture avec JaCoCo et publie les rapports dans l'onglet **Actions** ;
3. compile le dossier du projet (`doc/DOSSIER.md` → PDF).

[`codeql.yml`](.github/workflows/codeql.yml) analyse la qualité et la sécurité du code (Java et JavaScript).

## Versions

| Version | Contenu |
|---|---|
| **v0.2.0** | F7 e-mails réels (Erick) · F2 stock (Rayane) · parcours client B2C (Joël) · CI mise à jour (Node.js 24, CodeQL v4) |
| **v0.1.0** | Socle commun (Gradle, sécurité JWT, CI) · F5 utilisateurs & rôles · F6 commandes + écran cuisine temps réel · F7 notifications · F1 fournisseurs · menu client |

Détail et évolutions : [`doc/DOSSIER.md`](doc/DOSSIER.md).

---

## Organisation du code

```
app/src/main/java/com/ejr/managekitchen/
├── ManageKitchenApplication.java   # unique classe de démarrage
├── common/                # Socle partagé (sécurité JWT, erreurs, OpenAPI) — revue à 3 obligatoire
├── supplychain/           # Dev 1 — Rayane
│   ├── supplier/          # F1 Fournisseurs
│   ├── stock/             # F2 Stock & ingrédients
│   ├── recipe/            # F3 Recettes & menus
│   ├── finance/           # F4 Finances
│   └── shared/            # exceptions du domaine
├── operations/            # Dev 2 — Erick
│   ├── user/              # F5 Utilisateurs & rôles (+ auth/ : inscription, connexion JWT)
│   ├── order/             # F6 Commandes & statuts (+ kitchen/ : API de l'écran cuisine)
│   ├── notification/      # F7 Notifications
│   └── realtime/          # WebSocket STOMP (F6, F7)
└── customer/              # Dev 3 — Joël
    ├── menu/              # consultation du menu
    ├── map/               # F8 Géolocalisation & carte
    ├── recommendation/    # F9 Recommandations
    └── assistant/         # F10 Chat & assistant IA
```
Les dossiers marqués d'une fonctionnalité non encore développée sont créés par leur propriétaire au démarrage de la fonctionnalité.

Les points de contact entre les domaines sont décrits dans [`doc/CONTRATS_OPERATIONS.md`](doc/CONTRATS_OPERATIONS.md).

## Conventions d'équipe

- **Branches** : `main` reçoit uniquement les versions rendues ; `develop` est la branche d'intégration. Une branche par fonctionnalité, `feature/F<n>-<sujet>` (ex. `feature/F2-stock`), fusionnée dans `develop` par pull request relue par au moins une autre personne. Détails dans [`CONTRIBUTING.md`](CONTRIBUTING.md).
- **Migrations Flyway** : on nomme les fichiers par date pour éviter que deux personnes prennent le même numéro, au format `V<AAAA>_<MM>_<JJ>_<NN>__<description>.sql` (ex. `V2026_11_20_01__create_suppliers.sql`). **Une migration déjà fusionnée ne se modifie jamais** : on en crée une nouvelle.
- **Pas de clé étrangère entre domaines** : un domaine référence l'autre par son identifiant (ex. `order_items.dish_id`) et passe par son API ou ses interfaces.
- **Package `common`** : toute modification passe par une revue des trois membres.

## Documentation

Le dossier [`doc/`](doc/) contient le dossier de lancement (LaTeX et PDF) et les contrats entre domaines. Il est versionné en même temps que le code.

## Licence

[MIT](LICENSE)
