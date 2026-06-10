package ca.nl.cna.cp3566.store;

import java.util.List;

/**
 * MODEL (given — do not change).
 *
 * The JSON the checkout page POSTs to place an order:
 *   { "email": "ann@x.com", "items": [ {"productId":42,"quantity":3}, ... ] }
 *
 * Everything here is untrusted: the list could be empty, the quantities
 * negative, the ids invented. Your endpoint decides what is acceptable.
 */
public record OrderRequest(String email, List<OrderLine> items) { }
