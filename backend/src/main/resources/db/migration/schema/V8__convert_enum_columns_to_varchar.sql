-- Enum 컬럼을 varchar로 변경해 JPA EnumType.STRING과 일치시킵니다.
ALTER TABLE p_users
    ALTER COLUMN role TYPE varchar(255) USING role::text;

ALTER TABLE p_stores
    ALTER COLUMN status TYPE varchar(255) USING status::text;

ALTER TABLE p_orders
    ALTER COLUMN status TYPE varchar(255) USING status::text;

ALTER TABLE p_order_status_histories
    ALTER COLUMN from_status TYPE varchar(255) USING from_status::text,
    ALTER COLUMN to_status TYPE varchar(255) USING to_status::text;

ALTER TABLE p_payments
    ALTER COLUMN method TYPE varchar(255) USING method::text,
    ALTER COLUMN status TYPE varchar(255) USING status::text;

ALTER TABLE p_popups
    ALTER COLUMN category TYPE varchar(255) USING category::text,
    ALTER COLUMN status TYPE varchar(255) USING status::text;
