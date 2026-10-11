package SurplusFoodRescue.service;

import SurplusFoodRescue.model.Donation;
import SurplusFoodRescue.repository.DonationRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DonationService {

    private final DonationRepository donationRepository;

    public DonationService(
            DonationRepository donationRepository) {
        this.donationRepository = donationRepository;
    }

    public List<Donation> getAvailableDonations() {
        return donationRepository.findAvailableDonations();
    }

    public void createDonation(Donation donation) {

        if (donation.getFoodName() == null
                || donation.getFoodName().isBlank()) {
            throw new IllegalArgumentException(
                    "Food name is required.");
        }

        if (donation.getQuantity() <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero.");
        }

        if (donation.getUnit() == null
                || donation.getUnit().isBlank()) {
            throw new IllegalArgumentException(
                    "Unit is required.");
        }

        if (donation.getPreparedAt() == null
                || donation.getCollectionDeadline() == null) {
            throw new IllegalArgumentException(
                    "Preparation time and collection deadline are required.");
        }

        if (donation.getCollectionDeadline()
                .isBefore(donation.getPreparedAt())) {
            throw new IllegalArgumentException(
                    "Collection deadline cannot be before preparation time.");
        }

        if (donation.getCollectionDeadline()
                .isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Collection deadline must be in the future.");
        }

        if (donation.getPickupLocation() == null
                || donation.getPickupLocation().isBlank()) {
            throw new IllegalArgumentException(
                    "Pickup location is required.");
        }

        donationRepository.save(donation);
    }
}