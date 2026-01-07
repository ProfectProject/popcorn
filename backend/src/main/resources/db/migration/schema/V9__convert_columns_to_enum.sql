-- varchar 컬럼을 enum 타입으로 복구합니다.
DO $$ BEGIN
    CREATE TYPE user_role AS ENUM ('CUSTOMER', 'OWNER', 'MANAGER');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE store_status AS ENUM ('DRAFT', 'PENDING', 'ACTIVE', 'SUSPENDED', 'CLOSED', 'HIDDEN');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE popup_status AS ENUM ('DRAFT', 'REQUEST', 'APPROVED', 'OPEN', 'CLOSED', 'CANCELLED', 'HIDDEN');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE popup_category AS ENUM ('FOOD','IDOL','EXHIBITION','WORKSHOP','FASHION','BEAUTY','LIFESTYLE','ART','GAME','TECH','SPORTS','BOOK','PET','ETC');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE order_status AS ENUM ('REQUESTED','ACCEPTED','REJECTED','RESERVED','PAYMENT_PENDING','PAID','COMPLETED','CANCELLED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE payment_method AS ENUM ('CARD','TRANSFER','EASY_PAY');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE payment_status AS ENUM ('READY','PAID','FAILED','CANCELLED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

ALTER TABLE p_users
    ALTER COLUMN role TYPE user_role USING role::user_role;

ALTER TABLE p_stores
    ALTER COLUMN status TYPE store_status USING status::store_status;

ALTER TABLE p_orders
    ALTER COLUMN status TYPE order_status USING status::order_status;

ALTER TABLE p_order_status_histories
    ALTER COLUMN from_status TYPE order_status USING from_status::order_status,
    ALTER COLUMN to_status TYPE order_status USING to_status::order_status;

ALTER TABLE p_payments
    ALTER COLUMN method TYPE payment_method USING method::payment_method,
    ALTER COLUMN status TYPE payment_status USING status::payment_status;

ALTER TABLE p_popups
    ALTER COLUMN category TYPE popup_category USING category::popup_category,
    ALTER COLUMN status TYPE popup_status USING status::popup_status;
