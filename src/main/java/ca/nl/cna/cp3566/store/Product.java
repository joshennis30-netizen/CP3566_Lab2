package ca.nl.cna.cp3566.store;

/**
 * MODEL (given — do not change).
 *
 * One product in the catalogue. Same shape as the Module 4 Product, plus the
 * extra fields a storefront shows. The field list mirrors the public Fake Store
 * API (id, title, price, description, image, rating).
 *
 * Notice what is NOT here: stock. A record is immutable but stock changes when
 * people buy, so stock lives in the database (a column you can decrement), never
 * on the Product object. Keeping the product description and the live inventory
 * as two separate concerns is the whole reason the order endpoint is interesting.
 */
public record Product(
        int id,
        String title,
        String category,
        double price,
        String description,
        String image,
        Rating rating) { }
