package ca.nl.cna.cp3566.store;

/**
 * MODEL (given — do not change).
 *
 * What {@link CheckoutService#checkout} hands back: the order, plus a flag saying
 * whether this was a brand-new order (201 Created) or an idempotent replay of one
 * we had already placed (200 OK). The {@link Orders} resource uses the flag to
 * choose the status code.
 */
public record CheckoutResult(Order order, boolean replayed) { }
