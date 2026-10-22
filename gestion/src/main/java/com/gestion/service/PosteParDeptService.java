package com.gestion.service;

import com.gestion.model.PosteParDept;
import com.gestion.repository.PosteParDeptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PosteParDeptService {

    private final PosteParDeptRepository posteParDeptRepository;

    @Transactional(readOnly = true)
    public List<PosteParDept> listerTous() {
        return posteParDeptRepository.findAllByOrderByIdAsc();
    }
}
