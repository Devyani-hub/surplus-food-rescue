
package SurplusFoodRescue.repository;

import SurplusFoodRescue.model.FoodListing;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class FoodListingRepository {

    private final JdbcTemplate jdbc;

    public FoodListingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<FoodListing> mapper = (rs, rowNum) -> {
        FoodListing f = new FoodListing();
        f.setId(rs.getLong("id"));
        f.setDonorId(rs.getLong("donor_id"));
        f.setFoodType(rs.getString("food_type"));
        f.setQuantityKg(rs.getDouble("quantity_kg"));
        f.setLatitude(rs.getDouble("latitude"));
        f.setLongitude(rs.getDouble("longitude"));
        f.setPickupAddress(rs.getString("pickup_address"));

        Timestamp deadline = rs.getTimestamp("pickup_deadline");
        if (deadline != null) {
            f.setPickupDeadline(deadline.toLocalDateTime());
        }

        f.setStatus(rs.getString("status"));
        f.setDescription(rs.getString("description"));
        return f;
    };

    public long create(FoodListing f) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            var ps = connection.prepareStatement("""
                INSERT INTO food_listings
                (donor_id, food_type, quantity_kg, latitude, longitude,
                 pickup_address, pickup_deadline, status, description)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'AVAILABLE', ?)
                """, Statement.RETURN_GENERATED_KEYS);

            ps.setLong(1, f.getDonorId());
            ps.setString(2, f.getFoodType());
            ps.setDouble(3, f.getQuantityKg());
            ps.setDouble(4, f.getLatitude());
            ps.setDouble(5, f.getLongitude());
            ps.setString(6, f.getPickupAddress());
            ps.setTimestamp(7, Timestamp.valueOf(f.getPickupDeadline()));
            ps.setString(8, f.getDescription());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Could not obtain listing ID.");
        }
        return key.longValue();
    }

    public Optional<FoodListing> findById(long id) {
        return jdbc.query("SELECT * FROM food_listings WHERE id = ?",
                mapper, id).stream().findFirst();
    }

    public List<FoodListing> findAll() {
        return jdbc.query(
                "SELECT * FROM food_listings ORDER BY created_at DESC",
                mapper);
    }

    public List<FoodListing> findByStatus(String status) {
        return jdbc.query("""
                SELECT * FROM food_listings
                WHERE status = ? ORDER BY created_at DESC
                """, mapper, status);
    }

    public boolean markMatchedIfAvailable(long id) {
        return jdbc.update("""
                UPDATE food_listings SET status = 'MATCHED'
                WHERE id = ? AND status = 'AVAILABLE'
                """, id) == 1;
    }

    public boolean markCollected(long id) {
        return jdbc.update("""
                UPDATE food_listings SET status = 'COLLECTED'
                WHERE id = ? AND status = 'MATCHED'
                """, id) == 1;
    }

    public int expireOverdue() {
        return jdbc.update("""
                UPDATE food_listings SET status = 'EXPIRED'
                WHERE status = 'AVAILABLE'
                  AND pickup_deadline < CURRENT_TIMESTAMP
                """);
    }

    public List<FoodListing> findAvailable() {
        return jdbc.query("""
                SELECT * FROM food_listings
                WHERE status = 'AVAILABLE'
                  AND pickup_deadline > CURRENT_TIMESTAMP
                """, mapper);
    }

    public double getAllocatedKgToday(long organizationId) {
        Double total = jdbc.queryForObject("""
                SELECT COALESCE(SUM(f.quantity_kg), 0)
                FROM allocations a
                JOIN food_listings f ON f.id = a.listing_id
                WHERE a.organization_id = ?
                  AND DATE(a.created_at) = CURRENT_DATE
                  AND f.status IN ('MATCHED', 'COLLECTED')
                """, Double.class, organizationId);

        return total == null ? 0.0 : total;
    }

    public LocalDateTime getLastAllocationTime(long organizationId) {
        List<Timestamp> times = jdbc.query("""
                SELECT MAX(created_at) FROM allocations
                WHERE organization_id = ?
                """, (rs, rowNum) -> rs.getTimestamp(1), organizationId);

        if (times.isEmpty() || times.get(0) == null) {
            return null;
        }
        return times.get(0).toLocalDateTime();
    }
}
