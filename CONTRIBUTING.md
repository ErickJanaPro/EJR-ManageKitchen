# Guide de contribution - EJR-ManageKitchen (SaaS Restauration)

Ce document définit les standards techniques et le workflow que l'équipe (Dev 1, Dev 2, Dev 3) doit respecter pour garantir la cohérence de la plateforme.

## 1. Prérequis

* **Java 17** minimum (JDK)
* **Docker Desktop** (la base PostgreSQL est lancée avec `docker compose up -d`)
* **Git**
* IDE recommandé : **VS Code** (avec Extension Pack for Java) ou **IntelliJ IDEA**.

## 2. Setup initial

### Cloner et démarrer

```bash
git clone https://github.com/ErickJanaPro/EJR-ManageKitchen.git
cd EJR-ManageKitchen
```

**Aucune installation globale de Gradle n'est requise**, utilisez toujours le Wrapper inclus (`gradlew`) :

* **Windows :** `.\gradlew.bat build`
* **Mac/Linux :** `./gradlew build`

### Lancer l'application en local

```bash
docker compose up -d          # base PostgreSQL (ejr_kitchen / ejr / ejr)
./gradlew :app:bootRun        # Windows : .\gradlew.bat :app:bootRun
# -> http://localhost:8080 (front client), /kds/ (cuisine), /swagger-ui.html (API)
```

Comptes de test créés au premier démarrage : `admin@ejr.local`, `cuisinier@ejr.local`, `client@ejr.local` (mot de passe `password123`).

## 3. Règles non négociables (Stack & Architecture)

* **Outil de Build UNIQUE :** Le projet utilise **Gradle** (Kotlin DSL : `build.gradle.kts`). L'utilisation de Maven (`pom.xml`) est **strictement interdite** pour éviter les conflits. Si un fichier `pom.xml` est présent, il doit être supprimé.
* **Fichiers ignorés :** Ne **jamais** commiter les dossiers générés par le build ou l'IDE : `build/`, `target/`, `.gradle/`, `.vscode/`, `.idea/`.
* **Framework :** Spring Boot 3.5.16 (version déclarée dans `gradle/libs.versions.toml`) — Gradle 9.7.1 via le wrapper.
* **Dépendances :** toujours ajoutées dans `gradle/libs.versions.toml` puis référencées dans `app/build.gradle.kts`.

## 4. Architecture DDD (Domain-Driven Design)

Le projet est divisé en modules stricts. Vous ne devez modifier **que** les fichiers de votre domaine :

* 🚚 **Dev 1 (Supply Chain & Finances) :** `com.ejr.managekitchen.supplychain`
* ⚙️ **Dev 2 (Opérations & Workflow) :** `com.ejr.managekitchen.operations`
* 👤 **Dev 3 (Espace Client & IA) :** `com.ejr.managekitchen.customer`
* 🔒 **Socle commun :** `com.ejr.managekitchen.common` (sécurité JWT, erreurs, Swagger) — toute modification passe par une revue des trois membres.

### Un dossier par fonctionnalité

À l'intérieur de son domaine, chaque fonctionnalité a son propre dossier, qui contient **tout** ce qui la concerne (entité, repository, service, contrôleur, sous-dossier `dto/`) :

```
app/src/main/java/com/ejr/managekitchen/
├── ManageKitchenApplication.java   ← unique classe de démarrage
├── common/
├── supplychain/   supplier/ (F1) · stock/ (F2) · recipe/ (F3) · finance/ (F4) · shared/
├── operations/    user/ (F5, + auth) · order/ (F6, + kitchen) · notification/ (F7) · realtime/
└── customer/      menu/ · map/ (F8) · recommendation/ (F9) · assistant/ (F10)
```

Les tests suivent la même arborescence dans `app/src/test/java/`. Ils utilisent le profil `test` (base H2 en mémoire) : `@ActiveProfiles("test")`, et `@WithMockUser(roles = "ADMIN")` (ou un vrai JWT) pour appeler une API protégée.

### Base de données

* Migrations Flyway dans `app/src/main/resources/db/migration`, nommées par date : `V<AAAA>_<MM>_<JJ>_<NN>__<domaine>_<description>.sql` (ex. `V2026_11_20_01__supplychain_create_stock.sql`).
* Une migration déjà fusionnée ne se modifie **jamais** : on en crée une nouvelle.
* Pas de clé étrangère entre deux domaines : on référence l'autre domaine par son identifiant et on passe par son API ou ses interfaces (voir `doc/CONTRATS_OPERATIONS.md`).

## 5. Workflow Git

### Branches

* `main` : Production (ne jamais commiter directement dessus).
* `develop` : Branche commune d'intégration. Toutes les Pull Requests pointent ici.
* `feature/F<n>-<sujet>` : une branche par fonctionnalité (ex: `feature/F2-stock`, `feature/F10-assistant-ia`), supprimée après fusion.

### Cycle de vie d'une fonctionnalité

1. Créer sa branche depuis `develop` : `git checkout -b feature/nom-feature`
2. Coder et commiter régulièrement (Préfixes : `feat:`, `fix:`, `test:`, `chore:`).
3. **Mettre à jour avant PR :** `git pull origin develop` (pour récupérer le travail des autres et résoudre les conflits en local).
4. Pousser sa branche et ouvrir une Pull Request vers `develop`.

## 6. Tests et Qualité

* Le code doit compiler sans erreur avant toute PR : la **CI GitHub Actions** (`.github/workflows/ci.yml`) le vérifie automatiquement et doit être verte pour fusionner.
* Chaque PR doit être approuvée par **un autre membre** (règle de protection de `main` et `develop`).
* **Couverture :** Chaque nouvelle feature doit inclure ses tests (Unitaires ou MockMvc pour l'intégration).
* **JaCoCo :** L'objectif de couverture est d'environ 80%.

### Commandes utiles

```bash
./gradlew test           # Lancer tous les tests (Windows : .\gradlew.bat test)
# Rapport de couverture : app/build/reports/jacoco/test/html/index.html
.\gradlew.bat clean       # Nettoyer les caches (si problème de compilation)
```

## 7. Versions

* Chaque rendu correspond à une version figée par un **tag** sur `main` : `v0.1.0`, `v0.2.0`, `v1.0.0`.
* Procédure : PR `develop` → `main`, puis `git tag -a vX.Y.Z -m "..."` et `git push origin vX.Y.Z`.
* Le dossier `doc/DOSSIER.md` est mis à jour dans la même version (section « Journal des versions »).
