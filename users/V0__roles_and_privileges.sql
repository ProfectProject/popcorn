-- Logical DB separation for MSA domains (single physical PostgreSQL DB)
-- Port 5320 is an infrastructure setting; no SQL change needed here.

-- 0) Lock down public schema to prevent accidental cross-domain coupling.
REVOKE ALL ON SCHEMA public FROM PUBLIC;

-- 1) Domain roles (app + migrator)
DO $$
BEGIN
  CREATE ROLE user_auth_migrator LOGIN PASSWORD '${USER_AUTH_MIGRATOR_PASSWORD}';
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$
BEGIN
  CREATE ROLE user_auth_app LOGIN PASSWORD '${USER_AUTH_APP_PASSWORD}';
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$
BEGIN
  CREATE ROLE store_migrator LOGIN PASSWORD '${STORE_MIGRATOR_PASSWORD}';
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$
BEGIN
  CREATE ROLE store_app LOGIN PASSWORD '${STORE_APP_PASSWORD}';
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$
BEGIN
  CREATE ROLE order_migrator LOGIN PASSWORD '${ORDER_MIGRATOR_PASSWORD}';
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$
BEGIN
  CREATE ROLE order_app LOGIN PASSWORD '${ORDER_APP_PASSWORD}';
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$
BEGIN
  CREATE ROLE payment_migrator LOGIN PASSWORD '${PAYMENT_MIGRATOR_PASSWORD}';
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$
BEGIN
  CREATE ROLE payment_app LOGIN PASSWORD '${PAYMENT_APP_PASSWORD}';
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$
BEGIN
  CREATE ROLE qr_migrator LOGIN PASSWORD '${QR_MIGRATOR_PASSWORD}';
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$
BEGIN
  CREATE ROLE qr_app LOGIN PASSWORD '${QR_APP_PASSWORD}';
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$
BEGIN
  CREATE ROLE order_query_migrator LOGIN PASSWORD '${ORDER_QUERY_MIGRATOR_PASSWORD}';
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$
BEGIN
  CREATE ROLE order_query_app LOGIN PASSWORD '${ORDER_QUERY_APP_PASSWORD}';
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- 2) Schemas (owned by migrators)
CREATE SCHEMA IF NOT EXISTS user_auth AUTHORIZATION user_auth_migrator;
CREATE SCHEMA IF NOT EXISTS store AUTHORIZATION store_migrator;
CREATE SCHEMA IF NOT EXISTS "order" AUTHORIZATION order_migrator;
CREATE SCHEMA IF NOT EXISTS payment AUTHORIZATION payment_migrator;
CREATE SCHEMA IF NOT EXISTS qr AUTHORIZATION qr_migrator;
CREATE SCHEMA IF NOT EXISTS order_query AUTHORIZATION order_query_migrator;

-- 3) App role isolation (search_path + usage)
GRANT USAGE ON SCHEMA user_auth TO user_auth_app;
ALTER ROLE user_auth_app SET search_path = user_auth;

GRANT USAGE ON SCHEMA store TO store_app;
ALTER ROLE store_app SET search_path = store;

GRANT USAGE ON SCHEMA "order" TO order_app;
ALTER ROLE order_app SET search_path = "order";

GRANT USAGE ON SCHEMA payment TO payment_app;
ALTER ROLE payment_app SET search_path = payment;

GRANT USAGE ON SCHEMA qr TO qr_app;
ALTER ROLE qr_app SET search_path = qr;

GRANT USAGE ON SCHEMA order_query TO order_query_app;
ALTER ROLE order_query_app SET search_path = order_query;

-- 4) Existing object privileges (CRUD only inside own schema)
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA user_auth TO user_auth_app;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA user_auth TO user_auth_app;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA store TO store_app;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA store TO store_app;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA "order" TO order_app;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA "order" TO order_app;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA payment TO payment_app;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA payment TO payment_app;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA qr TO qr_app;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA qr TO qr_app;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA order_query TO order_query_app;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA order_query TO order_query_app;

-- 5) Default privileges for future objects (migrator creates; app uses)
ALTER DEFAULT PRIVILEGES FOR ROLE user_auth_migrator IN SCHEMA user_auth
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO user_auth_app;
ALTER DEFAULT PRIVILEGES FOR ROLE user_auth_migrator IN SCHEMA user_auth
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO user_auth_app;

ALTER DEFAULT PRIVILEGES FOR ROLE store_migrator IN SCHEMA store
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO store_app;
ALTER DEFAULT PRIVILEGES FOR ROLE store_migrator IN SCHEMA store
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO store_app;

ALTER DEFAULT PRIVILEGES FOR ROLE order_migrator IN SCHEMA "order"
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO order_app;
ALTER DEFAULT PRIVILEGES FOR ROLE order_migrator IN SCHEMA "order"
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO order_app;

ALTER DEFAULT PRIVILEGES FOR ROLE payment_migrator IN SCHEMA payment
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO payment_app;
ALTER DEFAULT PRIVILEGES FOR ROLE payment_migrator IN SCHEMA payment
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO payment_app;

ALTER DEFAULT PRIVILEGES FOR ROLE qr_migrator IN SCHEMA qr
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO qr_app;
ALTER DEFAULT PRIVILEGES FOR ROLE qr_migrator IN SCHEMA qr
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO qr_app;

ALTER DEFAULT PRIVILEGES FOR ROLE order_query_migrator IN SCHEMA order_query
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO order_query_app;
ALTER DEFAULT PRIVILEGES FOR ROLE order_query_migrator IN SCHEMA order_query
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO order_query_app;

-- 6) Optional verification (run after login as each app role)
-- Example: should fail with permission denied
-- SELECT * FROM "order".orders;