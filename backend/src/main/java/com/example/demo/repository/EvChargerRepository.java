package com.example.demo.repository;

import com.example.demo.entity.EvCharger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvChargerRepository extends JpaRepository<EvCharger, Long> {

    List<EvCharger> findByLocationId(Long locationId);

    List<EvCharger> findByLocationIdAndStatus(Long locationId, String status);

    boolean existsByChargerCode(String chargerCode);

    boolean existsByChargerCodeAndIdNot(String chargerCode, Long id);

    @Query(value = "SELECT * FROM EV_CHARGERS WHERE id = :id FOR UPDATE NOWAIT", nativeQuery = true)
    Optional<EvCharger> findByIdForUpdate(@Param("id") Long id);
}