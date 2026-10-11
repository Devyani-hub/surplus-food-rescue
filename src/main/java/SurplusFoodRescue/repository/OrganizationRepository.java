
package SurplusFoodRescue.repository;

import SurplusFoodRescue.model.Organization;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class OrganizationRepository {

    private final JdbcTemplate jdbc;

    public OrganizationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Organization> mapper = (rs, rowNum) -> {
        Organization o = new Organization();

        o.setId(rs.getLong("id"));
        o.setName(rs.getString("name"));
        o.setEmail(rs.getString("email"));
        o.setAddress(rs.getString("address"));
        o.setLatitude(rs.getDouble("latitude"));
        o.setLongitude(rs.getDouble("longitude"));
        o.setDailyCapacityKg(rs.getDouble("daily_capacity_kg"));

        return o;
    };

    @Transactional
    public long create(Organization o) {

        jdbc.update("""
            INSERT INTO organizations
            (name, email, address, latitude, longitude, daily_capacity_kg)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            o.getName(),
            o.getEmail(),
            o.getAddress(),
            o.getLatitude(),
            o.getLongitude(),
            o.getDailyCapacityKg()
        );

        Long id = jdbc.queryForObject(
                "SELECT LAST_INSERT_ID()", Long.class);

        for (String type : o.getAcceptedFoodTypes()) {
            jdbc.update("""
                INSERT INTO organization_food_types
                (organization_id, food_type)
                VALUES (?, ?)
                """, id, type.trim().toUpperCase());
        }

        return id;
    }

    // Find one organization by ID, including accepted food types.
    public Optional<Organization> findById(Long id) {

        List<Organization> results = jdbc.query(
                "SELECT * FROM organizations WHERE id = ?",
                mapper,
                id
        );

        if (results.isEmpty()) {
            return Optional.empty();
        }

        Organization organization = results.get(0);

        List<String> types = jdbc.queryForList("""
            SELECT food_type
            FROM organization_food_types
            WHERE organization_id = ?
            ORDER BY food_type
            """,
            String.class,
            organization.getId()
        );

        organization.setAcceptedFoodTypes(types);

        return Optional.of(organization);
    }

    public List<Organization> findAll() {

        List<Organization> organizations = jdbc.query(
                "SELECT * FROM organizations ORDER BY name",
                mapper
        );

        for (Organization o : organizations) {
            List<String> types = jdbc.queryForList("""
                SELECT food_type
                FROM organization_food_types
                WHERE organization_id = ?
                ORDER BY food_type
                """,
                String.class,
                o.getId()
            );

            o.setAcceptedFoodTypes(types);
        }

        return organizations;
    }

    public List<Organization> findAcceptingFoodType(String foodType) {

        List<Organization> organizations = jdbc.query("""
            SELECT o.*
            FROM organizations o
            JOIN organization_food_types t
              ON t.organization_id = o.id
            WHERE UPPER(t.food_type) = UPPER(?)
            ORDER BY o.id
            """,
            mapper,
            foodType
        );

        for (Organization o : organizations) {
            List<String> types = jdbc.queryForList("""
                SELECT food_type
                FROM organization_food_types
                WHERE organization_id = ?
                ORDER BY food_type
                """,
                String.class,
                o.getId()
            );

            o.setAcceptedFoodTypes(types);
        }

        return organizations;
    }
}
