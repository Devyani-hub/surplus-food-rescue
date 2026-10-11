
package SurplusFoodRescue.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GeocodingService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public double[] geocode(String address) {

        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException(
                    "A valid address is required.");
        }

        try {
            String encodedAddress = URLEncoder.encode(
                    address.trim(), StandardCharsets.UTF_8);

            String url = "https://nominatim.openstreetmap.org/search"
                    + "?format=json&limit=1&q=" + encodedAddress;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent",
                            "SurplusFoodRescue/1.0 (student project)")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "Address lookup failed. HTTP status: "
                                + response.statusCode());
            }

            String body = response.body();

            Pattern pattern = Pattern.compile(
                    "\"lat\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*"
                    + "\"lon\"\\s*:\\s*\"([^\"]+)\"");

            Matcher matcher = pattern.matcher(body);

            if (!matcher.find()) {
                throw new IllegalArgumentException(
                        "Address not found. Enter a more complete address.");
            }

            double latitude = Double.parseDouble(matcher.group(1));
            double longitude = Double.parseDouble(matcher.group(2));

            return new double[]{latitude, longitude};

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Address lookup was interrupted.", e);

        } catch (java.io.IOException e) {
            throw new IllegalStateException(
                    "Could not connect to the address lookup service. "
                    + "Check your internet connection.", e);
        }
    }
}
