package SurplusFoodRescue.controller;

import SurplusFoodRescue.model.Donation;
import SurplusFoodRescue.service.DonationService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DonationController {

    private final DonationService donationService;

    public DonationController(
            DonationService donationService) {
        this.donationService = donationService;
    }

    @GetMapping("/")
public String home() {
    return "forward:/index.html";
}

    @GetMapping("/donations")
    public String listDonations(Model model) {
        model.addAttribute(
                "donations",
                donationService.getAvailableDonations()
        );

        return "donations";
    }

    @GetMapping("/donations/new")
    public String showDonationForm(Model model) {
        model.addAttribute("donation", new Donation());
        return "donation-form";
    }

    @PostMapping("/donations")
    public String createDonation(
            @ModelAttribute Donation donation,
            RedirectAttributes redirectAttributes) {

        try {
            donationService.createDonation(donation);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Donation listed successfully!"
            );

            return "redirect:/donations";

        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

            return "redirect:/donations/new";
        }
    }
}