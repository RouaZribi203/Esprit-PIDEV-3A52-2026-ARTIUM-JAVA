# Artium - Plateforme Artistique Collaborative

Application desktop JavaFX developpee dans le cadre du projet PIDEV, avec une architecture modulaire autour de plusieurs espaces metier: administration, artiste et amateur.

## Vision du projet

Artium centralise plusieurs domaines fonctionnels dans une seule application:

- gestion des utilisateurs et authentification
- bibliotheque (livres, locations, lecture)
- oeuvres et collections
- galeries
- evenements et tickets
- musique et playlists
- reclamations et reponses

## Modules applicatifs

### 1) Espace Authentification

- navigation d'entree et pages d'acces (`views/auth`)
- controleurs principaux: `AuthLandingController`, `ConnexionController`, `InscriptionController`, `ForgotPasswordController`

### 2) Espace Admin

- tableau de bord et gestion globale (`views/MainLayout.fxml`, `views/pages`)
- supervision des contenus et des utilisateurs
- controleurs principaux: `MainController`, `DashboardController`, `LivresController`, `OeuvresAdminController`, `GaleriesController`, `EvenementsController`

### 3) Espace Artiste

- gestion des ressources de l'artiste (`views/artist`)
- publication et administration des contenus artistiques
- point d'entree: `views/artist/ArtistMain.fxml`

### 4) Espace Amateur

- consultation et interaction utilisateur (`views/amateur`)
- parcours de decouverte des contenus
- point d'entree: `views/amateur/AmateurMain.fxml`

## Architecture technique

Le projet suit une organisation en couches, proche d'un modele MVC:

- `controllers/` : logique d'interface JavaFX
- `entities/` : modeles metier (`User`, `Livre`, `Oeuvre`, `Evenement`, etc.)
- `services/` : acces aux donnees et regles metier (`Jdbc*Service` et services dedies)
- `utils/` : utilitaires transverses (`MyDatabase`, `SessionManager`, etc.)
- `resources/views/` : fichiers FXML et styles CSS

## Stack et dependances

Dependances principales declarees dans `pom.xml`:

- Java 17
- Maven
- JavaFX (`javafx-controls`, `javafx-fxml`, `javafx-media`, `javafx-swing`)
- MySQL Connector/J (`mysql-connector-java` 8.0.33)
- Apache PDFBox (`pdfbox` 2.0.30)
- jBCrypt (`jbcrypt` 0.4)

## Structure du repository

```text
Java/
  pom.xml
  README.md
  src/
	main/
	  java/
		controllers/
		entities/
		services/
		utils/
	  resources/
		views/
	test/
```

## Prerequis

Avant de lancer l'application:

- JDK 17 installe et configure (`JAVA_HOME`)
- Maven 3.8+ installe
- serveur MySQL accessible localement
- base de donnees `artium_db` creee

## Configuration base de donnees

La connexion est centralisee dans `src/main/java/utils/MyDatabase.java`.

Valeurs actuellement configurees dans le code:

- URL: `jdbc:mysql://localhost:3306/artium_db`
- utilisateur: `root`
- mot de passe: vide

Si besoin, adaptez ces valeurs a votre environnement local.

## Configuration OpenRouter pour les paroles IA

Le module amateur de generation de paroles lit automatiquement un fichier local:

- `config/openrouter.properties`

Exemple:

```properties
openrouter.apiKey=VOTRE_CLE_OPENROUTER
# openrouter.model=inclusionai/ling-2.6-1t:free
```

Si vous preferez, vous pouvez aussi utiliser:

- la variable d'environnement `OPENROUTER_API_KEY`
- la propriete JVM `-Dopenrouter.apiKey=...`

Priorite de lecture:

1. `config/openrouter.properties`
2. propriete JVM `openrouter.apiKey` / `openrouter.model`
3. variables d'environnement `OPENROUTER_API_KEY` / `OPENROUTER_MODEL`

## Lancement du projet

Depuis la racine du projet:

```powershell
mvn clean javafx:run
```

Point d'entree JavaFX: `controllers.MainFX` (configure dans `pom.xml`).

## Fonctionnalites techniques notables

- navigation entre scenes centralisee dans `MainFX`
- theming par role (`dashboard.css`, `artist-theme.css`, `amateur-theme.css`, `auth.css`)
- gestion de session via `SessionManager`
- persistance MySQL avec services JDBC dedies
- support de lecture/manipulation PDF via PDFBox

## Qualite et bonnes pratiques

- separer la logique UI (controllers) de la logique donnees (services)
- eviter les credentials en dur dans le code pour la production
- ajouter des tests unitaires dans `src/test/java`
- verifier les migrations SQL avant execution en equipe

## Contribution

1. Creer une branche de fonctionnalite.
2. Implementer les changements de maniere modulaire (controller/service/entity).
3. Verifier le lancement local (`mvn clean javafx:run`).
4. Ouvrir une Pull Request avec description claire des changements.

## Etat actuel

Ce README decrit l'architecture et le fonctionnement global constates dans le code present du projet.
