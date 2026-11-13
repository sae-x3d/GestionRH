package com.gestion.repository;

import com.gestion.model.ChecklistSortie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChecklistSortieRepository extends JpaRepository<ChecklistSortie, Integer> {

    List<ChecklistSortie> findBySortieIdSortieOrderByResponsableAscLibelleAsc(Integer idSortie);

    boolean existsBySortieIdSortie(Integer idSortie);
}
