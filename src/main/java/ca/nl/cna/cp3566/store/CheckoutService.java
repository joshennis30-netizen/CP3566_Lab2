package ca.nl.cna.cp3566.store;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * ARCHITECTURE (given — you implement the abstract steps in StandardCheckoutService).
 *
 * This is a TEMPLATE METHOD. The public method {@link #checkout} is {@code final}:
 * it fixes the ORDER OF OPERATIONS of a correct checkout and you cannot change it.
 * You fill in the two abstract steps it calls. That is the meaning of "implement
 * without breaking the structure" — the structure (validate, then take stock and
 * price inside ONE transaction, then persist, then respond) is guaranteed here;
 * the per-step detail is yours.
 *
 * The fixed flow:
 *   1. reject an empty cart                          -> 422   (requireItems, given)
 *   2. if this idempotency key was seen, replay it   -> 200   (given)
 *   3. open ONE transaction and, inside it:
 *        a. reserveAndPrice(...)   YOUR step: validate each line (422), check the
 *           product exists (404), take stock (409), price from the SERVER price.
 *        b. assemble(...)          YOUR step: totals, tax, order number.
 *        c. orders.insert(...)     write the order + lines on the same connection.
 *      commit — or, on any failure, the transaction rolls back so no stock is
 *      taken and no half-order is written.
 *   4. return 201 with the new order.
 */
public abstract class CheckoutService {

    /** Tax rate applied to the subtotal. Use taxRate() in assemble(). */
    protected static final double TAX_RATE = 0.13;

    protected final ProductDao products;
    protected final OrderDao orders;

    protected CheckoutService(ProductDao products, OrderDao orders) {
        this.products = products;
        this.orders = orders;
    }

    /** THE TEMPLATE METHOD — do not override (it is final). */
    public final CheckoutResult checkout(OrderRequest req, String idempotencyKey) {
        requireItems(req);                                  // step 1 (422)

        String key = (idempotencyKey == null || idempotencyKey.isBlank())
                ? null : idempotencyKey.trim();

        if (key != null) {                                  // step 2 (replay)
            Optional<Order> existing = orders.findByIdempotencyKey(key);
            if (existing.isPresent()) return new CheckoutResult(existing.get(), true);
        }

        try {
            Order order = Database.inTransaction(c -> {     // step 3 (one transaction)
                List<ConfirmedLine> lines = reserveAndPrice(c, req.items());  // YOUR step a
                Order o = assemble(req.email(), lines);                       // YOUR step b
                orders.insert(c, o, key);                                     // step c
                return o;
            });
            return new CheckoutResult(order, false);        // step 4 (201)
        } catch (ApiException e) {
            // If two identical requests raced on the same key, the loser may trip
            // the UNIQUE(idempotency_key) constraint (a 409). In that case the
            // original is now committed — replay it instead of erroring.
            if (e.status() == 409 && key != null) {
                Optional<Order> existing = orders.findByIdempotencyKey(key);
                if (existing.isPresent()) return new CheckoutResult(existing.get(), true);
            }
            throw e;   // a real validation / stock / not-found error
        }
    }

    // ---- the steps YOU implement (in StandardCheckoutService) --------------

    /**
     * For each line, IN ORDER, using the transaction connection {@code c}:
     *   - quantity must be >= 1                         -> throw ApiException.unprocessable (422)
     *   - the product must exist (products.findById)    -> throw ApiException.notFound (404)
     *   - take the stock (products.reserve on c); false -> throw ApiException.conflict (409)
     *   - build a ConfirmedLine using the SERVER price (product.price()).
     * Returns the priced lines. If you throw, the template's transaction rolls
     * back and every earlier reservation is undone for you — no manual release.
     */
    protected abstract List<ConfirmedLine> reserveAndPrice(Connection c, List<OrderLine> items)
            throws java.sql.SQLException;

    /**
     * Turn priced lines into a finished Order: subtotal = sum of line totals,
     * tax = subtotal * taxRate(), total = subtotal + tax (round money with
     * round2), a fresh order number from newOrderNumber(), and the placedAt
     * timestamp. The orderNumber you put here is what the Location header uses.
     */
    protected abstract Order assemble(String email, List<ConfirmedLine> lines);

    // ---- given helpers you may use -----------------------------------------

    /** Reject an empty/missing cart with 422 before any work happens. */
    protected final void requireItems(OrderRequest req) {
        if (req == null || req.items() == null || req.items().isEmpty()) {
            throw ApiException.unprocessable("Order must contain at least one item.");
        }
    }

    protected final double taxRate() { return TAX_RATE; }

    /** Round a money amount to 2 decimals, half-up — the way a till does. */
    protected final double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** A short order code like "FS-7Q3K9A". */
    protected final String newOrderNumber() {
        String alphabet = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ";  // no I/O to avoid confusion
        java.util.Random rng = new java.util.Random();
        StringBuilder sb = new StringBuilder("FS-");
        for (int i = 0; i < 6; i++) sb.append(alphabet.charAt(rng.nextInt(alphabet.length())));
        return sb.toString();
    }
}
