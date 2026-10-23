package com.gestion.repository;

import com.gestion.model.Parcours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParcoursRepository extends JpaRepository<Parcours, Integer> {

    List<Parcours> findAllByOrderByTypeAsc();
}
