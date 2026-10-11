package SurplusFoodRescue.model;

public class MatchResult {

    private Long organizationId;
    private String organizationName;
    private double distanceKm;
    private double estimatedTravelMinutes;
    private double distanceScore;
    private double capacityScore;
    private double fairnessScore;
    private double totalScore;
    private String reason;

    public MatchResult() {
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public double getEstimatedTravelMinutes() {
        return estimatedTravelMinutes;
    }

    public void setEstimatedTravelMinutes(double estimatedTravelMinutes) {
        this.estimatedTravelMinutes = estimatedTravelMinutes;
    }

    public double getDistanceScore() {
        return distanceScore;
    }

    public void setDistanceScore(double distanceScore) {
        this.distanceScore = distanceScore;
    }

    public double getCapacityScore() {
        return capacityScore;
    }

    public void setCapacityScore(double capacityScore) {
        this.capacityScore = capacityScore;
    }

    public double getFairnessScore() {
        return fairnessScore;
    }

    public void setFairnessScore(double fairnessScore) {
        this.fairnessScore = fairnessScore;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(double totalScore) {
        this.totalScore = totalScore;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
