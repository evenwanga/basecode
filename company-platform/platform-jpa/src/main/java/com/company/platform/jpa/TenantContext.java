package com.company.platform.jpa;

public final class TenantContext {

    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(String tenantId) {
        TENANT.set(tenantId);
    }

    public static String getTenantId() {
        return TENANT.get();
    }

    public static String requireTenantId() {
        String id = TENANT.get();
        if (id == null || id.isBlank()) {
            throw new IllegalStateException("缺少租户标识");
        }
        return id;
    }

    public static void clear() {
        TENANT.remove();
    }
}
