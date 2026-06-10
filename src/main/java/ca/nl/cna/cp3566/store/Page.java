package ca.nl.cna.cp3566.store;

import java.util.List;

/**
 * MODEL (given — do not change).
 *
 * One page of search results plus the bookkeeping the front end needs to draw
 * "Page 3 of 88" and enable/disable Next.
 *
 *   items     the products on THIS page (at most pageSize of them)
 *   total     how many products matched the filter overall (all pages)
 *   page      which page this is (1-based)
 *   pageSize  how many fit on a page
 */
public record Page(List<Product> items, int total, int page, int pageSize) { }
