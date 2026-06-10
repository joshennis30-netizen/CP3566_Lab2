package ca.nl.cna.cp3566.store;

/**
 * MODEL (given — do not change).
 *
 * A registered customer as it goes OUT to the browser. There is deliberately no
 * password field here: what you store (a hash) and what you return are two
 * different things, and the password must never leave the server.
 */
public record User(int id, String name, String email) { }
