package com.gestion.repository;

import com.gestion.model.PosteParDept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PosteParDeptRepository extends JpaRepository<PosteParDept, Integer> {

    List<PosteParDept> findAllByOrderByIdAsc();
}
