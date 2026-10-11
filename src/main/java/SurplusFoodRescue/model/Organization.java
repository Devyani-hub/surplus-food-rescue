package SurplusFoodRescue.model;

import java.util.ArrayList;
import java.util.List;

public class Organization {

    private Long id;
    private String name;
    private String email;
    private String address;
    private double latitude;
    private double longitude;
    private double dailyCapacityKg;
    private List<String> acceptedFoodTypes = new ArrayList<>();

    public Organization() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getDailyCapacityKg() {
        return dailyCapacityKg;
    }

    public void setDailyCapacityKg(double dailyCapacityKg) {
        this.dailyCapacityKg = dailyCapacityKg;
    }

    public List<String> getAcceptedFoodTypes() {
        return acceptedFoodTypes;
    }

    public void setAcceptedFoodTypes(List<String> acceptedFoodTypes) {
        this.acceptedFoodTypes = acceptedFoodTypes;
    }
}
