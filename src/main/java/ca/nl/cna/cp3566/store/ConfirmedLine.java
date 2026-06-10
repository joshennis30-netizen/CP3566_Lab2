package ca.nl.cna.cp3566.store;

/**
 * MODEL (given — do not change).
 *
 * One priced line on a FINISHED order. Unlike OrderLine (just id + quantity),
 * this carries the title and the prices the SERVER worked out, so the
 * confirmation never depends on a price the browser sent. Never trust a price
 * that came from the client.
 */
public record ConfirmedLine(int productId, String title,
                            double unitPrice, int quantity, double lineTotal) { }
