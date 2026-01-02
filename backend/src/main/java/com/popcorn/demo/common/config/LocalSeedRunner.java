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
		seedUsers();
		seedStores();
		boolean productsSeeded = seedProducts();
		seedProductSessions();
		seedSessionOptions();
		seedMerchVariants();

		log.info("LocalSeedRunner completed local JPA seeding.");
	}

	private boolean seedProducts() {
		String insertSql = buildProductsInsertSql();
		if (insertSql == null) {
			log.warn("Skipping product seeding due to unresolved required columns.");
			return false;
		}
		entityManager.createNativeQuery(insertSql).executeUpdate();
		return true;
	}

	private boolean seedStores() {
		String insertSql = buildStoresInsertSql();
		if (insertSql == null) {
			log.warn("Skipping store seeding due to unresolved required columns.");
			return false;
		}
		entityManager.createNativeQuery(insertSql).executeUpdate();
		return true;
	}

	private boolean seedUsers() {
		String tableName = resolveUserTableName();
		if (tableName == null) {
			log.warn("Skipping user seeding because user table was not found.");
			return false;
		}
		String insertSql = buildUsersInsertSql(tableName);
		if (insertSql == null) {
			log.warn("Skipping user seeding due to unresolved required columns.");
			return false;
		}
		entityManager.createNativeQuery(insertSql).executeUpdate();
		return true;
	}

	private void seedProductSessions() {
		syncSequence("p_product_sessions", "id");
		entityManager.createNativeQuery(
				"INSERT INTO p_product_sessions (id, product_id, start_at, end_at, status, created_at, updated_at) " +
				"SELECT v.id, v.product_id, v.start_at, v.end_at, v.status, NOW(), NOW() " +
				"FROM (VALUES " +
				"(1, 1, NOW() - INTERVAL '1 day', NOW() + INTERVAL '7 days', 'ACTIVE'), " +
				"(2, 2, NOW() - INTERVAL '2 days', NOW() + INTERVAL '5 days', 'ACTIVE'), " +
				"(3, 3, NOW() - INTERVAL '3 days', NOW() + INTERVAL '3 days', 'ACTIVE'), " +
				"(777, 55, NOW() - INTERVAL '1 day', NOW() + INTERVAL '7 days', 'ACTIVE')" +
				") v(id, product_id, start_at, end_at, status) " +
				"WHERE EXISTS (" +
				"SELECT 1 FROM p_products p WHERE p.id = v.product_id" +
				") AND NOT EXISTS (" +
				"SELECT 1 FROM p_product_sessions s WHERE s.id = v.id" +
				")")
			.executeUpdate();
	}

	private void seedSessionOptions() {
		syncSequence("p_session_options", "id");
		entityManager.createNativeQuery(
				"INSERT INTO p_session_options " +
				"(id, session_id, name, price, capacity, remaining, is_hidden, created_at, updated_at) " +
				"SELECT v.id, v.session_id, v.name, v.price, v.capacity, v.remaining, FALSE, NOW(), NOW() " +
				"FROM (VALUES " +
				"(1, 1, 'Local Session Option A', 1000, 100, 100), " +
				"(2, 1, 'Local Session Option B', 1500, 100, 100), " +
				"(3, 2, 'Local Session Option C', 1200, 80, 80), " +
				"(4, 3, 'Local Session Option D', 1800, 60, 60), " +
				"(88, 777, 'Local Session Option X', 2000, 100, 100)" +
				") v(id, session_id, name, price, capacity, remaining) " +
				"WHERE EXISTS (" +
				"SELECT 1 FROM p_product_sessions s WHERE s.id = v.session_id" +
				") AND NOT EXISTS (" +
				"SELECT 1 FROM p_session_options so WHERE so.id = v.id" +
				")")
			.executeUpdate();
	}

	private void seedMerchVariants() {
		syncSequence("p_merch_variants", "id");
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
				"WHERE EXISTS (" +
				"SELECT 1 FROM p_products p WHERE p.id = v.product_id" +
				") AND NOT EXISTS (" +
				"SELECT 1 FROM p_merch_variants mv WHERE mv.sku = v.sku" +
				")")
			.executeUpdate();
	}

	private String buildProductsInsertSql() {
		var requiredColumns = entityManager.createNativeQuery(
				"SELECT column_name, data_type, udt_name " +
				"FROM information_schema.columns " +
				"WHERE table_name = 'p_products' " +
				"AND is_nullable = 'NO' " +
				"AND column_default IS NULL " +
				"AND column_name <> 'id'"
		).getResultList();

		if (requiredColumns.isEmpty()) {
			return "INSERT INTO p_products (id) VALUES (1), (2), (3), (55) " +
					"ON CONFLICT (id) DO NOTHING";
		}

		StringBuilder columns = new StringBuilder();
		StringBuilder values = new StringBuilder();
		for (Object rowObj : requiredColumns) {
			Object[] row = (Object[]) rowObj;
			String columnName = String.valueOf(row[0]);
			String dataType = String.valueOf(row[1]);
			String udtName = String.valueOf(row[2]);
			String expression = resolveSeedExpression(dataType, udtName);
			if (expression == null) {
				log.warn("Unsupported required column for p_products seeding: {}", columnName);
				return null;
			}
			columns.append(", ").append(columnName);
			values.append(", ").append(expression);
		}

		return "INSERT INTO p_products (id" + columns + ") " +
				"SELECT v.id" + values +
				" FROM (VALUES (1), (2), (3), (55)) v(id) " +
				"WHERE NOT EXISTS (" +
				"SELECT 1 FROM p_products p WHERE p.id = v.id" +
				")";
	}

	private String buildStoresInsertSql() {
		var requiredColumns = entityManager.createNativeQuery(
				"SELECT column_name, data_type, udt_name " +
				"FROM information_schema.columns " +
				"WHERE table_name = 'p_stores' " +
				"AND is_nullable = 'NO' " +
				"AND column_default IS NULL " +
				"AND column_name <> 'id'"
		).getResultList();

		if (requiredColumns.isEmpty()) {
			return "INSERT INTO p_stores (id) VALUES (1), (10) " +
					"ON CONFLICT (id) DO NOTHING";
		}

		StringBuilder columns = new StringBuilder();
		StringBuilder values = new StringBuilder();
		for (Object rowObj : requiredColumns) {
			Object[] row = (Object[]) rowObj;
			String columnName = String.valueOf(row[0]);
			String dataType = String.valueOf(row[1]);
			String udtName = String.valueOf(row[2]);
			String expression = resolveSeedExpression(dataType, udtName);
			if (expression == null) {
				log.warn("Unsupported required column for p_stores seeding: {}", columnName);
				return null;
			}
			columns.append(", ").append(columnName);
			values.append(", ").append(expression);
		}

		return "INSERT INTO p_stores (id" + columns + ") " +
				"SELECT v.id" + values +
				" FROM (VALUES (1), (10)) v(id) " +
				"WHERE NOT EXISTS (" +
				"SELECT 1 FROM p_stores s WHERE s.id = v.id" +
				")";
	}

	private String resolveUserTableName() {
		var result = entityManager.createNativeQuery(
				"SELECT table_name " +
				"FROM information_schema.tables " +
				"WHERE table_name IN ('p_user', 'p_users') " +
				"ORDER BY table_name"
		).getResultList();
		if (result.isEmpty()) {
			return null;
		}
		return String.valueOf(result.get(0));
	}

	private String buildUsersInsertSql(String tableName) {
		var requiredColumns = entityManager.createNativeQuery(
				"SELECT column_name, data_type, udt_name " +
				"FROM information_schema.columns " +
				"WHERE table_name = :tableName " +
				"AND is_nullable = 'NO' " +
				"AND column_default IS NULL " +
				"AND column_name <> 'id'"
		).setParameter("tableName", tableName).getResultList();

		if (requiredColumns.isEmpty()) {
			return "INSERT INTO " + tableName + " (id) VALUES (1) " +
					"ON CONFLICT (id) DO NOTHING";
		}

		StringBuilder columns = new StringBuilder();
		StringBuilder values = new StringBuilder();
		for (Object rowObj : requiredColumns) {
			Object[] row = (Object[]) rowObj;
			String columnName = String.valueOf(row[0]);
			String dataType = String.valueOf(row[1]);
			String udtName = String.valueOf(row[2]);
			String expression = resolveUserSeedExpression(columnName, dataType, udtName);
			if (expression == null) {
				log.warn("Unsupported required column for {} seeding: {}", tableName, columnName);
				return null;
			}
			columns.append(", ").append(columnName);
			values.append(", ").append(expression);
		}

		return "INSERT INTO " + tableName + " (id" + columns + ") " +
				"SELECT v.id" + values +
				" FROM (VALUES (1)) v(id) " +
				"WHERE NOT EXISTS (" +
				"SELECT 1 FROM " + tableName + " u WHERE u.id = v.id" +
				")";
	}

	private String resolveUserSeedExpression(String columnName, String dataType, String udtName) {
		if ("email".equals(columnName)) {
			return "'seed@popcorn.local'";
		}
		if ("password".equals(columnName)) {
			return "'test'";
		}
		if ("phone".equals(columnName)) {
			return "'01000000000'";
		}
		if ("name".equals(columnName)) {
			return "'Seed User'";
		}
		if ("role".equals(columnName) && "USER-DEFINED".equals(dataType)) {
			return "'USER'::" + udtName;
		}
		if ("is_active".equals(columnName)) {
			return "TRUE";
		}
		return resolveSeedExpression(dataType, udtName);
	}

	private String resolveSeedExpression(String dataType, String udtName) {
		if ("character varying".equals(dataType) || "text".equals(dataType) || "character".equals(dataType)) {
			return "'Local Product'";
		}
		if ("integer".equals(dataType) || "bigint".equals(dataType) || "smallint".equals(dataType) || "numeric".equals(dataType)) {
			return "1";
		}
		if ("boolean".equals(dataType)) {
			return "TRUE";
		}
		if (dataType != null && dataType.startsWith("timestamp")) {
			return "NOW()";
		}
		if ("date".equals(dataType)) {
			return "CURRENT_DATE";
		}
		if ("uuid".equals(dataType)) {
			return "uuid_generate_v4()";
		}
		if ("USER-DEFINED".equals(dataType)) {
			String enumLabel = resolveEnumLabel(udtName);
			if (enumLabel == null) {
				return null;
			}
			return "'" + enumLabel.replace("'", "''") + "'::" + udtName;
		}
		return null;
	}

	private String resolveEnumLabel(String enumType) {
		var result = entityManager.createNativeQuery(
				"SELECT e.enumlabel " +
				"FROM pg_enum e " +
				"JOIN pg_type t ON e.enumtypid = t.oid " +
				"WHERE t.typname = :type " +
				"ORDER BY e.enumsortorder " +
				"LIMIT 1"
		).setParameter("type", enumType).getResultList();
		if (result.isEmpty()) {
			return null;
		}
		return String.valueOf(result.get(0));
	}

	private void syncSequence(String tableName, String columnName) {
		String sql = "SELECT CASE " +
				"WHEN pg_get_serial_sequence('" + tableName + "', '" + columnName + "') IS NULL THEN NULL " +
				"ELSE setval(pg_get_serial_sequence('" + tableName + "', '" + columnName + "'), " +
				"COALESCE((SELECT MAX(" + columnName + ") FROM " + tableName + "), 1), " +
				"(SELECT MAX(" + columnName + ") FROM " + tableName + ") IS NOT NULL) " +
				"END";
		entityManager.createNativeQuery(sql).getSingleResult();
	}
}
