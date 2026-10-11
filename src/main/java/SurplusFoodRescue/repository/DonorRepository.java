package SurplusFoodRescue.repository;

import SurplusFoodRescue.model.Donor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class DonorRepository {

    private final JdbcTemplate jdbc;

    public DonorRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Donor> mapper = (rs, rowNum) -> {
        Donor d = new Donor();

        d.setId(rs.getLong("id"));
        d.setName(rs.getString("name"));
        d.setEmail(rs.getString("email"));
        d.setAddress(rs.getString("address"));
        d.setLatitude(rs.getDouble("latitude"));
        d.setLongitude(rs.getDouble("longitude"));

        return d;
    };

    public long create(Donor donor) {
        jdbc.update("""
            INSERT INTO donors
            (name, email, address, latitude, longitude)
            VALUES (?, ?, ?, ?, ?)
            """,
            donor.getName(),
            donor.getEmail(),
            donor.getAddress(),
            donor.getLatitude(),
            donor.getLongitude()
        );

        return jdbc.queryForObject(
            "SELECT LAST_INSERT_ID()", Long.class);
    }

    public List<Donor> findAll() {
        return jdbc.query(
            "SELECT * FROM donors ORDER BY created_at DESC",
            mapper);
    }

    public Optional<Donor> findById(long id) {
        List<Donor> donors = jdbc.query(
            "SELECT * FROM donors WHERE id = ?",
            mapper, id);

        return donors.stream().findFirst();
    }
}
