-- 添加组织单元排序字段
ALTER TABLE organization_units ADD COLUMN IF NOT EXISTS sort_order INTEGER DEFAULT 0;

-- 创建索引优化查询
CREATE INDEX IF NOT EXISTS idx_organization_units_parent_sort ON organization_units(parent_id, sort_order);
