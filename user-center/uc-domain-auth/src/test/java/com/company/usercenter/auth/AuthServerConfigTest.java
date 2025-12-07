package com.company.usercenter.auth;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthServerConfigTest {

    @Test
    void jwkSourceShouldReusePersistedKey() throws Exception {
        Path dir = Files.createTempDirectory("jwk-test");
        Path file = dir.resolve("jwk.pem");

        AuthServerConfig config = new AuthServerConfig();
        RSAKey first = loadFirstKey(config.jwkSource(file.toString()));
        RSAKey second = loadFirstKey(config.jwkSource(file.toString()));

        assertThat(file).exists();
        assertThat(first.getKeyID()).isEqualTo(second.getKeyID());

        Files.deleteIfExists(file);
        Files.deleteIfExists(dir);
    }

    private RSAKey loadFirstKey(com.nimbusds.jose.jwk.source.JWKSource<SecurityContext> source) throws Exception {
        List<JWK> jwks = source.get(new JWKSelector(new JWKMatcher.Builder().build()), null);
        return (RSAKey) jwks.get(0);
    }
}
