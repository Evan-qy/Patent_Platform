package org.ihebut.patent.patent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Data
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 64)
    private String username;

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Column(name = "role_name", length = 64)
    private String roleName;

    @Column(name = "event_type", length = 32)
    private String eventType;

    @Column(nullable = false, length = 128)
    private String action;

    @Column(name = "resource_type", length = 64)
    private String resourceType;

    @Column(name = "resource_id", length = 128)
    private String resourceId;

    @Column(columnDefinition = "text")
    private String detail;

    @Column(name = "request_method", length = 16)
    private String requestMethod;

    @Column(name = "request_path", length = 255)
    private String requestPath;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "mac_address", length = 64)
    private String macAddress;

    @Column(name = "host_name", length = 128)
    private String hostName;

    @Column(length = 128)
    private String location;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "platform_type", length = 32)
    private String platformType;

    @Column(name = "operation_result", length = 16)
    private String operationResult;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
