package app.http;

import app.exception.ConflictException;
import app.exception.NotFoundException;
import app.exception.ValidationException;
import app.model.RentalInfo;
import com.sun.net.httpserver.HttpExchange;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class HttpUtils {
    public static final JsonMapper MAPPER = JsonMapper.builder().build();

    private HttpUtils() {}

    public static Map<String, String> parseQuery(String query) {
        if (query == null || query.isBlank()) return Collections.emptyMap();
        Map<String, String> map = new HashMap<>();
        for (String pair : query.split("&")) {
            if (pair.isBlank()) continue;
            int idx = pair.indexOf('=');
            if (idx > 0) {
                map.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
            } else {
                map.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
            }
        }
        return map;
    }

    public static <T> T readBody(HttpExchange exchange, Class<T> clazz) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            byte[] bytes = in.readAllBytes();
            if (bytes.length == 0) return null;
            return MAPPER.readValue(bytes, clazz);
        }
    }

    public static void sendJson(HttpExchange exchange, int status, Object data) throws IOException {
        if (data == null || status == 204) {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
            return;
        }
        byte[] bytes = MAPPER.writeValueAsBytes(data);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    public static void sendError(HttpExchange exchange, int status, String error, String message) {
        try {
            sendJson(exchange, status, new RentalInfo.ErrorResponse(status, error, message));
        } catch (IOException e) {
            exchange.close();
        }
    }

    public static void sendMethodNotAllowed(HttpExchange exchange, String... allowed) {
        exchange.getResponseHeaders().set("Allow", String.join(", ", allowed));
        sendError(exchange, 405, "Method Not Allowed", "Allowed: " + String.join(", ", allowed));
    }

    public static void handleException(HttpExchange exchange, Throwable t) {
        if (t instanceof NotFoundException) {
            sendError(exchange, 404, "Not Found", t.getMessage());
        } else if (t instanceof ConflictException) {
            sendError(exchange, 409, "Conflict", t.getMessage());
        } else if (t instanceof ValidationException) {
            sendError(exchange, 422, "Unprocessable Entity", t.getMessage());
        } else if (t instanceof JacksonException || t instanceof IllegalArgumentException) {
            sendError(exchange, 400, "Bad Request", t.getMessage());
        } else {
            sendError(exchange, 500, "Internal Server Error", "Unexpected error");
        }
    }
}
