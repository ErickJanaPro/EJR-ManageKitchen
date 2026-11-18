# Guide de contribution - EJR-ManageKitchen (SaaS Restauration)

Ce document définit les standards techniques et le workflow que l'équipe (Dev 1, Dev 2, Dev 3) doit respecter pour garantir la cohérence de la plateforme.

## 1. Prérequis

* **Java 17** minimum (JDK)
* **PostgreSQL** (version 15+)
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
.\gradlew.bat bootRun
# -> http://localhost:8080
```

## 3. Règles non négociables (Stack & Architecture)

* **Outil de Build UNIQUE :** Le projet utilise **Gradle** (Kotlin DSL : `build.gradle.kts`). L'utilisation de Maven (`pom.xml`) est **strictement interdite** pour éviter les conflits. Si un fichier `pom.xml` est présent, il doit être supprimé.
* **Fichiers ignorés :** Ne **jamais** commiter les dossiers générés par le build ou l'IDE : `build/`, `target/`, `.gradle/`, `.vscode/`, `.idea/`.
* **Framework :** Spring Boot 3.3.4.

## 4. Architecture DDD (Domain-Driven Design)

Le projet est divisé en modules stricts. Vous ne devez modifier **que** les fichiers de votre domaine :

* 🚚 **Dev 1 (Supply Chain & Finances) :** `com.ejr.managekitchen.supplychain`
* ⚙️ **Dev 2 (Opérations & Workflow) :** `com.ejr.managekitchen.operations`
* 👤 **Dev 3 (Espace Client & IA) :** `com.ejr.managekitchen.customer`

## 5. Workflow Git

### Branches

* `main` : Production (ne jamais commiter directement dessus).
* `develop` : Branche commune d'intégration. Toutes les Pull Requests pointent ici.
* `feature/issue-X` : Branche de travail personnelle (ex: `feature/dev3-squelette-client`).

### Cycle de vie d'une fonctionnalité

1. Créer sa branche depuis `develop` : `git checkout -b feature/nom-feature`
2. Coder et commiter régulièrement (Préfixes : `feat:`, `fix:`, `test:`, `chore:`).
3. **Mettre à jour avant PR :** `git pull origin develop` (pour récupérer le travail des autres et résoudre les conflits en local).
4. Pousser sa branche et ouvrir une Pull Request vers `develop`.

## 6. Tests et Qualité

* Le code doit compiler sans erreur avant toute PR.
* **Couverture :** Chaque nouvelle feature doit inclure ses tests (Unitaires ou MockMvc pour l'intégration).
* **JaCoCo :** L'objectif de couverture est d'environ 80%.

### Commandes utiles

```bash
.\gradlew.bat test        # Lancer tous les tests
.\gradlew.bat clean       # Nettoyer les caches (si problème de compilation)
```