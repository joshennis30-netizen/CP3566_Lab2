package ca.nl.cna.cp3566.store;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * ARCHITECTURE (given — extend it, do not change it).
 *
 * The shared base for every Data Access Object (DAO). A DAO is the only kind of
 * class that talks SQL; the rest of the app talks to DAOs, never to JDBC.
 *
 * WHY this is an abstract CLASS (not an interface):
 *   it carries real, shared behaviour that every DAO needs — getting a
 *   connection and running work inside a transaction. Subclasses INHERIT that
 *   plumbing instead of copying it. An interface could only declare the methods;
 *   an abstract class can hand you the implementation. It is abstract because a
 *   "DAO with no table" makes no sense on its own — you only ever use a concrete
 *   subclass (JdbcProductDao, JdbcOrderDao, JdbcUserDao).
 *
 * You will EXTEND this (indirectly, through ProductDao / OrderDao / UserDao).
 * Use open() inside your read methods, wrapped in try-with-resources:
 *
 *     try (Connection c = open();
 *          PreparedStatement ps = c.prepareStatement(SQL)) { ... }
 */
public abstract class AbstractDao {

    /** A JDBC connection. Always use it in try-with-resources so it is closed. */
    protected Connection open() throws SQLException {
        return Database.getConnection();
    }

    /**
     * Run a unit of work as one all-or-nothing transaction (commit on success,
     * roll back on any exception). Delegates to the single template in Database
     * so the rule lives in exactly one place. Most single-DAO writes do not need
     * this — the order/registration transactions are owned by the services — but
     * it is here if a DAO needs its own.
     */
    protected <R> R inTransaction(Database.TxWork<R> work) {
        return Database.inTransaction(work);
    }
}
