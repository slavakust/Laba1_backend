package app.http;

import app.model.Car;
import app.model.CarStatus;
import app.model.CarType;
import app.model.RentalInfo;
import app.service.RentalService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

public class CarHandler implements HttpHandler {
    private final RentalService rentalService;

    public CarHandler(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String prefix = "/cars";
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
                } else if (parts.length == 2) {
                    handleAction(exchange, method, parts[0], parts[1]);
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
                CarType type = q.containsKey("type") ? CarType.valueOf(q.get("type").trim().toUpperCase()) : null;
                CarStatus status = q.containsKey("status") ? CarStatus.valueOf(q.get("status").trim().toUpperCase()) : null;
                Integer minYear = q.containsKey("minYear") ? Integer.parseInt(q.get("minYear").trim()) : null;
                String model = q.get("model");
                HttpUtils.sendJson(exchange, 200, rentalService.listCars(type, status, minYear, model));
            }
            case "POST" -> {
                Map<?, ?> body = HttpUtils.readBody(exchange, Map.class);
                if (body == null) throw new IllegalArgumentException("Body is required");

                String id = (String) body.get("id");
                String brand = (String) body.get("brand");
                String model = (String) body.get("model");
                String plate = (String) body.get("plateNumber");
                int year = body.get("productionYear") instanceof Number n ? n.intValue() : 0;
                double mileage = body.get("mileage") instanceof Number n ? n.doubleValue() : 0.0;
                String typeStr = (String) body.get("type");
                CarType type = typeStr != null ? CarType.valueOf(typeStr.trim().toUpperCase()) : null;

                Car car = rentalService.createCar(id, brand, model, plate, year, mileage, type);
                exchange.getResponseHeaders().set("Location", "/cars/" + car.getId());
                HttpUtils.sendJson(exchange, 201, car);
            }
            default -> HttpUtils.sendMethodNotAllowed(exchange, "GET", "POST");
        }
    }

    private void handleItem(HttpExchange exchange, String method, String id) throws IOException {
        switch (method) {
            case "GET" -> HttpUtils.sendJson(exchange, 200, rentalService.getCar(id));
            case "PUT" -> {
                Map<?, ?> body = HttpUtils.readBody(exchange, Map.class);
                if (body == null) throw new IllegalArgumentException("Body is required");

                String brand = (String) body.get("brand");
                String model = (String) body.get("model");
                String plate = (String) body.get("plateNumber");
                Integer year = body.get("productionYear") instanceof Number n ? n.intValue() : null;
                Double mileage = body.get("mileage") instanceof Number n ? n.doubleValue() : null;

                HttpUtils.sendJson(exchange, 200, rentalService.updateCar(id, brand, model, plate, year, mileage));
            }
            case "DELETE" -> {
                rentalService.deleteCar(id);
                HttpUtils.sendJson(exchange, 204, null);
            }
            default -> HttpUtils.sendMethodNotAllowed(exchange, "GET", "PUT", "DELETE");
        }
    }

    private void handleAction(HttpExchange exchange, String method, String id, String action) throws IOException {
        if (!"POST".equalsIgnoreCase(method)) {
            HttpUtils.sendMethodNotAllowed(exchange, "POST");
            return;
        }

        if ("rent".equalsIgnoreCase(action)) {
            RentalInfo.RentRequest req = HttpUtils.readBody(exchange, RentalInfo.RentRequest.class);
            HttpUtils.sendJson(exchange, 200, rentalService.rentCar(id, req != null ? req.driverId() : null));
        } else if ("return".equalsIgnoreCase(action)) {
            RentalInfo.ReturnRequest req = HttpUtils.readBody(exchange, RentalInfo.ReturnRequest.class);
            HttpUtils.sendJson(exchange, 200, rentalService.returnCar(id,
                    req != null ? req.hours() : null,
                    req != null ? req.distance() : null));
        } else {
            HttpUtils.sendError(exchange, 404, "Not Found", "Unknown action: " + action);
        }
    }
}
