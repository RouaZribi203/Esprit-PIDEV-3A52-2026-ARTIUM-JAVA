# Artium - Gestion de Bibliothèque (Guide du Développeur)

Ce document récapitule la structure du projet et les responsabilités des fichiers principaux pour la gestion des livres.

## 📚 Modules de la Bibliothèque

### 🎨 1. Espace Artiste (Gestion de ses propres livres)
Cet espace permet à l'artiste de publier, modifier et supprimer ses propres livres.
- **Contrôleur (CRUD)** : [BibliothequArtisteController.java](file:///c:/Users/sarra/IdeaProjects/Esprit-PIDEV-3A52-2026-ARTIUM-JAVA/src/main/java/controllers/artist/BibliothequArtisteController.java)
- **Interface (Vue)** : [Bibliotheque.fxml](file:///c:/Users/sarra/IdeaProjects/Esprit-PIDEV-3A52-2026-ARTIUM-JAVA/src/main/resources/views/artist/Bibliotheque.fxml)

### 👤 2. Espace Amateur (Consultation et Lecture)
Espace de consultation pour les utilisateurs qui louent et lisent des livres.
- **Contrôleur (Vue/Filtrage)** : [BibliofrontController.java](file:///c:/Users/sarra/IdeaProjects/Esprit-PIDEV-3A52-2026-ARTIUM-JAVA/src/main/java/controllers/amateur/BibliofrontController.java)
- **Interface (Vue)** : [Bibliotheque.fxml](file:///c:/Users/sarra/IdeaProjects/Esprit-PIDEV-3A52-2026-ARTIUM-JAVA/src/main/resources/views/amateur/Bibliotheque.fxml)
- **Lecteur PDF** : [BookReaderController.java](file:///c:/Users/sarra/IdeaProjects/Esprit-PIDEV-3A52-2026-ARTIUM-JAVA/src/main/java/controllers/amateur/BookReaderController.java)

### 🛠️ 3. Dashboard Admin (Gestion Globale)
Espace d'administration pour la gestion complète de tous les livres du système.
- **Contrôleur (CRUD Complet)** : [LivresController.java](file:///c:/Users/sarra/IdeaProjects/Esprit-PIDEV-3A52-2026-ARTIUM-JAVA/src/main/java/controllers/LivresController.java)
- **Interface (Vue)** : [livres.fxml](file:///c:/Users/sarra/IdeaProjects/Esprit-PIDEV-3A52-2026-ARTIUM-JAVA/src/main/resources/views/pages/livres.fxml)

---

## 🏗️ Structure Technique

### 💾 Persistance (Services & Database)
- **Base de Données** : [MyDatabase.java](file:///c:/Users/sarra/IdeaProjects/Esprit-PIDEV-3A52-2026-ARTIUM-JAVA/src/main/java/utils/MyDatabase.java) (Singleton avec auto-reconnexion).
- **Service Livres** : [JdbcLivreService.java](file:///c:/Users/sarra/IdeaProjects/Esprit-PIDEV-3A52-2026-ARTIUM-JAVA/src/main/java/services/JdbcLivreService.java) (Logique SQL pour CREATE, READ, UPDATE, DELETE).
- **Service Collections** : [JdbcCollectionService.java](file:///c:/Users/sarra/IdeaProjects/Esprit-PIDEV-3A52-2026-ARTIUM-JAVA/src/main/java/services/JdbcCollectionService.java).
- **Service Locations** : [JdbcLocationLivreService.java](file:///c:/Users/sarra/IdeaProjects/Esprit-PIDEV-3A52-2026-ARTIUM-JAVA/src/main/java/services/JdbcLocationLivreService.java).

### 🖥️ Interfaces Globales (Layouts)
- **Point d'Entrée** : [MainFX.java](file:///c:/Users/sarra/IdeaProjects/Esprit-PIDEV-3A52-2026-ARTIUM-JAVA/src/main/java/controllers/MainFX.java).
- **Layout Admin** : [MainLayout.fxml](file:///c:/Users/sarra/IdeaProjects/Esprit-PIDEV-3A52-2026-ARTIUM-JAVA/src/main/resources/views/MainLayout.fxml).
- **Layout Amateur** : [AmateurMain.fxml](file:///c:/Users/sarra/IdeaProjects/Esprit-PIDEV-3A52-2026-ARTIUM-JAVA/src/main/resources/views/amateur/AmateurMain.fxml).
- **Styles CSS** : [dashboard.css](file:///c:/Users/sarra/IdeaProjects/Esprit-PIDEV-3A52-2026-ARTIUM-JAVA/src/main/resources/views/styles/dashboard.css).

---

## 🚀 Fonctionnalités Implémentées
- **Transitions Fluides** : Utilisation de `FadeTransition` pour les formulaires.
- **Validation Robuste** : Filtrage numérique en temps réel pour les prix.
- **Gestion PDF** : Chargement asynchrone des fichiers pour éviter le gel de l'interface.
- **Session Dynamique** : Utilisation de `SessionManager` pour identifier l'artiste connecté.
