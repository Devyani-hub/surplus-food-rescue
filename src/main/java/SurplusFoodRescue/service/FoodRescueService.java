
package SurplusFoodRescue.service;

import SurplusFoodRescue.model.Donor;
import SurplusFoodRescue.model.FoodListing;
import SurplusFoodRescue.model.MatchResult;
import SurplusFoodRescue.model.Organization;
import SurplusFoodRescue.repository.AllocationRepository;
import SurplusFoodRescue.repository.DonorRepository;
import SurplusFoodRescue.repository.FoodListingRepository;
import SurplusFoodRescue.repository.OrganizationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FoodRescueService {

    private final DonorRepository donors;
    private final OrganizationRepository organizations;
    private final FoodListingRepository listings;
    private final AllocationRepository allocations;
    private final MatchingService matching;
    private final GeocodingService geocoding;
    private final JdbcTemplate jdbcTemplate;

    public FoodRescueService(
            DonorRepository donors,
            OrganizationRepository organizations,
            FoodListingRepository listings,
            AllocationRepository allocations,
            MatchingService matching,
            GeocodingService geocoding,
            JdbcTemplate jdbcTemplate) {

        this.donors = donors;
        this.organizations = organizations;
        this.listings = listings;
        this.allocations = allocations;
        this.matching = matching;
        this.geocoding = geocoding;
        this.jdbcTemplate = jdbcTemplate;
    }

    // Register a donor.
    public long registerDonor(Donor donor) {

        if (donor == null) {
            throw new IllegalArgumentException(
                    "Donor details are required.");
        }

        if (isBlank(donor.getName())
                || isBlank(donor.getEmail())
                || isBlank(donor.getAddress())) {
            throw new IllegalArgumentException(
                    "Donor name, email and address are required.");
        }

        double[] coordinates =
                geocoding.geocode(donor.getAddress());

        donor.setLatitude(coordinates[0]);
        donor.setLongitude(coordinates[1]);

        return donors.create(donor);
    }

    // Register an organization.
    public long registerOrganization(Organization organization) {

        if (organization == null) {
            throw new IllegalArgumentException(
                    "Organization details are required.");
        }

        if (isBlank(organization.getName())
                || isBlank(organization.getEmail())
                || isBlank(organization.getAddress())) {
            throw new IllegalArgumentException(
                    "Organization name, email and address are required.");
        }

        if (organization.getDailyCapacityKg() <= 0) {
            throw new IllegalArgumentException(
                    "Daily capacity must be greater than zero.");
        }

        if (organization.getAcceptedFoodTypes() == null
                || organization.getAcceptedFoodTypes().isEmpty()) {
            throw new IllegalArgumentException(
                    "Select at least one accepted food type.");
        }

        organization.setAcceptedFoodTypes(
                organization.getAcceptedFoodTypes()
                        .stream()
                        .filter(type -> type != null && !type.isBlank())
                        .map(String::trim)
                        .map(String::toUpperCase)
                        .distinct()
                        .toList()
        );

        if (organization.getAcceptedFoodTypes().isEmpty()) {
            throw new IllegalArgumentException(
                    "Select at least one valid food type.");
        }

        double[] coordinates =
                geocoding.geocode(organization.getAddress());

        organization.setLatitude(coordinates[0]);
        organization.setLongitude(coordinates[1]);

        return organizations.create(organization);
    }

    // Create a food listing and automatically allocate the best match.
    @Transactional
    public FoodListing createListing(FoodListing listing) {

        if (listing == null) {
            throw new IllegalArgumentException(
                    "Food listing details are required.");
        }

        if (listing.getDonorId() == null
                || donors.findById(listing.getDonorId()).isEmpty()) {
            throw new IllegalArgumentException(
                    "A valid registered donor is required.");
        }

        if (isBlank(listing.getPickupAddress())) {
            throw new IllegalArgumentException(
                    "Food pickup address is required.");
        }

        if (isBlank(listing.getFoodType())) {
            throw new IllegalArgumentException(
                    "Food type is required.");
        }

        if (isBlank(listing.getDescription())) {
            throw new IllegalArgumentException(
                    "Food description is required.");
        }

        if (listing.getQuantityKg() <= 0) {
            throw new IllegalArgumentException(
                    "Food quantity must be greater than zero.");
        }

        if (listing.getPickupDeadline() == null
                || !listing.getPickupDeadline()
                        .isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Pickup deadline must be in the future.");
        }

        listing.setFoodType(
                listing.getFoodType().trim().toUpperCase());

        double[] coordinates =
                geocoding.geocode(listing.getPickupAddress());

        listing.setLatitude(coordinates[0]);
        listing.setLongitude(coordinates[1]);

        long listingId = listings.create(listing);
        return listings.findById(listingId)
                .orElseThrow(() -> new IllegalStateException(
                        "Could not retrieve the newly created listing."));
    }

    /** Atomically reserve an available listing for one organization. */
    @Transactional
    public void claimListing(long listingId, long organizationId) {
        Organization organization = organizations.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found. Register as an NGO first."));
        FoodListing listing = listings.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Food listing not found."));
        if (!"AVAILABLE".equalsIgnoreCase(listing.getStatus())) {
            throw new IllegalStateException("This food has already been reserved by another organization or is no longer available.");
        }
        if (organization.getAcceptedFoodTypes() == null ||
                !organization.getAcceptedFoodTypes().stream().anyMatch(t -> t.equalsIgnoreCase(listing.getFoodType()))) {
            throw new IllegalArgumentException("Your organization does not accept this food type.");
        }
        if (organization.getDailyCapacityKg() <= 0 || listing.getQuantityKg() > organization.getDailyCapacityKg()) {
            throw new IllegalArgumentException("This donation exceeds your organization's daily capacity.");
        }
        // This conditional update ensures only one simultaneous claim succeeds.
        if (!listings.markMatchedIfAvailable(listingId)) {
            throw new IllegalStateException("Another organization has just reserved this food. Refresh available donations.");
        }
        List<MatchResult> possible = matching.previewMatches(listing);
        double score = possible.stream()
                .filter(m -> m.getOrganizationId() != null && m.getOrganizationId().longValue() == organizationId)
                .mapToDouble(MatchResult::getTotalScore).findFirst().orElse(0.0);
        allocations.create(listingId, organizationId, score);
        jdbcTemplate.update("""
            INSERT INTO donor_notifications (donor_id, listing_id, organization_id, message)
            VALUES (?, ?, ?, ?)
            """, listing.getDonorId(), listingId, organizationId,
                organization.getName() + " has reserved your food donation. Please coordinate pickup using the contact email shown in your donor dashboard.");
    }

    // Preview potential matches or display the existing allocation.
    public List<MatchResult> previewMatches(long listingId) {

        FoodListing foodListing = listings.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Food listing not found."));

        String status = foodListing.getStatus();

        // Available listing: calculate possible matches.
        if ("AVAILABLE".equalsIgnoreCase(status)) {
            return matching.previewMatches(foodListing);
        }

        // Matched listing: retrieve the existing allocation.
        if ("MATCHED".equalsIgnoreCase(status)) {

            Long organizationId =
                    allocations.findOrganizationForListing(listingId);

            if (organizationId == null) {
                throw new IllegalStateException(
                        "Listing is matched, but no allocation was found.");
            }

            Organization organization = organizations.findById(organizationId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Allocated organization not found."));

            Double score =
                    allocations.findScoreForListing(listingId);

            if (score == null) {
                throw new IllegalStateException(
                        "Allocation score was not found.");
            }

            MatchResult result = new MatchResult();
            result.setOrganizationId(organizationId);
            result.setOrganizationName(organization.getName());
            result.setTotalScore(score);

            return List.of(result);
        }

        throw new IllegalStateException(
                "Matches cannot be previewed for a listing with status: "
                        + status);
    }

    // Mark collection only for the organization that claimed the donation.
    @Transactional
    public void markCollected(long listingId, long organizationId) {
        FoodListing listing = listings.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Food listing not found."));
        if (!"MATCHED".equalsIgnoreCase(listing.getStatus())) {
            throw new IllegalStateException("Only reserved donations can be marked collected.");
        }
        if (!allocations.isAssignedOrganization(listingId, organizationId)) {
            throw new SecurityException("Only the organization that reserved this donation can mark it collected.");
        }
        if (!listings.markCollected(listingId)) {
            throw new IllegalStateException("This donation has already been collected or is no longer available.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
