package com.mycompany.myapp.web.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.mycompany.myapp.web.websocket.dto.ActivityDTO;
import java.security.Principal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Unit tests for {@link ActivityService}.
 */
@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    @Mock
    private StompHeaderAccessor stompHeaderAccessor;

    @Mock
    private Principal principal;

    @InjectMocks
    private ActivityService activityService;

    @BeforeEach
    void setUp() {
        when(principal.getName()).thenReturn("testuser");
        when(stompHeaderAccessor.getSessionId()).thenReturn("test-session-123");

        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("IP_ADDRESS", "192.168.1.1");
        when(stompHeaderAccessor.getSessionAttributes()).thenReturn(sessionAttributes);
    }

    @Test
    void sendActivity_shouldProcessActivityDTOAndReturnEnhancedVersion() {
        // Given
        ActivityDTO inputActivity = new ActivityDTO();
        inputActivity.setPage("dashboard");

        // When
        ActivityDTO result = activityService.sendActivity(inputActivity, stompHeaderAccessor, principal);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserLogin()).isEqualTo("testuser");
        assertThat(result.getSessionId()).isEqualTo("test-session-123");
        assertThat(result.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(result.getPage()).isEqualTo("dashboard");
        assertThat(result.getTime()).isNotNull();
    }

    @Test
    void sendActivity_shouldSetCurrentTimeWhenProcessing() {
        // Given
        ActivityDTO inputActivity = new ActivityDTO();
        inputActivity.setPage("tasks");
        Instant beforeCall = Instant.now();

        // When
        ActivityDTO result = activityService.sendActivity(inputActivity, stompHeaderAccessor, principal);

        // Then
        Instant afterCall = Instant.now();
        assertThat(result.getTime()).isBetween(beforeCall, afterCall);
    }

    @Test
    void sendActivity_shouldHandleNullPage() {
        // Given
        ActivityDTO inputActivity = new ActivityDTO();
        inputActivity.setPage(null);

        // When
        ActivityDTO result = activityService.sendActivity(inputActivity, stompHeaderAccessor, principal);

        // Then
        assertThat(result.getPage()).isNull();
        assertThat(result.getUserLogin()).isEqualTo("testuser");
        assertThat(result.getSessionId()).isEqualTo("test-session-123");
    }

    @Test
    void sendActivity_shouldHandleEmptyPage() {
        // Given
        ActivityDTO inputActivity = new ActivityDTO();
        inputActivity.setPage("");

        // When
        ActivityDTO result = activityService.sendActivity(inputActivity, stompHeaderAccessor, principal);

        // Then
        assertThat(result.getPage()).isEmpty();
        assertThat(result.getUserLogin()).isEqualTo("testuser");
    }

    @Test
    void onApplicationEvent_shouldSendLogoutActivity() {
        // Given
        SessionDisconnectEvent event = mock(SessionDisconnectEvent.class);
        when(event.getSessionId()).thenReturn("disconnect-session-456");

        // When
        activityService.onApplicationEvent(event);

        // Then
        ArgumentCaptor<ActivityDTO> activityCaptor = ArgumentCaptor.forClass(ActivityDTO.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/tracker"), activityCaptor.capture());

        ActivityDTO sentActivity = activityCaptor.getValue();
        assertThat(sentActivity.getSessionId()).isEqualTo("disconnect-session-456");
        assertThat(sentActivity.getPage()).isEqualTo("logout");
    }

    @Test
    void sendActivity_shouldPreserveExistingActivityDTOFields() {
        // Given
        ActivityDTO inputActivity = new ActivityDTO();
        inputActivity.setPage("users");
        inputActivity.setUserLogin("originalUser"); // This should be overridden
        inputActivity.setSessionId("originalSession"); // This should be overridden

        // When
        ActivityDTO result = activityService.sendActivity(inputActivity, stompHeaderAccessor, principal);

        // Then
        assertThat(result.getPage()).isEqualTo("users");
        assertThat(result.getUserLogin()).isEqualTo("testuser"); // Should be overridden by principal
        assertThat(result.getSessionId()).isEqualTo("test-session-123"); // Should be overridden by accessor
    }

    @Test
    void sendActivity_shouldHandleSpecialCharactersInPage() {
        // Given
        ActivityDTO inputActivity = new ActivityDTO();
        inputActivity.setPage("tasks/edit/123?mode=advanced&type=urgent");

        // When
        ActivityDTO result = activityService.sendActivity(inputActivity, stompHeaderAccessor, principal);

        // Then
        assertThat(result.getPage()).isEqualTo("tasks/edit/123?mode=advanced&type=urgent");
    }
}
