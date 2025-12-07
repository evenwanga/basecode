package com.company.platform.web;

import com.company.platform.jpa.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantHeaderFilter extends OncePerRequestFilter {

    /**
     * 从请求头读取租户标识（X-Tenant-Id），写入线程上下文，响应结束时清理。
     * 对 /api/** 请求要求租户头（创建租户接口除外）。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String tenantId = request.getHeader("X-Tenant-Id");
            String path = request.getRequestURI();
            boolean requireTenant = path.startsWith("/api") && !path.startsWith("/api/tenants") && !path.startsWith("/actuator");
            if (requireTenant && (tenantId == null || tenantId.isBlank())) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("缺少租户标识");
                return;
            }
            if (tenantId != null && !tenantId.isBlank()) {
                TenantContext.setTenantId(tenantId);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
