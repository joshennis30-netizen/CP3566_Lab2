package ca.nl.cna.cp3566.store;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * ARCHITECTURE (given contract — you implement it in JdbcOrderDao).
 *
 * The orders' data-access CONTRACT. Storing an order writes to TWO tables
 * (orders + order_lines), so {@link #insert} takes the active transaction
 * connection from the service — it must use that connection and must NOT commit
 * (the service commits, so the whole order is one unit).
 *
 *   insert                 write the order row + all its line rows (uses the tx)
 *   findByNumber           read a whole order back (for the confirmation page)
 *   findByIdempotencyKey   look up an order we already placed for a given key
 */
public abstract class OrderDao extends AbstractDao {

    /**
     * Persist {@code order} and its lines using the caller's transaction
     * connection {@code c}. {@code idempotencyKey} may be null (store it as the
     * orders.idempotency_key value so a later retry can find this order).
     */
    public abstract void insert(Connection c, Order order, String idempotencyKey) throws SQLException;

    /** The full order (with its lines) by order number, or empty if unknown. */
    public abstract Optional<Order> findByNumber(String orderNumber);

    /** The order previously stored under this idempotency key, or empty. */
    public abstract Optional<Order> findByIdempotencyKey(String idempotencyKey);
}
