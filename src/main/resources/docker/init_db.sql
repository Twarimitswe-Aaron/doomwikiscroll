-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";  -- For text search

-- Create schema
CREATE SCHEMA IF NOT EXISTS wik;

-- Set search path
ALTER DATABASE wik_history SET search_path TO wik, public;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE wik_history TO wikuser;
GRANT ALL PRIVILEGES ON SCHEMA wik TO wikuser;
GRANT ALL PRIVILEGES ON SCHEMA public TO wikuser;
