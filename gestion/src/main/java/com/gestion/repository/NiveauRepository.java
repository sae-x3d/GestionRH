package com.gestion.repository;

import com.gestion.model.Niveau;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NiveauRepository extends JpaRepository<Niveau, Integer> {

    List<Niveau> findAllByOrderByIdNiveauAsc();
}
