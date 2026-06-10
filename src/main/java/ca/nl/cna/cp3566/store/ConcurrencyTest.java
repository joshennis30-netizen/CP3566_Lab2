package ca.nl.cna.cp3566.store;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * VERIFICATION (given). Proves your reserve() + transaction do not oversell.
 *
 * Sets product 1's stock to exactly 10, then fires 50 buyers at it at the same
 * instant. A correct implementation lets EXACTLY 10 succeed, turns 40 away with
 * 409, and leaves final stock at 0 — never negative. If you see more than 10
 * succeed, your stock take is racy (a SELECT-then-UPDATE instead of one guarded
 * UPDATE), or it is not inside the transaction.
 *
 * Run it once your DAOs/services are implemented:
 *   ./mvnw -q compile exec:java -Dexec.mainClass=ca.nl.cna.cp3566.store.ConcurrencyTest
 */
public final class ConcurrencyTest {

    private static final int PRODUCT_ID = 1;
    private static final int START_STOCK = 10;
    private static final int SHOPPERS = 50;

    public static void main(String[] args) throws Exception {
        Database.init();
        setStock(PRODUCT_ID, START_STOCK);
        System.out.printf("Product %d stock set to %d. Firing %d concurrent buyers…%n",
                PRODUCT_ID, START_STOCK, SHOPPERS);

        CheckoutService checkout = AppContext.checkoutService();
        OrderRequest cart = new OrderRequest("rush@x.com",
                List.of(new OrderLine(PRODUCT_ID, 1)));

        AtomicInteger ok = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(SHOPPERS);
        CountDownLatch ready = new CountDownLatch(SHOPPERS);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(SHOPPERS);

        for (int i = 0; i < SHOPPERS; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();                       // line up, then rush together
                    checkout.checkout(cart, null);
                    ok.incrementAndGet();
                } catch (ApiException e) {
                    if (e.status() == 409) conflict.incrementAndGet();
                    else other.incrementAndGet();
                } catch (Exception e) {
                    other.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        go.countDown();
        done.await();
        pool.shutdown();

        int finalStock = currentStock(PRODUCT_ID);
        System.out.println("------------------------------------------------------");
        System.out.printf("  succeeded (201): %d   (expected %d)%n", ok.get(), START_STOCK);
        System.out.printf("  rejected  (409): %d   (expected %d)%n", conflict.get(), SHOPPERS - START_STOCK);
        System.out.printf("  unexpected     : %d   (expected 0)%n", other.get());
        System.out.printf("  final stock    : %d   (expected 0, never negative)%n", finalStock);
        System.out.println("------------------------------------------------------");

        boolean pass = ok.get() == START_STOCK
                && conflict.get() == SHOPPERS - START_STOCK
                && other.get() == 0
                && finalStock == 0;
        System.out.println(pass ? "PASS — no overselling." : "FAIL — see numbers above.");
        System.exit(pass ? 0 : 1);
    }

    private static void setStock(int id, int qty) throws Exception {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE products SET stock = ? WHERE id = ?")) {
            ps.setInt(1, qty);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    private static int currentStock(int id) throws Exception {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT stock FROM products WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private ConcurrencyTest() { }
}
