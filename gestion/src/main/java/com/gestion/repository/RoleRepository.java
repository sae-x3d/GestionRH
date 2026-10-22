package com.gestion.repository;

import com.gestion.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    List<Role> findAllByOrderByNomRoleAsc();
}
