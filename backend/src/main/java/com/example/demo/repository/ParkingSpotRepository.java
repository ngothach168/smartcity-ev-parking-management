package com.example.demo.repository;

import com.example.demo.entity.ParkingSpot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {

    List<ParkingSpot> findByLocationId(Long locationId);

    List<ParkingSpot> findByLocationIdAndStatus(Long locationId, String status);

    boolean existsBySpotCode(String spotCode);

    boolean existsBySpotCodeAndIdNot(String spotCode, Long id);

    @Query(value = "SELECT * FROM PARKING_SPOTS WHERE id = :id FOR UPDATE NOWAIT", nativeQuery = true)
    Optional<ParkingSpot> findByIdForUpdate(@Param("id") Long id);
}