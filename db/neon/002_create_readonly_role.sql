-- Role criada via SQL para evitar membro de neon_superuser.
CREATE ROLE android_lab_reader LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT;
GRANT CONNECT ON DATABASE android_lab_001 TO android_lab_reader;
GRANT USAGE ON SCHEMA public TO android_lab_reader;
GRANT SELECT ON TABLE public.notes TO android_lab_reader;
