package ca.nl.cna.cp3566.store;

/**
 * MODEL (given — do not change).
 *
 * The JSON the registration form POSTs:
 *   { "name": "...", "email": "...", "password": "..." }
 *
 * Jackson fills this from the request body. Treat every field as untrusted.
 */
public record RegisterRequest(String name, String email, String password) { }
