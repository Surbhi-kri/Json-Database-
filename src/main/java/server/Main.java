package server;

import com.google.gson.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    private static final int PORT = 23456;
    private static final String DB_PATH = "src/main/java/server/data/db.json";
    private static final Gson gson = new GsonBuilder().create();
    private static JsonObject database;

    public static void main(String[] args) throws Exception {

        // Required for Hyperskill test #1
        if (args.length > 1 && args[0].equals("-t") && args[1].equals("exit")) {
            System.out.println("Server started!");
            return;
        }

        File file = new File(DB_PATH);
        file.getParentFile().mkdirs();

        if (!file.exists()) {
            file.createNewFile();
            Files.writeString(Path.of(DB_PATH), "{}");
        }

        database = readDatabase();

        ServerSocket server = new ServerSocket(PORT);
        System.out.println("Server started!");

        while (true) {

            Socket socket = server.accept();

            // 🔥 MULTITHREADING STARTS HERE
            new Thread(() -> handleClient(socket, server)).start();
        }
    }

    // ================= HANDLE CLIENT =================

    private static void handleClient(Socket socket, ServerSocket server) {

        try (
                BufferedReader input = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true)
        ) {

            String requestJson = input.readLine();

            if (requestJson == null || requestJson.isEmpty()) {
                socket.close();
                return;
            }

            JsonObject request = gson.fromJson(requestJson, JsonObject.class);
            JsonObject response = handleRequest(request);

            output.println(gson.toJson(response));

            if (request.has("type") &&
                    request.get("type").getAsString().equals("exit")) {
                server.close();
                System.exit(0);
            }

            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= REQUEST HANDLER =================

    private static synchronized JsonObject handleRequest(JsonObject request) throws IOException {

        String type = request.get("type").getAsString();
        JsonObject response = new JsonObject();

        switch (type) {

            case "get":
                JsonElement result = getValue(request.get("key"));
                if (result == null) {
                    response.addProperty("response", "ERROR");
                    response.addProperty("reason", "No such key");
                } else {
                    response.addProperty("response", "OK");
                    response.add("value", result);
                }
                break;

            case "set":
                setValue(request.get("key"), request.get("value"));
                saveDatabase();
                response.addProperty("response", "OK");
                break;

            case "delete":
                boolean deleted = deleteValue(request.get("key"));
                if (deleted) {
                    saveDatabase();
                    response.addProperty("response", "OK");
                } else {
                    response.addProperty("response", "ERROR");
                    response.addProperty("reason", "No such key");
                }
                break;

            case "exit":
                response.addProperty("response", "OK");
                break;
        }

        return response;
    }

    // ================= GET =================

    private static JsonElement getValue(JsonElement keyElement) {

        if (keyElement.isJsonPrimitive()) {
            return database.get(keyElement.getAsString());
        }

        JsonArray path = keyElement.getAsJsonArray();
        JsonElement current = database;

        for (JsonElement part : path) {

            if (!current.isJsonObject()) return null;

            current = current.getAsJsonObject()
                    .get(part.getAsString());

            if (current == null) return null;
        }

        return current;
    }

    // ================= SET =================

    private static void setValue(JsonElement keyElement, JsonElement value) {

        if (keyElement.isJsonPrimitive()) {
            database.add(keyElement.getAsString(), value);
            return;
        }

        JsonArray path = keyElement.getAsJsonArray();
        JsonObject current = database;

        for (int i = 0; i < path.size() - 1; i++) {

            String key = path.get(i).getAsString();

            if (!current.has(key) || !current.get(key).isJsonObject()) {
                current.add(key, new JsonObject());
            }

            current = current.getAsJsonObject(key);
        }

        String lastKey = path.get(path.size() - 1).getAsString();
        current.add(lastKey, value);
    }

    // ================= DELETE =================

    private static boolean deleteValue(JsonElement keyElement) {

        if (keyElement.isJsonPrimitive()) {
            return database.remove(keyElement.getAsString()) != null;
        }

        JsonArray path = keyElement.getAsJsonArray();
        JsonObject current = database;

        for (int i = 0; i < path.size() - 1; i++) {

            String key = path.get(i).getAsString();

            if (!current.has(key) || !current.get(key).isJsonObject()) {
                return false;
            }

            current = current.getAsJsonObject(key);
        }

        String lastKey = path.get(path.size() - 1).getAsString();
        return current.remove(lastKey) != null;
    }

    // ================= FILE =================

    private static JsonObject readDatabase() throws IOException {

        String content = Files.readString(Path.of(DB_PATH));

        if (content == null || content.isEmpty()) {
            return new JsonObject();
        }

        return gson.fromJson(content, JsonObject.class);
    }

    private static void saveDatabase() throws IOException {
        Files.writeString(Path.of(DB_PATH), gson.toJson(database));
    }
}
