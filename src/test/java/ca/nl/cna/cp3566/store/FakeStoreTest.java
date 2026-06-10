package ca.nl.cna.cp3566.store;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ============================================================================
 *  THE TESTS — run them with:   ./mvnw test
 * ============================================================================
 *
 * New to JUnit? Read TESTING.docx first — it explains everything in this file.
 * The 30-second version:
 *
 *   - Each method marked @Test is one independent check. JUnit runs them all and
 *     reports which passed and which failed.
 *   - Inside a test you call your code and then ASSERT what should be true:
 *       assertEquals(expected, actual)   - they must be equal
 *       assertTrue(condition)            - condition must be true
 *       assertThrows(Type.class, () -> …)- that code must throw that exception
 *     If an assertion is false (or the code throws unexpectedly), the test FAILS.
 *   - @BeforeAll runs once before any test (here: bring the database up).
 *
 * Right now, on the unfinished starter, EVERY test fails (your methods throw
 * "TODO: implement …"). Your job is to make them all GREEN. They check the exact
 * behaviour LAB2.docx asks for: catalogue lookups, order pricing and the
 * 422/404/409 status codes, no overselling, and registration. They are also the
 * autograder — green here is most of your marks.
 *
 * These tests call your DAOs/services DIRECTLY (no HTTP server needed) through
 * AppContext, exactly like the web layer does.
 */
@DisplayName("FakeStore — Lab 2 acceptance tests")
public class FakeStoreTest {

    // Bring the schema + catalogue up once, before any test runs.
    @BeforeAll
    static void setUp() {
        Database.init();
    }

    // =====================================================================
    //  Catalogue  (JdbcProductDao)
    // =====================================================================

    @Test
    @DisplayName("findById returns a known product")
    void findById_returnsProduct() {
        var product = AppContext.productDao().findById(1);
        assertTrue(product.isPresent(), "Product 1 should exist in the seeded catalogue");
        assertFalse(product.get().title().isBlank(), "A product should have a title");
    }

    @Test
    @DisplayName("findById of a missing id returns empty")
    void findById_unknown_returnsEmpty() {
        assertTrue(AppContext.productDao().findById(10_000_000).isEmpty(),
                "A non-existent id should give Optional.empty(), not throw");
    }

    @Test
    @DisplayName("categories are non-empty and alphabetical")
    void categories_nonEmpty_sorted() {
        List<String> cats = AppContext.productDao().categories();
        assertFalse(cats.isEmpty(), "There should be at least one category");
        for (int i = 1; i < cats.size(); i++) {
            assertTrue(cats.get(i - 1).compareToIgnoreCase(cats.get(i)) <= 0,
                    "Categories should come back sorted");
        }
    }

    @Test
    @DisplayName("search filters by category and reports a total")
    void search_byCategory_filters() {
        String category = AppContext.productDao().categories().get(0);
        ProductQuery q = new ProductQuery(null, category, null, null, null, 1, 12);
        Page page = AppContext.productDao().search(q);

        assertTrue(page.total() > 0, "There should be matches in category " + category);
        assertFalse(page.items().isEmpty(), "The first page should have items");
        assertTrue(page.items().size() <= 12, "A page must not exceed its pageSize");
        for (Product p : page.items()) {
            assertTrue(p.category().equalsIgnoreCase(category),
                    "Every result must be in the requested category");
        }
    }

    // =====================================================================
    //  Orders  (JdbcOrderDao + StandardCheckoutService)
    // =====================================================================

    @Test
    @DisplayName("a good order is priced from the server with correct tax and total")
    void order_prices_and_totals() throws SQLException {
        setStock(1, 100);                                  // make sure product 1 is buyable
        double price = AppContext.productDao().findById(1).orElseThrow().price();

        OrderRequest cart = new OrderRequest("buyer@x.com",
                List.of(new OrderLine(1, 2)));
        CheckoutResult result = AppContext.checkoutService().checkout(cart, null);
        Order order = result.order();

        double expectedSubtotal = round2(price * 2);
        double expectedTax = round2(expectedSubtotal * 0.13);
        double expectedTotal = round2(expectedSubtotal + expectedTax);

        assertEquals(expectedSubtotal, order.subtotal(), 0.001, "subtotal = 2 x server price");
        assertEquals(expectedTax, order.tax(), 0.001, "tax = 13% of subtotal");
        assertEquals(expectedTotal, order.total(), 0.001, "total = subtotal + tax");
        assertEquals(price, order.lines().get(0).unitPrice(), 0.001, "unit price comes from the server");
        assertTrue(order.orderNumber().startsWith("FS-"), "order number should look like FS-XXXXXX");
        assertFalse(result.replayed(), "a fresh order is not a replay");
    }

    @Test
    @DisplayName("the placed order can be read back")
    void order_can_be_read_back() throws SQLException {
        setStock(1, 100);
        Order placed = AppContext.checkoutService()
                .checkout(new OrderRequest("buyer@x.com", List.of(new OrderLine(1, 1))), null).order();

        var found = AppContext.orderDao().findByNumber(placed.orderNumber());
        assertTrue(found.isPresent(), "GET should find the order we just placed");
        assertEquals(placed.total(), found.get().total(), 0.001);
    }

    @Test
    @DisplayName("empty cart -> 422")
    void order_emptyCart_422() {
        ApiException ex = assertThrows(ApiException.class,
                () -> AppContext.checkoutService().checkout(new OrderRequest("x@x.com", List.of()), null));
        assertEquals(422, ex.status());
    }

    @Test
    @DisplayName("quantity 0 -> 422")
    void order_quantityZero_422() {
        ApiException ex = assertThrows(ApiException.class, () -> AppContext.checkoutService()
                .checkout(new OrderRequest("x@x.com", List.of(new OrderLine(1, 0))), null));
        assertEquals(422, ex.status());
    }

    @Test
    @DisplayName("unknown product -> 404")
    void order_unknownProduct_404() {
        ApiException ex = assertThrows(ApiException.class, () -> AppContext.checkoutService()
                .checkout(new OrderRequest("x@x.com", List.of(new OrderLine(10_000_000, 1))), null));
        assertEquals(404, ex.status());
    }

    @Test
    @DisplayName("not enough stock -> 409, and stock is never oversold")
    void order_overStock_409() throws SQLException {
        setStock(2, 2);                                    // only 2 in stock
        ApiException ex = assertThrows(ApiException.class, () -> AppContext.checkoutService()
                .checkout(new OrderRequest("x@x.com", List.of(new OrderLine(2, 5))), null));
        assertEquals(409, ex.status(), "asking for 5 when 2 are left is a conflict");
        assertEquals(2, currentStock(2), "a rejected order must not change stock");
    }

    @Test
    @DisplayName("a rejected multi-line order leaks no stock (transaction rolls back)")
    void order_rejected_leaksNoStock() throws SQLException {
        setStock(3, 50);
        setStock(4, 0);                                    // second line will fail (sold out)
        int before = currentStock(3);
        assertThrows(ApiException.class, () -> AppContext.checkoutService().checkout(
                new OrderRequest("x@x.com", List.of(new OrderLine(3, 1), new OrderLine(4, 1))), null));
        assertEquals(before, currentStock(3),
                "line 1's stock must be handed back when line 2 fails — one transaction");
    }

    @Test
    @DisplayName("same Idempotency-Key replays the original order; a new key makes a new order")
    void order_idempotency() throws SQLException {
        setStock(1, 100);
        String key = "test-key-" + System.nanoTime();      // unique so the test repeats cleanly
        OrderRequest cart = new OrderRequest("buyer@x.com", List.of(new OrderLine(1, 1)));

        CheckoutResult first = AppContext.checkoutService().checkout(cart, key);
        CheckoutResult again = AppContext.checkoutService().checkout(cart, key);
        assertEquals(first.order().orderNumber(), again.order().orderNumber(),
                "the same key must return the SAME order");
        assertTrue(again.replayed(), "the second call is a replay");

        CheckoutResult different = AppContext.checkoutService().checkout(cart, "other-" + System.nanoTime());
        assertFalse(different.order().orderNumber().equals(first.order().orderNumber()),
                "a different key is a different order");
    }

    // =====================================================================
    //  Registration  (JdbcUserDao + StandardRegistrationService)
    // =====================================================================

    @Test
    @DisplayName("a valid registration succeeds and stores a HASH, never the plaintext")
    void register_storesHash_notPlaintext() throws SQLException {
        String email = uniqueEmail();
        String password = "hunter2pass";
        User user = AppContext.registrationService().register(new RegisterRequest("Ann Lee", email, password));

        assertTrue(user.id() > 0, "a new account gets an id");
        String stored = storedHash(email.toLowerCase());
        assertNotNull(stored, "the account should be in the users table");
        assertFalse(stored.equals(password), "the stored value must NOT be the plaintext password");
        assertTrue(PasswordHash.verify(password, stored), "the stored hash must verify the password");
    }

    @Test
    @DisplayName("registering the same email twice -> 409")
    void register_duplicate_409() {
        String email = uniqueEmail();
        AppContext.registrationService().register(new RegisterRequest("Ann", email, "hunter2pass"));
        ApiException ex = assertThrows(ApiException.class, () -> AppContext.registrationService()
                .register(new RegisterRequest("Ann Again", email.toUpperCase(), "hunter2pass")));
        assertEquals(409, ex.status(), "duplicate email is a conflict, case-insensitive");
    }

    @Test
    @DisplayName("bad input -> 422 with a message per field")
    void register_badInput_422() {
        ApiException ex = assertThrows(ApiException.class, () -> AppContext.registrationService()
                .register(new RegisterRequest("", "not-an-email", "short")));
        assertEquals(422, ex.status());
        Map<String, String> fields = ex.fields();
        assertNotNull(fields, "validation errors should carry per-field messages");
        assertTrue(fields.containsKey("name"), "blank name should be reported");
        assertTrue(fields.containsKey("email"), "bad email should be reported");
        assertTrue(fields.containsKey("password"), "short password should be reported");
    }

    // =====================================================================
    //  Small test helpers (raw JDBC — fine inside a test)
    // =====================================================================

    /** A unique email per run, so re-running the tests never hits a stale duplicate. */
    private static String uniqueEmail() {
        return "u" + System.nanoTime() + "@x.com";
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static void setStock(int id, int n) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE products SET stock = ? WHERE id = ?")) {
            ps.setInt(1, n);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    private static int currentStock(int id) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT stock FROM products WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    private static String storedHash(String emailLower) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT password_hash FROM users WHERE email = ?")) {
            ps.setString(1, emailLower);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        }
    }
}
