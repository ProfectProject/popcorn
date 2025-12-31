package com.popcorn.demo.domain.merch.service;

import com.popcorn.demo.domain.merch.dto.MerchItemResponse;
import com.popcorn.demo.domain.merch.dto.MerchListResponse;
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
}
