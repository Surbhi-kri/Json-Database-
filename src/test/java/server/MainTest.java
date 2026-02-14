package server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @BeforeAll
    static void startServer() throws InterruptedException {

        Thread serverThread = new Thread(() -> {
            try {
                Main.main(new String[]{});
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        serverThread.setDaemon(true); // important
        serverThread.start();

        Thread.sleep(1000); // wait for server startup
    }

    @Test
    void testServerSetAndGet() throws Exception {

        Gson gson = new Gson();

        // -------- SET --------
        try (
                Socket socket = new Socket("localhost", 23456);
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader input = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()))
        ) {

            JsonObject request = new JsonObject();
            request.addProperty("type", "set");
            request.addProperty("key", "name");
            request.addProperty("value", "Sukumari");

            output.println(gson.toJson(request));
            String response = input.readLine();

            assertTrue(response.contains("OK"));
        }

        // -------- GET (new connection) --------
        try (
                Socket socket = new Socket("localhost", 23456);
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader input = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()))
        ) {

            JsonObject request = new JsonObject();
            request.addProperty("type", "get");
            request.addProperty("key", "name");

            output.println(gson.toJson(request));
            String response = input.readLine();

            assertTrue(response.contains("Sukumari"));
        }
    }
}
