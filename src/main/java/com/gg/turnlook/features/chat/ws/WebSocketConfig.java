package com.gg.turnlook.features.chat.ws;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/** Configuración STOMP sobre WebSocket nativa (sin SockJS). */
@Configuration
@EnableWebSocketMessageBroker
@AllArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtChannelInterceptor jwtChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Misma lista que SecurityConfig.corsConfigurationSource().
        registry.addEndpoint("/ws").setAllowedOrigins(
                "http://127.0.0.1:3000",
                "http://localhost:3000",
                "http://127.0.0.1:4200",
                "http://localhost:4200",
                "http://127.0.0.1:8080",
                "http://localhost:8080",
                "https://appointmee-front-production.up.railway.app",
                "https://appointmee-vcs2.onrender.com");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtChannelInterceptor);
    }
}
