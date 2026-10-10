-- DevTrack PostgreSQL Development Database Initialization
-- This script ensures required extensions are present and creates persistent storage for Keycloak.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create dedicated database for persistent Keycloak storage
CREATE DATABASE keycloak_db;
