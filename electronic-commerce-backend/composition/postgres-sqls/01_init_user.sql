DO $$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'kc_admin') THEN
            CREATE USER kc_admin WITH PASSWORD 'kc_123_admin';
        END IF;
    END
$$;


DO $$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'jzargo') THEN
            CREATE USER jzargo WITH PASSWORD 'jzargo123';
        END IF;
    END
$$;

