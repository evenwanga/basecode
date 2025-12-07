package com.company.usercenter.auth;

import com.company.usercenter.api.dto.ClientRegistrationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientApplicationServiceTest {

    private final TestRegisteredClientRepository repo = new TestRegisteredClientRepository();
    private final ClientApplicationService service = new ClientApplicationService(repo, new BCryptPasswordEncoder());

    @Test
    void registerClientSuccess() {
        var req = new ClientRegistrationRequest(
                "web-app",
                "secret",
                List.of("http://localhost:8080/callback"),
                List.of("openid", "profile")
        );

        var client = service.register(req);

        assertThat(client.getClientId()).isEqualTo("web-app");
        assertThat(client.getRedirectUris()).contains("http://localhost:8080/callback");
        assertThat(client.getScopes()).contains("profile");
        assertThat(client.getClientSecret()).isNotEqualTo("secret"); // 已加密
    }

    @Test
    void registerDuplicateShouldFail() {
        var req = new ClientRegistrationRequest(
                "dup",
                "secret",
                List.of("http://localhost/cb"),
                List.of("openid")
        );
        service.register(req);

        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("客户端已存在");
    }

    /**
     * 简单可变的 RegisteredClientRepository 便于单测。
     */
    static class TestRegisteredClientRepository implements RegisteredClientRepository {
        private final Map<String, org.springframework.security.oauth2.server.authorization.client.RegisteredClient> byId = new HashMap<>();
        private final Map<String, org.springframework.security.oauth2.server.authorization.client.RegisteredClient> byClientId = new HashMap<>();

        @Override
        public void save(org.springframework.security.oauth2.server.authorization.client.RegisteredClient registeredClient) {
            byId.put(registeredClient.getId(), registeredClient);
            byClientId.put(registeredClient.getClientId(), registeredClient);
        }

        @Override
        public org.springframework.security.oauth2.server.authorization.client.RegisteredClient findById(String id) {
            return byId.get(id);
        }

        @Override
        public org.springframework.security.oauth2.server.authorization.client.RegisteredClient findByClientId(String clientId) {
            return byClientId.get(clientId);
        }
    }
}
