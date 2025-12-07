-- 为角色与权限增加租户维度，确保按租户隔离
ALTER TABLE roles ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

-- 绑定租户外键
ALTER TABLE roles
    DROP CONSTRAINT IF EXISTS roles_tenant_fk,
    ADD CONSTRAINT roles_tenant_fk FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE permissions
    DROP CONSTRAINT IF EXISTS permissions_tenant_fk,
    ADD CONSTRAINT permissions_tenant_fk FOREIGN KEY (tenant_id) REFERENCES tenants(id);

-- 唯一约束调整为租户内唯一
ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_code_key;
ALTER TABLE permissions DROP CONSTRAINT IF EXISTS permissions_code_key;
ALTER TABLE roles ADD CONSTRAINT uk_roles_tenant_code UNIQUE (tenant_id, code);
ALTER TABLE permissions ADD CONSTRAINT uk_perms_tenant_code UNIQUE (tenant_id, code);

-- 角色权限表可选增加索引防重复
ALTER TABLE role_permissions DROP CONSTRAINT IF EXISTS uk_role_perm;
ALTER TABLE role_permissions ADD CONSTRAINT uk_role_perm UNIQUE (role_id, permission_id);
