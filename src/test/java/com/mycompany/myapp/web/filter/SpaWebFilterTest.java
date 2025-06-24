package com.mycompany.myapp.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link SpaWebFilter}.
 */
@ExtendWith(MockitoExtension.class)
class SpaWebFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private RequestDispatcher requestDispatcher;

    @InjectMocks
    private SpaWebFilter spaWebFilter;

    @BeforeEach
    void setUp() {
        when(request.getRequestDispatcher(anyString())).thenReturn(requestDispatcher);
    }

    @Test
    void doFilter_shouldForwardApiRequests() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/tasks");
        when(request.getContextPath()).thenReturn("");

        // When
        spaWebFilter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(requestDispatcher, never()).forward(any(), any());
    }

    @Test
    void doFilter_shouldForwardManagementRequests() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/management/health");
        when(request.getContextPath()).thenReturn("");

        // When
        spaWebFilter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(requestDispatcher, never()).forward(any(), any());
    }

    @Test
    void doFilter_shouldForwardSwaggerRequests() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/v3/api-docs");
        when(request.getContextPath()).thenReturn("");

        // When
        spaWebFilter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(requestDispatcher, never()).forward(any(), any());
    }

    @Test
    void doFilter_shouldForwardStaticResourceRequests() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/assets/img/logo.png");
        when(request.getContextPath()).thenReturn("");

        // When
        spaWebFilter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(requestDispatcher, never()).forward(any(), any());
    }

    @Test
    void doFilter_shouldForwardJavaScriptFiles() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/app.js");
        when(request.getContextPath()).thenReturn("");

        // When
        spaWebFilter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(requestDispatcher, never()).forward(any(), any());
    }

    @Test
    void doFilter_shouldRedirectSpaRoutesToIndex() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/dashboard");
        when(request.getContextPath()).thenReturn("");

        // When
        spaWebFilter.doFilter(request, response, filterChain);

        // Then
        verify(request).getRequestDispatcher("/index.html");
        verify(requestDispatcher).forward(request, response);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilter_shouldRedirectRootToIndex() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/");
        when(request.getContextPath()).thenReturn("");

        // When
        spaWebFilter.doFilter(request, response, filterChain);

        // Then
        verify(request).getRequestDispatcher("/index.html");
        verify(requestDispatcher).forward(request, response);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilter_shouldHandleContextPath() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/myapp/dashboard");
        when(request.getContextPath()).thenReturn("/myapp");

        // When
        spaWebFilter.doFilter(request, response, filterChain);

        // Then
        verify(request).getRequestDispatcher("/index.html");
        verify(requestDispatcher).forward(request, response);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilter_shouldForwardApiRequestsWithContextPath() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/myapp/api/tasks");
        when(request.getContextPath()).thenReturn("/myapp");

        // When
        spaWebFilter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(requestDispatcher, never()).forward(any(), any());
    }

    @Test
    void doFilter_shouldHandleNullContextPath() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/dashboard");
        when(request.getContextPath()).thenReturn(null);

        // When
        spaWebFilter.doFilter(request, response, filterChain);

        // Then
        verify(request).getRequestDispatcher("/index.html");
        verify(requestDispatcher).forward(request, response);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilter_shouldHandleEmptyPath() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("");
        when(request.getContextPath()).thenReturn("");

        // When
        spaWebFilter.doFilter(request, response, filterChain);

        // Then
        verify(request).getRequestDispatcher("/index.html");
        verify(requestDispatcher).forward(request, response);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilter_shouldHandleNonHttpServletRequest() throws IOException, ServletException {
        // Given
        ServletRequest nonHttpRequest = mock(ServletRequest.class);
        ServletResponse nonHttpResponse = mock(ServletResponse.class);

        // When
        spaWebFilter.doFilter(nonHttpRequest, nonHttpResponse, filterChain);

        // Then
        verify(filterChain).doFilter(nonHttpRequest, nonHttpResponse);
    }
}
