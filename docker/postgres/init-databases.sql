-- Script to initialize multiple databases in the PostgreSQL container
SELECT 'CREATE DATABASE siga_ipt_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'siga_ipt_db')\gexec

SELECT 'CREATE DATABASE keycloak_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'keycloak_db')\gexec
