package com.popcorn.demo.domain.users.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.popcorn.demo.common.entity.BaseEntity;
import com.popcorn.demo.domain.users.entity.enums.UserRole;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "p_users",
    indexes = {
        @Index(name = "idx_users_email", columnList = "email")
    }
)
@Getter
@Setter
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long userId;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 11)
    private String phone;

    @Column(length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private UserRole role;

    @Column(nullable = false)
    private boolean isActive = true;


    // ---- Audit Columns ----
    @Column
    private Long createdBy;

    @Column
    private Long updatedBy;

    @Column
    private LocalDateTime deletedAt; //baseEntity에 없음

    @Column
    private Long deletedBy;


}
