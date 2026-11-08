package com.gestion.service;

import com.gestion.model.Utilisateur;
import com.gestion.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordChangeServiceTests {
    private final UtilisateurRepository repository = mock(UtilisateurRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final PasswordChangeService service = new PasswordChangeService(repository, encoder);

    @Test void modifieLeMotDePasseEtIncrementeLaVersionDesSessions() {
        Utilisateur user = Utilisateur.builder().idUser(4).passwordHash("ancien-hash").build();
        when(repository.verrouillerParId(4)).thenReturn(Optional.of(user));
        when(encoder.matches("Ancien123", "ancien-hash")).thenReturn(true);
        when(encoder.matches("Nouveau123", "ancien-hash")).thenReturn(false);
        when(encoder.encode("Nouveau123")).thenReturn("nouveau-hash");

        service.changer(4, "Ancien123", "Nouveau123");

        assertEquals("nouveau-hash", user.getPasswordHash());
        assertEquals(1, user.getSecurityVersion());
        verify(repository).save(user);
    }

    @Test void refuseUnAncienMotDePasseIncorrect() {
        Utilisateur user = Utilisateur.builder().idUser(4).passwordHash("ancien-hash").build();
        when(repository.verrouillerParId(4)).thenReturn(Optional.of(user));
        when(encoder.matches("faux", "ancien-hash")).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> service.changer(4, "faux", "Nouveau123"));
        verify(repository, never()).save(any());
    }
}
