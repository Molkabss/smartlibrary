package com.smartlibrary.smartlibrary.controller;

import com.smartlibrary.smartlibrary.service.LibraryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Couche controleur : ne contient AUCUNE logique Jena/SPARQL, uniquement
 * le routage HTTP. Chaque methode delegue a LibraryService.
 */
@RestController
@RequestMapping("/api")
public class LibraryController {

    private final LibraryService service;

    public LibraryController(LibraryService service) {
        this.service = service;
    }

    // GET /api/documents
    @GetMapping("/documents")
    public List<Map<String, String>> tousLesDocuments() {
        return service.getTousLesDocuments();
    }

    // GET /api/auteurs/{nom}/livres
    @GetMapping("/auteurs/{nom}/livres")
    public List<Map<String, String>> livresParAuteur(@PathVariable String nom) {
        return service.getLivresParAuteur(nom);
    }

    // GET /api/genres/{nom}/livres
    @GetMapping("/genres/{nom}/livres")
    public List<Map<String, String>> livresParGenre(@PathVariable String nom) {
        return service.getLivresParGenre(nom);
    }

    // GET /api/exemplaires/disponibles
    @GetMapping("/exemplaires/disponibles")
    public List<Map<String, String>> exemplairesDisponibles() {
        return service.getExemplairesDisponibles();
    }

    // GET /api/membres/multi-emprunts?min=2  (min est optionnel, defaut = 2)
    @GetMapping("/membres/multi-emprunts")
    public List<Map<String, String>> membresMultiEmprunts(
            @RequestParam(name = "min", defaultValue = "2") int min) {
        return service.getMembresMultiEmprunts(min);
    }

    // GET /api/editeurs/top
    @GetMapping("/editeurs/top")
    public Map<String, String> editeurTop() {
        return service.getEditeurTop();
    }

    // GET /api/emprunts  ou  GET /api/emprunts?membre=Molka Ben Salah
    @GetMapping("/emprunts")
    public List<Map<String, String>> emprunts(
            @RequestParam(name = "membre", required = false) String membre) {
        return service.getEmprunts(membre);
    }
}
