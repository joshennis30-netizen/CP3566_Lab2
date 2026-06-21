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
    @Override
    public Optional<Product> findById(int id){
        // TODO:
        //   SELECT * FROM products WHERE id = ?
        //   - open() a connection in try-with-resources, bind id, run the query.
        //   - if there is a row, return Optional.of(mapRow(rs)); else Optional.empty().
        //   - on SQLException, throw ApiException.server("...").
        // Opens connection
        try (Connection conn = open();
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM products WHERE id = ?")) {
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
        } catch (SQLException e) {
            throw ApiException.server(e.getMessage());
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

        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        // Create list of bound values
        ArrayList<Object> boundList = new ArrayList<>();
        if (query.q() != null) {
            where.append(" AND (LOWER(title) LIKE ? OR LOWER(description) LIKE ?)");
            boundList.add("%" + query.q() + "%");
            boundList.add("%" + query.q() + "%");
        }
        if (query.category() != null) {
            where.append(" AND LOWER(category) = ?");
            boundList.add(query.category());
        }
        if (query.minPrice() != null) {
            where.append(" AND price >= ?");
            boundList.add(query.minPrice());
        }
        if (query.maxPrice() != null) {
            where.append(" AND price <= ?");
            boundList.add(query.maxPrice());
        }

        String orderBy = (" ORDER BY id ASC");
        // Product list for first PreparedStatement
        List<Product> items = new ArrayList<>();
        // Initialize total
        List<Integer> totals = new ArrayList<>();

        //Prepared statements
        try (
           Connection conn = open();
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM products" + where + orderBy + " LIMIT ? OFFSET ?")) {
            int i = 1;
                for (Object bound : boundList) {
                    ps.setObject(i++, bound);
                }
                ps.setInt(i++, query.safeSize());
                ps.setInt(i, query.offset());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(mapRow(rs));
                    }
                }
            try (PreparedStatement ps2 = conn.prepareStatement("SELECT COUNT(*) FROM products " + where)) {
                int i1 = 1;
                for (Object bound1 : boundList) {
                    ps2.setObject(i1++, bound1);
                }
                try (ResultSet rs2 = ps2.executeQuery()) {
                    rs2.next();
                    int total1 = rs2.getInt(1);
                    totals.add(total1);
                }
            }
        } catch(SQLException e) {
            throw ApiException.server(e.getMessage());
        }
        int total = totals.get(0);
        return new Page(items, total, query.safePage(), query.safeSize());
        //throw new UnsupportedOperationException("TODO: implement search");
    }

    @Override
    public List<String> categories() {
        // TODO:
        //   SELECT DISTINCT category FROM products ORDER BY category
        //   collect the strings into a List and return it.
        List<String> categories = new ArrayList<>();
        try (Connection conn = open();
            Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT DISTINCT category FROM products ORDER BY category")) {
                while (rs.next()) {
                    String category = rs.getString("category");
                    categories.add(category);
                }
            }
        } catch(SQLException e) {}
        return categories;
        //throw new UnsupportedOperationException("TODO: implement categories");
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

        List<Boolean> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?")) {
            ps.setInt(1, quantity);
            ps.setInt(2, productId);
            ps.setInt(3, quantity);
            result.add(ps.executeUpdate() == 1);
        } catch(SQLException e) {}
        return result.get(0);
        //throw new UnsupportedOperationException("TODO: implement reserve");
    }
}
