package com.example.demo.repository;

import com.example.demo.entity.EvCharger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvChargerRepository extends JpaRepository<EvCharger, Long> {

    List<EvCharger> findByLocationIdAndStatus(Long locationId, String status);
}
