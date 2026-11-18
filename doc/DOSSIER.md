---
title: "EJR-ManageKitchen"
subtitle: "Plateforme SaaS de Restauration Connectée (B2B & B2C) — Dossier de projet"
author:
  - MEKSEM Rayane
  - JANA GUANA Erick
  - KPINSO-FOLLY Joël
date: "Master 1 MIAGE — Année universitaire 2026-2027 — Version v0.1.0"
lang: fr
---

# Suivi du document

Ce dossier est versionné dans le dépôt principal (`doc/DOSSIER.md`) et compilé en PDF par l'intégration continue. Il évolue à chaque livraison de version.

| Version | Rendu | Contenu ajouté ou modifié |
|---|---|---|
| Rendu 1 | Dossier de lancement | Équipe, projet, scénarios, architecture envisagée, protocole IA, dépôt |
| **v0.1.0** | Rendu 2 | Structure du code unifiée, workflow Git, intégration continue, badges, journal des fonctionnalités livrées (section 8) |

# Présentation du projet et caractère innovant

La plateforme unifie deux univers traditionnellement séparés dans la restauration : la **gestion opérationnelle interne (B2B)** et l'**expérience client (B2C)**.

- **Espace Entreprise (B2B)** : gestion de la chaîne d'approvisionnement (fournisseurs, approvisionnements, fiches techniques des recettes, stock) et pilotage en cuisine grâce à un écran de cuisine (*Kitchen Display System*, KDS).
- **Espace Client (B2C)** : consultation du menu en fonction des stocks réels, commande en ligne, assistant conversationnel et suivi de livraison.

Trois innovations structurent le projet :

1. **Le temps réel** : grâce aux WebSockets, une commande client apparaît instantanément sur l'écran de cuisine, et chaque changement d'état (par exemple « Commande prête ») déclenche une notification immédiate chez le client.
2. **L'assistant IA contextuel** : un agent conversationnel guide le client en tenant compte de ses préférences (allergies, régime) et de la disponibilité réelle des plats.
3. **La géolocalisation** : localisation de l'établissement et suivi des livraisons sur une carte interactive.

# Équipe, comptes développeurs et répartition

L'équipe compte trois étudiants. Le travail est découpé selon les principes du *Domain-Driven Design* : chaque développeur est propriétaire d'un domaine métier, de la base de données jusqu'à l'API.

| Membre | Compte GitHub | Domaine | Feature majeure |
|---|---|---|---|
| MEKSEM Rayane | <https://github.com/Rayanemksm1> | Dev 1 — Supply Chain & Finances | Liaison recettes-stocks et décrémentation automatique |
| JANA GUANA Erick | <https://github.com/ErickJanaPro> | Dev 2 — Commandes & Opérations | KDS et machine à états temps réel |
| KPINSO-FOLLY Joël | <https://github.com/Joel4webb> | Dev 3 — Espace Client & IA | Assistant IA et parcours B2C |

## Périmètre fonctionnel (10 fonctionnalités)

| Domaine | Fonctionnalités |
|---|---|
| Supply Chain & Finances (Dev 1) | F1 Fournisseurs · F2 Stock · F3 Recettes et menus · F4 Finances |
| Commandes & Opérations (Dev 2) | F5 Utilisateurs et rôles · F6 Commandes et statuts · F7 Notifications |
| Espace Client & IA (Dev 3) | F8 Géolocalisation et carte · F9 Recommandations · F10 Chat et assistant IA |

# Scénario d'usage nominal

1. **Choix (Espace Client)** : le client se connecte et demande à l'assistant IA : « Je suis végétarien, que me conseilles-tu ? ». L'assistant interroge le catalogue des plats disponibles (Dev 1) et propose une sélection.
2. **Commande (API)** : le client valide son panier (`POST /api/orders`). La requête est traitée par le module Commandes (Dev 2).
3. **Décrémentation (Supply Chain)** : dans la même transaction, le module Commandes appelle le module Stock (Dev 1), qui déduit les ingrédients des plats commandés. Si le stock est insuffisant, la commande est refusée et rien n'est enregistré.
4. **Cuisine en temps réel (KDS)** : la commande apparaît instantanément sur l'écran de cuisine avec le statut *Nouvelle*. Le cuisinier la passe *En préparation*, puis *Prête*.
5. **Clôture et notification** : le passage à *Prête* notifie le client (WebSocket et e-mail). Il vient récupérer sa commande (*Récupérée*) ou elle part en livraison (*Livrée*), avec un suivi sur la carte (Dev 3).

# Architecture et frameworks

## Couches

| Couche | Choix retenus |
|---|---|
| Front-end | Pages HTML/CSS/JavaScript servies par Spring (espace client, écran cuisine) ; Leaflet.js et OpenStreetMap pour la carte |
| API | REST/JSON, documentée automatiquement avec OpenAPI/Swagger (`/swagger-ui.html`) ; WebSocket STOMP pour le temps réel |
| Back-end | Java 17+, Spring Boot 3.5, Spring Security avec jetons JWT, rôles CLIENT / CUISINIER / ADMIN |
| Base de données | PostgreSQL 16 (via Docker Compose), Spring Data JPA, migrations versionnées avec Flyway |
| Build et qualité | Gradle (Kotlin DSL) avec wrapper, tests JUnit 5 et MockMvc, couverture JaCoCo, GitHub Actions, CodeQL |

L'application est un **monolithe modulaire** : une seule application Spring Boot, découpée en domaines indépendants qui communiquent par des contrats explicites (interfaces Java, API REST, événements).

## Organisation du code

Le code est rangé par domaine, puis **par fonctionnalité** : chaque dossier contient tout ce qui concerne une fonctionnalité (entité, repository, service, contrôleur, DTO).

```
app/src/main/java/com/ejr/managekitchen/
|-- ManageKitchenApplication.java  (unique classe de démarrage)
|-- common/        sécurité JWT, gestion des erreurs, Swagger
|-- supplychain/   Dev 1 : supplier (F1), stock (F2), recipe (F3),
|                          finance (F4), shared
|-- operations/    Dev 2 : user (F5, + auth), order (F6, + kitchen),
|                          notification (F7), realtime
|-- customer/      Dev 3 : menu, map (F8), recommendation (F9),
                           assistant (F10)
```

## Communication entre domaines

- **Commandes -> Supply Chain** : le module Commandes définit deux interfaces, `MenuCatalogPort` (plats, prix, disponibilité) et `StockPort` (décrémentation), que le domaine Supply Chain implémente. En attendant, une implémentation simulée permet de développer et de tester les commandes.
- **Commandes -> Client et Notifications** : chaque création ou changement de statut publie un événement `OrderStatusChangedEvent`. Les notifications, l'écran de cuisine et le futur suivi de livraison y réagissent sans dépendre du code des commandes.
- Les contrats détaillés sont décrits dans `doc/CONTRATS_OPERATIONS.md`.

## Machine à états d'une commande

```
NOUVELLE -> EN_PREPARATION -> PRETE -> LIVREE      (livraison)
                                   -> RECUPEREE   (retrait sur place)
```

Toute autre transition est refusée (HTTP 409). Seuls le cuisinier et l'administrateur peuvent changer un statut.

# Protocole d'usage de l'IA

## L'IA dans le produit (feature de Dev 3)

- **Technologie** : Spring AI pour abstraire les appels au modèle de langage ; fournisseur OpenAI ou Google Gemini.
- **Pas d'hallucination de plats** : à l'ouverture du chat, le back-end récupère le menu actif (sans les plats en rupture) et l'injecte dans une instruction système masquée, avec le profil du client (allergies, régime). L'IA a pour consigne de ne proposer que des plats présents dans ce menu.
- **Repli en cas d'indisponibilité** : en cas d'erreur de l'API externe (quota, délai dépassé), l'interface bascule automatiquement sur un affichage statique « Nos suggestions du chef », construit à partir des plats les plus vendus (déjà disponible côté Commandes : `OrderQueryService.bestSellers`).

## L'IA dans le développement

L'équipe s'est aidée d'un assistant IA conversationnel pour rédiger le plan de projet, produire le squelette du domaine Commandes & Opérations et unifier la structure du dépôt. Tout le code proposé est relu, compris, testé (`./gradlew test`) et validé par l'équipe avant d'être fusionné par pull request. Le membre qui fusionne reste responsable du code intégré.

# Dépôt, organisation et qualité

## Dépôt

- **URL** : <https://github.com/ErickJanaPro/EJR-ManageKitchen>
- **Documentation** : dossier `/doc` (ce document, contrats entre domaines), `README.md` (installation), `CONTRIBUTING.md` (règles d'équipe).

## Workflow Git

- `main` reçoit uniquement les versions rendues, chacune figée par un tag (`v0.1.0` pour ce rendu).
- `develop` est la branche d'intégration et la branche par défaut du dépôt.
- Une branche par fonctionnalité, `feature/F<n>-<sujet>`, fusionnée dans `develop` par pull request.
- `main` et `develop` sont **protégées** : pas de push direct, et chaque pull request doit être approuvée par un autre membre.

## Intégration continue

À chaque push sur `main` / `develop` et à chaque pull request, GitHub Actions :

1. compile le projet et lance tous les tests ;
2. mesure la couverture de code avec JaCoCo, publie les rapports et met à jour le badge de couverture ;
3. compile ce dossier en PDF ;
4. analyse la qualité et la sécurité du code avec CodeQL (Java et JavaScript).

## README et badges

Le `README.md` affiche les badges de **build (CI)**, de **version** (dernier tag), de **licence** (MIT), de **couverture de tests** (JaCoCo) et de **qualité du code** (CodeQL), ainsi que les instructions d'installation : prérequis, variables d'environnement, base de données via Docker Compose et lancement avec Gradle.

# Journal des versions

## v0.1.0 (Rendu 2)

### Socle commun

- Structure unique : un seul projet Gradle, package `com.ejr.managekitchen`, un dossier par fonctionnalité.
- Sécurité : authentification par jeton JWT, rôles CLIENT / CUISINIER / ADMIN, réponses d'erreur homogènes.
- Base PostgreSQL lancée avec Docker Compose ; schéma versionné par Flyway.
- Intégration continue, badges et documentation (voir section précédente).

### Fonctionnalités livrées par membre

**Erick — Commandes & Opérations (feature majeure : KDS et machine à états temps réel) — livrée**

- F5 : inscription, connexion par JWT, gestion des comptes par l'administrateur, comptes de démonstration.
- F6 : passage de commande avec vérification des plats et décrémentation du stock dans la même transaction ; machine à états avec historique des statuts ; écran de cuisine (`/kds/`) mis à jour en temps réel par WebSocket.
- F7 : notification de la cuisine à chaque nouvelle commande, et du client quand sa commande est prête (WebSocket, plus e-mail simulé dans les journaux en attendant le choix d'un service d'envoi).
- Tests : machine à états (tests unitaires) ; scénario complet de commande, contrôle d'accès par rôle et isolation des commandes entre clients (tests d'intégration).

**Rayane — Supply Chain & Finances**

- F1 : gestion des fournisseurs (création, consultation, modification, suppression) et des ingrédients rattachés, avec contrôle des doublons ; tests d'intégration.
- Feature majeure (liaison recettes-stocks et décrémentation automatique) : *à compléter par Rayane selon l'état fusionné à la date du tag*.

**Joël — Espace Client & IA**

- Squelette de l'Espace Client : page d'accueil (`index.html`, `style.css`, `app.js`) qui charge et affiche le menu dynamiquement, avec Leaflet.js déjà intégré pour la future carte (F8).
- API B2C de consultation du menu : `GET /api/customer/menu`, alimentée par un service de données de démonstration en attendant le catalogue réel du domaine Supply Chain.
- Guide de contribution de l'équipe (`CONTRIBUTING.md`) : Gradle comme outil de build unique, architecture DDD, workflow Git.
- Feature majeure (assistant IA et parcours B2C) : la v0.1.0 en pose la première brique, la consultation du menu ; l'assistant IA reste à livrer.

### Changements par rapport au dossier de lancement

- Package `com.restaurant` remplacé par `com.ejr.managekitchen`, conformément au guide de contribution de l'équipe ; organisation par fonctionnalité dans chaque domaine.
- Outil de build unique : Gradle (Maven abandonné).
- Ajout du statut `RECUPEREE` pour le retrait sur place, à côté de `LIVREE`.
- La décrémentation du stock a lieu dès la validation de la commande, dans la même transaction.

## Prochaines versions

- **v0.2.0 (Rendu 3 à suivre)**

# Points ouverts

| Sujet | Décision à prendre |
|---|---|
| Service d'e-mail | Choisir un service d'envoi réel pour les notifications (aujourd'hui simulé) |
| Fournisseur d'IA | Choisir entre OpenAI et Gemini (coût, quotas, clé d'API) |
| Livraison | Aucun rôle de livreur n'est défini : le cuisinier ou l'administrateur clôture la commande |
| Accès au back-office | Restreindre la gestion des fournisseurs au rôle ADMIN quand l'interface d'administration sera prête |
