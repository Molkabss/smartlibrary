# SmartLibrary — Base de connaissances bibliographique

Mini-projet Web Sémantique (BDIA3) : une base de connaissances RDF/RDFS
modélisant une bibliothèque (documents, auteurs, membres, emprunts), interrogée
en SPARQL avec inférence RDFS, et exposée via une API REST Spring Boot + Apache Jena.

## Sommaire

- [Modèle RDFS](#modèle-rdfs)
- [Requêtes SPARQL](#requêtes-sparql)
- [Inférence RDFS](#inférence-rdfs)
- [Service REST](#service-rest)
- [Tests](#tests)
- [Lancer le projet](#lancer-le-projet)
- [Structure du projet](#structure-du-projet)

## Modèle RDFS

Défini dans [`src/main/resources/smartlibrary-schema.ttl`](src/main/resources/smartlibrary-schema.ttl),
schéma graphique dans [`docs/schema.drawio`](docs/schema.drawio).

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

**Vocabulaire externe réutilisé** : `foaf:Person` (FOAF) comme superclasse d'`Auteur` et `Membre`, et `foaf:name` pour leurs noms.

**10 propriétés**, toutes avec `domain`/`range` explicites : `titre`, `anneePublication`, `aPourAuteur`, `aEcritRoman` (`rdfs:subPropertyOf aPourAuteur`), `aPourEditeur`, `aPourGenre`, `aPourExemplaire`, `concerneExemplaire`, `effectuePar`, `dateEmprunt`.

## Requêtes SPARQL

9 requêtes dans [`src/main/resources/smartlibrary-queries.sparql`](src/main/resources/smartlibrary-queries.sparql), détaillées dans [`docs/requetes-sparql.md`](docs/requetes-sparql.md). Techniques couvertes : `ORDER BY`, jointures, `GROUP BY`/`COUNT`/`HAVING`, `OPTIONAL`/`FILTER (!BOUND(...))`, `FILTER NOT EXISTS`, `COUNT(DISTINCT ...)`, chemins de propriété à 3 sauts.

## Inférence RDFS

Démontrée dans [`InferenceDemo.java`](src/main/java/com/smartlibrary/smartlibrary/InferenceDemo.java), 3 règles appliquées avec le raisonneur RDFS d'Apache Jena :

| Règle | Triplet explicite | Triplet déduit |
|---|---|---|
| `rdfs:subClassOf` | `Le Petit Prince a Livre` | devient aussi `Document` |
| `rdfs:subPropertyOf` | `aEcritRoman (Auteur → Livre)` | génère `aPourAuteur` |
| `rdfs:range` | `aPourGenre → "Jeunesse"` | `"Jeunesse"` devient `Genre` |

Résultat mesuré : **182 triplets sans inférence → 249 avec inférence** (+67 déduits). La requête "documents + auteur via `aPourAuteur`" passe de **0 résultat** (sans inférence) à **5 résultats** (avec inférence) — preuve concrète de l'intérêt du raisonneur.

## Service REST

API Spring Boot ([`LibraryController`](src/main/java/com/smartlibrary/smartlibrary/controller/LibraryController.java) → [`LibraryService`](src/main/java/com/smartlibrary/smartlibrary/service/LibraryService.java) → requêtes SPARQL paramétrées via `ParameterizedSparqlString`) :

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/api/documents` | Tous les documents, triés par année |
| GET | `/api/auteurs/{nom}/livres` | Livres d'un auteur (nécessite l'inférence) |
| GET | `/api/genres/{nom}/livres` | Livres d'un genre |
| GET | `/api/exemplaires/disponibles` | Exemplaires jamais empruntés |
| GET | `/api/membres/multi-emprunts?min=N` | Membres ayant ≥ N emprunts |
| GET | `/api/editeurs/top` | Éditeur le plus actif |
| GET | `/api/emprunts?membre=...` | Emprunts, filtre optionnel par membre |

Les erreurs (ressource introuvable, paramètre invalide) sont gérées de façon centralisée par [`GlobalExceptionHandler`](src/main/java/com/smartlibrary/smartlibrary/exception/GlobalExceptionHandler.java), qui renvoie toujours du JSON propre avec le bon code HTTP.

## Tests

Tests unitaires dans [`LibraryServiceTest`](src/test/java/com/smartlibrary/smartlibrary/service/LibraryServiceTest.java), exécutés avec le contexte Spring complet (`@SpringBootTest`). Ils valident notamment que les requêtes basées sur l'inférence RDFS (auteur via `aPourAuteur`, genre `Jeunesse`) donnent les résultats attendus — garantissant que le raisonneur fonctionne, pas seulement que le code compile.

## Lancer le projet

Prérequis : JDK 21+, Maven (ou le wrapper `./mvnw` fourni).

```bash
./mvnw spring-boot:run
```

L'API est alors disponible sur `http://localhost:8080/api/...`.

## Structure du projet

```
smartlibrary/
├── docs/                          # schéma graphique, notes SPARQL
├── src/main/java/.../controller/  # LibraryController (REST)
├── src/main/java/.../service/     # LibraryService (Jena/SPARQL)
├── src/main/java/.../exception/   # gestion d'erreurs centralisée
├── src/main/resources/            # schéma RDFS, données, requêtes SPARQL
└── src/test/java/...              # tests unitaires
```
