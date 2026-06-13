package ca.nl.cna.cp3566.store;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * ====================  YOUR CODE  ====================
 * The concrete checkout. The abstract {@link CheckoutService} already runs the
 * fixed flow (empty-cart check, idempotency, the transaction, persist, respond).
 * You implement the two steps it calls. You may use the inherited helpers:
 * {@code products}, {@code orders}, {@code round2(...)}, {@code taxRate()},
 * {@code newOrderNumber()}.
 * =====================================================
 */
public class StandardCheckoutService extends CheckoutService {

    public StandardCheckoutService(ProductDao products, OrderDao orders) {
        super(products, orders);   // wiring handed in by AppContext — leave as-is
    }

    @Override
    protected List<ConfirmedLine> reserveAndPrice(Connection c, List<OrderLine> items)
            throws SQLException {
        // TODO: walk the items IN ORDER and build a List<ConfirmedLine>. For each line:
        //   1. if line.quantity() < 1  -> throw ApiException.unprocessable("...")          // 422
        //   2. Product p = products.findById(line.productId())
        //          .orElseThrow(() -> ApiException.notFound("No product with id ..."));    // 404
        //   3. if (!products.reserve(c, line.productId(), line.quantity()))
        //          throw ApiException.conflict("Not enough stock for \"" + p.title() + "\""); // 409
        //   4. add new ConfirmedLine(p.id(), p.title(), p.price(),                          // SERVER price
        //                            line.quantity(), round2(p.price() * line.quantity()));
        // If you throw at any point, the template rolls the transaction back — every
        // reservation you already made is undone automatically.
        throw new UnsupportedOperationException("TODO: implement reserveAndPrice");
    }

    @Override
    protected Order assemble(String email, List<ConfirmedLine> lines) {
        // TODO: turn the priced lines into the finished Order:
        //   double subtotal = round2( sum of l.lineTotal() );
        //   double tax      = round2( subtotal * taxRate() );
        //   double total    = round2( subtotal + tax );
        //   String number   = newOrderNumber();
        //   String placedAt = java.time.OffsetDateTime.now().toString();
        //   return new Order(number, email, lines, subtotal, tax, total, placedAt);

        throw new UnsupportedOperationException("TODO: implement assemble");
    }
}
