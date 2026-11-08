package com.gestion.service;

import com.gestion.model.Employe;
import com.gestion.model.Utilisateur;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    @Value("${app.mail.from:no-reply@gestionrh.local}")
    private String expediteur;

    public void envoyer(Utilisateur destinataire, String sujet, String corps) {
        envoyer(destinataire, sujet, corps, false);
    }

    public void envoyer(Utilisateur destinataire, String sujet, String corps, boolean contenuSensible) {
        String adresse = adresse(destinataire);
        if (adresse == null) {
            return;
        }
        if (!enabled) {
            log.info("[EMAIL SIMULE] a={} sujet={} corps={}", adresse, sujet,
                    contenuSensible ? "[contenu sensible masque]" : corps);
            return;
        }
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.warn("Aucun JavaMailSender configure, email non envoye a {}", adresse);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(expediteur);
            message.setTo(adresse);
            message.setSubject(sujet);
            message.setText(corps);
            sender.send(message);
        } catch (Exception ex) {
            log.warn("Echec de l'envoi de l'email a {} : {}", adresse, ex.getMessage());
        }
    }

    private String adresse(Utilisateur destinataire) {
        if (destinataire == null) {
            return null;
        }
        if (destinataire.getEmail() != null && !destinataire.getEmail().isBlank()) {
            return destinataire.getEmail().trim();
        }
        Employe employe = destinataire.getEmploye();
        if (employe != null && employe.getEmail() != null && !employe.getEmail().isBlank()) {
            return employe.getEmail().trim();
        }
        return null;
    }
}
