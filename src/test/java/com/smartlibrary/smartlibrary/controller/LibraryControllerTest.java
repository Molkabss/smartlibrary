package com.smartlibrary.smartlibrary.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'integration REST : un test par endpoint, qui verifie le code HTTP
 * et le contenu JSON reellement renvoye par l'API (contrairement a
 * LibraryServiceTest qui teste directement la couche service en Java).
 */
@SpringBootTest
@AutoConfigureMockMvc
class LibraryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // ----------------------------------------------------------------
    // GET /api/documents
    // ----------------------------------------------------------------
    @Test
    void getDocuments_retourne200EtLes6Documents() throws Exception {
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].titre").value("Sciences & Avenir - Numéro Spécial"));
    }

    // ----------------------------------------------------------------
    // GET /api/auteurs/{nom}/livres
    // ----------------------------------------------------------------
    @Test
    void getLivresParAuteur_auteurConnu_retourne200EtSesLivres() throws Exception {
        mockMvc.perform(get("/api/auteurs/{nom}/livres", "Antoine de Saint-Exupéry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titre").value("Le Petit Prince"));
    }

    @Test
    void getLivresParAuteur_auteurInconnu_retourne404() throws Exception {
        mockMvc.perform(get("/api/auteurs/{nom}/livres", "Auteur Inexistant"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    // ----------------------------------------------------------------
    // GET /api/genres/{nom}/livres
    // ----------------------------------------------------------------
    @Test
    void getLivresParGenre_genreConnu_retourne200EtSesLivres() throws Exception {
        mockMvc.perform(get("/api/genres/{nom}/livres", "Roman"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getLivresParGenre_genreInconnu_retourne404() throws Exception {
        mockMvc.perform(get("/api/genres/{nom}/livres", "Genre Inexistant"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ----------------------------------------------------------------
    // GET /api/exemplaires/disponibles
    // ----------------------------------------------------------------
    @Test
    void getExemplairesDisponibles_retourne200EtPE001() throws Exception {
        mockMvc.perform(get("/api/exemplaires/disponibles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("PE-001"));
    }

    // ----------------------------------------------------------------
    // GET /api/membres/multi-emprunts
    // ----------------------------------------------------------------
    @Test
    void getMembresMultiEmprunts_avecMinimumPardefaut_retourne200Et2Membres() throws Exception {
        mockMvc.perform(get("/api/membres/multi-emprunts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ----------------------------------------------------------------
    // GET /api/editeurs/top
    // ----------------------------------------------------------------
    @Test
    void getEditeurTop_retourne200EtGallimard() throws Exception {
        mockMvc.perform(get("/api/editeurs/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Gallimard"))
                .andExpect(jsonPath("$.total").value("3"));
    }

    // ----------------------------------------------------------------
    // GET /api/emprunts
    // ----------------------------------------------------------------
    @Test
    void getEmprunts_sansFiltre_retourne200EtLes5Emprunts() throws Exception {
        mockMvc.perform(get("/api/emprunts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void getEmprunts_filtreParMembre_retourne200EtSesEmprunts() throws Exception {
        mockMvc.perform(get("/api/emprunts").param("membre", "Molka Ben Salah"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}