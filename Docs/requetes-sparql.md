# Requêtes SPARQL — SmartLibrary

Fichier source : [`src/main/resources/smartlibrary-queries.sparql`](../src/main/resources/smartlibrary-queries.sparql)

| # | Question métier | Technique | Niveau |
|---|---|---|---|
| 1 | Tous les documents, triés du plus récent au plus ancien | `ORDER BY DESC` | Simple |
| 2 | Documents + auteur via `aPourAuteur` (requête clé pour l'inférence : 0 résultat sans inférence, 5 avec) | jointure simple | Simple |
| 3 | Nombre de livres par auteur | `GROUP BY`, `COUNT` | Moyen |
| 4 | Livres sans genre renseigné | `OPTIONAL` + `FILTER (!BOUND(...))` | Moyen |
| 5 | Exemplaires jamais empruntés | `FILTER NOT EXISTS` | Difficile |
| 6 | Membres ayant emprunté plus d'un document | `GROUP BY`, `HAVING` | Difficile |
| 7 | Éditeur ayant publié le plus de documents | `COUNT`, `ORDER BY DESC`, `LIMIT` | Difficile |
| 8 | Auteurs ayant écrit dans plusieurs genres | `COUNT(DISTINCT ...)`, `HAVING` | Difficile |
| 9 | Détail complet d'un emprunt (membre + livre + date) | chemin de propriété à 3 sauts | Difficile |