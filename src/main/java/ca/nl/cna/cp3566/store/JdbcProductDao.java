package ca.nl.cna.cp3566.store;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ====================  YOUR CODE  ====================
 * The JDBC catalogue repository. Implement the four methods the abstract
 * {@link ProductDao} declares. Reuse the given {@code mapRow(rs)} helper to build
 * a Product from a result row. Every value that comes from the request MUST be a
 * bound "?" parameter (PreparedStatement) — never string-concatenated SQL.

 * Remove each {@code throw new UnsupportedOperationException(...)} as you go, and
 * write enough inline comments that a marker can follow your reasoning.
 * =====================================================
 */
public class JdbcProductDao extends ProductDao {
    // Assign URL, USER, and PASS to use connection
    String URL = "jdbc:h2:./data/fakestore.mv.db;MODE=MySQL;DATABASE_TO_LOWER=TRUE";
    String USER = "sa";
    String PASS = "";
    @Override
    public Optional<Product> findById(int id){
        // TODO:
        //   SELECT * FROM products WHERE id = ?
        //   - open() a connection in try-with-resources, bind id, run the query.
        //   - if there is a row, return Optional.of(mapRow(rs)); else Optional.empty().
        //   - on SQLException, throw ApiException.server("...").
        try {
            // Opens connection
            Connection conn = DriverManager.getConnection(URL, USER, PASS);
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM products WHERE id = ?");
            // bound id
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Returns row if it exists
                    return Optional.of(mapRow(rs));
                } else {
                    return Optional.empty();
                }
            }
        } catch(SQLException e) {
            throw ApiException.server("...");
        }
    }

    @Override
    public Page search(ProductQuery query) {
        // TODO: build ONE shared WHERE clause + a list of bound values, then use
        //       it for both the page query and the COUNT(*) so they cannot disagree.
        //
        //   WHERE 1 = 1
        //     + (q?       -> " AND (LOWER(title) LIKE ? OR LOWER(description) LIKE ?)"  with "%"+q+"%")
        //     + (category?-> " AND LOWER(category) = ?")
        //     + (minPrice?-> " AND price >= ?")
        //     + (maxPrice?-> " AND price <= ?")
        //
        //   ORDER BY: pick a FIXED column from a whitelist based on query.sort()
        //     price_asc -> price ASC, price_desc -> price DESC,
        //     rating -> rating_rate DESC, title -> LOWER(title) ASC, else id ASC.
        //     (Never put the raw sort string into the SQL.)
        //
        //   Page query: SELECT * FROM products <where> <orderBy> LIMIT ? OFFSET ?
        //     bind query.safeSize() and query.offset().
        //   Count:      SELECT COUNT(*) FROM products <where>   (same bound values)
        //
        //   return new Page(items, total, query.safePage(), query.safeSize());
        StringBuilder where = new StringBuilder("WHERE (LOWER(q) LIKE ?) AND (LOWER(category) = ?) AND (minPrice >= ?) AND (maxPrice <= ?)");
        ArrayList<Object> boundList = new ArrayList<Object>();
        boundList.add(query.q());
        boundList.add(query.category());
        boundList.add(query.minPrice());
        boundList.add(query.maxPrice());

        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASS);
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM products <where> <orderBy> LIMIT ? OFFSET ?");
            PreparedStatement ps2 = conn.prepareStatement("SELECT COUNT(*) FROM products <where>");
        } catch(SQLException e) {}

        throw new UnsupportedOperationException("TODO: implement search");
    }

    @Override
    public List<String> categories() {
        // TODO:
        //   SELECT DISTINCT category FROM products ORDER BY category
        //   collect the strings into a List and return it.
        try {
            // Opens connection
            Connection conn = DriverManager.getConnection(URL, USER, PASS);
            Statement stmt = conn.createStatement();
            try (ResultSet rs = stmt.executeQuery("SELECT DISTINCT category FROM products ORDER BY category")) {
                List<String> categories = new ArrayList<>();
                while (rs.next()) {
                    categories.add(rs.getString("category"));
                }
                return categories;
            }
        } catch(SQLException e) {}
        return null;
    }

    @Override
    public boolean reserve(Connection c, int productId, int quantity) throws SQLException {
        // TODO: take stock with ONE guarded UPDATE on the GIVEN connection c
        //       (do NOT open your own — you must be inside the order's transaction):
        //
        //   UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?
        //
        //   Return (executeUpdate() == 1). 0 rows updated means "not enough stock"
        //   (the service turns that into a 409). Because the check and the take
        //   are one atomic statement under a row lock, two shoppers can never both
        //   buy the last unit. THINK ABOUT why a SELECT-then-UPDATE would not be safe.
        try {
            PreparedStatement ps = c.prepareStatement("UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?");
            ps.setInt(2, productId);
            ps.setInt(3, quantity);
            return(ps.executeUpdate() == 1);

        } catch(SQLException e) {}
        return false;
    }
}
