package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;


public class Main {
    static void main() {
        try (var server = new ServerSocket(8080)) {
            while (true) {
                try (var socket = server.accept();
                     var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     var out = socket.getOutputStream()) {
                    String requestLine = in.readLine();
                    System.out.println(requestLine);
                    String line;
                    while ((line = in.readLine()) != null && !line.isEmpty()) {
                        System.out.println(line);
                    }
                    byte[] body = "Привет мир ДВФУ!".getBytes(StandardCharsets.UTF_8);
                    out.write(("HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/plain; charset=utf-8\r\n" +
                            "Content-Length: " + body.length + "\r\n\r\n")
                            .getBytes(StandardCharsets.UTF_8));
                    out.write(body);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
