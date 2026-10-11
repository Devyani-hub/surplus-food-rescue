package SurplusFoodRescue.controller;

import SurplusFoodRescue.model.Donor;
import SurplusFoodRescue.model.FoodListing;
import SurplusFoodRescue.model.MatchResult;
import SurplusFoodRescue.model.Organization;
import SurplusFoodRescue.repository.AllocationRepository;
import SurplusFoodRescue.repository.DonorRepository;
import SurplusFoodRescue.repository.FoodListingRepository;
import SurplusFoodRescue.repository.OrganizationRepository;
import SurplusFoodRescue.service.FoodRescueService;

import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FoodRescueController {

    private final FoodRescueService service;
    private final DonorRepository donors;
    private final OrganizationRepository organizations;
    private final FoodListingRepository listings;
    private final AllocationRepository allocations;
    private final JdbcTemplate jdbc;

    public FoodRescueController(
            FoodRescueService service,
            DonorRepository donors,
            OrganizationRepository organizations,
            FoodListingRepository listings,
            AllocationRepository allocations,
            JdbcTemplate jdbc) {

        this.service = service;
        this.donors = donors;
        this.organizations = organizations;
        this.listings = listings;
        this.allocations = allocations;
        this.jdbc = jdbc;
    }

    @PostMapping("/donors")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createDonor(
            @RequestBody Donor donor) {

        return Map.of(
                "id", service.registerDonor(donor),
                "message", "Donor registered successfully");
    }

    @GetMapping("/donors")
    public List<Donor> getDonors() {
        return donors.findAll();
    }

    @PostMapping("/organizations")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createOrganization(
            @RequestBody Organization organization) {

        return Map.of(
                "id", service.registerOrganization(organization),
                "message", "Organization registered successfully");
    }

    @GetMapping("/organizations")
    public List<Organization> getOrganizations() {
        return organizations.findAll();
    }

    @PostMapping("/listings")
    @ResponseStatus(HttpStatus.CREATED)
    public FoodListing createListing(
            @RequestBody FoodListing listing) {

        return service.createListing(listing);
    }

    @GetMapping("/listings")
    public List<FoodListing> getListings(
            @RequestParam(required = false) String status) {

        if (status == null || status.isBlank()) {
            return listings.findAll();
        }

        return listings.findByStatus(status.trim().toUpperCase());
    }

    @GetMapping("/listings/{id}")
    public FoodListing getListing(@PathVariable long id) {

        return listings.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Listing not found."));
    }

    @PostMapping("/listings/{id}/claim")
    public ResponseEntity<Map<String, Object>> claimListing(
            @PathVariable long id, @RequestBody Map<String, Long> request) {
        Long organizationId = request.get("organizationId");
        if (organizationId == null || organizationId <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Register your organization before claiming food."));
        }
        try {
            service.claimListing(id, organizationId);
            return ResponseEntity.ok(Map.of("message", "Food reserved. The donor has been notified.", "listingId", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/donors/{donorId}/notifications")
    public List<Map<String, Object>> donorNotifications(@PathVariable long donorId) {
        return jdbc.queryForList("""
            SELECT n.id, n.listing_id AS listingId, n.message, n.created_at AS createdAt,
                   o.name AS organizationName, o.email AS organizationEmail
            FROM donor_notifications n
            JOIN organizations o ON o.id = n.organization_id
            WHERE n.donor_id = ? ORDER BY n.created_at DESC
            """, donorId);
    }

    @GetMapping("/organizations/{organizationId}/claimed")
    public List<FoodListing> getClaimedListings(@PathVariable long organizationId) {
        return jdbc.query("""
            SELECT f.* FROM food_listings f JOIN allocations a ON a.listing_id = f.id
            WHERE a.organization_id = ? AND f.status IN ('MATCHED', 'COLLECTED')
            ORDER BY f.created_at DESC
            """, (rs, rowNum) -> {
                FoodListing f = new FoodListing();
                f.setId(rs.getLong("id")); f.setDonorId(rs.getLong("donor_id"));
                f.setFoodType(rs.getString("food_type")); f.setQuantityKg(rs.getDouble("quantity_kg"));
                f.setLatitude(rs.getDouble("latitude")); f.setLongitude(rs.getDouble("longitude"));
                f.setPickupAddress(rs.getString("pickup_address"));
                java.sql.Timestamp deadline = rs.getTimestamp("pickup_deadline");
                if (deadline != null) f.setPickupDeadline(deadline.toLocalDateTime());
                f.setStatus(rs.getString("status")); f.setDescription(rs.getString("description"));
                return f;
            }, organizationId);
    }

    @GetMapping("/listings/{id}/matches")
    public List<MatchResult> previewMatches(
            @PathVariable long id) {

        return service.previewMatches(id);
    }

    @GetMapping("/listings/{id}/allocation")
    public Map<String, Object> getAllocation(
            @PathVariable long id) {

        Long organizationId =
                allocations.findOrganizationForListing(id);

        if (organizationId == null) {
            return Map.of(
                    "listingId", id,
                    "allocated", false);
        }

        return Map.of(
                "listingId", id,
                "allocated", true,
                "organizationId", organizationId);
    }

    @PostMapping("/listings/{id}/collected")
    public ResponseEntity<Map<String, String>> markCollected(
            @PathVariable long id, @RequestBody Map<String, Long> request) {
        Long organizationId = request.get("organizationId");
        if (organizationId == null || organizationId <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Organization ID is required."));
        }
        try {
            service.markCollected(id, organizationId);
            return ResponseEntity.ok(Map.of("message", "Food marked as collected."));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
