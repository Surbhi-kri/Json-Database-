package client;

import com.google.gson.*;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    private static final String ADDRESS = "localhost";

    private static final int PORT = 23456;
    private static final String DATA_PATH = "src/client/data/";
    private static final Gson gson = new GsonBuilder().create();

    public static void main(String[] args) {

        System.out.println("Client started!");

        try (
                Socket socket = new Socket(ADDRESS, PORT);
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader input = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()))
        ) {

            JsonObject request;

            // ----- FILE INPUT -----
            if (containsArg(args, "-in")) {

                String fileName = getArgValue(args, "-in");
                String content = Files.readString(Path.of(DATA_PATH + fileName));

                request = gson.fromJson(content, JsonObject.class);

                System.out.println("Sent:");
                System.out.println(content);

            } else {

                request = new JsonObject();

                if (containsArg(args, "-t")) {
                    request.addProperty("type", getArgValue(args, "-t"));
                }

                if (containsArg(args, "-k")) {
                    String keyValue = getArgValue(args, "-k");

                    if (keyValue.startsWith("[")) {
                        request.add("key",
                                JsonParser.parseString(keyValue));
                    } else {
                        request.addProperty("key", keyValue);
                    }
                }

                if (containsArg(args, "-v")) {
                    String value = getArgValue(args, "-v");

                    try {
                        request.add("value",
                                JsonParser.parseString(value));
                    } catch (Exception e) {
                        request.addProperty("value", value);
                    }
                }

                System.out.println("Sent: " + gson.toJson(request));
            }

            output.println(gson.toJson(request));

            String responseJson = input.readLine();

            System.out.println("Received:");
            System.out.println(responseJson);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean containsArg(String[] args, String key) {
        for (String arg : args) {
            if (arg.equals(key)) return true;
        }
        return false;
    }

    private static String getArgValue(String[] args, String key) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(key)) {
                return args[i + 1];
            }
        }
        return "";
    }
}
