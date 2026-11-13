package com.gestion.repository;

import com.gestion.enums.StatutAbsence;
import com.gestion.model.Absence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Collection;

public interface AbsenceRepository extends JpaRepository<Absence, Integer> {
    List<Absence> findByEmployeIdEmpOrderByDateDebutDesc(Integer idEmp);
    List<Absence> findByStatutOrderByDateDebutAsc(StatutAbsence statut);
    boolean existsByEmployeIdEmpAndStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
            Integer idEmp, StatutAbsence statut, LocalDate fin, LocalDate debut);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT a.employe.idEmp FROM Absence a WHERE a.employe.departement.idDept = :idDept AND a.statut IN :statuts AND a.dateDebut <= :jour AND a.dateFin >= :jour")
    List<Integer> employesAbsentsEquipe(@org.springframework.data.repository.query.Param("idDept") Integer idDept,
                                        @org.springframework.data.repository.query.Param("jour") LocalDate jour,
                                        @org.springframework.data.repository.query.Param("statuts") Collection<StatutAbsence> statuts);
}
