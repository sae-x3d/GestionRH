package com.gestion.repository;

import com.gestion.model.SoldeConge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SoldeCongeRepository extends JpaRepository<SoldeConge, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SoldeConge s WHERE s.employe.idEmp = :idEmp AND s.typeConge.idTypeConge = :idType ORDER BY s.dateExpiration ASC, s.dateAcquisition ASC")
    List<SoldeConge> verrouillerLotsFifo(@Param("idEmp") Integer idEmp, @Param("idType") Integer idType);

    List<SoldeConge> findByEmployeIdEmpAndTypeCongeIdTypeCongeOrderByDateExpirationAscDateAcquisitionAsc(
            Integer idEmp, Integer idTypeConge);

    List<SoldeConge> findByEmployeIdEmpOrderByDateExpirationAscDateAcquisitionAsc(Integer idEmp);

    List<SoldeConge> findByDateExpirationBeforeAndQuantiteRestanteGreaterThan(
            LocalDate date, BigDecimal minimum);

    @Query("""
            SELECT COALESCE(SUM(s.quantiteRestante), 0) FROM SoldeConge s
            WHERE s.employe.idEmp = :idEmp
              AND s.typeConge.idTypeConge = :idType
              AND s.quantiteRestante > 0
              AND (s.dateExpiration IS NULL OR s.dateExpiration >= :aujourdHui)
            """)
    BigDecimal totalDisponible(@Param("idEmp") Integer idEmp, @Param("idType") Integer idType,
                               @Param("aujourdHui") LocalDate aujourdHui);

    boolean existsByEmployeIdEmpAndTypeCongeIdTypeCongeAndDateAcquisitionBetween(
            Integer idEmp, Integer idTypeConge, LocalDate debut, LocalDate fin);
}
