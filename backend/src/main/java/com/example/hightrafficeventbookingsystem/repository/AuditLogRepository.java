package com.example.hightrafficeventbookingsystem.repository;

import com.example.hightrafficeventbookingsystem.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
