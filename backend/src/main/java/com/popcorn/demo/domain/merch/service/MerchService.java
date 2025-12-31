package com.popcorn.demo.domain.merch.service;

import com.popcorn.demo.domain.merch.dto.MerchCreateRequest;
import com.popcorn.demo.domain.merch.dto.MerchIdResponse;
import com.popcorn.demo.domain.merch.dto.MerchItemResponse;
import com.popcorn.demo.domain.merch.dto.MerchListResponse;
import com.popcorn.demo.domain.merch.dto.MerchUpdateRequest;
import com.popcorn.demo.domain.merch.entity.MerchVariant;
import com.popcorn.demo.domain.merch.exception.MerchNotFoundException;
import com.popcorn.demo.domain.merch.repository.MerchVariantRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MerchService {
    private final MerchVariantRepository merchVariantRepository;

    @Transactional(readOnly = true)
    public MerchListResponse list(UUID productId) {
        List<MerchItemResponse> items = merchVariantRepository
                .findAllByProductIdAndDeletedAtIsNullOrderByCreatedAtDesc(productId)
                .stream()
                .map(MerchItemResponse::from)
                .collect(Collectors.toList());
        return new MerchListResponse(items);
    }

    @Transactional
    public MerchIdResponse create(UUID productId, MerchCreateRequest request) {
        boolean isHidden = Boolean.TRUE.equals(request.getIsHidden());
        MerchVariant merch = MerchVariant.create(
                productId,
                request.getSku(),
                request.getName(),
                request.getPrice(),
                request.getStock(),
                isHidden
        );
        merchVariantRepository.save(merch);
        return new MerchIdResponse(merch.getId());
    }

    @Transactional
    public MerchIdResponse update(UUID productId, UUID merchId, MerchUpdateRequest request) {
        MerchVariant merch = getMerch(productId, merchId);
        merch.update(
                request.getSku(),
                request.getName(),
                request.getPrice(),
                request.getStock(),
                request.getIsHidden()
        );
        return new MerchIdResponse(merch.getId());
    }

    private MerchVariant getMerch(UUID productId, UUID merchId) {
        return merchVariantRepository
                .findByIdAndProductIdAndDeletedAtIsNull(merchId, productId)
                .orElseThrow(MerchNotFoundException::new);
    }
}
