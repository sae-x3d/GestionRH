package com.gestion.service;

import com.gestion.model.Utilisateur;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class EmailServiceTests {
    @Test void emailSimuleParDefautSansContacterLeRelais() {
        @SuppressWarnings("unchecked") ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        JavaMailSender sender = mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        EmailService service = new EmailService(provider);
        Utilisateur destinataire = Utilisateur.builder().email("test@example.org").build();

        service.envoyer(destinataire, "Test", "Message de test");

        verify(provider, never()).getIfAvailable();
        verify(sender, never()).send(any(SimpleMailMessage.class));
    }

    @Test void emailActifEnvoieUnMessageAuRelaisConfigure() {
        @SuppressWarnings("unchecked") ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        JavaMailSender sender = mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        EmailService service = new EmailService(provider);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "expediteur", "rh@example.org");

        service.envoyer(Utilisateur.builder().email("test@example.org").build(), "Test", "Message");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertEquals("test@example.org", captor.getValue().getTo()[0]);
        assertEquals("rh@example.org", captor.getValue().getFrom());
        assertEquals("Test", captor.getValue().getSubject());
    }
}
