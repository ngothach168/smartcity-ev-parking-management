package com.example.demo.repository;

import com.example.demo.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    @Query(value = "SELECT * FROM SESSIONS WHERE id = :id FOR UPDATE NOWAIT", nativeQuery = true)
    Optional<Session> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT s FROM Session s
            JOIN FETCH s.user
            LEFT JOIN FETCH s.parkingSpot ps
            LEFT JOIN FETCH ps.location
            LEFT JOIN FETCH s.evCharger ec
            LEFT JOIN FETCH ec.location
            WHERE s.id = :id
            """)
    Optional<Session> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT s FROM Session s
            JOIN FETCH s.user
            LEFT JOIN FETCH s.parkingSpot ps
            LEFT JOIN FETCH ps.location
            LEFT JOIN FETCH s.evCharger ec
            LEFT JOIN FETCH ec.location
            WHERE s.user.id = :userId
            ORDER BY s.reservedAt DESC
            """)
    List<Session> findByUserIdWithDetails(@Param("userId") Long userId);

    @Query("""
            SELECT s FROM Session s
            JOIN FETCH s.user
            LEFT JOIN FETCH s.parkingSpot ps
            LEFT JOIN FETCH ps.location
            LEFT JOIN FETCH s.evCharger ec
            LEFT JOIN FETCH ec.location
            ORDER BY s.reservedAt DESC
            """)
    List<Session> findAllWithDetails();

    @Query("""
            SELECT s FROM Session s
            JOIN FETCH s.user
            LEFT JOIN FETCH s.parkingSpot ps
            LEFT JOIN FETCH ps.location
            LEFT JOIN FETCH s.evCharger ec
            LEFT JOIN FETCH ec.location
            WHERE s.status = :status
            ORDER BY s.reservedAt DESC
            """)
    List<Session> findByStatusWithDetails(@Param("status") String status);
}