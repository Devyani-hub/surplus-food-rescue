package SurplusFoodRescue.service;

import SurplusFoodRescue.matching.DistanceCalculator;
import SurplusFoodRescue.model.FoodListing;
import SurplusFoodRescue.model.MatchResult;
import SurplusFoodRescue.model.Organization;
import SurplusFoodRescue.repository.FoodListingRepository;
import SurplusFoodRescue.repository.OrganizationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

@Service
public class MatchingService {

    private static final double DISTANCE_WEIGHT = 0.50;
    private static final double CAPACITY_WEIGHT = 0.25;
    private static final double FAIRNESS_WEIGHT = 0.25;

    private static final double MAX_DISTANCE_SCORE_KM = 100.0;
    private static final double SPEED_KM_PER_HOUR = 30.0;
    private static final double BUFFER_MINUTES = 15.0;

    private final FoodListingRepository listingRepository;
    private final OrganizationRepository organizationRepository;

    public MatchingService(
            FoodListingRepository listingRepository,
            OrganizationRepository organizationRepository) {
        this.listingRepository = listingRepository;
        this.organizationRepository = organizationRepository;
    }

    public List<MatchResult> previewMatches(FoodListing listing) {

        List<MatchResult> results = new ArrayList<>();

        if (listing.getPickupDeadline() == null
                || !listing.getPickupDeadline().isAfter(LocalDateTime.now())) {
            return results;
        }

        List<Organization> organizations =
                organizationRepository.findAcceptingFoodType(
                        listing.getFoodType());

        for (Organization organization : organizations) {

            double remainingCapacity =
                    organization.getDailyCapacityKg()
                    - listingRepository.getAllocatedKgToday(
                            organization.getId());

            if (remainingCapacity < listing.getQuantityKg()) {
                continue;
            }

            double distance = DistanceCalculator.haversine(
                    listing.getLatitude(),
                    listing.getLongitude(),
                    organization.getLatitude(),
                    organization.getLongitude());

            double travelMinutes =
                    (distance / SPEED_KM_PER_HOUR) * 60.0
                    + BUFFER_MINUTES;

            if (LocalDateTime.now().plusNanos(
                    (long) (travelMinutes * 60_000_000_000L))
                    .isAfter(listing.getPickupDeadline())) {
                continue;
            }

            double distanceScore = Math.max(
                    0.0,
                    100.0 * (1.0 - distance / MAX_DISTANCE_SCORE_KM));

            double capacityScore = Math.min(
                    100.0,
                    100.0 * listing.getQuantityKg()
                    / organization.getDailyCapacityKg());

            LocalDateTime lastAllocation =
                    listingRepository.getLastAllocationTime(
                            organization.getId());

            double fairnessScore;

            if (lastAllocation == null) {
                fairnessScore = 100.0;
            } else {
                long hoursSinceLastAllocation =
                        java.time.Duration.between(
                                lastAllocation,
                                LocalDateTime.now()).toHours();

                fairnessScore = Math.min(
                        100.0,
                        Math.max(0.0,
                                hoursSinceLastAllocation / 168.0 * 100.0));
            }

            double totalScore =
                    distanceScore * DISTANCE_WEIGHT
                    + capacityScore * CAPACITY_WEIGHT
                    + fairnessScore * FAIRNESS_WEIGHT;

            MatchResult result = new MatchResult();

            result.setOrganizationId(organization.getId());
            result.setOrganizationName(organization.getName());
            result.setDistanceKm(round(distance));
            result.setEstimatedTravelMinutes(round(travelMinutes));
            result.setDistanceScore(round(distanceScore));
            result.setCapacityScore(round(capacityScore));
            result.setFairnessScore(round(fairnessScore));
            result.setTotalScore(round(totalScore));
            result.setReason(
                    "Eligible food type, sufficient capacity, "
                    + "and estimated arrival before pickup deadline.");

            results.add(result);
        }

        PriorityQueue<MatchResult> queue = new PriorityQueue<>(
                Comparator.comparingDouble(MatchResult::getTotalScore)
                        .reversed());

        queue.addAll(results);

        List<MatchResult> ranked = new ArrayList<>();

        while (!queue.isEmpty()) {
            ranked.add(queue.poll());
        }

        return ranked;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
