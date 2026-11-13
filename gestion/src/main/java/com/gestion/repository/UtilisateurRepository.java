package com.gestion.repository;

import com.gestion.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
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

    @Query("""
            SELECT u FROM Utilisateur u
            JOIN FETCH u.role r
            WHERE UPPER(r.nomRole) IN ('RH', 'ADMINISTRATEUR', 'ROLE_RH', 'ROLE_ADMINISTRATEUR')
            """)
    List<Utilisateur> trouverGestionnairesCandidatures();

    @Query("""
            SELECT u FROM Utilisateur u
            JOIN FETCH u.role r
            LEFT JOIN FETCH u.employe e
            LEFT JOIN FETCH e.departement
            WHERE UPPER(r.nomRole) IN :roles
            """)
    List<Utilisateur> trouverParRoles(@Param("roles") List<String> roles);

    @Query("""
            SELECT u FROM Utilisateur u
            JOIN FETCH u.role r
            JOIN FETCH u.employe e
            JOIN FETCH e.departement d
            WHERE UPPER(r.nomRole) IN ('MANAGER', 'ROLE_MANAGER')
              AND d.idDept = :idDepartement
            """)
    List<Utilisateur> trouverManagersParDepartement(@Param("idDepartement") Integer idDepartement);

    @Query("""
            SELECT COUNT(u) > 0 FROM Utilisateur u
            JOIN u.role r
            JOIN u.employe e
            JOIN e.departement d
            WHERE UPPER(r.nomRole) IN ('MANAGER', 'ROLE_MANAGER')
              AND d.idDept = :idDepartement
            """)
    boolean existsManagerPourDepartement(@Param("idDepartement") Integer idDepartement);

    Optional<Utilisateur> findByEmployeIdEmp(Integer idEmp);

    Optional<Utilisateur> findFirstByEmailIgnoreCaseAndActifTrue(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    @Query("select u.securityVersion from Utilisateur u where u.idUser = :id")
    Optional<Integer> findSecurityVersionById(@Param("id") Integer id);
}
