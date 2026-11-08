package com.gestion.dto;

import java.util.List;

public record MenuSection(String titre, List<MenuLien> liens) {
}
