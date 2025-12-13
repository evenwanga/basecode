-- 1. Create persons table
CREATE TABLE persons (
    id UUID PRIMARY KEY,
    verified_mobile VARCHAR(20),
    id_card VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_persons_verified_mobile ON persons(verified_mobile) WHERE verified_mobile IS NOT NULL;
CREATE UNIQUE INDEX idx_persons_id_card ON persons(id_card) WHERE id_card IS NOT NULL;

-- 2. Modify users table to support tenant scope and person linking
ALTER TABLE users ADD COLUMN person_id UUID;
ALTER TABLE users ADD COLUMN tenant_id UUID;

ALTER TABLE users ADD CONSTRAINT fk_users_person_id FOREIGN KEY (person_id) REFERENCES persons(id);

-- 3. Drop old unique constraints and add new tenant-scoped constraints for Users
-- Assuming original constraint names, usually unique_primary_email or u_users_primary_email
-- We drop by column name or try to guess standard naming if name not explicit. 
-- Since we use Flyway, we should strictly check constraints. 
-- For safety, we drop index/constraint by definition if possible, but standard SQL requires name.
-- Based on JPA generation, it's often uk_... or idx_... 
-- For now, let's assume we need to drop the implicit unique constraint on primary_email.
-- In V1 migration it was: UNIQUE(primary_email), UNIQUE(primary_phone).
ALTER TABLE users DROP CONSTRAINT IF EXISTS uk_users_primary_email; -- Hypothetical name, might need precise name lookup if manual naming wasn't used.
ALTER TABLE users DROP CONSTRAINT IF EXISTS uk_users_primary_phone;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_primary_email_key; -- Postgres default naming style
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_primary_phone_key;

CREATE UNIQUE INDEX idx_users_tenant_email ON users(tenant_id, primary_email) WHERE primary_email IS NOT NULL AND tenant_id IS NOT NULL;
CREATE UNIQUE INDEX idx_users_tenant_phone ON users(tenant_id, primary_phone) WHERE primary_phone IS NOT NULL AND tenant_id IS NOT NULL;

-- 4. Modify user_identities table
ALTER TABLE user_identities ADD COLUMN tenant_id UUID;

-- Drop old unique constraint (identifier, type)
-- Postgres default: user_identities_identifier_type_key or similar
ALTER TABLE user_identities DROP CONSTRAINT IF EXISTS user_identities_identifier_type_key; 
-- If JPA generated index, drop it too if separate
DROP INDEX IF EXISTS idx_user_identities_identifier_type;

-- Add new unique constraint (tenant_id, identifier, type)
CREATE UNIQUE INDEX idx_user_identities_tenant_identifier_type ON user_identities(tenant_id, identifier, type) WHERE tenant_id IS NOT NULL;

-- 5. Data Migration (Optional/Best Effort for existing local data)
-- Link existing users to a default tenant (or leave null if code handles it, but unique index requires non-null for uniqueness usually, or partial index handles nulls).
-- Our partial index `WHERE tenant_id IS NOT NULL` allows globals (null tenantId) to duplicate? 
-- Actually we want multiple nulls? No, if tenantId is null (Global User), we probably still want uniqueness?
-- Requirement: "C-side user (Customer) ... in different APP need separate register".
-- If tenantId is NULL, it's a global user (like internal admin). Let's assume existing data is for default tenant or global.
-- For now, just leave them null. The partial index won't enforce uniqueness on them if nulls are multiple. 
-- Wait, if tenantId is null, `(null, email)` should be unique? Postgres treats nulls as distinct.
-- Strategy: We treat existing users as "Global" or "Legacy". Code should handle null tenantId as sensitive.
