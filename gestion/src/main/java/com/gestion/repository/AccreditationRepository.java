package com.gestion.repository;

import com.gestion.model.Accreditation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccreditationRepository extends JpaRepository<Accreditation, Integer> {

    List<Accreditation> findByUtilisateurIdUserOrderByDateObtentionDesc(Integer idUser);

    List<Accreditation> findByCandidatureIdCandidatureOrderByDateObtentionDesc(Integer idCandidature);

    List<Accreditation> findByStatutOrderByDateObtentionDesc(String statut);

    List<Accreditation> findAllByOrderByDateCreationDesc();

    @Query("""
            select a from Accreditation a
            where a.utilisateur.idUser = :idUser
              and a.statut = 'VALIDEE'
              and a.dateObtention <= :date
              and (a.dateExpiration is null or a.dateExpiration >= :date)
            order by a.dateObtention desc
            """)
    List<Accreditation> trouverValides(@Param("idUser") Integer idUser, @Param("date") LocalDate date);

    @Query("""
            select a from Accreditation a
            where a.candidature.idCandidature = :idCandidature
              and a.statut = 'VALIDEE'
              and a.dateObtention <= :date
              and (a.dateExpiration is null or a.dateExpiration >= :date)
            order by a.dateObtention desc
            """)
    List<Accreditation> trouverValidesParCandidature(@Param("idCandidature") Integer idCandidature,
                                                     @Param("date") LocalDate date);

    Optional<Accreditation> findFirstByCandidatureIdCandidatureOrderByDateObtentionDesc(Integer idCandidature);
}
