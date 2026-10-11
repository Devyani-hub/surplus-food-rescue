package SurplusFoodRescue.repository;

import SurplusFoodRescue.model.Donation;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class DonationRepository {

    private final JdbcTemplate jdbcTemplate;

    public DonationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Donation> donationRowMapper =
            (rs, rowNum) -> {
                Donation donation = new Donation();

                donation.setId(rs.getLong("id"));
                donation.setFoodName(
                        rs.getString("food_name"));
                donation.setDescription(
                        rs.getString("description"));
                donation.setQuantity(
                        rs.getInt("quantity"));
                donation.setUnit(
                        rs.getString("unit"));

                Timestamp prepared =
                        rs.getTimestamp("prepared_at");

                if (prepared != null) {
                    donation.setPreparedAt(
                            prepared.toLocalDateTime());
                }

                donation.setPickupLocation(
                        rs.getString("pickup_location"));

                Timestamp deadline =
                        rs.getTimestamp("collection_deadline");

                if (deadline != null) {
                    donation.setCollectionDeadline(
                            deadline.toLocalDateTime());
                }

                donation.setStatus(
                        rs.getString("status"));

                return donation;
            };

    public List<Donation> findAvailableDonations() {
        String sql = """
                SELECT *
                FROM donations
                WHERE status = ?
                ORDER BY created_at DESC
                """;

        return jdbcTemplate.query(
                sql,
                donationRowMapper,
                "AVAILABLE"
        );
    }

    public int save(Donation donation) {
        String sql = """
                INSERT INTO donations
                (
                    food_name,
                    description,
                    quantity,
                    unit,
                    prepared_at,
                    pickup_location,
                    collection_deadline,
                    status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        return jdbcTemplate.update(
                sql,
                donation.getFoodName(),
                donation.getDescription(),
                donation.getQuantity(),
                donation.getUnit(),
                Timestamp.valueOf(donation.getPreparedAt()),
                donation.getPickupLocation(),
                Timestamp.valueOf(
                        donation.getCollectionDeadline()),
                "AVAILABLE"
        );
    }
}