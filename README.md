
# SmartLibrary — Base de connaissances bibliographique

Mini-projet Web Sémantique (BDIA3) : une base de connaissances RDF/RDFS
modélisant une bibliothèque (documents, auteurs, membres, emprunts), interrogée
en SPARQL avec inférence RDFS, et exposée via une API REST Spring Boot + Apache Jena.

## Sommaire

- [Technologies utilisées](#technologies-utilisées)
- [Architecture du projet](#architecture-du-projet)
- [Présentation du dataset](#présentation-du-dataset)
- [Modèle RDFS](#modèle-rdfs)
- [Requêtes SPARQL](#requêtes-sparql)
- [Inférence RDFS](#inférence-rdfs)
- [Service REST](#service-rest)
- [Gestion des erreurs](#gestion-des-erreurs)
- [Tests](#tests)
- [Lancer le projet](#lancer-le-projet)
- [Déclaration d'utilisation de l'IA générative](#déclaration-dutilisation-de-lia-générative)

## Technologies utilisées

| Technologie | Version | Rôle |
|---|---|---|
| Java | 21 (JDK 25 utilisé en pratique) | Langage de programmation |
| Spring Boot | 4.1.2 | Framework Web et injection de dépendances |
| Apache Jena | 5.3.0 | Manipulation RDF/RDFS, raisonneur, SPARQL |
| Maven | — | Gestion de build et dépendances |
| JUnit | 6.0.3 | Tests unitaires |
| AssertJ | 3.27.7 | Assertions lisibles dans les tests |
| Spring MockMvc | — | Tests d'intégration REST (simulation de requêtes HTTP) |

## Architecture du projet

smartlibrary/
├── docs/                                     # schéma graphique, notes SPARQL
│   ├── schema.drawio
│   └── requetes-sparql.md
├── src/
│   ├── main/
│   │   ├── java/com/smartlibrary/smartlibrary/
│   │   │   ├── SmartlibraryApplication.java  # Classe principale Spring Boot
│   │   │   ├── InferenceDemo.java            # Démo des 3 règles d'inférence RDFS
│   │   │   ├── controller/
│   │   │   │   └── LibraryController.java    # 7 endpoints REST sous /api
│   │   │   ├── service/
│   │   │   │   └── LibraryService.java       # Chargement Jena, inférence, requêtes SPARQL
│   │   │   └── exception/
│   │   │       ├── GlobalExceptionHandler.java    # Gestion centralisée des erreurs (404, 400, 500)
│   │   │       └── ResourceNotFoundException.java # Exception métier 404
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── smartlibrary-schema.ttl       # Vocabulaire RDFS (classes, propriétés)
│   │       ├── smartlibrary-data.ttl         # Instances
│   │       └── smartlibrary-queries.sparql   # Les 9 requêtes SPARQL
│   └── test/
│       └── java/com/smartlibrary/smartlibrary/
│           ├── service/
│           │   └── LibraryServiceTest.java       # Tests métier (requêtes, inférence, 404)
│           └── controller/
│               └── LibraryControllerTest.java    # Tests d'intégration REST (MockMvc)
└── pom.xml
## Présentation du dataset

Le dataset (`smartlibrary-data.ttl`) contient :

- 4 auteurs (Antoine de Saint-Exupéry, Victor Hugo, Isaac Asimov, Albert Camus)
- 3 éditeurs (Gallimard, Hachette, Folio)
- 4 genres (Roman, Science-fiction, Philosophie, Jeunesse)
- 5 livres + 1 revue = 6 documents
- 6 exemplaires physiques
- 3 membres (Molka Ben Salah, Ahmed Trabelsi, Sana Gharbi)
- 5 emprunts

## Modèle RDFS

Défini dans [`src/main/resources/smartlibrary-schema.ttl`](src/main/resources/smartlibrary-schema.ttl), schéma graphique dans [`docs/schema.drawio`](docs/schema.drawio).

**9 classes** :

| Classe | Hiérarchie |
|---|---|
| `sl:Document` | — |
| `sl:Livre` | `rdfs:subClassOf sl:Document` |
| `sl:Revue` | `rdfs:subClassOf sl:Document` |
| `sl:Auteur` | `rdfs:subClassOf foaf:Person` |
| `sl:Membre` | `rdfs:subClassOf foaf:Person` |
| `sl:Editeur` | — |
| `sl:Genre` | — |
| `sl:Exemplaire` | — |
| `sl:Emprunt` | — |

**Vocabulaire externe réutilisé** : FOAF — `foaf:Person` comme superclasse d'`Auteur` et `Membre`, et `foaf:name` pour leurs noms.

**10 propriétés**, toutes avec `domain`/`range` explicites :

| Propriété | Domaine | Range | Sous-propriété de |
|---|---|---|---|
| `sl:titre` | `Document` | `xsd:string` | — |
| `sl:anneePublication` | `Document` | `xsd:gYear` | — |
| `sl:aPourAuteur` | `Document` | `Auteur` | — |
| `sl:aEcritRoman` | `Auteur` | `Livre` | `sl:aPourAuteur` |
| `sl:aPourEditeur` | `Document` | `Editeur` | — |
| `sl:aPourGenre` | `Document` | `Genre` | — |
| `sl:aPourExemplaire` | `Livre` | `Exemplaire` | — |
| `sl:concerneExemplaire` | `Emprunt` | `Exemplaire` | — |
| `sl:effectuePar` | `Emprunt` | `Membre` | — |
| `sl:dateEmprunt` | `Emprunt` | `xsd:date` | — |

## Requêtes SPARQL

9 requêtes dans [`src/main/resources/smartlibrary-queries.sparql`](src/main/resources/smartlibrary-queries.sparql), détaillées dans [`docs/requetes-sparql.md`](docs/requetes-sparql.md). Techniques couvertes : `ORDER BY`, jointures, `GROUP BY`/`COUNT`/`HAVING`, `OPTIONAL`/`FILTER (!BOUND(...))`, `FILTER NOT EXISTS`, `COUNT(DISTINCT ...)`, chemins de propriété à 3 sauts.

## Inférence RDFS

Démontrée dans [`InferenceDemo.java`](src/main/java/com/smartlibrary/smartlibrary/InferenceDemo.java), 3 règles appliquées avec le raisonneur RDFS natif d'Apache Jena (`ReasonerRegistry.getRDFSReasoner()`) :

| Règle | Triplet explicite | Triplet déduit |
|---|---|---|
| `rdfs:subClassOf` | `Le Petit Prince a Livre` | devient aussi `Document` |
| `rdfs:subPropertyOf` | `aEcritRoman (Auteur → Livre)` | génère `aPourAuteur` |
| `rdfs:range` | `aPourGenre → "Jeunesse"` | `"Jeunesse"` devient `Genre` |

Résultat mesuré : **182 triplets sans inférence → 249 avec inférence** (+67 déduits). La requête "documents + auteur via `aPourAuteur`" passe de **0 résultat** (sans inférence) à **5 résultats** (avec inférence) — preuve concrète de l'intérêt du raisonneur.

Le service REST (`LibraryService`) utilise en permanence le modèle **avec inférence** (`InfModel`), chargé une seule fois au démarrage.

## Service REST

API Spring Boot ([`LibraryController`](src/main/java/com/smartlibrary/smartlibrary/controller/LibraryController.java) → [`LibraryService`](src/main/java/com/smartlibrary/smartlibrary/service/LibraryService.java) → requêtes SPARQL paramétrées via `ParameterizedSparqlString`, protégées contre l'injection SPARQL) :

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/api/documents` | Tous les documents, triés par année |
| GET | `/api/auteurs/{nom}/livres` | Livres d'un auteur (nécessite l'inférence) |
| GET | `/api/genres/{nom}/livres` | Livres d'un genre |
| GET | `/api/exemplaires/disponibles` | Exemplaires jamais empruntés |
| GET | `/api/membres/multi-emprunts?min=N` | Membres ayant ≥ N emprunts (défaut : 2) |
| GET | `/api/editeurs/top` | Éditeur le plus actif |
| GET | `/api/emprunts?membre=...` | Emprunts, filtre optionnel par membre |

## Gestion des erreurs

Toutes les erreurs retournent un format JSON unifié, construit par `GlobalExceptionHandler` :

```json
{
  "timestamp": "2026-11-02T22:10:00",
  "status": 404,
  "error": "Not Found",
  "message": "Aucun auteur trouve avec le nom : Inconnu"
}
```

- **404** : ressource introuvable (`ResourceNotFoundException`)
- **400** : paramètre invalide (`MethodArgumentTypeMismatchException`)
- **500** : toute autre erreur serveur

## Tests

Deux suites de tests automatisées :

- [`LibraryServiceTest`](src/test/java/com/smartlibrary/smartlibrary/service/LibraryServiceTest.java) (12 tests) — tests métier : requêtes SPARQL, inférence RDFS, exceptions, agrégations, directement sur `LibraryService`.
- [`LibraryControllerTest`](src/test/java/com/smartlibrary/smartlibrary/controller/LibraryControllerTest.java) (10 tests) — tests d'intégration REST avec `MockMvc` : un ou plusieurs tests par endpoint, vérifiant le code HTTP (200/404) et le JSON réellement renvoyé par l'API.

Elles couvrent notamment :

- le tri des documents par année,
- les requêtes qui ne fonctionnent **que grâce à l'inférence RDFS** (auteur via `aPourAuteur`, genre `Jeunesse` déduit par la règle `range`), vérifiées à la fois côté service et côté réponse HTTP,
- les exceptions 404 (auteur/genre inconnu),
- les agrégations (`GROUP BY`/`HAVING`, éditeur le plus actif),
- la négation (`FILTER NOT EXISTS` sur les exemplaires disponibles),
- le filtrage optionnel des emprunts par membre.

Soit **22 tests** au total (+ 1 test `contextLoads` généré par défaut par Spring Boot).

### Lancer les tests

```bash
./mvnw test
```

## Lancer le projet

Prérequis : JDK 21+, Maven (ou le wrapper `./mvnw` fourni).

```bash
./mvnw spring-boot:run
```

L'application démarre sur `http://localhost:8080`.

### Tester les endpoints

```bash
# Tous les documents
curl http://localhost:8080/api/documents

# Livres d'un auteur (démontre l'inférence RDFS)
curl http://localhost:8080/api/auteurs/Antoine%20de%20Saint-Exup%C3%A9ry/livres

# Exemplaires disponibles
curl http://localhost:8080/api/exemplaires/disponibles

# Membres ayant emprunté au moins 2 documents
curl "http://localhost:8080/api/membres/multi-emprunts?min=2"
```

## Déclaration d'utilisation de l'IA générative

Ce projet a été réalisé avec l'assistance d'une IA générative (Claude) pour :
- la mise en place de l'environnement de développement (JDK, Maven, IntelliJ, dépendances Jena/Spring Boot),
- l'aide à la rédaction et la correction du vocabulaire RDFS, des requêtes SPARQL et du code Java,
- la rédaction de cette documentation (README, `docs/`).

La compréhension du modèle, les choix de modélisation et la validation de chaque partie ont été faits par moi-même.