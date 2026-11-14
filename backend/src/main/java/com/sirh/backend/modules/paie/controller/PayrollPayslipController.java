package com.sirh.backend.modules.paie.controller;

import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.paie.service.PayrollPayslipService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PayrollPayslipController {
    private final PayrollPayslipService service;
    public PayrollPayslipController(PayrollPayslipService service) { this.service = service; }

    @GetMapping("/api/employe/paie/bulletins")
    public List<Map<String, Object>> own(@AuthenticationPrincipal AuthUserPrincipal user) {
        return service.ownBulletins(user.getId());
    }

    @GetMapping("/api/employe/paie/bulletins/{id}/pdf")
    public ResponseEntity<byte[]> ownPdf(@AuthenticationPrincipal AuthUserPrincipal user, @PathVariable Integer id) {
        return pdf(service.ownPdf(user.getId(), id), "bulletin-paie-" + id + ".pdf");
    }

    @GetMapping("/api/rh/paie/bulletins")
    public List<Map<String, Object>> rh(@AuthenticationPrincipal AuthUserPrincipal user,
            @RequestParam(required = false) Integer periodeId) {
        return service.rhBulletins(user.getId(), periodeId);
    }

    @GetMapping("/api/rh/paie/bulletins/{id}/pdf")
    public ResponseEntity<byte[]> rhPdf(@AuthenticationPrincipal AuthUserPrincipal user, @PathVariable Integer id) {
        return pdf(service.rhPdf(user.getId(), id), "bulletin-paie-" + id + ".pdf");
    }

    private static ResponseEntity<byte[]> pdf(byte[] document, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setCacheControl("private, no-store");
        return ResponseEntity.ok().headers(headers).body(document);
    }
}
