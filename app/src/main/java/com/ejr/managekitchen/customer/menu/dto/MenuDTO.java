package com.ejr.managekitchen.customer.menu.dto;

public class MenuDTO {
    private Long id;
    private String nom;
    private String description;
    private Double prix;
    private String imageUrl;

    public MenuDTO(Long id, String nom, String description, Double prix, String imageUrl) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.imageUrl = imageUrl;
    }

    // Getters
    public Long getId() { return id; }
    public String getNom() { return nom; }
    public String getDescription() { return description; }
    public Double getPrix() { return prix; }
    public String getImageUrl() { return imageUrl; }
}