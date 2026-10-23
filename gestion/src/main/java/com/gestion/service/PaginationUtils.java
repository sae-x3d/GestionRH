package com.gestion.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;

public class PaginationUtils {

    public static <T> Page<T> paginerListe(List<T> listeComplete, int page, int size) {
        if (listeComplete == null) {
            listeComplete = new ArrayList<>();
        }
        if (size < 1) {
            size = 10;
        }

        int totalElements = listeComplete.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        if (totalPages == 0) {
            totalPages = 1;
        }
        if (page >= totalPages) {
            page = totalPages - 1;
        }
        if (page < 0) {
            page = 0;
        }

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);

        return new PageImpl<>(listeComplete.subList(fromIndex, toIndex),
                PageRequest.of(page, size), totalElements);
    }
}
