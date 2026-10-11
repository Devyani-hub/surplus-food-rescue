package SurplusFoodRescue.service;

import SurplusFoodRescue.repository.FoodListingRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpiryScheduler {

    private final FoodListingRepository listings;

    public ExpiryScheduler(FoodListingRepository listings) {
        this.listings = listings;
    }

    @Scheduled(fixedDelay = 60000)
    public void expireOverdueListings() {
        int count = listings.expireOverdue();

        if (count > 0) {
            System.out.println(
                    "Expired " + count + " overdue food listing(s).");
        }
    }
}