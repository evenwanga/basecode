package com.company.usercenter.auth;

import com.company.usercenter.identity.IdentityService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.UUID;

@Configuration
public class AuthServerConfig {

    @Bean
    public UserDetailsService userDetailsService(IdentityService identityService) {
        return new DomainUserDetailsService(identityService);
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /**
     * 授权服务器安全链：开启 OAuth2/OIDC 端点，使用 JWT。
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authServerSecurityFilterChain(HttpSecurity http,
                                                             AuthenticationManager authenticationManager) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();
        RequestMatcher endpointsMatcher = authorizationServerConfigurer.getEndpointsMatcher();

        http.securityMatcher(endpointsMatcher)
                .authenticationManager(authenticationManager)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(endpointsMatcher))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
                .apply(authorizationServerConfigurer);

        authorizationServerConfigurer.oidc(Customizer.withDefaults());
        return http.build();
    }

    /**
     * 应用接口安全链：暂时放行公共接口，其余仍可按需保护。
     */
    @Bean
    @Order(2)
    public SecurityFilterChain appSecurityFilterChain(HttpSecurity http,
                                                      AuthenticationManager authenticationManager) throws Exception {
        http.authenticationManager(authenticationManager);
        http.csrf(AbstractHttpConfigurer::disable);
        http.authorizeHttpRequests(registry -> registry
                .requestMatchers("/api/identities/register", "/api/identities/otp/**",
                        "/api/tenants/**", "/actuator/**",
                        "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated());
        http.formLogin(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate,
                                                           RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate,
                                                                         RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(@Value("${auth.jwk.path:var/jwk-rsa.pem}") String jwkPath) {
        RSAKey rsaKey = loadOrCreateRsaKey(jwkPath);
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(
            @Value("${auth.issuer-uri:http://localhost:8080}") String issuerUri) {
        return AuthorizationServerSettings.builder()
                .issuer(issuerUri)
                .build();
    }

    private RSAKey loadOrCreateRsaKey(String pemPath) {
        if (pemPath == null || pemPath.isBlank()) {
            return buildRsaKey(generateRsaKey());
        }
        Path path = Paths.get(pemPath);
        try {
            if (Files.exists(path)) {
                RSAPrivateCrtKey privateKey = readPrivateKey(path);
                RSAPublicKey publicKey = derivePublicKey(privateKey);
                return buildRsaKey(privateKey, publicKey);
            }
            KeyPair pair = generateRsaKey();
            writePrivateKey(path, pair.getPrivate());
            return buildRsaKey(pair);
        } catch (Exception ex) {
            throw new IllegalStateException("无法加载或创建 JWK", ex);
        }
    }

    private RSAKey buildRsaKey(KeyPair keyPair) {
        return buildRsaKey((RSAPrivateKey) keyPair.getPrivate(), (RSAPublicKey) keyPair.getPublic());
    }

    private RSAKey buildRsaKey(RSAPrivateKey privateKey, RSAPublicKey publicKey) {
        String keyId = thumbprint(publicKey);
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(keyId)
                .build();
    }

    private RSAPrivateCrtKey readPrivateKey(Path path) throws Exception {
        String pem = Files.readString(path);
        String sanitized = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(sanitized);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        return (RSAPrivateCrtKey) java.security.KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    private RSAPublicKey derivePublicKey(RSAPrivateCrtKey privateKey) throws Exception {
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent());
        return (RSAPublicKey) java.security.KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    private void writePrivateKey(Path path, PrivateKey privateKey) throws IOException {
        byte[] encoded = privateKey.getEncoded();
        String base64 = Base64.getEncoder().encodeToString(encoded);
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN PRIVATE KEY-----\n");
        for (int i = 0; i < base64.length(); i += 64) {
            int end = Math.min(i + 64, base64.length());
            sb.append(base64, i, end).append("\n");
        }
        sb.append("-----END PRIVATE KEY-----\n");
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, sb.toString());
    }

    private String thumbprint(PublicKey publicKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(publicKey.getEncoded());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception ex) {
            return UUID.randomUUID().toString();
        }
    }

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("无法生成 RSA 密钥", ex);
        }
    }
}
