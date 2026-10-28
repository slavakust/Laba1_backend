package app.http;

import app.model.Driver;
import app.service.RentalService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

public class DriverHandler implements HttpHandler {
    private final RentalService rentalService;

    public DriverHandler(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String prefix = "/drivers";
            if (!path.startsWith(prefix)) {
                HttpUtils.sendError(exchange, 404, "Not Found", "Path not found");
                return;
            }

            String sub = path.substring(prefix.length());
            if (sub.startsWith("/")) sub = sub.substring(1);
            if (sub.endsWith("/")) sub = sub.substring(0, sub.length() - 1);

            String method = exchange.getRequestMethod();

            if (sub.isEmpty()) {
                handleCollection(exchange, method);
            } else {
                String[] parts = sub.split("/");
                if (parts.length == 1) {
                    handleItem(exchange, method, parts[0]);
                } else {
                    HttpUtils.sendError(exchange, 404, "Not Found", "Unknown path");
                }
            }
        } catch (Throwable t) {
            HttpUtils.handleException(exchange, t);
        }
    }

    private void handleCollection(HttpExchange exchange, String method) throws IOException {
        switch (method) {
            case "GET" -> {
                Map<String, String> q = HttpUtils.parseQuery(exchange.getRequestURI().getQuery());
                Integer minAge = q.containsKey("minAge") ? Integer.parseInt(q.get("minAge").trim()) : null;
                Boolean hasCar = q.containsKey("hasCar") ? Boolean.parseBoolean(q.get("hasCar").trim()) : null;
                Boolean hasDeposit = q.containsKey("hasDeposit") ? Boolean.parseBoolean(q.get("hasDeposit").trim()) : null;
                HttpUtils.sendJson(exchange, 200, rentalService.listDrivers(minAge, hasCar, hasDeposit));
            }
            case "POST" -> {
                Map<?, ?> body = HttpUtils.readBody(exchange, Map.class);
                if (body == null) throw new IllegalArgumentException("Body is required");

                String id = (String) body.get("id");
                String name = (String) body.get("name");
                String email = (String) body.get("email");
                int age = body.get("age") instanceof Number n ? n.intValue() : 0;
                int exp = body.get("experience") instanceof Number n ? n.intValue() : 0;
                boolean deposit = Boolean.TRUE.equals(body.get("hasDeposit"));

                Driver driver = rentalService.createDriver(id, name, email, age, exp, deposit);
                exchange.getResponseHeaders().set("Location", "/drivers/" + driver.getId());
                HttpUtils.sendJson(exchange, 201, driver);
            }
            default -> HttpUtils.sendMethodNotAllowed(exchange, "GET", "POST");
        }
    }

    private void handleItem(HttpExchange exchange, String method, String id) throws IOException {
        switch (method) {
            case "GET" -> HttpUtils.sendJson(exchange, 200, rentalService.getDriver(id));
            case "PUT" -> {
                Map<?, ?> body = HttpUtils.readBody(exchange, Map.class);
                if (body == null) throw new IllegalArgumentException("Body is required");

                String name = (String) body.get("name");
                String email = (String) body.get("email");
                Integer age = body.get("age") instanceof Number n ? n.intValue() : null;
                Integer exp = body.get("experience") instanceof Number n ? n.intValue() : null;
                Boolean deposit = body.containsKey("hasDeposit") ? Boolean.TRUE.equals(body.get("hasDeposit")) : null;

                HttpUtils.sendJson(exchange, 200, rentalService.updateDriver(id, name, email, age, exp, deposit));
            }
            case "DELETE" -> {
                rentalService.deleteDriver(id);
                HttpUtils.sendJson(exchange, 204, null);
            }
            default -> HttpUtils.sendMethodNotAllowed(exchange, "GET", "PUT", "DELETE");
        }
    }
}
