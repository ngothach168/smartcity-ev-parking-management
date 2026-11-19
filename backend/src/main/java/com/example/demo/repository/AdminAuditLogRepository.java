package com.example.demo.repository;

import com.example.demo.entity.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    @Query("SELECT a FROM AdminAuditLog a JOIN FETCH a.admin ORDER BY a.createdAt DESC")
    List<AdminAuditLog> findAllWithAdminOrderByCreatedAtDesc();
}