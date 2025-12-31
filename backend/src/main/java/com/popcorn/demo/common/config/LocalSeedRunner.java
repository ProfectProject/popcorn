package com.popcorn.demo.common.config;

import jakarta.persistence.EntityManager;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
@Profile("local")
public class LocalSeedRunner implements ApplicationRunner {
	private final EntityManager entityManager;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		seedProducts();
		seedProductSessions();
		seedSessionOptions();
		seedMerchVariants();

		log.info("LocalSeedRunner completed local JPA seeding.");
	}

	private void seedProducts() {
		entityManager.createNativeQuery(
				"INSERT INTO p_products (id) VALUES (1), (2), (3) " +
				"ON CONFLICT (id) DO NOTHING")
			.executeUpdate();
	}

	private void seedProductSessions() {
		entityManager.createNativeQuery(
				"INSERT INTO p_product_sessions (product_id, start_at, end_at, status, created_at, updated_at) " +
				"SELECT v.product_id, v.start_at, v.end_at, v.status, NOW(), NOW() " +
				"FROM (VALUES " +
				"(1, NOW() - INTERVAL '1 day', NOW() + INTERVAL '7 days', 'ACTIVE'), " +
				"(2, NOW() - INTERVAL '2 days', NOW() + INTERVAL '5 days', 'ACTIVE'), " +
				"(3, NOW() - INTERVAL '3 days', NOW() + INTERVAL '3 days', 'ACTIVE')" +
				") v(product_id, start_at, end_at, status) " +
				"WHERE NOT EXISTS (" +
				"SELECT 1 FROM p_product_sessions s WHERE s.product_id = v.product_id" +
				")")
			.executeUpdate();
	}

	private void seedSessionOptions() {
		entityManager.createNativeQuery(
				"INSERT INTO p_session_options " +
				"(session_id, name, price, capacity, remaining, is_hidden, created_at, updated_at) " +
				"SELECT v.session_id, v.name, v.price, v.capacity, v.remaining, FALSE, NOW(), NOW() " +
				"FROM (VALUES " +
				"(1, 'Local Session Option A', 1000, 100, 100), " +
				"(1, 'Local Session Option B', 1500, 100, 100), " +
				"(2, 'Local Session Option C', 1200, 80, 80), " +
				"(3, 'Local Session Option D', 1800, 60, 60)" +
				") v(session_id, name, price, capacity, remaining) " +
				"WHERE NOT EXISTS (" +
				"SELECT 1 FROM p_session_options so " +
				"WHERE so.session_id = v.session_id AND so.name = v.name" +
				")")
			.executeUpdate();
	}

	private void seedMerchVariants() {
		entityManager.createNativeQuery(
				"INSERT INTO p_merch_variants " +
				"(product_id, sku, name, price, stock, is_hidden, created_at, updated_at) " +
				"SELECT v.product_id, v.sku, v.name, v.price, v.stock, FALSE, NOW(), NOW() " +
				"FROM (VALUES " +
				"(1, 'LOCAL-POPCORN-01', 'Local Popcorn (S)', 3500, 100), " +
				"(1, 'LOCAL-POPCORN-02', 'Local Popcorn (M)', 4500, 100), " +
				"(2, 'LOCAL-GOODS-01', 'Local Goods Pack', 12000, 50), " +
				"(3, 'LOCAL-TICKET-01', 'Local Movie Ticket', 15000, 200)" +
				") v(product_id, sku, name, price, stock) " +
				"WHERE NOT EXISTS (" +
				"SELECT 1 FROM p_merch_variants mv WHERE mv.sku = v.sku" +
				")")
			.executeUpdate();
	}
}
