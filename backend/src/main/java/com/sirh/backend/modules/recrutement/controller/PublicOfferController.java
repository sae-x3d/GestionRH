package com.sirh.backend.modules.recrutement.controller;

import java.util.List;
import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.recrutement.dto.PublicOfferResponse;
import com.sirh.backend.modules.recrutement.dto.StaffOfferResponse;
import com.sirh.backend.modules.recrutement.service.PublicOfferService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/offres")
public class PublicOfferController {
    private final PublicOfferService offers;

    public PublicOfferController(PublicOfferService offers) { this.offers = offers; }

    @GetMapping
    public List<PublicOfferResponse> list() { return offers.list(); }

    @GetMapping("/gestion")
    public List<StaffOfferResponse> staffList(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return offers.listForStaff(principal);
    }

    @GetMapping("/{id}")
    public PublicOfferResponse get(@PathVariable Integer id) { return offers.get(id); }
}
