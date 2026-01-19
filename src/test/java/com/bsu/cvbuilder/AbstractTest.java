package com.bsu.cvbuilder;

import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.dto.auth.AuthRequest;
import com.bsu.cvbuilder.domain.dto.auth.AuthResponse;
import com.bsu.cvbuilder.domain.dto.auth.RegisterAuthDto;
import com.bsu.cvbuilder.util.JsonHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Slf4j
@Testcontainers
@SpringBootTest
@Import(RedisAutoConfiguration.class)
@EnableConfigurationProperties(ApplicationProperties.class)
@TestPropertySource(properties = {
        "app.cache.verification=300",
        "app.security.access-secret=dummy-secret-at-least-32-chars-long",
        "app.security.refresh-secret=dummy-secret-at-least-32-chars-long",
        "app.security.access-lifetime=3600",
        "app.security.refresh-lifetime=86400",
        "app.security.decode-signature=test"
})
public abstract class AbstractTest {

    protected TestRestTemplate restTemplate = new TestRestTemplate();

    @LocalServerPort
    public int port;

    static final MongoDBContainer mongoDBContainer;

    static {
        mongoDBContainer = new MongoDBContainer("mongo:6.0");
        mongoDBContainer.start();
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Container
    @ServiceConnection
    public static final GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    protected AuthResponse register(RegisterAuthDto registerAuthDto) {
        log.info("Authorizing request: {}", registerAuthDto);
        if (registerAuthDto == null) {
            registerAuthDto = RegisterAuthDto.builder()
                    .firstName("first")
                    .lastName("last")
                    .password("password")
                    .email("email")
                    .build();
        }
        ResponseEntity<AuthResponse> responseEntity = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/auth/register",
                HttpMethod.POST,
                new HttpEntity(registerAuthDto),
                AuthResponse.class
        );
        log.info("Authorizing response: {}", responseEntity.getBody());
        return responseEntity.getBody();
    }
}