package com.aurevia.cityexplorer.controller;

import java.time.LocalDateTime;
import java.security.Principal;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.aurevia.cityexplorer.model.ContactForm;
import com.aurevia.cityexplorer.model.ContactMessage;
import com.aurevia.cityexplorer.model.Review;
import com.aurevia.cityexplorer.model.ReviewForm;
import com.aurevia.cityexplorer.repository.ContactMessageRepository;
import com.aurevia.cityexplorer.service.PortalService;
import com.aurevia.cityexplorer.service.UserService;

@Controller
public class PortalController {

    private final PortalService portalService;
    private final UserService userService;
    private final ContactMessageRepository contactMessageRepository;

    public PortalController(PortalService portalService,
                            UserService userService,
                            ContactMessageRepository contactMessageRepository) {
        this.portalService = portalService;
        this.userService = userService;
        this.contactMessageRepository = contactMessageRepository;
    }

    @GetMapping("/portal")
    public String portalHome(Model model, Principal principal) {
        var currentUser = userService.findByEmail(principal.getName()).orElseThrow();
        model.addAttribute("cities", portalService.getCityCards());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isAdmin", currentUser.isAdmin());
        return "portal";
    }

    @GetMapping("/about-us")
    public String aboutUs(Model model, Principal principal) {
        addFooterPageContext(model, principal);
        return "about-us";
    }

    @GetMapping("/contact-us")
    public String contactUs(Model model, Principal principal) {
        addFooterPageContext(model, principal);
        if (!model.containsAttribute("contactForm")) {
            var currentUser = userService.findByEmail(principal.getName()).orElseThrow();
            ContactForm contactForm = new ContactForm();
            contactForm.setFullName(currentUser.getFullName());
            contactForm.setEmail(currentUser.getEmail());
            model.addAttribute("contactForm", contactForm);
        }
        return "contact-us";
    }

    @PostMapping("/contact-us")
    public String submitContact(@Valid @ModelAttribute("contactForm") ContactForm contactForm,
                                BindingResult bindingResult,
                                Principal principal,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        var currentUser = userService.findByEmail(principal.getName()).orElseThrow();
        if (bindingResult.hasErrors()) {
            model.addAttribute("currentUser", currentUser);
            return "contact-us";
        }

        ContactMessage contactMessage = new ContactMessage();
        contactMessage.setFullName(contactForm.getFullName().trim());
        contactMessage.setEmail(contactForm.getEmail().trim());
        contactMessage.setCity(trimToNull(contactForm.getCity()));
        contactMessage.setSubject(contactForm.getSubject().trim());
        contactMessage.setMessage(contactForm.getMessage().trim());
        contactMessage.setCreatedAt(LocalDateTime.now());
        contactMessage.setUser(currentUser);
        contactMessageRepository.save(contactMessage);

        redirectAttributes.addFlashAttribute("contactSuccess", "Thanks, your message was received. We will review it soon.");
        return "redirect:/contact-us";
    }

    @GetMapping("/privacy-policy")
    public String privacyPolicy(Model model, Principal principal) {
        addFooterPageContext(model, principal);
        return "privacy-policy";
    }

    @GetMapping("/terms-of-use")
    public String termsOfUse(Model model, Principal principal) {
        addFooterPageContext(model, principal);
        return "terms-of-use";
    }

    @GetMapping("/cities/search")
    public String searchCity(@RequestParam("query") String query,
                             RedirectAttributes redirectAttributes) {
        return portalService.resolveCitySlug(query)
                .map(slug -> "redirect:/cities/" + slug)
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("searchError", "City not found. Please try another destination.");
                    return "redirect:/portal";
                });
    }

    @GetMapping("/cities/{citySlug}")
    public String cityPage(@PathVariable String citySlug, Model model, Principal principal) {
        var city = portalService.getCity(citySlug).orElseThrow();
        model.addAttribute("city", city);
        model.addAttribute("featuredCategory", portalService.getFeaturedCategory(citySlug).orElse(null));
        model.addAttribute("reviews", portalService.getReviews(citySlug));
        model.addAttribute("reviewForm", new ReviewForm());
        model.addAttribute("currentUser", userService.findByEmail(principal.getName()).orElseThrow());
        return "city";
    }

    @PostMapping("/cities/{citySlug}/reviews")
    public String submitReview(@PathVariable String citySlug,
                               @Valid @ModelAttribute("reviewForm") ReviewForm reviewForm,
                               BindingResult bindingResult,
                               Principal principal,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        var city = portalService.getCity(citySlug).orElseThrow();
        if (bindingResult.hasErrors()) {
            model.addAttribute("city", city);
            model.addAttribute("featuredCategory", portalService.getFeaturedCategory(citySlug).orElse(null));
            model.addAttribute("reviews", portalService.getReviews(citySlug));
            model.addAttribute("currentUser", userService.findByEmail(principal.getName()).orElseThrow());
            return "city";
        }

        Review review = new Review();
        review.setRating(reviewForm.getRating());
        review.setComment(reviewForm.getComment().trim());
        review.setUser(userService.findByEmail(principal.getName()).orElseThrow());
        portalService.addReview(citySlug, review);
        redirectAttributes.addFlashAttribute("reviewSuccess", "Your review was published.");
        return "redirect:/cities/" + citySlug + "#reviews";
    }

    @GetMapping("/cities/{citySlug}/categories/{categorySlug}")
    public String categoryPage(@PathVariable String citySlug,
                               @PathVariable String categorySlug,
                               Model model,
                               Principal principal) {
        var city = portalService.getCity(citySlug).orElseThrow();
        var category = portalService.getCategory(citySlug, categorySlug).orElseThrow();
        model.addAttribute("city", city);
        model.addAttribute("category", category);
        model.addAttribute("currentUser", userService.findByEmail(principal.getName()).orElseThrow());
        return "category";
    }

    @GetMapping("/cities/{citySlug}/categories/{categorySlug}/places/{placeSlug}")
    public String placePage(@PathVariable String citySlug,
                            @PathVariable String categorySlug,
                            @PathVariable String placeSlug,
                            Model model,
                            Principal principal) {
        var city = portalService.getCity(citySlug).orElseThrow();
        var category = portalService.getCategory(citySlug, categorySlug).orElseThrow();
        var place = portalService.getPlace(citySlug, categorySlug, placeSlug).orElseThrow();
        model.addAttribute("city", city);
        model.addAttribute("category", category);
        model.addAttribute("place", place);
        model.addAttribute("reviews", portalService.getReviews(citySlug, categorySlug, placeSlug));
        model.addAttribute("reviewForm", new ReviewForm());
        model.addAttribute("currentUser", userService.findByEmail(principal.getName()).orElseThrow());
        return "place";
    }

    @PostMapping("/cities/{citySlug}/categories/{categorySlug}/places/{placeSlug}/reviews")
    public String submitPlaceReview(@PathVariable String citySlug,
                                    @PathVariable String categorySlug,
                                    @PathVariable String placeSlug,
                                    @Valid @ModelAttribute("reviewForm") ReviewForm reviewForm,
                                    BindingResult bindingResult,
                                    Principal principal,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        var city = portalService.getCity(citySlug).orElseThrow();
        var category = portalService.getCategory(citySlug, categorySlug).orElseThrow();
        var place = portalService.getPlace(citySlug, categorySlug, placeSlug).orElseThrow();
        if (bindingResult.hasErrors()) {
            model.addAttribute("city", city);
            model.addAttribute("category", category);
            model.addAttribute("place", place);
            model.addAttribute("reviews", portalService.getReviews(citySlug, categorySlug, placeSlug));
            model.addAttribute("currentUser", userService.findByEmail(principal.getName()).orElseThrow());
            return "place";
        }

        Review review = new Review();
        review.setRating(reviewForm.getRating());
        review.setComment(reviewForm.getComment().trim());
        review.setUser(userService.findByEmail(principal.getName()).orElseThrow());
        portalService.addReview(citySlug, categorySlug, placeSlug, review);
        redirectAttributes.addFlashAttribute("reviewSuccess", "Your place review was published.");
        return "redirect:/cities/" + citySlug + "/categories/" + categorySlug + "/places/" + placeSlug + "#reviews";
    }

    @GetMapping("/cities/{citySlug}/genz")
    public String genzPage(@PathVariable String citySlug, Model model, Principal principal) {
        var city = portalService.getCity(citySlug).orElseThrow();
        model.addAttribute("city", city);
        model.addAttribute("genzData", portalService.getGenZModeData(citySlug));
        model.addAttribute("currentUser", userService.findByEmail(principal.getName()).orElseThrow());
        return "genz";
    }

    private void addFooterPageContext(Model model, Principal principal) {
        model.addAttribute("currentUser", userService.findByEmail(principal.getName()).orElseThrow());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
