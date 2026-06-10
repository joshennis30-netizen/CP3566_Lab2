package ca.nl.cna.cp3566.store;

import java.util.List;

/**
 * MODEL (given — do not change).
 *
 * A placed order, exactly as it goes back to the browser and as it is stored for
 * the confirmation lookup.
 *
 *   orderNumber  a short code you generate, e.g. "FS-7Q3K9A"
 *   email        who placed it
 *   lines        the priced lines (see ConfirmedLine)
 *   subtotal     sum of the line totals
 *   tax          subtotal * tax rate
 *   total        subtotal + tax
 *   placedAt     ISO timestamp string
 */
public record Order(String orderNumber, String email, List<ConfirmedLine> lines,
                    double subtotal, double tax, double total, String placedAt) { }
