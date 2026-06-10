package ca.nl.cna.cp3566.store;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * ====================  YOUR CODE  ====================
 * The JDBC accounts repository. Implement the two methods from {@link UserDao}.
 * Both receive the registration transaction's connection — use it.
 * =====================================================
 */
public class JdbcUserDao extends UserDao {

    @Override
    public boolean emailExists(Connection c, String emailLower) throws SQLException {
        // TODO (use the GIVEN connection c):
        //   SELECT 1 FROM users WHERE email = ?
        //   return true if there is a row.
        throw new UnsupportedOperationException("TODO: implement emailExists");
    }

    @Override
    public int insert(Connection c, String name, String emailLower,
                      String passwordHash, String createdAt) throws SQLException {
        // TODO (use the GIVEN connection c):
        //   INSERT INTO users (name, email, password_hash, created_at) VALUES (?, ?, ?, ?)
        //   - prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        //   - executeUpdate(), then read getGeneratedKeys() to get the new id and return it.
        //   Store passwordHash exactly as given. NEVER store a raw password.
        throw new UnsupportedOperationException("TODO: implement insert");
    }
}
