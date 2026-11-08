package com.gestion.repository;

import com.gestion.model.ConsommationConge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsommationCongeRepository extends JpaRepository<ConsommationConge, Integer> {

    List<ConsommationConge> findByDemandeCongeIdDemandeConge(Integer idDemandeConge);

    void deleteByDemandeCongeIdDemandeConge(Integer idDemandeConge);
}
