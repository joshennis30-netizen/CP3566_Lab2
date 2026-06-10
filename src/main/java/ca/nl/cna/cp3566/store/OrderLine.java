package ca.nl.cna.cp3566.store;

/**
 * MODEL (given — do not change).
 *
 * One line of an INCOMING order: "productId 42, quantity 3". This is what the
 * checkout page sends per cart item. It is untrusted — the quantity could be
 * zero or negative, the id could be made up. Your code decides what is allowed.
 */
public record OrderLine(int productId, int quantity) { }
