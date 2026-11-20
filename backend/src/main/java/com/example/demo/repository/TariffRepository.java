package com.example.demo.repository;

import com.example.demo.entity.Tariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TariffRepository extends JpaRepository<Tariff, Long> {

    List<Tariff> findAllByOrderByServiceTypeAscStartTimeAsc();

    List<Tariff> findByServiceTypeOrderByStartTimeAsc(String serviceType);

    @Query(value = "SELECT * FROM TARIFFS WHERE id = :id FOR UPDATE NOWAIT", nativeQuery = true)
    Optional<Tariff> findByIdForUpdate(@Param("id") Long id);
}