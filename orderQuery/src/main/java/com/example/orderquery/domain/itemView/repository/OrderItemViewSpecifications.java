package com.example.orderquery.domain.itemView.repository;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.example.orderquery.domain.itemView.dto.OrderItemQuery;
import com.example.orderquery.domain.itemView.entity.OrderItemView;
import com.example.orderquery.domain.itemView.entity.OrderStatus;
import com.example.orderquery.domain.itemView.entity.PaymentStatus;

import jakarta.persistence.criteria.Predicate;

public final class OrderItemViewSpecifications {

    private OrderItemViewSpecifications() {
    }

    public static Specification<OrderItemView> byStorePopupAndFilters(UUID storeId,
                                                                      UUID popupId,
                                                                      OrderItemQuery query,
                                                                      OrderStatus orderStatus,
                                                                      PaymentStatus paymentStatus) {
        return (root, cq, cb) -> {
            Predicate predicate = cb.equal(root.get("storeId"), storeId);
            predicate = cb.and(predicate, cb.equal(root.get("id").get("popupId"), popupId));

            if (orderStatus != null) {
                predicate = cb.and(predicate, cb.equal(root.get("orderStatus"), orderStatus));
            }
            if (paymentStatus != null) {
                predicate = cb.and(predicate, cb.equal(root.get("paymentStatus"), paymentStatus));
            }
            if (query.getItemType() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("itemType"), query.getItemType()));
            }
            if (query.getCheckedIn() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("checkedIn"), query.getCheckedIn()));
            }
            if (query.getFrom() != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("orderedAt"), query.getFrom()));
            }
            if (query.getTo() != null) {
                predicate = cb.and(predicate, cb.lessThan(root.get("orderedAt"), query.getTo()));
            }

            return predicate;
        };
    }
}
