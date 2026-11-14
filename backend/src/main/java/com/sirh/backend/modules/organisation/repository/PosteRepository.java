package com.sirh.backend.modules.organisation.repository;

import java.util.List;
import com.sirh.backend.modules.organisation.entity.PosteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PosteRepository extends JpaRepository<PosteEntity, Integer> {
    List<PosteEntity> findAllByDepartementIdAndActifTrueOrderByIntitule(Integer departementId);
}
