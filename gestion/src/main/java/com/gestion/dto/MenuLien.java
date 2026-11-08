package com.gestion.dto;

public record MenuLien(String libelle, String url, boolean actif) {

    public MenuLien(String libelle, String url) {
        this(libelle, url, false);
    }

    public MenuLien avecActif(boolean actif) {
        return new MenuLien(libelle, url, actif);
    }
}
