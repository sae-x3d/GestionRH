package com.gestion.service;

import com.gestion.model.Role;
import com.gestion.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public List<Role> listerTous() {
        return roleRepository.findAllByOrderByNomRoleAsc();
    }
}
