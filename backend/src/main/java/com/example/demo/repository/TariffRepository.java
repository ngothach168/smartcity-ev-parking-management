package com.example.demo.repository;

import com.example.demo.entity.Tariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TariffRepository extends JpaRepository<Tariff, Long> {

    // Giữ nguyên tên hàm cũ để SessionService không phải sửa, nhưng chỉ trả khung đang hiệu lực
    @Query("SELECT t FROM Tariff t WHERE t.active = true ORDER BY t.serviceType ASC, t.startTime ASC")
    List<Tariff> findAllByOrderByServiceTypeAscStartTimeAsc();

    @Query("SELECT t FROM Tariff t WHERE t.serviceType = :serviceType AND t.active = true ORDER BY t.startTime ASC")
    List<Tariff> findByServiceTypeOrderByStartTimeAsc(@Param("serviceType") String serviceType);

    @Query(value = "SELECT * FROM TARIFFS WHERE id = :id AND is_active = 1 FOR UPDATE NOWAIT", nativeQuery = true)
    Optional<Tariff> findByIdForUpdate(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Tariff t SET t.active = false WHERE t.serviceType = :type AND t.active = true")
    int deactivateByServiceType(@Param("type") String type);
}