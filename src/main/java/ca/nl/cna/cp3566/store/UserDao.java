package ca.nl.cna.cp3566.store;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * ARCHITECTURE (given contract — you implement it in JdbcUserDao).
 *
 * The accounts' data-access CONTRACT. Both methods take the active transaction
 * connection so "check the email is free" and "insert the account" happen as one
 * unit (the RegistrationService owns that transaction).
 *
 *   emailExists  is this (lower-cased) email already taken?  -> 409 if so
 *   insert       create the account, return the new id
 */
public abstract class UserDao extends AbstractDao {

    /** True if an account already exists for this lower-cased email. */
    public abstract boolean emailExists(Connection c, String emailLower) throws SQLException;

    /**
     * Insert a new account using the caller's transaction connection and return
     * the generated id. The password is already hashed by the service — store the
     * hash exactly as given, never a raw password.
     */
    public abstract int insert(Connection c, String name, String emailLower,
                               String passwordHash, String createdAt) throws SQLException;
}
