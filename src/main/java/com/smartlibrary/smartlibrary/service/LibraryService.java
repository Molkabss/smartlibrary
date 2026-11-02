package com.smartlibrary.smartlibrary.service;

import com.smartlibrary.smartlibrary.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Couche service : toute la logique Jena (chargement du graphe, inference,
 * execution des requetes SPARQL) vit ici, separee du controleur REST.
 *
 * Le modele RDF (schema + donnees + inference RDFS) est charge UNE SEULE
 * FOIS au demarrage de l'application (voir @PostConstruct), puis reutilise
 * pour toutes les requetes - on ne recharge jamais les fichiers a chaque
 * appel HTTP.
 */
@Service
public class LibraryService {

    private static final String NS = "http://www.smartlibrary.org/ontology#";

    private static final String PREFIXES =
            "PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
            "PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#> " +
            "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
            "PREFIX sl:   <" + NS + "> ";

    /** Le graphe complet : schema + donnees + triplets deduits par le raisonneur RDFS. */
    private Model model;

    @PostConstruct
    public void init() {
        Model schema = chargerDepuisLeClasspath("smartlibrary-schema.ttl");
        Model data = chargerDepuisLeClasspath("smartlibrary-data.ttl");

        Model base = ModelFactory.createDefaultModel();
        base.add(schema);
        base.add(data);

        Reasoner reasoner = ReasonerRegistry.getRDFSReasoner().bindSchema(schema);
        this.model = ModelFactory.createInfModel(reasoner, base);
    }

    private Model chargerDepuisLeClasspath(String nomFichier) {
        Model m = ModelFactory.createDefaultModel();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(nomFichier)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Fichier introuvable dans src/main/resources : " + nomFichier);
            }
            RDFDataMgr.read(m, in, Lang.TURTLE);
        } catch (Exception e) {
            throw new IllegalStateException("Erreur de chargement de " + nomFichier, e);
        }
        return m;
    }

    /**
     * Execute une requete SELECT (construite via ParameterizedSparqlString, donc
     * les parametres utilisateur sont injectes de maniere SURE, sans risque
     * d'injection SPARQL) et convertit chaque ligne de resultat en Map prete
     * a etre serialisee en JSON par Spring.
     */
    private List<Map<String, String>> executerSelect(ParameterizedSparqlString pss) {
        List<Map<String, String>> resultats = new ArrayList<>();
        Query query = pss.asQuery();
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet rs = qexec.execSelect();
            List<String> variables = rs.getResultVars();
            while (rs.hasNext()) {
                QuerySolution solution = rs.nextSolution();
                Map<String, String> ligne = new LinkedHashMap<>();
                for (String var : variables) {
                    if (!solution.contains(var)) {
                        ligne.put(var, null);
                        continue;
                    }
                    RDFNode node = solution.get(var);
                    ligne.put(var, node.isLiteral()
                            ? node.asLiteral().getLexicalForm()
                            : node.toString());
                }
                resultats.add(ligne);
            }
        }
        return resultats;
    }

    // ================================================================
    // ENDPOINT 1 - Tous les documents, tries du plus recent au plus ancien
    // ================================================================
    public List<Map<String, String>> getTousLesDocuments() {
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(PREFIXES +
                "SELECT ?titre ?annee WHERE { " +
                "  ?doc a sl:Document ; sl:titre ?titre ; sl:anneePublication ?annee . " +
                "} ORDER BY DESC(?annee)");
        return executerSelect(pss);
    }

    // ================================================================
    // ENDPOINT 2 - Livres d'un auteur donne (parametre : chemin d'URL)
    // Utilise sl:aPourAuteur, donc ne fonctionne QUE grace a l'inference RDFS
    // (nos donnees declarent seulement sl:aEcritRoman).
    // ================================================================
    public List<Map<String, String>> getLivresParAuteur(String nomAuteur) {
        if (!auteurExiste(nomAuteur)) {
            throw new ResourceNotFoundException("Aucun auteur trouve avec le nom : " + nomAuteur);
        }
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(PREFIXES +
                "SELECT ?titre ?annee WHERE { " +
                "  ?livre sl:titre ?titre ; sl:anneePublication ?annee ; sl:aPourAuteur ?auteur . " +
                "  ?auteur foaf:name ?nom . FILTER (?nom = ?nomParam) " +
                "} ORDER BY ?titre");
        pss.setLiteral("nomParam", nomAuteur);
        return executerSelect(pss);
    }

    private boolean auteurExiste(String nomAuteur) {
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(PREFIXES +
                "ASK { ?a a sl:Auteur ; foaf:name ?nom . FILTER (?nom = ?nomParam) }");
        pss.setLiteral("nomParam", nomAuteur);
        try (QueryExecution qexec = QueryExecutionFactory.create(pss.asQuery(), model)) {
            return qexec.execAsk();
        }
    }

    // ================================================================
    // ENDPOINT 3 - Livres d'un genre donne (parametre : chemin d'URL)
    // ================================================================
    public List<Map<String, String>> getLivresParGenre(String nomGenre) {
        if (!genreExiste(nomGenre)) {
            throw new ResourceNotFoundException("Aucun genre trouve avec le nom : " + nomGenre);
        }
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(PREFIXES +
                "SELECT ?titre WHERE { " +
                "  ?livre a sl:Livre ; sl:titre ?titre ; sl:aPourGenre ?genre . " +
                "  ?genre rdfs:label ?nom . FILTER (?nom = ?nomParam) " +
                "} ORDER BY ?titre");
        pss.setLiteral("nomParam", nomGenre);
        return executerSelect(pss);
    }

    private boolean genreExiste(String nomGenre) {
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(PREFIXES +
                "ASK { ?g a sl:Genre ; rdfs:label ?nom . FILTER (?nom = ?nomParam) }");
        pss.setLiteral("nomParam", nomGenre);
        try (QueryExecution qexec = QueryExecutionFactory.create(pss.asQuery(), model)) {
            return qexec.execAsk();
        }
    }

    // ================================================================
    // ENDPOINT 4 - Exemplaires actuellement disponibles
    // ================================================================
    public List<Map<String, String>> getExemplairesDisponibles() {
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(PREFIXES +
                "SELECT ?code WHERE { " +
                "  ?ex a sl:Exemplaire ; rdfs:label ?code . " +
                "  FILTER NOT EXISTS { ?emp sl:concerneExemplaire ?ex . } " +
                "} ORDER BY ?code");
        return executerSelect(pss);
    }

    // ================================================================
    // ENDPOINT 5 - Membres ayant emprunte au moins N documents
    // (parametre : query param avec valeur par defaut, gere dans le controleur)
    // ================================================================
    public List<Map<String, String>> getMembresMultiEmprunts(int min) {
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(PREFIXES +
                "SELECT ?nom (COUNT(?emp) AS ?total) WHERE { " +
                "  ?emp a sl:Emprunt ; sl:effectuePar ?m . ?m foaf:name ?nom . " +
                "} GROUP BY ?nom HAVING (COUNT(?emp) >= ?minParam) ORDER BY DESC(?total)");
        pss.setLiteral("minParam", min);
        return executerSelect(pss);
    }

    // ================================================================
    // ENDPOINT 6 - L'editeur ayant publie le plus de documents
    // ================================================================
    public Map<String, String> getEditeurTop() {
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(PREFIXES +
                "SELECT ?nom (COUNT(?doc) AS ?total) WHERE { " +
                "  ?doc sl:aPourEditeur ?ed . ?ed rdfs:label ?nom . " +
                "} GROUP BY ?nom ORDER BY DESC(?total) LIMIT 1");
        List<Map<String, String>> resultats = executerSelect(pss);
        if (resultats.isEmpty()) {
            throw new ResourceNotFoundException("Aucun editeur trouve dans la base.");
        }
        return resultats.get(0);
    }

    // ================================================================
    // ENDPOINT 7 - Emprunts, avec filtre optionnel par nom de membre
    // (parametre : query param optionnel, gere dans le controleur)
    // ================================================================
    public List<Map<String, String>> getEmprunts(String nomMembre) {
        boolean filtrer = nomMembre != null && !nomMembre.isBlank();

        StringBuilder sb = new StringBuilder(PREFIXES);
        sb.append("SELECT ?nomMembre ?titreLivre ?date WHERE { ")
          .append("  ?emp a sl:Emprunt ; sl:effectuePar ?m ; sl:concerneExemplaire ?ex ; sl:dateEmprunt ?date . ")
          .append("  ?m foaf:name ?nomMembre . ")
          .append("  ?livre sl:aPourExemplaire ?ex ; sl:titre ?titreLivre . ");
        if (filtrer) {
            sb.append("  FILTER (?nomMembre = ?nomParam) ");
        }
        sb.append("} ORDER BY ?date");

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(sb.toString());
        if (filtrer) {
            pss.setLiteral("nomParam", nomMembre);
        }
        return executerSelect(pss);
    }
}
