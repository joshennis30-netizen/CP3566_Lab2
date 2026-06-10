package ca.nl.cna.cp3566.store;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * INFRASTRUCTURE (given — do not change).
 *
 * The database: an embedded H2 file under ./data, using the same JDBC you wrote
 * in Module 2. It owns three things:
 *
 *   getConnection()  - hand out a JDBC connection (your DAOs call this, usually
 *                      via AbstractDao.open()).
 *   init()           - create the schema and seed the catalogue (once) at startup.
 *   inTransaction()  - the shared "do this as one all-or-nothing unit" template
 *                      that the CheckoutService and RegistrationService use.
 *
 * Schema:
 *   products(id, title, category, price, description, image,
 *            rating_rate, rating_count, stock)
 *   users(id, name, email UNIQUE, password_hash, created_at)
 *   orders(order_number PK, email, subtotal, tax, total, placed_at,
 *          idempotency_key UNIQUE)
 *   order_lines(id, order_number FK, product_id, title, unit_price,
 *               quantity, line_total)
 */
public final class Database {

    // File-based so data survives a restart. AUTO_SERVER lets a second process
    // (e.g. the H2 console, or ConcurrencyTest in another JVM) open the same
    // file. LOCK_TIMEOUT gives concurrent transactions time to serialise.
    private static final String URL =
            "jdbc:h2:file:./data/fakestore;AUTO_SERVER=TRUE;LOCK_TIMEOUT=10000";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    private Database() { }

    /** A fresh connection. Callers wrap this in try-with-resources. */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /** Create the schema (if needed) and seed the catalogue the first time. */
    public static void init() {
        try (Connection c = getConnection()) {
            createSchema(c);
            seedProductsIfEmpty(c);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialise the database", e);
        }
    }

    /**
     * A unit of work that runs inside a transaction and may return a value.
     * It is handed an open connection and may throw SQLException.
     */
    @FunctionalInterface
    public interface TxWork<R> {
        R run(Connection c) throws SQLException;
    }

    /**
     * TEMPLATE METHOD for a transaction. Opens a connection, turns auto-commit
     * OFF, runs the work, and COMMITS. If the work throws anything, it ROLLS BACK
     * and rethrows. This is the single definition of "all-or-nothing" in the app,
     * so no individual method can get commit/rollback wrong.
     */
    public static <R> R inTransaction(TxWork<R> work) {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                R result = work.run(c);
                c.commit();
                return result;
            } catch (RuntimeException | SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            // Re-throw as the app's own exception type so the mapper formats it.
            if (e instanceof java.sql.SQLIntegrityConstraintViolationException
                    || "23505".equals(e.getSQLState())) {
                // a UNIQUE/PK violation — let callers translate to 409 if they want
                throw new ApiException(409, "conflict", "A uniqueness constraint was violated.");
            }
            throw ApiException.server("Transaction failed: " + e.getMessage());
        }
    }

    // ---- schema + seeding ---------------------------------------------------

    private static void createSchema(Connection c) throws SQLException {
        String[] ddl = {
            """
            CREATE TABLE IF NOT EXISTS products (
                id            INT PRIMARY KEY,
                title         VARCHAR(255) NOT NULL,
                category      VARCHAR(64)  NOT NULL,
                price         DOUBLE       NOT NULL,
                description   VARCHAR(1024),
                image         VARCHAR(512),
                rating_rate   DOUBLE       NOT NULL,
                rating_count  INT          NOT NULL,
                stock         INT          NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS users (
                id            INT AUTO_INCREMENT PRIMARY KEY,
                name          VARCHAR(255) NOT NULL,
                email         VARCHAR(255) NOT NULL UNIQUE,
                password_hash VARCHAR(512) NOT NULL,
                created_at    VARCHAR(40)  NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS orders (
                order_number    VARCHAR(16) PRIMARY KEY,
                email           VARCHAR(255),
                subtotal        DOUBLE NOT NULL,
                tax             DOUBLE NOT NULL,
                total           DOUBLE NOT NULL,
                placed_at       VARCHAR(40) NOT NULL,
                idempotency_key VARCHAR(255) UNIQUE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS order_lines (
                id           INT AUTO_INCREMENT PRIMARY KEY,
                order_number VARCHAR(16) NOT NULL,
                product_id   INT NOT NULL,
                title        VARCHAR(255) NOT NULL,
                unit_price   DOUBLE NOT NULL,
                quantity     INT NOT NULL,
                line_total   DOUBLE NOT NULL,
                CONSTRAINT fk_line_order FOREIGN KEY (order_number)
                    REFERENCES orders(order_number)
            )
            """
        };
        try (Statement st = c.createStatement()) {
            for (String sql : ddl) st.execute(sql);
        }
    }

    /** Load the catalogue from the ASOS CSV the first time only (row-count guard). */
    private static void seedProductsIfEmpty(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM products")) {
            rs.next();
            if (rs.getInt(1) > 0) return;   // already seeded
        }

        List<CsvProductSource.SeedProduct> seed = CsvProductSource.load();
        String sql = """
            INSERT INTO products
                (id, title, category, price, description, image,
                 rating_rate, rating_count, stock)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        boolean autoCommit = c.getAutoCommit();
        c.setAutoCommit(false);
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (CsvProductSource.SeedProduct sp : seed) {
                Product p = sp.product();
                ps.setInt(1, p.id());
                ps.setString(2, p.title());
                ps.setString(3, p.category());
                ps.setDouble(4, p.price());
                ps.setString(5, p.description());
                ps.setString(6, p.image());
                ps.setDouble(7, p.rating().rate());
                ps.setInt(8, p.rating().count());
                ps.setInt(9, sp.stock());
                ps.addBatch();
            }
            ps.executeBatch();
            c.commit();
        } catch (SQLException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(autoCommit);
        }
        System.out.println("Seeded " + seed.size() + " real ASOS products into H2.");
    }
}
