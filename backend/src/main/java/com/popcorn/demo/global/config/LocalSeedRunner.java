package com.popcorn.demo.global.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
@Profile("local")
public class LocalSeedRunner implements ApplicationRunner {
	private final JdbcTemplate jdbcTemplate;
	private static final String STORE_ID_1 = "00000000-0000-0000-0000-000000000001";
	private static final String STORE_ID_10 = "00000000-0000-0000-0000-000000000010";
	private static final String PRODUCT_ID_1 = "00000000-0000-0000-0000-000000000101";
	private static final String PRODUCT_ID_2 = "00000000-0000-0000-0000-000000000102";
	private static final String PRODUCT_ID_3 = "00000000-0000-0000-0000-000000000103";
	private static final String PRODUCT_ID_55 = "00000000-0000-0000-0000-000000000155";
	private static final String SESSION_ID_1 = "00000000-0000-0000-0000-000000000201";
	private static final String SESSION_ID_2 = "00000000-0000-0000-0000-000000000202";
	private static final String SESSION_ID_3 = "00000000-0000-0000-0000-000000000203";
	private static final String SESSION_ID_777 = "00000000-0000-0000-0000-000000000777";
	private static final String OPTION_ID_1 = "00000000-0000-0000-0000-000000000301";
	private static final String OPTION_ID_2 = "00000000-0000-0000-0000-000000000302";
	private static final String OPTION_ID_3 = "00000000-0000-0000-0000-000000000303";
	private static final String OPTION_ID_4 = "00000000-0000-0000-0000-000000000304";
	private static final String OPTION_ID_88 = "00000000-0000-0000-0000-000000000388";
	private static final String ORDER_ID_1001 = "00000000-0000-0000-0000-000000001001";
	private static final String MERCH_VARIANT_ID_401 = "00000000-0000-0000-0000-000000000401";
	private static final String MERCH_VARIANT_ID_402 = "00000000-0000-0000-0000-000000000402";
	private static final String MERCH_VARIANT_ID_403 = "00000000-0000-0000-0000-000000000403";
	private static final String MERCH_VARIANT_ID_404 = "00000000-0000-0000-0000-000000000404";

	@Override
	@Transactional(transactionManager = "jdbcTransactionManager")
	public void run(ApplicationArguments args) {
		seedUsers();
		seedStores();
		seedProducts();
		seedProductSessions();
		seedSessionOptions();
		seedMerchVariants();
		seedTestOrder();

		log.info("LocalSeedRunner completed local JDBC seeding.");
	}

	private boolean seedProducts() {
		String insertSql = buildProductsInsertSql();
		if (insertSql == null) {
			log.warn("Skipping product seeding due to unresolved required columns.");
			return false;
		}
		jdbcTemplate.update(insertSql);
		return true;
	}

	private boolean seedStores() {
		String insertSql = buildStoresInsertSql();
		if (insertSql == null) {
			log.warn("Skipping store seeding due to unresolved required columns.");
			return false;
		}
		jdbcTemplate.update(insertSql);
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
		jdbcTemplate.update(insertSql);
		return true;
	}

	private void seedProductSessions() {
		syncSequence("p_product_sessions", "id");
		boolean sessionIdIsUuid = isUuidColumn("p_product_sessions", "id");
		boolean productIdIsUuid = isUuidColumn("p_product_sessions", "product_id");
		jdbcTemplate.update(
				"INSERT INTO p_product_sessions (id, product_id, start_at, end_at, status, created_at, updated_at) " +
				"SELECT v.id, v.product_id, v.start_at, v.end_at, v.status, NOW(), NOW() " +
				"FROM (VALUES " +
				"(" + formatIdLiteral(sessionIdIsUuid, "1", SESSION_ID_1) + ", " +
				formatIdLiteral(productIdIsUuid, "1", PRODUCT_ID_1) + ", NOW() - INTERVAL '1 day', NOW() + INTERVAL '7 days', 'OPEN'::session_status), " +
				"(" + formatIdLiteral(sessionIdIsUuid, "2", SESSION_ID_2) + ", " +
				formatIdLiteral(productIdIsUuid, "2", PRODUCT_ID_2) + ", NOW() - INTERVAL '2 days', NOW() + INTERVAL '5 days', 'OPEN'::session_status), " +
				"(" + formatIdLiteral(sessionIdIsUuid, "3", SESSION_ID_3) + ", " +
				formatIdLiteral(productIdIsUuid, "3", PRODUCT_ID_3) + ", NOW() - INTERVAL '3 days', NOW() + INTERVAL '3 days', 'OPEN'::session_status), " +
				"(" + formatIdLiteral(sessionIdIsUuid, "777", SESSION_ID_777) + ", " +
				formatIdLiteral(productIdIsUuid, "55", PRODUCT_ID_55) + ", NOW() - INTERVAL '1 day', NOW() + INTERVAL '7 days', 'OPEN'::session_status)" +
				") v(id, product_id, start_at, end_at, status) " +
				"WHERE EXISTS (" +
				"SELECT 1 FROM p_products p WHERE p.id = v.product_id" +
				") AND NOT EXISTS (" +
				"SELECT 1 FROM p_product_sessions s WHERE s.id = v.id" +
				")");
	}

	private void seedSessionOptions() {
		syncSequence("p_session_options", "id");
		boolean optionIdIsUuid = isUuidColumn("p_session_options", "id");
		boolean sessionIdIsUuid = isUuidColumn("p_session_options", "session_id");
		jdbcTemplate.update(
				"INSERT INTO p_session_options " +
				"(id, session_id, name, price, capacity, remaining, is_hidden, created_at, updated_at) " +
				"SELECT v.id, v.session_id, v.name, v.price, v.capacity, v.remaining, FALSE, NOW(), NOW() " +
				"FROM (VALUES " +
				"(" + formatIdLiteral(optionIdIsUuid, "1", OPTION_ID_1) + ", " +
				formatIdLiteral(sessionIdIsUuid, "1", SESSION_ID_1) + ", 'Local Session Option A', 1000, 100, 100), " +
				"(" + formatIdLiteral(optionIdIsUuid, "2", OPTION_ID_2) + ", " +
				formatIdLiteral(sessionIdIsUuid, "1", SESSION_ID_1) + ", 'Local Session Option B', 1500, 100, 100), " +
				"(" + formatIdLiteral(optionIdIsUuid, "3", OPTION_ID_3) + ", " +
				formatIdLiteral(sessionIdIsUuid, "2", SESSION_ID_2) + ", 'Local Session Option C', 1200, 80, 80), " +
				"(" + formatIdLiteral(optionIdIsUuid, "4", OPTION_ID_4) + ", " +
				formatIdLiteral(sessionIdIsUuid, "3", SESSION_ID_3) + ", 'Local Session Option D', 1800, 60, 60), " +
				"(" + formatIdLiteral(optionIdIsUuid, "88", OPTION_ID_88) + ", " +
				formatIdLiteral(sessionIdIsUuid, "777", SESSION_ID_777) + ", 'Local Session Option X', 2000, 100, 100)" +
				") v(id, session_id, name, price, capacity, remaining) " +
				"WHERE EXISTS (" +
				"SELECT 1 FROM p_product_sessions s WHERE s.id = v.session_id" +
				") AND NOT EXISTS (" +
				"SELECT 1 FROM p_session_options so WHERE so.id = v.id" +
				")");
	}

	private void seedMerchVariants() {
		syncSequence("p_merch_variants", "id");
		boolean merchVariantIdIsUuid = isUuidColumn("p_merch_variants", "id");
		boolean productIdIsUuid = isUuidColumn("p_merch_variants", "product_id");
		jdbcTemplate.update(
				"INSERT INTO p_merch_variants " +
				"(id, product_id, sku, name, price, stock, is_hidden, created_at, updated_at) " +
				"SELECT v.id, v.product_id, v.sku, v.name, v.price, v.stock, FALSE, NOW(), NOW() " +
				"FROM (VALUES " +
				"(" + formatIdLiteral(merchVariantIdIsUuid, "401", MERCH_VARIANT_ID_401) + ", " +
				formatIdLiteral(productIdIsUuid, "1", PRODUCT_ID_1) + ", 'LOCAL-POPCORN-01', 'Local Popcorn (S)', 3500, 100), " +
				"(" + formatIdLiteral(merchVariantIdIsUuid, "402", MERCH_VARIANT_ID_402) + ", " +
				formatIdLiteral(productIdIsUuid, "1", PRODUCT_ID_1) + ", 'LOCAL-POPCORN-02', 'Local Popcorn (M)', 4500, 100), " +
				"(" + formatIdLiteral(merchVariantIdIsUuid, "403", MERCH_VARIANT_ID_403) + ", " +
				formatIdLiteral(productIdIsUuid, "2", PRODUCT_ID_2) + ", 'LOCAL-GOODS-01', 'Local Goods Pack', 12000, 50), " +
				"(" + formatIdLiteral(merchVariantIdIsUuid, "404", MERCH_VARIANT_ID_404) + ", " +
				formatIdLiteral(productIdIsUuid, "3", PRODUCT_ID_3) + ", 'LOCAL-TICKET-01', 'Local Movie Ticket', 15000, 200)" +
				") v(id, product_id, sku, name, price, stock) " +
				"WHERE EXISTS (" +
				"SELECT 1 FROM p_products p WHERE p.id = v.product_id" +
				") AND NOT EXISTS (" +
				"SELECT 1 FROM p_merch_variants mv WHERE mv.id = v.id" +
				")");
	}

	private void seedTestOrder() {
		syncSequence("p_orders", "id");
		boolean orderIdIsUuid = isUuidColumn("p_orders", "id");
		boolean storeIdIsUuid = isUuidColumn("p_orders", "store_id");
		boolean productIdIsUuid = isUuidColumn("p_orders", "product_id");
		jdbcTemplate.update(
				"INSERT INTO p_orders " +
				"(id, order_no, customer_id, store_id, product_id, order_type, status, cancelable_until, total_amount, created_at, updated_at, idempotency_key, version) " +
				"SELECT " + formatIdLiteral(orderIdIsUuid, "1001", ORDER_ID_1001) +
				", 'O20251231-001001', 1, " +
				formatIdLiteral(storeIdIsUuid, "10", STORE_ID_10) + ", " +
				formatIdLiteral(productIdIsUuid, "55", PRODUCT_ID_55) +
				", 'RESERVATION', 'REQUESTED', NOW() + INTERVAL '1 day', 2000, NOW(), NOW(), NULL, 0 " +
				"WHERE NOT EXISTS (" +
				"SELECT 1 FROM p_orders o WHERE o.id = " + formatIdLiteral(orderIdIsUuid, "1001", ORDER_ID_1001) +
				")");
	}

	private String buildProductsInsertSql() {
		List<Map<String, Object>> requiredColumns = jdbcTemplate.queryForList(
				"SELECT column_name, data_type, udt_name " +
				"FROM information_schema.columns " +
				"WHERE table_name = 'p_products' " +
				"AND is_nullable = 'NO' " +
				"AND column_default IS NULL " +
				"AND column_name <> 'id'"
		);

		boolean productIdIsUuid = isUuidColumn("p_products", "id");
		boolean storeIdIsUuid = isUuidColumn("p_products", "store_id");
		boolean includeStoreId = false;
		for (Map<String, Object> row : requiredColumns) {
			String columnName = String.valueOf(row.get("column_name"));
			if ("store_id".equals(columnName)) {
				includeStoreId = true;
				break;
			}
		}
		if (requiredColumns.isEmpty()) {
			return "INSERT INTO p_products (id) VALUES (" +
					formatIdLiteral(productIdIsUuid, "1", PRODUCT_ID_1) + "), (" +
					formatIdLiteral(productIdIsUuid, "2", PRODUCT_ID_2) + "), (" +
					formatIdLiteral(productIdIsUuid, "3", PRODUCT_ID_3) + "), (" +
					formatIdLiteral(productIdIsUuid, "55", PRODUCT_ID_55) + ") " +
					"ON CONFLICT (id) DO NOTHING";
		}

		StringBuilder columns = new StringBuilder();
		StringBuilder values = new StringBuilder();
		for (Map<String, Object> row : requiredColumns) {
			String columnName = String.valueOf(row.get("column_name"));
			String dataType = String.valueOf(row.get("data_type"));
			String udtName = String.valueOf(row.get("udt_name"));
			if ("store_id".equals(columnName)) {
				continue;
			}
			String expression = resolveSeedExpression(dataType, udtName);
			if (expression == null) {
				log.warn("Unsupported required column for p_products seeding: {}", columnName);
				return null;
			}
			columns.append(", ").append(columnName);
			values.append(", ").append(expression);
		}

		if (includeStoreId) {
			columns.append(", store_id");
			values.append(", v.store_id");
		}

		return "INSERT INTO p_products (id" + columns + ") " +
				"SELECT v.id" + values +
				" FROM (VALUES (" +
				formatIdLiteral(productIdIsUuid, "1", PRODUCT_ID_1) + ", " +
				formatIdLiteral(storeIdIsUuid, "1", STORE_ID_1) + "), (" +
				formatIdLiteral(productIdIsUuid, "2", PRODUCT_ID_2) + ", " +
				formatIdLiteral(storeIdIsUuid, "1", STORE_ID_1) + "), (" +
				formatIdLiteral(productIdIsUuid, "3", PRODUCT_ID_3) + ", " +
				formatIdLiteral(storeIdIsUuid, "1", STORE_ID_1) + "), (" +
				formatIdLiteral(productIdIsUuid, "55", PRODUCT_ID_55) + ", " +
				formatIdLiteral(storeIdIsUuid, "10", STORE_ID_10) + ")) v(id, store_id) " +
				"WHERE NOT EXISTS (" +
				"SELECT 1 FROM p_products p WHERE p.id = v.id" +
				")";
	}

	private String buildStoresInsertSql() {
		List<Map<String, Object>> requiredColumns = jdbcTemplate.queryForList(
				"SELECT column_name, data_type, udt_name " +
				"FROM information_schema.columns " +
				"WHERE table_name = 'p_stores' " +
				"AND is_nullable = 'NO' " +
				"AND column_default IS NULL " +
				"AND column_name <> 'id'"
		);

		boolean storeIdIsUuid = isUuidColumn("p_stores", "id");
		if (requiredColumns.isEmpty()) {
			return "INSERT INTO p_stores (id) VALUES (" +
					formatIdLiteral(storeIdIsUuid, "1", STORE_ID_1) + "), (" +
					formatIdLiteral(storeIdIsUuid, "10", STORE_ID_10) + ") " +
					"ON CONFLICT (id) DO NOTHING";
		}

		StringBuilder columns = new StringBuilder();
		StringBuilder values = new StringBuilder();
		for (Map<String, Object> row : requiredColumns) {
			String columnName = String.valueOf(row.get("column_name"));
			String dataType = String.valueOf(row.get("data_type"));
			String udtName = String.valueOf(row.get("udt_name"));
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
				" FROM (VALUES (" + formatIdLiteral(storeIdIsUuid, "1", STORE_ID_1) + "), (" +
				formatIdLiteral(storeIdIsUuid, "10", STORE_ID_10) + ")) v(id) " +
				"WHERE NOT EXISTS (" +
				"SELECT 1 FROM p_stores s WHERE s.id = v.id" +
				")";
	}

	private String resolveUserTableName() {
		List<Map<String, Object>> result = jdbcTemplate.queryForList(
				"SELECT table_name " +
				"FROM information_schema.tables " +
				"WHERE table_name IN ('p_user', 'p_users') " +
				"ORDER BY table_name"
		);
		if (result.isEmpty()) {
			return null;
		}
		return String.valueOf(result.get(0).get("table_name"));
	}

	private String buildUsersInsertSql(String tableName) {
		List<Map<String, Object>> requiredColumns = jdbcTemplate.queryForList(
				"SELECT column_name, data_type, udt_name " +
				"FROM information_schema.columns " +
				"WHERE table_name = ? " +
				"AND is_nullable = 'NO' " +
				"AND column_default IS NULL " +
				"AND column_name <> 'id'",
				tableName
		);

		if (requiredColumns.isEmpty()) {
			return "INSERT INTO " + tableName + " (id) VALUES (1) " +
					"ON CONFLICT (id) DO NOTHING";
		}

		StringBuilder columns = new StringBuilder();
		StringBuilder values = new StringBuilder();
		for (Map<String, Object> row : requiredColumns) {
			String columnName = String.valueOf(row.get("column_name"));
			String dataType = String.valueOf(row.get("data_type"));
			String udtName = String.valueOf(row.get("udt_name"));
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
		if ("integer".equals(dataType) || "bigint".equals(dataType) || "smallint".equals(dataType)
				|| "numeric".equals(dataType)) {
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
		List<Map<String, Object>> result = jdbcTemplate.queryForList(
				"SELECT e.enumlabel " +
				"FROM pg_enum e " +
				"JOIN pg_type t ON e.enumtypid = t.oid " +
				"WHERE t.typname = ? " +
				"ORDER BY e.enumsortorder " +
				"LIMIT 1",
				enumType
		);
		if (result.isEmpty()) {
			return null;
		}
		return String.valueOf(result.get(0).get("enumlabel"));
	}

	private void syncSequence(String tableName, String columnName) {
		// Skip sequence sync for UUID columns as MAX() function doesn't work on UUID type
		if (isUuidColumn(tableName, columnName)) {
			log.debug("Skipping sequence sync for UUID column: {}.{}", tableName, columnName);
			return;
		}

		String sql = "SELECT CASE " +
				"WHEN pg_get_serial_sequence('" + tableName + "', '" + columnName + "') IS NULL THEN NULL " +
				"ELSE setval(pg_get_serial_sequence('" + tableName + "', '" + columnName + "'), " +
				"COALESCE((SELECT MAX(" + columnName + ") FROM " + tableName + "), 1), " +
				"(SELECT MAX(" + columnName + ") FROM " + tableName + ") IS NOT NULL) " +
				"END";
		jdbcTemplate.queryForObject(sql, Object.class);
	}

	private boolean isUuidColumn(String tableName, String columnName) {
		List<Map<String, Object>> result = jdbcTemplate.queryForList(
				"SELECT data_type FROM information_schema.columns " +
				"WHERE table_name = ? AND column_name = ?",
				tableName,
				columnName
		);
		if (result.isEmpty()) {
			return false;
		}
		return "uuid".equals(String.valueOf(result.get(0).get("data_type")));
	}

	private String formatIdLiteral(boolean isUuid, String numericId, String uuidValue) {
		if (!isUuid) {
			return numericId;
		}
		return "'" + uuidValue + "'::uuid";
	}
}
