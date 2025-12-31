package com.popcorn.demo.domain.merch.service;

import com.popcorn.demo.domain.merch.dto.MerchItemResponse;
import com.popcorn.demo.domain.merch.dto.MerchListResponse;
import com.popcorn.demo.domain.merch.dto.MerchCreateRequest;
import com.popcorn.demo.domain.merch.dto.MerchIdResponse;
import com.popcorn.demo.domain.merch.repository.MerchVariantRepository;
import com.popcorn.demo.domain.merch.entity.MerchVariant;
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
}
