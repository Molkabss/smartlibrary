# Stratégie de tests — SmartLibrary

Deux niveaux de tests, complémentaires :

## 1. Tests de service (`LibraryServiceTest`, 12 tests)

Appellent directement les méthodes de `LibraryService` en Java. Vérifient le
contenu exact des résultats (titres, comptes, ordre), avec le modèle RDF
**avec inférence RDFS** activée (comme en production).

## 2. Tests d'intégration REST (`LibraryControllerTest`, 10 tests)

Utilisent `MockMvc` pour simuler de vraies requêtes HTTP sur `/api/...`,
sans serveur réellement démarré. Vérifient le code de statut HTTP (200, 404)
et la structure du JSON renvoyé — donc toute la chaîne HTTP → Controller →
Service → Jena.

## Répartition par endpoint

| Endpoint | Tests |
|---|---|
| `/api/documents` | 1 |
| `/api/auteurs/{nom}/livres` | 2 (connu, inconnu → 404) |
| `/api/genres/{nom}/livres` | 2 (connu, inconnu → 404) |
| `/api/exemplaires/disponibles` | 1 |
| `/api/membres/multi-emprunts` | 1 |
| `/api/editeurs/top` | 1 |
| `/api/emprunts` | 2 (sans filtre, avec filtre) |