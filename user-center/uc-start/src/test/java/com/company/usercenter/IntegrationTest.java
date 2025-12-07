package com.company.usercenter;

import com.company.usercenter.identity.IdentityService;
import com.company.usercenter.tenant.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class IntegrationTest {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("user_center_test")
            .withUsername("user_center")
            .withPassword("password");

    @DynamicPropertySource
    static void pgProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", pg::getJdbcUrl);
        registry.add("spring.datasource.username", pg::getUsername);
        registry.add("spring.datasource.password", pg::getPassword);
    }

    @Autowired
    TenantService tenantService;

    @Autowired
    IdentityService identityService;

    @LocalServerPort
    int port;

    @Test
    void createTenantAndRegisterUser() {
        var tenant = tenantService.createTenant("t1", "租户1");
        assertThat(tenant.getId()).isNotNull();

        var user = identityService.registerUser("张三", "test@example.com", "Passw0rd!");
        assertThat(user.getId()).isNotNull();

        identityService.addMembership(user.getId(), tenant.getId(), null, "admin");
        assertThat(identityService.findByEmail("test@example.com")).isPresent();

        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        ResponseEntity<String> okResp = client.get()
                .uri("/api/tenants/org-units")
                .header("X-Tenant-Id", tenant.getId().toString())
                .retrieve()
                .toEntity(String.class);
        assertThat(okResp.getStatusCode().is2xxSuccessful()).isTrue();

        ResponseEntity<Void> badResp = client.get()
                .uri("/api/tenants/org-units")
                .exchange((req, resp) -> ResponseEntity.status(resp.getStatusCode()).build());
        assertThat(badResp.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void otpFlowWithFixedCode() {
        var tenant = tenantService.createTenant("t2", "租户2");
        var user = identityService.registerUser("李四", "otp@example.com", "Passw0rd!");
        identityService.addMembership(user.getId(), tenant.getId(), null, "user");

        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        ResponseEntity<String> sendResp = client.post()
                .uri("/api/identities/otp/send")
                .header("X-Tenant-Id", tenant.getId().toString())
                .body(Map.of("email", "otp@example.com"))
                .retrieve()
                .toEntity(String.class);
        assertThat(sendResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(sendResp.getBody()).contains("000000");

        ResponseEntity<String> verifyResp = client.post()
                .uri("/api/identities/otp/verify")
                .header("X-Tenant-Id", tenant.getId().toString())
                .body(Map.of("email", "otp@example.com", "code", "000000"))
                .retrieve()
                .toEntity(String.class);
        assertThat(verifyResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(verifyResp.getBody()).contains("\"success\":true");

        ResponseEntity<String> verifyAgain = client.post()
                .uri("/api/identities/otp/verify")
                .header("X-Tenant-Id", tenant.getId().toString())
                .body(Map.of("email", "otp@example.com", "code", "000000"))
                .retrieve()
                .toEntity(String.class);
        assertThat(verifyAgain.getBody()).contains("\"success\":false");
    }
}
