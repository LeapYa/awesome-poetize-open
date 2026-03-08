package com.ld.poetry.service.ai;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SseRequestUtilsTest {

    @Test
    void shouldRecognizeSseRequestFromAcceptHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept", "text/event-stream");

        assertTrue(SseRequestUtils.isSseRequest(request));
    }

    @Test
    void shouldRecognizeSseRequestFromResponseContentType() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/ai/chat/sendMessageStream");
        request.setContentType("application/json");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType("text/event-stream");

        assertTrue(SseRequestUtils.isSseRequest(request, response));
    }

    @Test
    void shouldRecognizeClientCancellationBySocketMessage() {
        Throwable error = new IOException(new SocketException("An established connection was aborted by the software in your host machine"));

        assertTrue(SseRequestUtils.isClientCancellation(error));
    }

    @Test
    void shouldRecognizeSseWriteIoExceptionAsCancellation() {
        IOException error = new IOException("stream closed");
        error.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("org.springframework.web.servlet.mvc.method.annotation.SseEmitter", "send", "SseEmitter.java", 129)
        });

        assertTrue(SseRequestUtils.isClientCancellation(error));
    }

    @Test
    void shouldRecognizeCancellationException() {
        assertTrue(SseRequestUtils.isClientCancellation(new CancellationException("client stopped stream")));
    }

    @Test
    void shouldNotTreatRegularBusinessErrorAsCancellation() {
        assertFalse(SseRequestUtils.isClientCancellation(new IllegalStateException("AI 服务暂时不可用")));
    }
}
