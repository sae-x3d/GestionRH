package com.gestion.repository;

import com.gestion.model.Poste;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PosteRepository extends JpaRepository<Poste, Integer> {

    List<Poste> findAllByOrderByNomPosteAsc();
}
