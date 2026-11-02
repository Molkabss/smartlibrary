package com.smartlibrary.smartlibrary;

import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.util.FileManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstration de l'inference RDFS pour SmartLibrary.
 *
 * Ce programme charge le vocabulaire (schema) et les donnees (instances),
 * puis compare les resultats AVEC et SANS inference RDFS activee.
 *
 * Deux regles d'inference sont demontrees :
 *  1. rdfs:subClassOf  -> un Livre devient automatiquement un Document
 *  2. rdfs:subPropertyOf -> aEcritRoman genere automatiquement aPourAuteur
 *
 * A executer directement (clic droit -> Run 'InferenceDemo.main()').
 */
public class InferenceDemo {

    public static void main(String[] args) {

        // ------------------------------------------------------------
        // 1. Chargement du schema et des donnees
        // ------------------------------------------------------------
        String schemaPath = "src/main/resources/smartlibrary-schema.ttl";
        String dataPath   = "src/main/resources/smartlibrary-data.ttl";

        Model schema = FileManager.get().loadModel(schemaPath);
        Model data   = FileManager.get().loadModel(dataPath);

        // Modele de base : schema + donnees, SANS raisonneur.
        // C'est ce que Jena "voit" si on ne demande aucune inference.
        Model modeleSansInference = ModelFactory.createDefaultModel();
        modeleSansInference.add(schema);
        modeleSansInference.add(data);

        System.out.println("=== Nombre de triplets SANS inference : "
                + modeleSansInference.size());

        // ------------------------------------------------------------
        // 2. Creation du modele AVEC inference RDFS
        // ------------------------------------------------------------
        // Le raisonneur RDFS de Jena lit le schema (les regles subClassOf
        // et subPropertyOf qu'on y a declarees) et les applique a
        // l'ensemble schema + donnees, pour une comparaison equitable
        // avec le modele sans inference (meme contenu de depart).
        Reasoner reasoner = ReasonerRegistry.getRDFSReasoner();
        reasoner = reasoner.bindSchema(schema);
        InfModel modeleAvecInference = ModelFactory.createInfModel(reasoner, modeleSansInference);

        System.out.println("=== Nombre de triplets AVEC inference  : "
                + modeleAvecInference.size());
        System.out.println();

        // ------------------------------------------------------------
        // 3. REGLE 1 - rdfs:subClassOf
        // Verifions qu'un Livre est maintenant reconnu comme Document
        // ------------------------------------------------------------
        System.out.println("--- REGLE 1 : rdfs:subClassOf (Livre -> Document) ---");

        Resource livrePetitPrince = modeleAvecInference.getResource(
                "http://www.smartlibrary.org/resource#livre_petit_prince");
        Resource classeDocument = modeleAvecInference.getResource(
                "http://www.smartlibrary.org/ontology#Document");

        boolean estDocument = modeleAvecInference.contains(
                livrePetitPrince,
                modeleAvecInference.getProperty(
                        "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                classeDocument);

        System.out.println("'Le Petit Prince' est-il reconnu comme Document ? "
                + estDocument
                + "  (ce triplet n'existe PAS explicitement dans smartlibrary-data.ttl,"
                + " il est deduit par le raisonneur)");
        System.out.println();

        // ------------------------------------------------------------
        // 4. REGLE 2 - rdfs:subPropertyOf
        // Verifions qu'un triplet aEcritRoman genere bien aPourAuteur
        // ------------------------------------------------------------
        System.out.println("--- REGLE 2 : rdfs:subPropertyOf (aEcritRoman -> aPourAuteur) ---");

        Resource auteur = modeleAvecInference.getResource(
                "http://www.smartlibrary.org/resource#auteur_saint_exupery");
        Property aPourAuteur = modeleAvecInference.getProperty(
                "http://www.smartlibrary.org/ontology#aPourAuteur");

        boolean relationDeduite = modeleAvecInference.contains(
                livrePetitPrince, aPourAuteur, auteur);

        System.out.println("'Le Petit Prince' aPourAuteur 'Saint-Exupery' ? "
                + relationDeduite
                + "  (seul aEcritRoman a ete declare dans les donnees,"
                + " aPourAuteur est deduit)");
        System.out.println();

        // ------------------------------------------------------------
        // 5. REGLE 3 - rdfs:range
        // Le genre "Jeunesse" n'a volontairement AUCUN "a sl:Genre" explicite
        // dans smartlibrary-data.ttl. Il est seulement relie au Petit Prince
        // via sl:aPourGenre. Comme le schema declare rdfs:range sl:Genre pour
        // aPourGenre, le raisonneur doit deduire tout seul que ce genre est
        // bien une instance de sl:Genre.
        // ------------------------------------------------------------
        System.out.println("--- REGLE 3 : rdfs:range (aPourGenre -> Genre) ---");

        Resource genreJeunesse = modeleAvecInference.getResource(
                "http://www.smartlibrary.org/resource#genre_jeunesse");
        Resource classeGenre = modeleAvecInference.getResource(
                "http://www.smartlibrary.org/ontology#Genre");
        Property rdfType = modeleAvecInference.getProperty(
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type");

        boolean typeDeduitAvecInference =
                modeleAvecInference.contains(genreJeunesse, rdfType, classeGenre);
        boolean typeDeduitSansInference =
                modeleSansInference.contains(genreJeunesse, rdfType, classeGenre);

        System.out.println("'Jeunesse' est-il un sl:Genre SANS inference ? "
                + typeDeduitSansInference
                + "  (aucun 'a sl:Genre' n'est ecrit dans les donnees pour cette ressource)");
        System.out.println("'Jeunesse' est-il un sl:Genre AVEC inference ? "
                + typeDeduitAvecInference
                + "  (deduit uniquement parce que aPourGenre a pour rdfs:range sl:Genre)");
        System.out.println();

        // ------------------------------------------------------------
        // 6. COMPARAISON SPARQL - la Requete 2 avec et sans inference
        // ------------------------------------------------------------
        String requete =
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
                "PREFIX sl:   <http://www.smartlibrary.org/ontology#> " +
                "SELECT ?titreDocument ?nomAuteur WHERE { " +
                "  ?doc sl:titre ?titreDocument ; sl:aPourAuteur ?auteur . " +
                "  ?auteur foaf:name ?nomAuteur . " +
                "} ORDER BY ?titreDocument";

        System.out.println("--- COMPARAISON : Requete 2 (documents + auteur via aPourAuteur) ---");

        System.out.println("SANS inference :");
        afficherResultats(requete, modeleSansInference);

        System.out.println();
        System.out.println("AVEC inference :");
        afficherResultats(requete, modeleAvecInference);
    }

    /**
     * Execute une requete SPARQL sur un modele donne et affiche les resultats.
     */
    private static void afficherResultats(String requeteTexte, Model modele) {
        Query query = QueryFactory.create(requeteTexte);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, modele)) {
            ResultSet resultats = qexec.execSelect();
            List<String> lignes = new ArrayList<>();
            while (resultats.hasNext()) {
                var solution = resultats.nextSolution();
                lignes.add("  - " + solution.get("titreDocument") +
                        " --> " + solution.get("nomAuteur"));
            }
            if (lignes.isEmpty()) {
                System.out.println("  (aucun resultat)");
            } else {
                lignes.forEach(System.out::println);
            }
            System.out.println("  Total : " + lignes.size() + " resultat(s)");
        }
    }
}
