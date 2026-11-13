package com.gestion.repository;

import com.gestion.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {
    Optional<PasswordResetToken> findByTokenHashAndDateUtilisationIsNull(String tokenHash);
    void deleteByUtilisateurIdUser(Integer idUser);
}
