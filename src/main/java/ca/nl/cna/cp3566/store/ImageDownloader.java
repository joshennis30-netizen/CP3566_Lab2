package ca.nl.cna.cp3566.store;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TOOLING (given — run ONCE; instructors/maintainers, not students).
 *
 * Downloads the catalogue images referenced in the ASOS CSV and stores them
 * LOCALLY under src/main/resources/web/images/<id>.jpg, so the running store
 * serves its own images and STUDENTS NEVER FETCH ANYTHING AT RUNTIME.
 *
 * It pulls the first image of each of the 5000 seeded products (one per product —
 * that is the image the catalogue actually shows), reusing the same cleaned list
 * {@link CsvProductSource#load()} produces. It skips files that already exist, so
 * you can re-run it to fill in gaps, and it never aborts the whole run on a single
 * failure — it just records it.
 *
 * Run it from the project root:
 *   ./mvnw -q compile exec:java -Dexec.mainClass=ca.nl.cna.cp3566.store.ImageDownloader
 *
 * NOTE: the ASOS CDN (Akamai) rate-limits/blocks some networks and datacenter IPs,
 * and the dataset is from 2023 so a few products are delisted (404). Expect a
 * handful of failures; {@link CsvProductSource} falls back to a placeholder image
 * for any id that has no local file, so the store still renders cleanly. Run this
 * on a normal residential/office connection for the best hit rate.
 */
public final class ImageDownloader {

    private static final Path OUT_DIR = Path.of("src", "main", "resources", "web", "images");
    private static final int THREADS = 8;
    private static final String UA =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
          + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUT_DIR);

        // Reuse the SAME cleaned product list the database is seeded from, so the
        // <id>.jpg filenames line up exactly with the product ids in the store.
        List<CsvProductSource.SeedProduct> products = CsvProductSource.load();
        System.out.println("Downloading first image for " + products.size()
                + " products into " + OUT_DIR.toAbsolutePath());

        HttpClient http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        AtomicInteger ok = new AtomicInteger();
        AtomicInteger skipped = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        for (CsvProductSource.SeedProduct sp : products) {
            int id = sp.product().id();
            String url = sp.product().image();
            Path target = OUT_DIR.resolve(id + ".jpg");

            pool.submit(() -> {
                if (Files.exists(target)) { skipped.incrementAndGet(); return; }
                // Only the remote ASOS/CDN images are worth fetching; a picsum
                // placeholder url means the row had no image — leave it to the fallback.
                if (url == null || url.startsWith("/") || url.contains("picsum.photos")) {
                    failed.incrementAndGet();
                    return;
                }
                try {
                    HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                            .timeout(Duration.ofSeconds(25))
                            .header("User-Agent", UA)
                            .header("Referer", "https://www.asos.com/")
                            .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                            .GET().build();
                    HttpResponse<byte[]> res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
                    if (res.statusCode() == 200 && res.body().length > 0) {
                        Files.write(target, res.body());
                        int n = ok.incrementAndGet();
                        if (n % 250 == 0) System.out.println("  ...downloaded " + n);
                    } else {
                        failed.incrementAndGet();
                    }
                } catch (IOException | InterruptedException e) {
                    failed.incrementAndGet();
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.HOURS);

        System.out.println("------------------------------------------------------");
        System.out.println("  downloaded : " + ok.get());
        System.out.println("  skipped    : " + skipped.get() + " (already on disk)");
        System.out.println("  failed     : " + failed.get() + " (blocked/404/no-url -> placeholder used)");
        System.out.println("  folder     : " + OUT_DIR.toAbsolutePath());
        System.out.println("------------------------------------------------------");
        System.out.println("Re-run to retry the failures. CsvProductSource now serves /images/<id>.jpg "
                + "for any product that has a local file.");
    }

    private ImageDownloader() { }
}
