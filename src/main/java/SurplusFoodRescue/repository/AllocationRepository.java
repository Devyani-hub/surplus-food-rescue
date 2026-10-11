
package SurplusFoodRescue.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AllocationRepository {

    private final JdbcTemplate jdbc;

    public AllocationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void create(long listingId, long organizationId, double score) {
        jdbc.update("""
            INSERT INTO allocations (listing_id, organization_id, score)
            VALUES (?, ?, ?)
            """, listingId, organizationId, score);
    }

    public Long findOrganizationForListing(long listingId) {
        return jdbc.query("""
            SELECT organization_id
            FROM allocations
            WHERE listing_id = ?
            """,
            rs -> rs.next() ? rs.getLong("organization_id") : null,
            listingId);
    }

    public boolean isAssignedOrganization(long listingId, long organizationId) {
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*) FROM allocations
            WHERE listing_id = ? AND organization_id = ?
            """, Integer.class, listingId, organizationId);
        return count != null && count == 1;
    }

    public Double findScoreForListing(long listingId) {
        return jdbc.query("""
            SELECT score
            FROM allocations
            WHERE listing_id = ?
            """,
            rs -> rs.next() ? rs.getDouble("score") : null,
            listingId);
    }
}
