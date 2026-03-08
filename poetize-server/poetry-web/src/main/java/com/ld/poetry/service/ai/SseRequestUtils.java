package com.ld.poetry.service.ai;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.net.SocketException;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CancellationException;

/**
 * SSE 请求与客户端主动断连识别工具。
 */
public final class SseRequestUtils {

    private static final String EVENT_STREAM = MediaType.TEXT_EVENT_STREAM_VALUE;

    private SseRequestUtils() {
    }

    public static boolean isSseRequest(HttpServletRequest request) {
        return isSseRequest(request, null);
    }

    public static boolean isSseRequest(HttpServletRequest request, HttpServletResponse response) {
        if (request == null) {
            return false;
        }

        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains(EVENT_STREAM)) {
            return true;
        }

        String contentType = request.getContentType();
        if (contentType != null && contentType.contains(EVENT_STREAM)) {
            return true;
        }

        if (response != null) {
            String responseContentType = response.getContentType();
            if (responseContentType != null && responseContentType.contains(EVENT_STREAM)) {
                return true;
            }
        }

        Object producibleTypes = request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
        if (producibleTypes instanceof Collection<?> collection) {
            for (Object candidate : collection) {
                if (candidate instanceof MediaType mediaType
                        && EVENT_STREAM.equals(mediaType.toString())) {
                    return true;
                }
            }
        }

        String requestUri = request.getRequestURI();
        return requestUri != null
                && (requestUri.contains("/ai/chat/sendMessageStream")
                        || requestUri.contains("/ai/chat/sendStreamMessage"));
    }

    public static boolean isClientCancellation(Throwable throwable) {
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth++ < 12) {
            if (current instanceof CancellationException || current instanceof InterruptedException) {
                return true;
            }

            if (current instanceof SocketException) {
                return true;
            }

            if (current instanceof IOException && hasSseWriteStack(current)) {
                return true;
            }

            String className = current.getClass().getName();
            if (className.contains("AsyncRequestNotUsableException")
                    || className.contains("ClientAbortException")
                    || className.contains("EofException")) {
                return true;
            }

            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("broken pipe")
                        || normalized.contains("connection reset by peer")
                        || normalized.contains("forcibly closed")
                        || normalized.contains("an established connection was aborted")
                        || normalized.contains("closed by interrupt")
                        || normalized.contains("connection aborted")) {
                    return true;
                }
            }

            current = current.getCause();
        }

        return false;
    }

    private static boolean hasSseWriteStack(Throwable throwable) {
        for (StackTraceElement element : throwable.getStackTrace()) {
            String className = element.getClassName();
            if (className.contains("ResponseBodyEmitter")
                    || className.contains("SseEmitter")
                    || className.contains("StandardServletAsyncWebRequest")
                    || className.contains("SocketDispatcher")
                    || className.contains("NioEndpoint")) {
                return true;
            }
        }
        return false;
    }
}
