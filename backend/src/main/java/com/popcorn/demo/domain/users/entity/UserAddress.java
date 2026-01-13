package com.popcorn.demo.domain.users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Data;


import lombok.ToString;
import lombok.EqualsAndHashCode;


import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.popcorn.demo.common.entity.BaseEntity;

@Entity
@Table(name = "p_customer_addresses")
@Getter
@Setter
@EqualsAndHashCode(callSuper=false)
public class UserAddress extends BaseEntity {

    @Id
	@UuidGenerator
	@Column(name = "addr_id")
	private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @Column(name = "addr_name", length = 50)
    private String addrName;   // 집, 회사

    @Column(nullable = false, length = 255)  // 실제 주소지 적음
    private String address1;

    @Column(name = "address2", length = 100)
    private String address2;   // 동, 호수

    @Column(length = 10)
    private String postalCode;   // 우편번호

    @Column(nullable = false)
    private Boolean isDefault = false;   // 기본 배송지 여부(false로 기본설정)

}
