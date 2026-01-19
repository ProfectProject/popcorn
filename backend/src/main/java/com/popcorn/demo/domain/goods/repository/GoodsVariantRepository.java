package com.popcorn.demo.domain.goods.repository;

import com.popcorn.demo.domain.goods.entity.GoodsVariant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GoodsVariantRepository extends JpaRepository<GoodsVariant, UUID> {
    List<GoodsVariant> findAllByPopupIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID popupId);
    List<GoodsVariant> findAllByPopupIdAndIsActiveTrueAndDeletedAtIsNullOrderByCreatedAtDesc(UUID popupId);

    Optional<GoodsVariant> findByIdAndPopupIdAndDeletedAtIsNull(UUID id, UUID popupId);
}
