package ca.nl.cna.cp3566.store;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * ARCHITECTURE (given contract — you implement it in JdbcProductDao).
 *
 * The catalogue's data-access CONTRACT: everything the rest of the app needs to
 * do with products, named in terms of WHAT, not HOW. The web layer ({@link
 * Products}, {@link Categories}) depends only on this abstract type, so the
 * storage technology (here, JDBC + H2) can change without touching the web layer.
 * That is the Dependency Inversion idea: high-level code depends on an
 * abstraction, the low-level JDBC depends on the same abstraction.
 *
 * The abstract methods below are your TODO list for the catalogue. Implement them
 * in {@link JdbcProductDao}. A ready-made row mapper (mapRow) is provided so all
 * three read methods can build a Product the same way — reuse it.
 *
 *   findById   one product by id (or empty)
 *   search     filter + sort + paginate, returning a Page (items + total)
 *   categories the distinct category names, for the dropdown
 *   reserve    take stock atomically inside an existing transaction (the 409 check)
 */
public abstract class ProductDao extends AbstractDao {

    /** One product by id, or Optional.empty() if there is no such product. */
    public abstract Optional<Product> findById(int id);

    /** Filter/sort/paginate the catalogue; total is the full match count. */
    public abstract Page search(ProductQuery query);

    /** Every distinct category name, in a stable (alphabetical) order. */
    public abstract List<String> categories();

    /**
     * Take {@code quantity} units of product {@code productId} if — and only if —
     * that many are in stock, using the CALLER'S transaction connection {@code c}
     * (do NOT open your own). Returns true if the stock was taken, false if there
     * was not enough. This is the oversell-proof core: do it with ONE guarded
     * statement, not a read-then-write (see the hint in JdbcProductDao).
     */
    public abstract boolean reserve(Connection c, int productId, int quantity) throws SQLException;

    /**
     * GIVEN helper — build a Product (with its nested Rating) from the current
     * row of a {@code SELECT * FROM products ...} result. Reuse this in findById
     * and search so the mapping lives in one place.
     */
    protected Product mapRow(ResultSet rs) throws SQLException {
        Rating rating = new Rating(rs.getDouble("rating_rate"), rs.getInt("rating_count"));
        return new Product(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("category"),
                rs.getDouble("price"),
                rs.getString("description"),
                rs.getString("image"),
                rating);
    }
}
