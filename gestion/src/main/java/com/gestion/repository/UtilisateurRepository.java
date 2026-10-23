package com.gestion.repository;

import com.gestion.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Utilisateur u left join fetch u.employe where u.idUser = :id")
    Optional<Utilisateur> verrouillerParId(@Param("id") Integer id);

    @Query("""
            SELECT u FROM Utilisateur u
            JOIN FETCH u.role
            WHERE u.username = :username
            """)
    Optional<Utilisateur> findByUsernameWithRole(@Param("username") String username);
}
