package com.smartlibrary.smartlibrary.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires de LibraryService : verifient le comportement des
 * requetes SPARQL (y compris celles qui dependent de l'inference RDFS)
 * sur le jeu de donnees reel (smartlibrary-data.ttl).
 */
@SpringBootTest
class LibraryServiceTest {

    @Autowired
    private LibraryService libraryService;

    @Test
    void getTousLesDocuments_retourneLes6DocumentsTriesParAnneeDesc() {
        List<Map<String, String>> documents = libraryService.getTousLesDocuments();

        assertThat(documents).hasSize(6);
        // Le plus recent (la revue, 2024) doit etre en premier
        assertThat(documents.get(0).get("titre")).isEqualTo("Sciences & Avenir - Numéro Spécial");
        // Le plus ancien (Les Misérables, 1862) doit etre en dernier
        assertThat(documents.get(documents.size() - 1).get("titre")).isEqualTo("Les Misérables");
    }

    @Test
    void getLivresParAuteur_fonctionneGraceAInference() {
        // Les donnees ne declarent que sl:aEcritRoman (pas sl:aPourAuteur).
        // Sans inference RDFS (subPropertyOf), ce resultat serait vide.
        List<Map<String, String>> livres = libraryService.getLivresParAuteur("Antoine de Saint-Exupéry");

        assertThat(livres).hasSize(1);
        assertThat(livres.get(0).get("titre")).isEqualTo("Le Petit Prince");
    }

    @Test
    void getLivresParAuteur_plusieursLivresMemeAuteur() {
        List<Map<String, String>> livres = libraryService.getLivresParAuteur("Albert Camus");

        assertThat(livres).hasSize(2);
        assertThat(livres)
                .extracting(l -> l.get("titre"))
                .containsExactlyInAnyOrder("L'Étranger", "La Peste");
    }

    @Test
    void getLivresParAuteur_auteurInconnu_leveResourceNotFoundException() {
        assertThatThrownBy(() -> libraryService.getLivresParAuteur("Auteur Inexistant"))
                .isInstanceOf(com.smartlibrary.smartlibrary.exception.ResourceNotFoundException.class);
    }

    @Test
    void getLivresParGenre_genreJeunesse_fonctionneGraceAInferenceDeRange() {
        // "Jeunesse" n'a pas de "a sl:Genre" explicite : son type Genre est
        // deduit par la regle rdfs:range sur sl:aPourGenre.
        List<Map<String, String>> livres = libraryService.getLivresParGenre("Jeunesse");

        assertThat(livres).hasSize(1);
        assertThat(livres.get(0).get("titre")).isEqualTo("Le Petit Prince");
    }

    @Test
    void getLivresParGenre_genreInconnu_leveResourceNotFoundException() {
        assertThatThrownBy(() -> libraryService.getLivresParGenre("Genre Inexistant"))
                .isInstanceOf(com.smartlibrary.smartlibrary.exception.ResourceNotFoundException.class);
    }

    @Test
    void getExemplairesDisponibles_neRetourneQueLesNonEmpruntes() {
        List<Map<String, String>> disponibles = libraryService.getExemplairesDisponibles();

        assertThat(disponibles).hasSize(1);
        assertThat(disponibles.get(0).get("code")).isEqualTo("PE-001");
    }

    @Test
    void getMembresMultiEmprunts_avecMinimumDeDeux() {
        List<Map<String, String>> membres = libraryService.getMembresMultiEmprunts(2);

        assertThat(membres).hasSize(2);
        assertThat(membres)
                .extracting(m -> m.get("nom"))
                .containsExactlyInAnyOrder("Molka Ben Salah", "Ahmed Trabelsi");
    }

    @Test
    void getMembresMultiEmprunts_seuilTropHaut_neRetourneRien() {
        List<Map<String, String>> membres = libraryService.getMembresMultiEmprunts(3);

        assertThat(membres).isEmpty();
    }

    @Test
    void getEditeurTop_retourneGallimardAvecTroisDocuments() {
        Map<String, String> top = libraryService.getEditeurTop();

        assertThat(top.get("nom")).isEqualTo("Gallimard");
        assertThat(top.get("total")).isEqualTo("3");
    }

    @Test
    void getEmprunts_sansFiltre_retourneLesCinqEmprunts() {
        List<Map<String, String>> emprunts = libraryService.getEmprunts(null);

        assertThat(emprunts).hasSize(5);
    }

    @Test
    void getEmprunts_filtreParMembre_neRetourneQueSesEmprunts() {
        List<Map<String, String>> emprunts = libraryService.getEmprunts("Molka Ben Salah");

        assertThat(emprunts).hasSize(2);
        assertThat(emprunts)
                .extracting(e -> e.get("titreLivre"))
                .containsExactlyInAnyOrder("Le Petit Prince", "Fondation");
    }
}