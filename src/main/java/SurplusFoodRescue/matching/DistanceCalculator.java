package SurplusFoodRescue.matching;

public final class DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private DistanceCalculator() {
    }

    public static double haversine(
            double lat1, double lon1,
            double lat2, double lon2) {

        double latitudeDifference =
                Math.toRadians(lat2 - lat1);

        double longitudeDifference =
                Math.toRadians(lon2 - lon1);

        double a = Math.sin(latitudeDifference / 2)
                * Math.sin(latitudeDifference / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(longitudeDifference / 2)
                * Math.sin(longitudeDifference / 2);

        double c = 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    public static double estimatedTravelMinutes(double distanceKm) {
        return (distanceKm / 30.0) * 60.0 + 15.0;
    }
}
