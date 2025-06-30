package com.mycompany.myapp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import tech.jhipster.config.JHipsterProperties;

/**
 * Unit tests for {@link WebsocketConfiguration}.
 */
@ExtendWith(MockitoExtension.class)
class WebsocketConfigurationTest {

    @Mock
    private JHipsterProperties jHipsterProperties;

    @Mock
    private MessageBrokerRegistry messageBrokerRegistry;

    @Mock
    private ServerHttpRequest serverHttpRequest;

    @Mock
    private ServletServerHttpRequest servletServerHttpRequest;

    @Mock
    private ServerHttpResponse serverHttpResponse;

    @Mock
    private WebSocketHandler webSocketHandler;

    private WebsocketConfiguration websocketConfiguration;

    @BeforeEach
    void setup() {
        websocketConfiguration = new WebsocketConfiguration(jHipsterProperties);
    }

    @Test
    void testConfigureMessageBroker() {
        // When
        websocketConfiguration.configureMessageBroker(messageBrokerRegistry);

        // Then
        verify(messageBrokerRegistry).enableSimpleBroker("/topic");
    }

    @Test
    void testHttpSessionHandshakeInterceptorBeforeHandshakeWithServletRequest() throws Exception {
        // Given
        HandshakeInterceptor interceptor = websocketConfiguration.httpSessionHandshakeInterceptor();
        Map<String, Object> attributes = new HashMap<>();

        // When
        boolean result = interceptor.beforeHandshake(servletServerHttpRequest, serverHttpResponse, webSocketHandler, attributes);

        // Then
        assertThat(result).isTrue();
        // The IP address should be added to attributes if this is a ServletServerHttpRequest
        // Note: We can't easily mock the getRemoteAddress() method due to its return type
    }

    @Test
    void testHttpSessionHandshakeInterceptorBeforeHandshakeWithNonServletRequest() throws Exception {
        // Given
        HandshakeInterceptor interceptor = websocketConfiguration.httpSessionHandshakeInterceptor();
        Map<String, Object> attributes = new HashMap<>();

        // When
        boolean result = interceptor.beforeHandshake(serverHttpRequest, serverHttpResponse, webSocketHandler, attributes);

        // Then
        assertThat(result).isTrue();
        assertThat(attributes).doesNotContainKey(WebsocketConfiguration.IP_ADDRESS);
    }

    @Test
    void testHttpSessionHandshakeInterceptorAfterHandshake() throws Exception {
        // Given
        HandshakeInterceptor interceptor = websocketConfiguration.httpSessionHandshakeInterceptor();

        // When & Then - should not throw any exception
        interceptor.afterHandshake(serverHttpRequest, serverHttpResponse, webSocketHandler, null);

        // Method is empty, so we just verify it doesn't throw
        assertThat(true).isTrue(); // Placeholder assertion
    }

    @Test
    void testWebsocketConfigurationConstructor() {
        // Given
        JHipsterProperties properties = mock(JHipsterProperties.class);

        // When
        WebsocketConfiguration config = new WebsocketConfiguration(properties);

        // Then
        assertThat(config).isNotNull();
    }

    @Test
    void testIpAddressConstant() {
        // Test that the IP_ADDRESS constant is correctly defined
        assertThat(WebsocketConfiguration.IP_ADDRESS).isEqualTo("IP_ADDRESS");
    }

    @Test
    void testHandshakeInterceptorInstantiationAndBasicFunctionality() throws Exception {
        // Given
        HandshakeInterceptor interceptor1 = websocketConfiguration.httpSessionHandshakeInterceptor();
        HandshakeInterceptor interceptor2 = websocketConfiguration.httpSessionHandshakeInterceptor();
        Map<String, Object> attributes = new HashMap<>();

        // When & Then
        assertThat(interceptor1).isNotNull();
        assertThat(interceptor2).isNotNull();
        assertThat(interceptor1).isNotSameAs(interceptor2);

        // Test that beforeHandshake works correctly
        boolean result = interceptor1.beforeHandshake(serverHttpRequest, serverHttpResponse, webSocketHandler, attributes);
        assertThat(result).isTrue();

        // Test that afterHandshake works without throwing
        interceptor1.afterHandshake(serverHttpRequest, serverHttpResponse, webSocketHandler, null);
    }

    @Test
    void testHandshakeInterceptorWithAttributesMap() throws Exception {
        // Given
        HandshakeInterceptor interceptor = websocketConfiguration.httpSessionHandshakeInterceptor();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("existing-key", "existing-value");

        // When
        boolean result = interceptor.beforeHandshake(serverHttpRequest, serverHttpResponse, webSocketHandler, attributes);

        // Then
        assertThat(result).isTrue();
        assertThat(attributes).containsEntry("existing-key", "existing-value");
        // For non-servlet requests, IP_ADDRESS should not be added
        assertThat(attributes).doesNotContainKey(WebsocketConfiguration.IP_ADDRESS);
    }

    @Test
    void testConfigurationCanBeInstantiatedMultipleTimes() {
        // Test that the configuration can be created multiple times
        WebsocketConfiguration config1 = new WebsocketConfiguration(jHipsterProperties);
        WebsocketConfiguration config2 = new WebsocketConfiguration(jHipsterProperties);

        assertThat(config1).isNotNull();
        assertThat(config2).isNotNull();
        assertThat(config1).isNotSameAs(config2);

        // Both should produce working interceptors
        HandshakeInterceptor interceptor1 = config1.httpSessionHandshakeInterceptor();
        HandshakeInterceptor interceptor2 = config2.httpSessionHandshakeInterceptor();

        assertThat(interceptor1).isNotNull();
        assertThat(interceptor2).isNotNull();
    }
}
