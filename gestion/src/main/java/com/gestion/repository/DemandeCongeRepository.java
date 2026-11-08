package com.gestion.repository;

import com.gestion.enums.StatutDemandeConge;
import com.gestion.model.DemandeConge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface DemandeCongeRepository extends JpaRepository<DemandeConge, Integer> {

    List<DemandeConge> findByEmployeIdEmpOrderByDateCreationDesc(Integer idEmp);

    List<DemandeConge> findByStatutOrderByDateDebutAsc(StatutDemandeConge statut);

    List<DemandeConge> findByStatutOrderByDateSoumissionAscIdDemandeCongeAsc(StatutDemandeConge statut);

    List<DemandeConge> findByStatutInAndEmployeDepartementIdDeptOrderByDateDebutAsc(
            Collection<StatutDemandeConge> statuts, Integer idDept);

    List<DemandeConge> findByStatutInAndEmployeDepartementIdDeptOrderByDateSoumissionAscIdDemandeCongeAsc(
            Collection<StatutDemandeConge> statuts, Integer idDept);

    List<DemandeConge> findByStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqualOrderByDateDebutAsc(
            StatutDemandeConge statut, LocalDate fin, LocalDate debut);

    @Query("""
            SELECT COUNT(d) > 0 FROM DemandeConge d
            WHERE d.employe.idEmp = :idEmp
              AND d.statut IN :statuts
              AND d.dateDebut <= :fin
              AND d.dateFin >= :debut
            """)
    boolean existsChevauchement(@Param("idEmp") Integer idEmp,
                                @Param("debut") LocalDate debut,
                                @Param("fin") LocalDate fin,
                                @Param("statuts") Collection<StatutDemandeConge> statuts);

    boolean existsByEmployeIdEmpAndStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
            Integer idEmp, StatutDemandeConge statut, LocalDate fin, LocalDate debut);

    @Query("SELECT DISTINCT d.employe.idEmp FROM DemandeConge d WHERE d.employe.departement.idDept = :idDept AND d.statut IN :statuts AND d.dateDebut <= :jour AND d.dateFin >= :jour")
    List<Integer> employesAbsentsEquipe(@Param("idDept") Integer idDept, @Param("jour") LocalDate jour,
                                        @Param("statuts") Collection<StatutDemandeConge> statuts);
}
