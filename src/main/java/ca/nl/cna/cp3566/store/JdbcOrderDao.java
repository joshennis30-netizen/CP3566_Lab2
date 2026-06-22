package ca.nl.cna.cp3566.store;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ====================  YOUR CODE  ====================
 * The JDBC orders repository. Implement the three methods from {@link OrderDao}.

 * insert() is called from inside the checkout transaction and is handed that
 * connection — use it, and do NOT commit (the service commits). The reads open
 * their own connection.
 * =====================================================
 */
public class JdbcOrderDao extends OrderDao {

    @Override
    public void insert(Connection c, Order order, String idempotencyKey) throws SQLException {
        // TODO (use the GIVEN connection c — this is part of the order transaction):
        //   1. INSERT INTO orders
        //        (order_number, email, subtotal, tax, total, placed_at, idempotency_key)
        //      VALUES (?, ?, ?, ?, ?, ?, ?)
        //      - idempotencyKey may be null; setString(7, null) is fine
        //        (multiple NULLs are allowed by the UNIQUE column).
        //   2. INSERT INTO order_lines
        //        (order_number, product_id, title, unit_price, quantity, line_total)
        //      VALUES (?, ?, ?, ?, ?, ?)   -- once per ConfirmedLine in order.lines()
        //      (a batch is tidy: addBatch() in the loop, then executeBatch()).
        List<PreparedStatement> statements = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO orders (order_number, email, subtotal, tax, total, placed_at, idempotency_key) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, order.orderNumber());
            ps.setString(2, order.email());
            ps.setDouble(3, order.subtotal());
            ps.setDouble(4, order.tax());
            ps.setDouble(5, order.total());
            ps.setString(6, order.placedAt());
            ps.setString(7, idempotencyKey);
            ps.executeUpdate();
        }
        try (PreparedStatement ps2 = c.prepareStatement("INSERT INTO order_lines (order_number, product_id, title, unit_price, quantity, line_total) VALUES (?, ?, ?, ?, ?, ?)")) {
            for (ConfirmedLine line : order.lines()) {
                ps2.setString(1, order.orderNumber());
                ps2.setInt(2, line.productId());
                ps2.setString(3, line.title());
                ps2.setDouble(4, line.unitPrice());
                ps2.setDouble(5, line.quantity());
                ps2.setDouble(6, line.lineTotal());
                ps2.addBatch();
            }
            statements.add(ps2);
        }
        PreparedStatement ps = statements.get(0);
        ps.executeBatch();
    }

    @Override
    public Optional<Order> findByNumber(String orderNumber) {
        // TODO:
        //   - SELECT email, subtotal, tax, total, placed_at FROM orders WHERE order_number = ?
        //     (empty -> Optional.empty()).
        //   - SELECT product_id, title, unit_price, quantity, line_total
        //       FROM order_lines WHERE order_number = ? ORDER BY id
        //     build a List<ConfirmedLine>.
        //   - return Optional.of(new Order(orderNumber, email, lines, subtotal, tax, total, placedAt)).
        List<ConfirmedLine> lines = new ArrayList<>();
        ArrayList<String> strings = new ArrayList<>();
        ArrayList<Double> doubles = new ArrayList<>();
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement("SELECT email, subtotal, tax, total, placed_at FROM orders WHERE order_number = ?");
             PreparedStatement ps2 = c.prepareStatement("SELECT product_id, title, unit_price, quantity, line_total FROM order_lines WHERE order_number = ? ORDER BY id")) {
            ps.setString(1, orderNumber);
            ps2.setString(1, orderNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                } else if (rs.next()) {
                    String email1 = rs.getString("email");
                    double subtotal1 = rs.getDouble("subtotal");
                    double tax1 = rs.getDouble("tax");
                    double total1 = rs.getDouble("total");
                    String placedAt1 = rs.getString("placed_at");
                    strings.add(email1);
                    strings.add(placedAt1);
                    doubles.add(subtotal1);
                    doubles.add(tax1);
                    doubles.add(total1);
                }
            }
            try (ResultSet rs2 = ps2.executeQuery()) {
                if (rs2.next()) {
                    int product_id = rs2.getInt("product_id");
                    String title = rs2.getString("title");
                    Double unit_price = rs2.getDouble("unit_price");
                    int quantity = rs2.getInt("quantity");
                    Double line_total = rs2.getDouble("line_total");
                    lines.add(new ConfirmedLine(product_id, title, unit_price, quantity, line_total));
                }
            }
        } catch (SQLException e) {}
        String email = strings.get(0);
        String placedAt = strings.get(1);
        Double subtotal = doubles.get(0);
        Double tax = doubles.get(1);
        Double total = doubles.get(2);
        return Optional.of(new Order(orderNumber, email, lines, subtotal, tax, total, placedAt));
    }

    @Override
    public Optional<Order> findByIdempotencyKey(String idempotencyKey) {
        // TODO:
        //   SELECT order_number FROM orders WHERE idempotency_key = ?
        //   if found, reuse findByNumber(thatNumber); else Optional.empty().
        ArrayList<Optional> orders = new ArrayList<>();
        try(Connection c = open();
            PreparedStatement ps = c.prepareStatement("SELECT order_number FROM orders WHERE idempotency_key = ?")) {
            ps.setString(1, idempotencyKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                String orderNumber = rs.getString("order_number");
                orders.add(findByNumber(orderNumber));
            }
        } catch (SQLException e) {}
        return orders.get(0);
    }
}
