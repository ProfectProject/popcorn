package com.popcorn.demo.domain.merch.repository;

import com.popcorn.demo.domain.merch.entity.MerchVariant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchVariantRepository extends JpaRepository<MerchVariant, UUID> {
    List<MerchVariant> findAllByProductIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID productId);

    Optional<MerchVariant> findByIdAndProductIdAndDeletedAtIsNull(UUID id, UUID productId);
}
