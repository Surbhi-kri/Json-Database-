package client;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import server.Main;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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

        serverThread.setDaemon(true);
        serverThread.start();

        // wait for server to start
        Thread.sleep(1000);
    }

    @Test
    void testClientSetRequest() {

        String[] args = {
                "-t", "set",
                "-k", "name",
                "-v", "Sukumari"
        };

        assertDoesNotThrow(() -> client.Main.main(args));
    }

    @Test
    void testClientGetRequest() {

        String[] args = {
                "-t", "get",
                "-k", "name"
        };

        assertDoesNotThrow(() -> client.Main.main(args));
    }
}
