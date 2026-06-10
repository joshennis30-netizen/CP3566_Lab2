package ca.nl.cna.cp3566.store;

/**
 * MODEL (given — do not change).
 *
 * The star rating attached to a product: {@code "rating": {"rate": 4.2, "count": 340}}.
 * The ASOS dataset has no ratings, so {@link CsvProductSource} synthesises a
 * stable one per product when it seeds the catalogue.
 */
public record Rating(double rate, int count) { }
