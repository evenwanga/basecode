-- ==========================================
-- V5: 添加租户类型字段和平台租户初始化
-- TDD 6.1: Tenant 类型支持
-- ==========================================

-- 1. 添加租户类型字段
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS type VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER';

-- 2. 创建类型索引（加速按类型查询）
CREATE INDEX IF NOT EXISTS idx_tenants_type ON tenants(type);

-- 3. 插入平台租户（系统内置，code 为 __platform__）
INSERT INTO tenants (id, code, name, type, status, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    '__platform__',
    '平台',
    'PLATFORM',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (code) DO NOTHING;

-- 4. 为平台租户创建根组织
INSERT INTO organization_units (id, tenant_id, parent_id, name, status, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    NULL,
    'Root',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT DO NOTHING;




