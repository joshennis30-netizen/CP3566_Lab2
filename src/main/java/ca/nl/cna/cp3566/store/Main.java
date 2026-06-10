package ca.nl.cna.cp3566.store;

import jakarta.ws.rs.core.UriBuilder;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.File;
import java.net.URI;

/**
 * INFRASTRUCTURE (given — do not change). The launcher.
 *
 * Same shape as Module 4's Main — boot Grizzly, hand requests to Jersey — plus:
 *   - it brings the database up first (schema + one-time ASOS seed),
 *   - it serves the HTML/CSS/JS so you can open the store in a browser.
 *
 * Run it (green arrow, or `./mvnw exec:java`) and open:
 *   http://localhost:8081/              the store
 *   http://localhost:8081/api/products  the raw API
 *
 * NOTE: until you implement your DAOs/services and they are wired in AppContext,
 * the API calls will fail (the catalogue page will show an error). Implement
 * them and the store comes alive — that is the lab.
 */
public class Main {

    private static final int PORT = 8081;

    public static final URI BASE_URI =
            UriBuilder.fromUri("http://localhost/").port(PORT).path("api").build();

    public static void main(String[] args) {
        // Bring the database up: create the schema, seed the catalogue once.
        Database.init();

        // Scan THIS package for @Path classes and @Provider exception mappers.
        ResourceConfig config = new ResourceConfig().packages(Main.class.getPackageName());

        // Register Jackson WITHOUT its built-in exception mappers, so our own
        // JsonExceptionMapper owns malformed-JSON errors (uniform 400).
        config.register(JacksonFeature.withoutExceptionMappers());

        // Build the server but do not start it yet ('false'), so we can add the
        // static-file handler first.
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, config, false);

        File webDir = new File("src/main/resources/web").getAbsoluteFile();
        if (!webDir.isDirectory()) {
            throw new IllegalStateException(
                    "Run this from the project root. Could not find: " + webDir);
        }
        StaticHttpHandler staticFiles = new StaticHttpHandler(webDir.getAbsolutePath());
        server.getServerConfiguration().addHttpHandler(staticFiles, "/");

        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException("Could not start the server on port " + PORT, e);
        }

        System.out.println();
        System.out.println("===========================================================");
        System.out.println(" FakeStore is running.");
        System.out.println("   Store : http://localhost:" + PORT + "/");
        System.out.println("   API   : http://localhost:" + PORT + "/api/products");
        System.out.println(" Stop: red square in IntelliJ (or Ctrl+C in the terminal).");
        System.out.println("===========================================================");

        try {
            Thread.sleep(Long.MAX_VALUE);   // block until the JVM is stopped
        } catch (InterruptedException ignored) {
            server.shutdownNow();
        }
    }
}
