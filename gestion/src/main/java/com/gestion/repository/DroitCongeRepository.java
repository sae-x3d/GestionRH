package com.gestion.repository;

import com.gestion.model.DroitConge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DroitCongeRepository extends JpaRepository<DroitConge, Integer> {

    List<DroitConge> findByTypeCongeIdTypeCongeOrderByAncienneteMinMoisAsc(Integer idTypeConge);

    List<DroitConge> findAllByOrderByAncienneteMinMoisAsc();

    Optional<DroitConge> findFirstByTypeCongeIdTypeCongeAndAncienneteMinMoisLessThanEqualOrderByAncienneteMinMoisDesc(
            Integer idTypeConge, Integer ancienneteMois);
}
