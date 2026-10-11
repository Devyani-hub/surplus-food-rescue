
package SurplusFoodRescue.repository;

import SurplusFoodRescue.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void createUser(User user) {
        jdbc.update("""
            INSERT INTO users (name, email, password_hash, role)
            VALUES (?, ?, ?, ?)
            """,
            user.getName(),
            user.getEmail(),
            user.getPasswordHash(),
            user.getRole()
        );
    }

    public Optional<User> findByEmail(String email) {
        return jdbc.query("""
            SELECT id, name, email, password_hash, role
            FROM users
            WHERE email = ?
            """,
            rs -> {
                if (!rs.next()) {
                    return Optional.empty();
                }

                User user = new User();
                user.setId(rs.getLong("id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setPasswordHash(
                    rs.getString("password_hash")
                );
                user.setRole(rs.getString("role"));

                return Optional.of(user);
            },
            email
        );
    }
}
