package ca.nl.cna.cp3566.store;

import java.sql.*;

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
//        try {
//            PreparedStatement ps = c.prepareStatement("SELECT 1 FROM users WHERE email = ?");
//            ps.setString(1, emailLower);
//            try (ResultSet rs = ps.executeQuery()) {
//                if (rs.next()) {
//                    return true;
//                }
//            }
//        } catch (SQLException e) {}
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
//        String sql = "INSERT INTO users (name, email, password_hash, created_at) VALUES (?, ?, ?, ?)";
//        try {
//            PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
//            ps.setString(1, name);
//            ps.setString(2, emailLower);
//            ps.setString(3, passwordHash);
//            ps.setString(4, createdAt);
//            ps.executeUpdate();
//            ResultSet rs = ps.getGeneratedKeys();
//            if (rs.next()) {
//                return rs.getInt(1);
//            }
//        } catch(SQLException e) {}
        throw new UnsupportedOperationException("TODO: implement insert");
    }
}
