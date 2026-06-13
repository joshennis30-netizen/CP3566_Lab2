package ca.nl.cna.cp3566.store;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
        try {
            PreparedStatement ps = c.prepareStatement("INSERT INTO orders (order_number, email, subtotal, tax, total, placed_at, idempotency_key) VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setString(1, order.orderNumber());
            ps.setString(2, order.email());
            ps.setDouble(3, order.subtotal());
            ps.setDouble(4, order.tax());
            ps.setDouble(5, order.total());
            ps.setString(6, order.placedAt());
            ps.setString(7, null);
        } catch(SQLException e) {}
        throw new UnsupportedOperationException("TODO: implement insert");
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
        throw new UnsupportedOperationException("TODO: implement findByNumber");
    }

    @Override
    public Optional<Order> findByIdempotencyKey(String idempotencyKey) {
        // TODO:
        //   SELECT order_number FROM orders WHERE idempotency_key = ?
        //   if found, reuse findByNumber(thatNumber); else Optional.empty().
        throw new UnsupportedOperationException("TODO: implement findByIdempotencyKey");
    }
}
