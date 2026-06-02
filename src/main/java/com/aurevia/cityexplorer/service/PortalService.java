package com.aurevia.cityexplorer.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.aurevia.cityexplorer.model.AdminCityForm;
import com.aurevia.cityexplorer.model.AdminPlaceForm;
import com.aurevia.cityexplorer.model.ManagedCity;
import com.aurevia.cityexplorer.model.ManagedPlace;
import com.aurevia.cityexplorer.model.Review;
import com.aurevia.cityexplorer.repository.ManagedCityRepository;
import com.aurevia.cityexplorer.repository.ManagedPlaceRepository;
import com.aurevia.cityexplorer.repository.ReviewRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PortalService {

    private static final DateTimeFormatter REVIEW_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);
    private static final int BASE_CATEGORY_PLACE_COUNT = 6;
    private static final int ENRICHED_CATEGORY_PLACE_COUNT = 31;

    private final ReviewRepository reviewRepository;
    private final ManagedCityRepository managedCityRepository;
    private final ManagedPlaceRepository managedPlaceRepository;
    private final Map<String, CityPage> cities;
    private final Map<String, String> cityAliases;
    private final List<FlightOption> flightOptions;

    public PortalService(ReviewRepository reviewRepository,
                         ManagedCityRepository managedCityRepository,
                         ManagedPlaceRepository managedPlaceRepository) {
        this.reviewRepository = reviewRepository;
        this.managedCityRepository = managedCityRepository;
        this.managedPlaceRepository = managedPlaceRepository;
        this.cities = buildCities();
        this.cityAliases = buildAliases();
        this.flightOptions = List.of(
                new FlightOption("AI-202", "Air India", "Delhi", "Chandigarh", "07:15", "08:20", 4299),
                new FlightOption("6E-411", "IndiGo", "Delhi", "Chandigarh", "10:10", "11:15", 3899),
                new FlightOption("UK-981", "Vistara", "Mumbai", "Chandigarh", "12:45", "15:00", 6499),
                new FlightOption("SG-118", "SpiceJet", "Bengaluru", "Chandigarh", "18:20", "21:10", 7199),
                new FlightOption("6E-733", "IndiGo", "Mumbai", "Goa", "08:40", "09:55", 4599),
                new FlightOption("AI-554", "Air India", "Delhi", "Srinagar", "09:25", "10:50", 6999),
                new FlightOption("UK-340", "Vistara", "Delhi", "Jaipur", "11:30", "12:35", 3699),
                new FlightOption("AI-214", "Air India", "Delhi", "Chandigarh", "13:30", "14:35", 4499),
                new FlightOption("6E-621", "IndiGo", "Delhi", "Chandigarh", "16:05", "17:10", 4099),
                new FlightOption("UK-973", "Vistara", "Delhi", "Chandigarh", "20:15", "21:20", 4699),
                new FlightOption("AI-411", "Air India", "Mumbai", "Jaipur", "06:50", "08:35", 5299),
                new FlightOption("6E-218", "IndiGo", "Bengaluru", "Jaipur", "09:15", "11:40", 5999),
                new FlightOption("SG-276", "SpiceJet", "Ahmedabad", "Jaipur", "14:25", "15:45", 3499),
                new FlightOption("UK-454", "Vistara", "Kolkata", "Jaipur", "17:35", "20:10", 6499),
                new FlightOption("6E-502", "IndiGo", "Delhi", "Agra", "07:00", "08:05", 3299),
                new FlightOption("AI-309", "Air India", "Mumbai", "Agra", "10:20", "12:30", 5599),
                new FlightOption("UK-612", "Vistara", "Bengaluru", "Agra", "13:40", "16:10", 6199),
                new FlightOption("SG-441", "SpiceJet", "Jaipur", "Agra", "18:05", "19:10", 2999),
                new FlightOption("6E-894", "IndiGo", "Kolkata", "Agra", "20:30", "22:35", 5799),
                new FlightOption("AI-672", "Air India", "Delhi", "Varanasi", "06:40", "08:05", 4199),
                new FlightOption("6E-245", "IndiGo", "Mumbai", "Varanasi", "09:55", "12:05", 5899),
                new FlightOption("UK-733", "Vistara", "Bengaluru", "Varanasi", "12:30", "15:00", 6699),
                new FlightOption("SG-528", "SpiceJet", "Kolkata", "Varanasi", "16:45", "18:05", 3999),
                new FlightOption("6E-917", "IndiGo", "Jaipur", "Varanasi", "19:20", "21:05", 4599),
                new FlightOption("AI-190", "Air India", "Mumbai", "Delhi", "06:10", "08:20", 5499),
                new FlightOption("6E-302", "IndiGo", "Bengaluru", "Delhi", "09:05", "11:45", 6299),
                new FlightOption("UK-808", "Vistara", "Kolkata", "Delhi", "12:15", "14:35", 5699),
                new FlightOption("SG-722", "SpiceJet", "Jaipur", "Delhi", "17:40", "18:45", 3299),
                new FlightOption("6E-155", "IndiGo", "Goa", "Delhi", "20:05", "22:50", 6999),
                new FlightOption("AI-486", "Air India", "Delhi", "Udaipur", "07:35", "09:05", 4399),
                new FlightOption("6E-710", "IndiGo", "Mumbai", "Udaipur", "10:40", "12:10", 3999),
                new FlightOption("UK-520", "Vistara", "Bengaluru", "Udaipur", "13:25", "15:50", 6099),
                new FlightOption("SG-309", "SpiceJet", "Jaipur", "Udaipur", "16:15", "17:20", 2899),
                new FlightOption("6E-642", "IndiGo", "Kolkata", "Udaipur", "19:00", "21:45", 6799),
                new FlightOption("AI-735", "Air India", "Delhi", "Jaisalmer", "06:55", "08:45", 5199),
                new FlightOption("6E-367", "IndiGo", "Jaipur", "Jaisalmer", "09:30", "10:50", 3499),
                new FlightOption("UK-429", "Vistara", "Mumbai", "Jaisalmer", "12:10", "14:05", 5899),
                new FlightOption("SG-812", "SpiceJet", "Ahmedabad", "Jaisalmer", "15:20", "16:55", 4299),
                new FlightOption("6E-280", "IndiGo", "Bengaluru", "Jaisalmer", "18:45", "21:25", 7199),
                new FlightOption("AI-804", "Air India", "Delhi", "Goa", "06:30", "09:10", 6599),
                new FlightOption("UK-928", "Vistara", "Bengaluru", "Goa", "11:25", "12:45", 4299),
                new FlightOption("SG-601", "SpiceJet", "Ahmedabad", "Goa", "14:50", "16:35", 4999),
                new FlightOption("6E-746", "IndiGo", "Kolkata", "Goa", "19:15", "22:05", 7299),
                new FlightOption("AI-248", "Air India", "Delhi", "Mumbai", "07:10", "09:20", 5499),
                new FlightOption("6E-509", "IndiGo", "Jaipur", "Mumbai", "10:35", "12:25", 4799),
                new FlightOption("UK-315", "Vistara", "Bengaluru", "Mumbai", "13:05", "14:50", 4399),
                new FlightOption("SG-770", "SpiceJet", "Goa", "Mumbai", "17:10", "18:25", 3299),
                new FlightOption("6E-983", "IndiGo", "Kolkata", "Mumbai", "20:20", "23:10", 6699),
                new FlightOption("AI-512", "Air India", "Delhi", "Maharashtra", "06:45", "08:55", 5599),
                new FlightOption("6E-687", "IndiGo", "Jaipur", "Maharashtra", "09:40", "11:35", 4899),
                new FlightOption("UK-826", "Vistara", "Bengaluru", "Maharashtra", "12:50", "14:35", 4499),
                new FlightOption("SG-218", "SpiceJet", "Goa", "Maharashtra", "16:30", "17:45", 3399),
                new FlightOption("6E-401", "IndiGo", "Kolkata", "Maharashtra", "19:55", "22:45", 6799),
                new FlightOption("AI-963", "Air India", "Delhi", "Kochi", "06:20", "09:45", 7499),
                new FlightOption("6E-345", "IndiGo", "Mumbai", "Kochi", "10:15", "12:10", 4999),
                new FlightOption("UK-604", "Vistara", "Bengaluru", "Kochi", "13:00", "14:10", 3599),
                new FlightOption("SG-190", "SpiceJet", "Goa", "Kochi", "16:25", "18:00", 4299),
                new FlightOption("6E-778", "IndiGo", "Kolkata", "Kochi", "20:05", "23:20", 7999),
                new FlightOption("AI-366", "Air India", "Delhi", "Darjeeling", "07:05", "09:25", 6899),
                new FlightOption("6E-914", "IndiGo", "Mumbai", "Darjeeling", "10:45", "13:30", 7599),
                new FlightOption("UK-242", "Vistara", "Kolkata", "Darjeeling", "14:10", "15:20", 3799),
                new FlightOption("SG-635", "SpiceJet", "Bengaluru", "Darjeeling", "17:15", "20:05", 8199),
                new FlightOption("6E-157", "IndiGo", "Jaipur", "Darjeeling", "21:00", "23:45", 7299),
                new FlightOption("AI-702", "Air India", "Delhi", "Leh", "05:55", "07:20", 7299),
                new FlightOption("6E-430", "IndiGo", "Mumbai", "Leh", "08:35", "11:25", 8499),
                new FlightOption("UK-510", "Vistara", "Chandigarh", "Leh", "12:15", "13:35", 5799),
                new FlightOption("SG-284", "SpiceJet", "Jaipur", "Leh", "15:40", "17:45", 7699),
                new FlightOption("6E-901", "IndiGo", "Bengaluru", "Leh", "18:30", "22:05", 9299),
                new FlightOption("6E-608", "IndiGo", "Mumbai", "Srinagar", "12:20", "14:55", 7899),
                new FlightOption("UK-716", "Vistara", "Chandigarh", "Srinagar", "15:30", "16:50", 4899),
                new FlightOption("SG-392", "SpiceJet", "Jaipur", "Srinagar", "18:10", "20:05", 6099),
                new FlightOption("6E-144", "IndiGo", "Bengaluru", "Srinagar", "21:00", "00:20", 8999),
                new FlightOption("AI-607", "Air India", "Delhi", "Manali", "06:25", "07:55", 5299),
                new FlightOption("6E-772", "IndiGo", "Chandigarh", "Manali", "09:10", "10:20", 3499),
                new FlightOption("UK-384", "Vistara", "Mumbai", "Manali", "12:35", "15:05", 7199),
                new FlightOption("SG-509", "SpiceJet", "Jaipur", "Manali", "16:45", "18:25", 4899),
                new FlightOption("6E-260", "IndiGo", "Bengaluru", "Manali", "19:30", "22:30", 8499),
                new FlightOption("AI-428", "Air India", "Delhi", "Shimla", "07:20", "08:35", 3999),
                new FlightOption("6E-689", "IndiGo", "Chandigarh", "Shimla", "10:05", "10:55", 2799),
                new FlightOption("UK-931", "Vistara", "Mumbai", "Shimla", "13:15", "15:35", 6799),
                new FlightOption("SG-487", "SpiceJet", "Jaipur", "Shimla", "17:05", "18:40", 4499),
                new FlightOption("6E-350", "IndiGo", "Bengaluru", "Shimla", "20:10", "23:05", 7999),
                new FlightOption("AI-844", "Air India", "Delhi", "Rishikesh", "06:15", "07:20", 3799),
                new FlightOption("6E-521", "IndiGo", "Mumbai", "Rishikesh", "09:45", "12:05", 6299),
                new FlightOption("UK-265", "Vistara", "Bengaluru", "Rishikesh", "13:20", "16:05", 7199),
                new FlightOption("SG-174", "SpiceJet", "Jaipur", "Rishikesh", "17:25", "18:50", 3999),
                new FlightOption("6E-706", "IndiGo", "Kolkata", "Rishikesh", "20:40", "23:20", 6899),
                new FlightOption("AI-259", "Air India", "Delhi", "Chamba", "06:50", "08:20", 4599),
                new FlightOption("6E-319", "IndiGo", "Chandigarh", "Chamba", "09:35", "10:45", 3299),
                new FlightOption("UK-742", "Vistara", "Mumbai", "Chamba", "12:40", "15:25", 7399),
                new FlightOption("SG-956", "SpiceJet", "Jaipur", "Chamba", "16:30", "18:15", 5099),
                new FlightOption("6E-872", "IndiGo", "Bengaluru", "Chamba", "19:20", "22:35", 8699)
        );
    }

    public List<CityCard> getCityCards() {
        List<CityCard> builtIn = cities.values().stream()
                .map(city -> new CityCard(city.slug(), city.name(), city.tagline(), city.heroImage()))
                .toList();
        List<CityCard> managed = managedCityRepository.findAll().stream()
                .sorted(Comparator.comparing(ManagedCity::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(city -> new CityCard(city.getSlug(), city.getName(), city.getTagline(), city.getHeroImage()))
                .toList();
        List<CityCard> all = new ArrayList<>(managed);
        all.addAll(builtIn);
        return all;
    }

    public Optional<CityPage> getCity(String slug) {
        CityPage builtIn = cities.get(slug);
        if (builtIn != null) {
            return Optional.of(mergeCityWithManagedContent(builtIn, slug));
        }
        return managedCityRepository.findBySlug(slug).map(city -> toCityPage(city, slug));
    }

    public Optional<String> resolveCitySlug(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalize(input);
        if (getCity(normalized).isPresent()) {
            return Optional.of(normalized);
        }
        String alias = cityAliases.get(normalized);
        if (alias != null) {
            return Optional.of(alias);
        }
        return managedCityRepository.findAll().stream()
                .filter(city -> normalize(city.getName()).equals(normalized)
                        || normalize(city.getSlug()).equals(normalized)
                        || parseMultiline(city.getSearchKeywords()).stream().map(this::normalize).anyMatch(normalized::equals))
                .map(ManagedCity::getSlug)
                .findFirst();
    }

    public Optional<CategoryPage> getCategory(String citySlug, String categorySlug) {
        return getCity(citySlug).flatMap(city -> city.categories().stream()
                .filter(category -> category.slug().equals(categorySlug))
                .findFirst());
    }

    public Optional<CategoryPage> getFeaturedCategory(String citySlug) {
        return getCity(citySlug).flatMap(city -> city.categories().stream()
                .filter(category -> !category.places().isEmpty())
                .findFirst()
                .or(() -> city.categories().stream().findFirst()));
    }

    public GenZModeData getGenZModeData(String citySlug) {
        CityPage city = getCity(citySlug).orElseThrow();
        return buildGenZModeData(city);
    }

    public List<GenZCategory> getGenZCategories(String citySlug) {
        CityPage city = getCity(citySlug).orElseThrow();
        GenZModeData data = buildGenZModeData(city);
        return List.of(
                new GenZCategory(
                        "cafes",
                        "Gen Z Cafes",
                        "Coffee, brunch, study tables, and hangout-friendly food stops in " + city.name() + ".",
                        genZCategoryImage("cafes"),
                        data.vibe().getOrDefault("chill", List.of()).stream()
                                .filter(place -> "cafe".equals(place.type()))
                                .limit(8)
                                .toList()),
                new GenZCategory(
                        "popular-places",
                        "Photo Spots",
                        "Camera-ready landmarks, viewpoints, and social plans around " + city.name() + ".",
                        genZCategoryImage("popular-places"),
                        data.trending()),
                new GenZCategory(
                        "tourist-places",
                        "Bunk Spots",
                        "Low-effort places, short escapes, and friend-group plans around " + city.name() + ".",
                        genZCategoryImage("tourist-places"),
                        data.bunkSpots()),
                new GenZCategory(
                        "nightlife",
                        "Night Energy",
                        "Late food, lounge mood, music plans, and group-night ideas in " + city.name() + ".",
                        genZCategoryImage("nightlife"),
                        data.clubs()),
                new GenZCategory(
                        "hidden-gems",
                        "Hidden Gems",
                        "Low-key, pretty, and less obvious plans around " + city.name() + ".",
                        genZCategoryImage("hidden-gems"),
                        data.hiddenGems()),
                new GenZCategory(
                        "restaurants",
                        "Food Plans",
                        "Restaurants, snacks, and food-crawl picks for " + city.name() + ".",
                        genZCategoryImage("restaurants"),
                        genZPlacesFromCategory(city, "restaurants", "cafe")));
    }

    public Optional<PlacePage> getPlace(String citySlug, String categorySlug, String placeSlug) {
        return getCategory(citySlug, categorySlug)
                .flatMap(category -> category.places().stream()
                        .filter(place -> place.slug().equals(placeSlug))
                        .findFirst()
                        .map(place -> toPlacePage(citySlug, category, place)));
    }

    public List<ReviewView> getReviews(String citySlug) {
        return reviewRepository.findTop8ByCitySlugAndCategorySlugIsNullAndPlaceSlugIsNullOrderByCreatedAtDesc(citySlug).stream()
                .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                .map(review -> new ReviewView(
                        review.getUser().getFullName(),
                        review.getRating(),
                        review.getComment(),
                        review.getCreatedAt().format(REVIEW_TIME)))
                .toList();
    }

    public List<ReviewView> getReviews(String citySlug, String categorySlug) {
        return reviewRepository.findTop8ByCitySlugAndCategorySlugAndPlaceSlugIsNullOrderByCreatedAtDesc(citySlug, categorySlug).stream()
                .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                .map(review -> new ReviewView(
                        review.getUser().getFullName(),
                        review.getRating(),
                        review.getComment(),
                        review.getCreatedAt().format(REVIEW_TIME)))
                .toList();
    }

    public List<ReviewView> getReviews(String citySlug, String categorySlug, String placeSlug) {
        return reviewRepository.findTop8ByCitySlugAndCategorySlugAndPlaceSlugOrderByCreatedAtDesc(citySlug, categorySlug, placeSlug).stream()
                .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                .map(review -> new ReviewView(
                        review.getUser().getFullName(),
                        review.getRating(),
                        review.getComment(),
                        review.getCreatedAt().format(REVIEW_TIME)))
                .toList();
    }

    public void addReview(String citySlug, Review review) {
        review.setCitySlug(citySlug);
        review.setCategorySlug(null);
        review.setPlaceSlug(null);
        review.setCreatedAt(LocalDateTime.now());
        reviewRepository.save(review);
    }

    public void addReview(String citySlug, String categorySlug, Review review) {
        review.setCitySlug(citySlug);
        review.setCategorySlug(categorySlug);
        review.setPlaceSlug(null);
        review.setCreatedAt(LocalDateTime.now());
        reviewRepository.save(review);
    }

    public void addReview(String citySlug, String categorySlug, String placeSlug, Review review) {
        review.setCitySlug(citySlug);
        review.setCategorySlug(categorySlug);
        review.setPlaceSlug(placeSlug);
        review.setCreatedAt(LocalDateTime.now());
        reviewRepository.save(review);
    }

    public List<FlightOption> searchFlights(String fromCity, String toCity) {
        return flightOptions.stream()
                .filter(option -> option.fromCity().equalsIgnoreCase(fromCity) || fromCity == null || fromCity.isBlank())
                .filter(option -> option.toCity().equalsIgnoreCase(toCity) || toCity == null || toCity.isBlank())
                .toList();
    }

    public List<HotelOption> searchHotels(String cityQuery) {
        String resolvedSlug = resolveCitySlug(cityQuery).orElse(normalize(cityQuery));
        return getCity(resolvedSlug)
                .map(city -> city.categories().stream()
                        .filter(category -> "hotels".equals(category.slug()))
                        .flatMap(category -> category.places().stream()
                                .map(place -> new HotelOption(city.name(), place.name(), place.description(), place.image(), 4200 + (place.name().length() * 110L))))
                        .toList())
                .orElse(List.of());
    }

    public List<String> getAdminCategoryOptions() {
        return List.of(
                "popular-places",
                "tourist-places",
                "hotels",
                "cafes",
                "educational-places",
                "emergency-services");
    }

    public ManagedCity addManagedCity(AdminCityForm form) {
        ManagedCity city = new ManagedCity();
        city.setSlug(buildUniqueSlug(form.getName()));
        applyManagedCityForm(city, form);
        return managedCityRepository.save(city);
    }

    public ManagedCity updateManagedCity(Long cityId, AdminCityForm form) {
        ManagedCity city = managedCityRepository.findById(cityId).orElseThrow();
        applyManagedCityForm(city, form);
        return managedCityRepository.save(city);
    }

    public void deleteManagedCity(Long cityId) {
        ManagedCity city = managedCityRepository.findById(cityId).orElseThrow();
        managedPlaceRepository.deleteByCitySlug(city.getSlug());
        managedCityRepository.delete(city);
    }

    public ManagedPlace addManagedPlace(AdminPlaceForm form) {
        String citySlug = sanitizeCitySlug(form.getCitySlug());
        if (getCity(citySlug).isEmpty()) {
            throw new IllegalArgumentException("Selected city does not exist.");
        }
        ManagedPlace place = new ManagedPlace();
        applyManagedPlaceForm(place, form);
        return managedPlaceRepository.save(place);
    }

    public ManagedPlace updateManagedPlace(Long placeId, AdminPlaceForm form) {
        String citySlug = sanitizeCitySlug(form.getCitySlug());
        if (getCity(citySlug).isEmpty()) {
            throw new IllegalArgumentException("Selected city does not exist.");
        }
        ManagedPlace place = managedPlaceRepository.findById(placeId).orElseThrow();
        applyManagedPlaceForm(place, form);
        return managedPlaceRepository.save(place);
    }

    public void deleteManagedPlace(Long placeId) {
        managedPlaceRepository.deleteById(placeId);
    }

    public List<ManagedCity> getManagedCities() {
        return managedCityRepository.findAll();
    }

    public List<ManagedPlaceView> getManagedPlaceViews() {
        Map<String, String> cityNames = getCityCards().stream()
                .collect(Collectors.toMap(CityCard::slug, CityCard::name, (first, second) -> first, LinkedHashMap::new));

        return managedPlaceRepository.findAllByOrderByCitySlugAscNameAsc().stream()
                .map(place -> new ManagedPlaceView(
                        place.getId(),
                        place.getCitySlug(),
                        cityNames.getOrDefault(place.getCitySlug(), place.getCitySlug()),
                        place.getCategorySlug(),
                        labelForCategory(place.getCategorySlug()),
                        place.getName(),
                        place.getDescription(),
                        place.getImageUrl(),
                        place.getGalleryImages(),
                        place.getInsight(),
                        place.getHistory(),
                        place.getAddress(),
                        place.getTimings(),
                        place.getPriceRange(),
                        place.getDetailTitleOne(),
                        place.getDetailBodyOne(),
                        place.getDetailTitleTwo(),
                        place.getDetailBodyTwo(),
                        place.getDetailTitleThree(),
                        place.getDetailBodyThree(),
                        place.getDetailTitleFour(),
                        place.getDetailBodyFour(),
                        place.getVisitorNotes(),
                        parseMultiline(place.getGalleryImages()).size()))
                .toList();
    }

    private Map<String, CityPage> buildCities() {
        Map<String, CityPage> editableData = loadEditableCityData();
        if (!editableData.isEmpty()) {
            return editableData;
        }

        Map<String, CityPage> data = new LinkedHashMap<>();

        data.put("jaipur", curatedCity(
                "jaipur", "Jaipur", "Pink City palaces, bazaars, forts, rooftop cafes, and royal stays.",
                "https://s7ap1.scene7.com/is/image/incredibleindia/hawa-mahal-jaipur-rajasthan-city-1-hero?qlt=82&ts=1742200253577", "Rajasthan",
                List.of(new QuickFact("Amber Fort", "Icon"), new QuickFact("Old City", "Markets"), new QuickFact("Royal", "Heritage")),
              
              
                List.of(
                        place("Hawa Mahal", "The landmark facade that defines Jaipur's old city.", "https://static.toiimg.com/thumb/msid-103378759,width-748,height-499,resizemode=4,imgsize-147006/.jpg"),
                        place("Amber Fort", "A hilltop fort with grand courtyards and sweeping views.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/17/d3/a8/57/images-30-largejpg.jpg?w=700&h=400&s=1"),
                        place("City Palace", "Historic palace complex blending Rajput and Mughal design.", "https://static.toiimg.com/photo/48774127.cms"),
place("Jantar Mantar", "UNESCO-listed astronomical observatory with massive instruments.", "https://s7ap1.scene7.com/is/image/incredibleindia/jantar-mantar-jaipur-rajasthan-1-attr-hero?qlt=82&ts=1742159939291"),

place("Nahargarh Fort", "Fort offering panoramic views of Jaipur city.", "https://www.cdn.travejar.com/storage/india_attraction_tour/1680068430.webp"),

place("Jaigarh Fort", "Home to the world's largest cannon on wheels.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/15/06/25/d9/jaigarh-fort.jpg?w=900&h=500&s=1"),

place("Albert Hall Museum", "Oldest museum in Rajasthan with Indo-Saracenic architecture.", "https://s7ap1.scene7.com/is/image/incredibleindia/albert-hall-museum-jaipur-rajasthan-3-attr-hero?qlt=82&ts=1742199803796"),

place("Jal Mahal", "Beautiful palace situated in the middle of Man Sagar Lake.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/0a/c4/72/f6/jal-mahal-jaipur-tour.jpg?w=900&h=500&s=1"),

place("Birla Mandir", "White marble temple dedicated to Lord Vishnu and Lakshmi.", "https://rajputanacabs.b-cdn.net/wp-content/uploads/2025/09/Birla-Temple-Jaipur-AI.webp"),

place("Galta Ji Temple", "Ancient temple complex famous for natural water springs and monkeys.", "https://static.toiimg.com/photo/msid-74337738,width-96,height-65.cms"),

place("Patrika Gate", "Colorful gateway known for vibrant Rajasthani art.", "https://patrikagate.org/wp-content/uploads/2025/01/Patrika_gate_Jaipur-banner.jpg"),

place("Sisodia Rani Garden", "Beautiful garden with fountains and painted pavilions.", "https://media-cdn.tripadvisor.com/media/photo-s/1a/32/65/e2/sisodia-rani-palace-and.jpg"),

place("Vidyadhar Garden", "Peaceful garden inspired by Mughal architecture.", "https://jaipurtourism.co.in/images//tourist-places/vidyadhar-garden-jaipur/vidyadhar-garden-jaipur-tourism-entry-ticket-price.jpg"),

place("Central Park", "Large green space with jogging tracks and a giant Indian flag.", "https://s7ap1.scene7.com/is/image/incredibleindia/central-park-jaipur-rajasthan-2-attr-hero?qlt=82&ts=1742170547811"),

place("Bapu Bazaar", "Famous market for textiles, handicrafts, and souvenirs.", "https://jaipur-tourism.com/wp-content/uploads/2026/01/Bapu-Bazar-Jaipur.jpg"),

place("Chokhi Dhani", "Ethnic village resort offering Rajasthani culture and food.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/16/64/14/90/chokhi-dhani-resort.jpg?w=900&h=500&s=1"),

place("Raj Mandir Cinema", "Famous heritage cinema hall with unique architecture.", "https://www.easeindiatrip.com/blog/wp-content/uploads/2025/08/Jaipur-Raj-Mandir-Cinema-02.jpg"),

place("Kanak Vrindavan Garden", "Scenic garden near Amber Fort with temples and greenery.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/17/1b/97/5f/caption.jpg?w=900&h=500&s=1"),

place("Gaitore Ki Chhatriyan", "Royal cenotaphs of Jaipur's former rulers.", "https://zbanmukljbifxbgmjngh.supabase.co/storage/v1/object/public/place-images/gatore-ki-chhatriyan/1767375973924-5t8ppy.webp"),

place("Anokhi Museum", "Museum dedicated to traditional block printing art.", "https://s7ap1.scene7.com/is/image/incredibleindia/anokhi-museum-of-hand-printing-jaipur-blog-art-hero?qlt=82&ts=1742199973973"),
place("Panna Meena Ka Kund", "Historic stepwell known for its symmetrical staircases.", "https://www.trawell.in/admin/images/upload/panna-meena-ka-kund.jpg"),

place("Amar Jawan Jyoti", "War memorial honoring soldiers with eternal flame.", "https://www.holidify.com/images/cmsuploads/compressed/amar-jawan-jyoti-jaipur_20220328112715.jpeg"),

place("Jawahar Circle Garden", "One of Asia's largest circular parks with musical fountain.", "https://s7ap1.scene7.com/is/image/incredibleindia/jawahar-circle-jaipur-rajasthan-1-attr-hero?qlt=82&ts=1742190477621"),

place("Smriti Van", "Eco-park and biodiversity forest with peaceful walking trails.", "https://kingslandholiday.com/wp-content/uploads/2024/07/Smriti-Van-Jaipur1-1024x1024.jpg"),

place("Ram Niwas Garden", "Historic garden with greenery and cultural attractions.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/16/af/d0/af/the-nightly-illumination.jpg?w=900&h=500&s=1"),

place("Leopard Safari Jhalana", "Wildlife reserve known for leopard sightings.", "https://www.pelago.com/img/products/IN-India/safari-in-jhalana-leopard-conservation-reserve-jaipur/b1c0b789-d30e-4066-81e4-3f128cb7cec1_safari-in-jhalana-leopard-conservation-reserve-jaipur-xlarge.webp"),

place("Chulgiri Jain Temple", "Hilltop temple with scenic views of Jaipur.", "https://i0.wp.com/shutterholictv.com/wp-content/uploads/2018/01/Chulgiri-Digamber-Jain-Temple-Inside-View.jpg"),

place("Statue Circle", "Famous landmark and hangout spot in Jaipur.", "https://content.jdmagicbox.com/comp/jaipur/a4/0141px141.x141.220920212147.g5a4/catalogue/statue-circle-jaipur-parks-8iyowwwJtO.jpg"),

place("Rajputana Palace", "Heritage-style property reflecting royal architecture.", "https://www.itchotels.com/content/dam/itchotels/in/umbrella/itc/hotels-listing/hotels-listing-card/itc-rajputana.jpg"),

place("Toran Dwar", "Grand gateway structure symbolizing Rajasthani culture.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQElTOUcZwmV_J8DBmmJQIPgANleZiPspDGTw&s")
                ),

                List.of(
                        place("Tapri Central", "Popular city cafe for chai, snacks, and skyline seating.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/20/2c/41/14/the-outer-area-of-tapri.jpg?w=900&h=-1&s=1"),
                        place("Town Coffee", "Minimal modern cafe for specialty coffee breaks.", "https://media-cdn.tripadvisor.com/media/photo-m/1280/2f/9d/f4/f4/ambience-so-pretty.jpg"),
                        place("Curious Life Coffee", "Easygoing spot for brunch and casual work sessions.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/1d/72/f4/9d/1.jpg?w=500&h=-1&s=1"),
                       
place("Wind View Cafe", "Rooftop cafe offering stunning views of Hawa Mahal.", "https://im.whatshot.in/img/2019/Jan/wind-view-1548413080.jpg"),

place("Cafe Lazy Mojo", "Quirky cafe known for continental dishes and chill ambience.", "https://dineout-media-assets.swiggy.com/swiggy/image/upload/fl_lossy,f_auto,q_auto/DINEOUT_ALL_RESTAURANTS/IMAGES/RESTAURANT_IMAGE_SERVICE/2024/10/19/ba4f32ff-6732-4fb3-a938-1354f862b8e1_amb0014d38199610ff64786b36e85c9609a2f88.JPG"),

place("Anokhi Cafe", "Minimalist cafe serving organic and healthy meals.", "https://lh3.googleusercontent.com/gps-cs-s/APNQkAEsorKp2WFzEEvnsjJfyyRH3bFvOWTPDUCrbOfGY9C5alF8HifoDlUZ24P2dKhGUVjpJrYmGfzvdYFYQYdFFfTixunBor7eWzLN7mmmEBqzJsYnihw1BRVsjI9t6YBc9PmlbxNm8Q=s680-w680-h510-rw"),

place("Townsend - Bar & Kitchen", "Modern cafe-bar offering global cuisine and drinks.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/1a/8c/48/06/townsend.jpg?w=900&h=500&s=1"),

place("Zolocrust", "Farm-to-table cafe with fresh bakery items and open kitchen.", "https://d3gw4aml0lneeh.cloudfront.net/assets/locations/T6pVP4ZPRBKM.jpg"),

place("Cafe Bae", "Aesthetic cafe known for desserts and Instagrammable interiors.", "https://dt4l9bx31tioh.cloudfront.net/eazymedia/restaurant/645642/restaurant420170929114202.jpg?width=750&height=436&mode=fit"),

place("The Stag Rooftop Restro Cafe", "Rooftop cafe with beautiful fort and city views.", "https://media-cdn.tripadvisor.com/media/photo-s/0c/ab/58/e5/the-stag-rooftop-restro.jpg"),

place("Cafe Quaint", "Small cozy cafe serving comfort food and desserts.", "https://dineout-media-assets.swiggy.com/swiggy/image/upload/fl_lossy,f_auto,q_auto/v1706216893/29756484b50bec7899f2aed9d66910c8.jpg"),

place("The Farm Coffee Bar", "Peaceful cafe offering organic meals and calm vibes.", "https://b.zmtcdn.com/data/pictures/5/19267395/2ff1bb076da52475f493463e32afa0a5.jpg?fit=around|960:500&crop=960:500;*,*"),

place("Bar Palladio Cafe", "Luxury garden cafe with Italian-inspired design.", "https://media-cdn.tripadvisor.com/media/photo-m/1280/1a/25/9e/09/photo0jpg.jpg"),

place("Stepout Cafe", "Casual cafe perfect for quick bites and hangouts.", "https://dineout-media-assets.swiggy.com/swiggy/image/upload/fl_lossy,f_auto,q_auto,w_600,h_468/v1660815253/z11fbyfyniqmzqa21g8g.jpg"),

place("Cafe Auberge", "Hidden gem cafe known for European-style dishes.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/29/f9/50/cb/caption.jpg?w=900&h=-1&s=1"),

place("The Magnolia Cafe", "Calm and aesthetic cafe perfect for peaceful hangouts.", "https://b.zmtcdn.com/data/pictures/1/20288821/1718cb35f04fd9f46079aba09dcf5795.jpeg?fit=around|960:500&crop=960:500;*,*"),

place("Cafe White Sage", "Minimal aesthetic cafe known for clean interiors and coffee.", "https://meraakikitchen.com/wp-content/uploads/2019/04/IMG_4133-01.jpg"),

place("The Eclectica Cafe", "Artistic cafe with colorful decor and cozy vibe.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/12/59/a9/f0/fine-dining-area-with.jpg?w=900&h=500&s=1"),

place("Skyfall By Replay", "Rooftop lounge cafe with party vibe and city view.", "https://dineout-media-assets.swiggy.com/swiggy/image/upload/fl_lossy,f_auto,q_auto,w_600,h_468/DINEOUT_ALL_RESTAURANTS/IMAGES/RESTAURANT_IMAGE_SERVICE/2025/2/5/76693c99-95ae-4771-81ce-38d9588b7834_image85fae2e858f30405ba9bf2467c75fcf0f.JPG"),


place("The Penthouse", "Luxury rooftop cafe with panoramic Jaipur views.", "https://b.zmtcdn.com/data/pictures/0/18514070/35b9f5a8250df4514707c6f390c63f1c.jpeg"),

place("Cafe Rasa", "Peaceful cafe with artistic interiors and good coffee.", "https://b.zmtcdn.com/data/pictures/7/19100227/99599dca0e6e0e48675263379ba1cec3_featured_v2.jpg"),

place("The Socialite", "Modern cafe with stylish interiors and lively vibe.", "https://b.zmtcdn.com/data/pictures/6/19077456/98e305bae20ad0605d9730a52970a97e.jpg?fit=around|960:500&crop=960:500;*,*"),

place("Cafe Baramasi", "Cute cafe with cozy seating and dessert options.", "https://b.zmtcdn.com/data/pictures/8/20450788/ebeeeba55db2e6a1c01bdbecd7323c0a.jpg?fit=around|960:500&crop=960:500;*,*"),

place("The Yellow House Cafe", "Bright aesthetic cafe with warm and cozy ambience.", "https://dineout-media-assets.swiggy.com/swiggy/image/upload/fl_lossy,f_auto,q_auto/v1675370140/b3f68780e3a60323d6e999f68904cfc6.jpg"),

place("Cafe Noir", "European-style cafe known for elegant vibes and coffee.", "https://b.zmtcdn.com/data/pictures/2/20562862/3b1a7735bf26407bb5eb24f9d9c2f4ae.jpeg?fit=around|960:500&crop=960:500;*,*"),

place("Jaipur Jungle", "Nature-themed cafe with greenery and open seating.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/13/28/01/37/jaipur-jungle-pure-veg.jpg?w=900&h=500&s=1"),

place("Cafe Basil", "Budget-friendly cafe with tasty food and chill vibe.", "https://b.zmtcdn.com/data/pictures/1/20901601/79b016022ee7ff8eeb4496dc25562e87.jpg?fit=around|750:500&crop=750:500;*,*"),

place("Cafe 202", "Simple and cozy cafe known for quick bites.", "https://b.zmtcdn.com/data/pictures/0/19703300/24377d236fb862c7a0c0e1b49a21708c.jpg?fit=around|750:500&crop=750:500;*,*"),

place("The Pink Cup Cafe", "Cute pink-themed cafe perfect for Instagram pictures.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/2a/b6/b5/eb/51-shades-of-pink.jpg?w=900&h=500&s=1"),

place("Cafe Bliss", "Relaxing cafe with calm vibe and good beverages.", "https://b.zmtcdn.com/data/pictures/8/21049338/a1ad744106caf5cd60db2c93ec27169d.jpg")  ),
              

List.of(
                     
                        place("Samode Haveli", "Boutique heritage hotel with classic Jaipur charm.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/10/4c/76/f1/pool--v17046873.jpg?w=900&h=500&s=1"),
                        place("ITC Rajputana", "Premium central stay for leisure and business travelers.", "https://www.itchotels.com/content/dam/itchotels/in/umbrella/itc/hotels-listing/hotels-listing-card/itc-rajputana.jpg"),
                        place("Rambagh Palace", "Luxury heritage hotel offering royal palace experience.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/05/ec/03/e0/facade-of-rambagh-palace.jpg?w=900&h=500&s=1"),

place("Taj Jai Mahal Palace", "Elegant hotel with Mughal gardens and grand interiors.", "https://cdn.sanity.io/images/ocl5w36p/prod5/221c1d3d595a93504a20e35cb2e6bbe9d3b4f26a-1280x1760.jpg?w=480&auto=format&dpr=2"),

place("The Oberoi Rajvilas", "Ultra-luxury resort with villas, pools, and serene ambience.", "https://etimg.etb2bimg.com/photo/97027081.cms"),

place("Fairmont Jaipur", "Grand palace-style hotel with lavish decor and views.", "https://etimg.etb2bimg.com/photo/102669659.cms"),

place("Hilton Jaipur", "Modern hotel with rooftop pool and city views.", "https://cf.bstatic.com/xdata/images/hotel/max1024x768/857076445.jpg?k=479272e3040d563784b074f6a467d484152dc4fc8898c38209e7c61a48218601&o="),

place("Radisson Blu Jaipur", "Stylish hotel close to airport with premium amenities.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/2d/ef/69/0d/exterior.jpg?w=900&h=500&s=1"),

place("Holiday Inn Jaipur City Centre", "Comfortable hotel ideal for business and leisure stays.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSZx8y-uS5f2UYF5Ky3bgAVsSxPfH6KWCZoCg&s"),

place("Alsisar Haveli", "Heritage haveli hotel with traditional architecture.", "https://media-cdn.tripadvisor.com/media/photo-s/17/ca/0a/ad/alsisar-haveli.jpg"),

place("Narain Niwas Palace", "Vintage palace hotel with old-world charm.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/2c/f3/63/25/caption.jpg?w=900&h=500&s=1"),

place("The Lalit Jaipur", "Luxury hotel near airport with grand interiors.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/08/86/8a/0f/the-lalit-jaipur.jpg?w=900&h=500&s=1"),

place("Sarovar Premiere Jaipur", "Mid-range hotel with modern facilities.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/12/fb/2e/c5/front-facade.jpg?w=900&h=500&s=1"),

place("Golden Tulip Jaipur", "Comfortable stay option near major attractions.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/07/97/0b/72/golden-tulip-jaipur.jpg?w=900&h=500&s=1"),

place("Hotel Arya Niwas", "Budget-friendly heritage hotel with peaceful courtyard.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/15/51/a5/11/hertage-100-year-old.jpg?w=700&h=-1&s=1"),

place("Umaid Bhawan Heritage House", "Beautiful heritage stay with traditional decor.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/0e/eb/8b/9c/swimming-pool.jpg?w=900&h=500&s=1"),
place("Pearl Palace Heritage", "...", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/05/ef/fa/62/pearl-palace-heritage.jpg?w=900&h=500&s=1"),

place("The Fern Residency Jaipur", "...", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/15/4c/1f/e0/bird-eye-view.jpg?w=900&h=500&s=1"),

place("Ibis Jaipur", "...", "https://cf.bstatic.com/xdata/images/hotel/max1024x768/24057483.jpg?k=02a045e4c4176541e2539395db8b2447fd27954de752518fcea8138d04486589&o="),

place("Trident Jaipur", "...", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/01/f7/e0/77/exterior.jpg?w=900&h=500&s=1"),

place("Shiv Vilas Resort", "...", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/05/cc/b9/b8/the-shiv-vilas-resort.jpg?w=900&h=500&s=1"),

place("Buena Vista Luxury Garden Spa Resort", "...", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/1a/85/2a/a7/buena-vista-resort.jpg?w=900&h=500&s=1"),

place("Le Meridien Jaipur Resort & Spa", "...", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/2e/d4/6f/f5/exterior.jpg?w=900&h=500&s=1"),

place("The Tree House Resort", "...", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/06/9d/42/b9/the-tree-house-resort.jpg?w=900&h=500&s=1"),

place("Chokhi Dhani Resort", "...", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/16/64/14/90/chokhi-dhani-resort.jpg?w=900&h=500&s=1"),

place("Hotel Clarks Amer", "...", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/10/26/c9/eb/pool--v17049117.jpg?w=900&h=500&s=1"),

place("Lords Plaza Jaipur", "...", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/07/df/02/26/lords-plaza-jaipur.jpg?w=900&h=500&s=1"),

place("Hotel Sarang Palace", "...", "https://media-cdn.tripadvisor.com/media/photo-s/14/5b/2b/5c/hotel-sarang-palace.jpg"),

place("Khandela Haveli", "...", "https://q-xx.bstatic.com/xdata/images/hotel/max500/594244903.jpg?k=1d2f8b451ca55b459680cfb9e0c2c40335d3e7f1c737a00f9f197d18070712df&o="),

place("Zone by The Park Jaipur", "Modern and vibrant hotel with colorful interiors and youthful vibe.", "https://gos3.ibcdn.com/6832a8466b8111e7827b0a4cef95d023.jpg")

),
               
               
                List.of(
                        place("Jal Mahal", "Photogenic water palace best seen at dusk.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/0a/c4/72/f6/jal-mahal-jaipur-tour.jpg?w=900&h=500&s=1"),
                        place("Nahargarh Fort", "Sunset lookout over the full cityscape.", "https://www.thepinkcityholidays.com/wp-content/uploads/2025/06/Nahargarh-Fort-Jaipur.jpg"),
                      
place("Bagru Village", "Famous for traditional hand block printing and crafts.", "https://static.wixstatic.com/media/2bebb0_fb26ac92f22147ce88c948fd00f62bcd~mv2_d_2048_1358_s_2.jpg/v1/fill/w_1000,h_663,al_c,q_85,usm_0.66_1.00_0.01,enc_avif,quality_auto/2bebb0_fb26ac92f22147ce88c948fd00f62bcd~mv2_d_2048_1358_s_2.jpg"),

place("Sanganer", "Historic town known for handmade paper and textiles.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/1a/8b/45/ee/caption.jpg?w=1200&h=-1&s=1"),

place("Chandlai Lake", "Scenic lake attracting migratory birds and nature lovers.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/07/03/af/42/chandlai-lake.jpg?w=900&h=-1&s=1"),

place("Jamwa Ramgarh", "Historic dam and peaceful countryside destination.", "https://i.ytimg.com/vi/b6Vp3lKX8HU/maxresdefault.jpg"),

place("Bhangarh Fort", "Famous haunted fort with fascinating ruins and legends.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTm4lLOce8ctIkgh9VWzMVCBiTmJtn0Ngqn3g&s"),

place("Abhaneri Stepwell (Chand Baori)", "Ancient stepwell with intricate geometric design.", "https://media-cdn.tripadvisor.com/media/attractions-splice-spp-674x446/0b/27/50/fe.jpg"),

place("Sambhar Lake", "India's largest saltwater lake with surreal landscapes.", "https://s7ap1.scene7.com/is/image/incredibleindia/sambhar-lake-jaipur-rajasthan-1-attr-hero?qlt=82&ts=1742161079901"),

place("Achrol Fort", "Heritage fort offering rural tourism experiences.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTPuRDjRkAGyPwsXcqSYlrtjswEvGQjImhqzQ&s"),

place("Samode Palace", "Luxurious heritage palace with stunning architecture.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/2b/81/58/7b/caption.jpg?w=900&h=500&s=1"),

place("Nahargarh Biological Park", "Wildlife park home to lions, tigers, and deer.", "https://jaipurthrumylens.com/wp-content/uploads/2018/02/nahargarh-biological-zoological-park-kukas-jaipur-image.jpg"),

place("Elephant Village Jaipur", "Unique experience interacting with elephants ethically.", "https://obms-tourist.rajasthan.gov.in/uploads/images_1_7e465b15f3.jpeg"),

place("Jawahar Kala Kendra Theatre", "Cultural hub for performances and art exhibitions.", "https://jaipurtourism.co.in/images/places-to-visit/header/jawahar-kala-kendra-jaipur-tourism-entry-fee-timings-holidays-reviews-header.jpg"),

place("Pink City Streets", "Vibrant old city lanes showcasing Jaipur's heritage.", "https://www.shadowsgalore.com/wp-content/uploads/2013/03/Jaipur-Pink-City1.jpg"),

place("Maharani Ki Chhatri", "Royal cenotaphs with intricate carvings.", "https://s7ap1.scene7.com/is/image/incredibleindia/moosi-maharani-ki-chhatri-alwar-rajasthan-1-attr-hero?qlt=82&ts=1726658776904"),

place("Central Museum Jaipur", "Museum displaying artifacts and historical items.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQQ-QnpGEpa0oN7nS9YO0rY2u1YGeXcGwB4Vw&s"),

place("Vidyadhar Ka Bagh Temple Area", "Historic garden area with scenic architecture and greenery.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTeAjRCb8QKmAbtVoqc-sxXsxhg5ocdeR026Q&s"),

place("Charan Mandir", "Hilltop temple offering panoramic views of Jaipur.", "https://indiainlens.com/wp-content/uploads/2025/12/Charan-Mandir-Arial-View-1024x536.jpg"),

place("Surya Mandir Jaipur", "Sun temple located on hilltop with sunrise views.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/09/fe/50/48/surya-mandir.jpg?w=700&h=-1&s=1"),

place("Nahargarh Sunset Point", "Popular viewpoint for breathtaking sunsets.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSnm_flCUAFJSl_clPL_ypp1xxNn26tKl6Htg&s"),

place("Gatore Ki Chhatriyan Garden Area", "Peaceful heritage site with royal cenotaphs.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/2b/8c/d4/2a/caption.jpg?w=900&h=500&s=1"),

place("Zorawar Singh Gate", "Ancient gateway with historic significance.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTmDfquFLk-9S0raxRsw-9encYcqLRXAd3cXw&s"),

place("Choti Chaupar", "Historic square and vibrant marketplace area.", "https://hindi.oneindia.com/img/2020/02/jaipurchhotichauparlookchanged-1580896988.jpg"),

place("Badi Chaupar", "Central square in Jaipur known for local shopping.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTeoxw2TCkcHHc7zXMCRHw6cMunPqd4w7RJAw&s"),

place("Masala Chowk", "Open-air food court with local street food variety.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/14/86/12/93/dsc-0147-largejpg.jpg?w=900&h=-1&s=1"),

place("Patrica Gate", "a symbol of rajasthan's rich art and culture.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTjxNZiL5L0Yn-ik9MXFtUJ2DhmvlLj7t75Wg&s"),

place("Rajasthan International Centre", "Modern cultural and exhibition venue.", "https://www.architectandinteriorsindia.com/wp-content/uploads/cloud/2024/03/18/WhatsApp-Image-2024-01-25-at-4.55.12-PM-1-1024x576.png"),

place("Pink Square Mall", "Shopping and entertainment destination in Jaipur.", "https://img.staticmb.com/mbcontent/images/crop/uploads/2024/10/Pink-Square-Mall-is-decorated-during-festive-seasons-with-lights_0_1200.jpg.webp"),

place("Toran Dwar", "The Gateway of Jaipur.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQElTOUcZwmV_J8DBmmJQIPgANleZiPspDGTw&s")
                        
                ),

                  List.of(
                        place("University of Rajasthan", "One of the oldest universities offering diverse undergraduate and postgraduate programs.", "https://campuspro.co.in/collage-image/1749038377_row_11.jpg"),

place("Malaviya National Institute of Technology (MNIT)", "Premier engineering institute and National Institute of Technology.", "https://www.guidanceforever.org/wp-content/uploads/2023/10/malaviya-national-institute-of-technology-jaipur-featured.jpg"),

place("JECRC University", "Private university known for engineering, management, and research programs.", "https://jecrcuniversity.edu.in/wp-content/uploads/2023/06/Drone-Image-JU.png"),

place("Manipal University Jaipur", "Modern university offering global-level education and campus facilities.", "https://images.indianexpress.com/2022/03/manipal-lead-1.jpg"),

place("Amity University Jaipur", "Well-known private university with diverse courses and infrastructure.", "https://collegeassist.in/_next/image?url=https%3A%2F%2Fnurturflows.s3.ap-south-1.amazonaws.com%2Fbanner%2F15094564471443174784AUJNEW.jpg&w=828&q=75"),

place("Jaipur National University", "Multi-disciplinary university with medical, engineering, and management courses.", "https://images.shiksha.com/mediadata/images/1583911645php0u4sxi.png"),

place("Poornima University", "Popular university focused on engineering, architecture, and design.", "https://images.shiksha.com/mediadata/images/1745496915phpyf90RL.jpeg"),

place("Banasthali Vidyapith", "Renowned women's university offering holistic education.", "https://dcx0p3on5z8dw.cloudfront.net/Aakash/s3fs-public/inline-images/Banasthali%20Vidyapith1.png?jP.bwBDvxJxAvkIoPosYQ4._MC.9dVzM"),

place("IIHMR University", "Top institute for healthcare management and research.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR0-uLQ3vEUkVZW9BSkQDvxi7b1pxL_w4_4wQ&s"),

place("IIS University (International College for Girls)", "Well-known institution for girls with various academic programs.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQn-18aCrLU1Hik29LiKwpI1NkE2rM0i9kxxA&s"),

place("St. Xavier's College Jaipur", "Prestigious college offering arts, science, and commerce education.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT1sDEk-W5-oXE4MXcgFxjXDvUVfDP1TtAtQA&s"),

place("Maharani College Jaipur", "Historic women's college with strong academic reputation.", "https://images.shiksha.com/mediadata/images/articles/1657014768phpDXjle4.jpeg"),

place("Maharaja College Jaipur", "Well-known government college for higher education.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRWjtZ1faTWlbAri9t4Mts5dWBfawlLkCqAuw&s"),
place("Albert Hall Museum", "Oldest museum in Rajasthan showcasing art, artifacts, and historical exhibits.", "https://upload.wikimedia.org/wikipedia/commons/1/18/Albert_Hall_%28_Jaipur_%29.jpg"),

place("Jawahar Kala Kendra", "Cultural and arts center hosting exhibitions, theatre, and workshops.", "https://upload.wikimedia.org/wikipedia/commons/4/41/2022_July_-_JawaharKalaKendra_Jaipur_13.jpg"),

place("City Palace Museum", "Museum inside City Palace displaying royal costumes, weapons, and artifacts.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/13/96/8a/82/this-is-the-time-when.jpg?w=900&h=500&s=1"),

place("Anokhi Museum of Hand Printing", "Unique museum dedicated to traditional block printing techniques.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT6ITvGo8hfTH0E9B_k0t_l_S6KaS65p02-DA&s"),

place("Amrapali Museum", "Museum showcasing ancient Indian jewelry and craftsmanship.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/16/c2/eb/df/museum-building.jpg?w=1200&h=-1&s=1"),

place("Jaigarh Fort Museum", "Displays ancient weapons, cannons, and military history.", "https://upload.wikimedia.org/wikipedia/commons/3/34/Rajasthan-Jaipur-Jaigarh-Fort-compound-Apr-2004-00.JPG"),

place("Nahargarh Fort Wax Museum", "Wax statues of famous personalities with educational storytelling.", "https://upload.wikimedia.org/wikipedia/commons/3/34/Rajasthan-Jaipur-Jaigarh-Fort-compound-Apr-2004-00.JPG"),

place("Science Park Jaipur", "Interactive science learning space for students and visitors.", "https://www.pinkcitypost.com/wp-content/uploads/2019/03/science-park-jaipur573485.jpg"),

place("Birla Planetarium Jaipur", "Educational planetarium offering astronomy shows and space learning.", "https://yometro.com/images/places/bm-birla-planetarium.jpg"),

place("Central Park Jaipur (Open Learning Space)", "Public space often used for reading, yoga, and informal learning.", "https://jaipurtourism.co.in/images/places-to-visit/header/central-park-jaipur-tourism-entry-fee-timings-holidays-reviews-header.jpg"),

place("Rajasthan International Centre (RIC)", "Modern space for lectures, exhibitions, and intellectual events.", "https://www.architectandinteriorsindia.com/wp-content/uploads/cloud/2024/03/18/WhatsApp-Image-2024-01-25-at-4.55.12-PM-1-1024x576.png"),

place("Art Chill Gallery Amber", "Art gallery showcasing contemporary and traditional artworks.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/11/49/9c/dd/gallery-artchill-amber.jpg?w=1200&h=-1&s=1"),

place("Sawai Man Singh Museum Library", "Historic library with rare books, manuscripts, and research material.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/10/22/ba/1b/palads-garden.jpg?w=1200&h=-1&s=1"),

place("Birla Science & Technology Centre", "Interactive science museum with exhibits on physics, space, and technology.", "https://media1.thrillophilia.com/filestore/m9vmtwrkkptufd7kkm30an5z4kgu_j5ip4aeucy04vswc86j51gtmo70r_shutterstock_2231719273.webp?w=340&q=70&dpr=2"),

place("Pink City Art Gallery", "Local art gallery showcasing modern paintings and creative exhibitions.", "https://assets.architecturaldigest.in/photos/67765bbea2a6a04da42d5d73/master/w_1600%2Cc_limit/%25C2%25A9%25EF%25B8%258F%2520Dayanita%2520Singh.%2520%25C2%25A9%25EF%25B8%258F%2520Tanya%2520Goel.%2520%25C2%25A9%25EF%25B8%258F%2520L.N.%2520Tallur.%2520Courtesy%2520Nature%2520Morte.%2520Photo%2520Credits_%2520Gourab%2520Ganguli.jpg"),

place("Jaipur Wax Museum Learning Zone", "Interactive exhibits combining entertainment with educational insights.", "https://media-cdn.tripadvisor.com/media/attractions-splice-spp-674x446/06/fa/96/94.jpg"),

place("State Archives Jaipur", "Repository of historical documents and records for academic research.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcS8ahYxFmh0DyxKXp4XuLh4ujwhfYfwAYwZ4g&s")
       ),
                List.of("Royal architecture and fort circuits", "Strong mix of heritage hotels and lively cafes", "Excellent for shopping, culture, and day-long city tours")));

        data.put("agra", curatedCity(
                "agra", "Agra", "Mughal heritage, iconic monuments, marble craft, and classic North Indian food.",
                "https://s7ap1.scene7.com/is/image/incredibleindia/taj-mahal-agra-uttar-pradesh-city-1-hero?qlt=82&ts=1742179413209", "Uttar Pradesh",
                List.of(new QuickFact("Taj Mahal", "World Wonder"), new QuickFact("Mughal", "History"), new QuickFact("Marble", "Craft")),
               // =======================
// AGRA - POPULAR PLACES
// =======================
List.of(
    place("Taj Mahal", "World-famous marble monument and symbol of love.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTJr2zJ9Pbqxq1FcEx0VkWFKFCTx7aC2jUFwg&s"),
    place("Agra Fort", "Historic Mughal fort packed with royal architecture.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTyiXS1-5k60WSDpn94FgaaODZ0T3j8aK7pEg&s"),
    place("Mehtab Bagh", "Garden offering beautiful Taj Mahal sunset views.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQj7cYRh_4OD-9819lE6qZmqCATVVvt3HsgHw&s"),
    place("Fatehpur Sikri", "UNESCO-listed Mughal city near Agra.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/2e/7b/57/01/caption.jpg?w=500&h=500&s=1"),
    place("Itimad-ud-Daulah", "Elegant marble tomb known as Baby Taj.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTvC2VA5JKpBgSpbXBkZJrh2_6DcSKu_N63RA&s"),
    place("Kinari Bazaar", "Busy local shopping market with Agra street energy.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTvC2VA5JKpBgSpbXBkZJrh2_6DcSKu_N63RA&s"),
    place("Jama Masjid Agra", "Historic Mughal mosque in old Agra.", "https://www.cdn.travejar.com/storage/india_attraction_tour/1680076371.webp"),
    place("Mankameshwar Temple", "Popular spiritual temple near Agra Fort.", "https://www.indiaeasytrip.com/states-of-india/mankameshwar-temple.jpg"),
    place("Akbar's Tomb", "Beautiful Mughal-era tomb in Sikandra.", "https://static.wixstatic.com/media/055605_abaf1984ee884bb6939e54eab2f098bd~mv2.png/v1/fill/w_1920,h_1080,al_c/f1a22cb0-cac0-42d3-aa29-fc8f39c2441817.png"),
    place("Chini Ka Rauza", "Persian-style Mughal monument with tile work.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTUA57AeDKn2LHbcYZwnC424gb8g81qRBRbSg&s"),
    place("Guru Ka Taal", "Historic Sikh pilgrimage site in Agra.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTy50EYJ8-N7MK-qR7HFlEbN4ixrDh_PR9Enw&s"),
    place("Soami Bagh", "Peaceful marble temple under construction.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTwsjA5l2jMdbv_Pby-CzDFnt3aWS3uCM0wTA&s"),
    place("Keetham Lake", "Nature-focused picnic and birdwatching spot.", "https://agratourism.in/images/v2/places-to-visit/keetham-lake-agra-tourism-header.jpg"),
    place("Dolphin Water Park", "Family-friendly water and amusement park.", "https://tripxl.com/blog/wp-content/uploads/2024/11/Dolphin-Water-Park-Agra-Cover.jpg"),
    place("Anguri Bagh", "Garden area inside Agra Fort complex.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/0b/2d/d6/9a/anguri-bagh.jpg?w=1200&h=-1&s=1"),
    place("Taj Museum", "Museum displaying Mughal artifacts and history.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTy574uTht2yb4zqoti_MaBXmose0z_ExjJRA&s"),
    place("Ram Bagh", "Historic Mughal garden beside Yamuna River.", "https://s7ap1.scene7.com/is/image/incredibleindia/ram-bagh-agra-uttar-pradesh-2-attr-hero?qlt=82&ts=1726650361671"),
    place("Wildlife SOS", "Elephant conservation and rescue center.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/07/15/bc/f0/wildlife-sos.jpg?w=800&h=500&s=1"),
    place("Sur Sarovar Bird Sanctuary", "Nature destination filled with migratory birds.", "https://s7ap1.scene7.com/is/image/incredibleindia/sur-sarovar-bird-sanctuary-agra-2-attr-hero?qlt=82&ts=1726649967220"),
    place("Bateshwar Temples", "Historic temple complex near Agra.", "https://media.assettype.com/outlooktraveller/2025-08-12/uje2377v/kkmuhammed120250812.jpg?w=1200&ar=40%3A21&auto=format%2Ccompress&ogImage=true&mode=crop&enlarge=true&overlay=false&overlay_position=bottom&overlay_width=100"),
    place("Gyarah Sidi", "Hidden Mughal-era astronomical structure.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/16/a9/fb/43/gyarah-sidi.jpg?w=700&h=400&s=1"),
    place("Kalakriti Cultural Show", "Evening cultural performance inspired by Mughal history.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/2a/c1/99/f2/the-hall-600-seating.jpg?w=1200&h=-1&s=1")
),

// =======================
// AGRA - CAFES
// =======================
List.of(
    place("Cafe Sheroes Hangout", "Social-impact cafe run by acid-attack survivors.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/25/bd/34/57/agra-cafe-related-place.jpg?w=900&h=500&s=1"),
    place("Tea'se Me", "Rooftop cafe with Taj Mahal views.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/1a/d0/e6/5d/section-3.jpg?w=500&h=-1&s=1"),
    place("Unplugged Courtyard", "Popular rooftop dining and cafe spot.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/1c/83/8b/4c/unplugged-courtyard.jpg?w=500&h=-1&s=1"),
    place("Mocha Cafe", "Modern cafe with desserts and shakes.", "https://media-cdn.tripadvisor.com/media/photo-s/13/78/3a/d8/photo6jpg.jpg"),
    place("Cafe Turquoise Cottage", "Relaxed rooftop cafe near Taj area.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/15/b9/9b/de/amazing-ambience.jpg?w=500&h=-1&s=1"),
    place("The Salt Cafe", "Rooftop Taj-view cafe with evening vibes.", "https://cdn0.weddingwire.in/vendor/5693/3_2/960/jpeg/banquet-halls-the-salt-cafe-agra-event-space-10_15_305693-166859184164057.jpeg"),
    place("Bon Barbecue", "Casual food and cafe destination for groups.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/09/3d/c4/80/bon-barbecue-restaurant.jpg?w=900&h=-1&s=1"),
    place("The Palm Burj", "Stylish rooftop restaurant and cafe.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/1c/5b/10/b6/img-20201121-193742-largejpg.jpg?w=900&h=500&s=1"),
    place("Costa Coffee Agra", "International-style coffee chain stop.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/11/e6/03/94/img-20180128-161006-largejpg.jpg?w=900&h=500&s=1"),
    place("Cafe Mango Tree", "Popular rooftop cafe near Taj Mahal.", "https://content3.jdmagicbox.com/comp/rewa/f7/9999p7662.7662.221007161847.w1f7/catalogue/mango-tree-party-lawn-and-cafe-rewa-apsu-rewa-banquet-halls-outside-catering--gvsqazpk5w.jpg"),
    place("Molecule Agra", "Modern cafe with music and nightlife vibe.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQLM8F2_aH3QPo_9LchLcauzJG2qnLwEULtRg&s"),
    place("The Chocolate Room", "Dessert-focused cafe with cozy seating.", "https://b.zmtcdn.com/data/pictures/5/21141675/df61cc3bcceb9b4e7ac98530e3accd94.jpg"),
    place("Urban Deck", "Open rooftop cafe for evening hangouts.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/21/d1/70/49/nice-evening-view.jpg?w=1200&h=1200&s=1"),
    place("Cafe Baker's Street", "Bakery cafe with snacks and pastries.", "https://b.zmtcdn.com/data/pictures/0/21300690/061aa703c335053dcda5ef5e40a0410c.jpg"),
    place("Sky Grill", "Rooftop dining with panoramic city views.", "https://dineout-media-assets.swiggy.com/swiggy/image/upload/fl_lossy,f_auto,q_auto,w_600,h_468/v1698315233/ec7a9a0d05e50bf21e112b993529d27a.jpg"),
    place("Chapter 1 Cafe", "Modern cafe with peaceful interiors.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/15/c9/6d/fa/img-20181214-173330-341.jpg?w=500&h=-1&s=1"),
    place("Cafe TC", "Relaxed cafe for coffee and snacks.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/15/b9/9b/de/amazing-ambience.jpg?w=500&h=-1&s=1"),
    place("Downtown Cafe", "Casual cafe popular among local students.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQGuehqK5jnDPOChQq4XK4_e04H4XsbXIBpNA&s"),
    place("Open Terrace Cafe", "Evening cafe with rooftop atmosphere.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/1a/d0/e6/5d/section-3.jpg?w=900&h=500&s=1"),
    place("Infini Cafe", "Modern aesthetic cafe with dessert menu.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/1a/cf/5d/3e/caption.jpg?w=400&h=300&s=1")
   
),
               List.of(
    place("The Oberoi Amarvilas", "Luxury Taj-facing hotel with world-class hospitality.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
    place("ITC Mughal", "Mughal-inspired luxury resort with grand interiors.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
    place("Taj Hotel & Convention Centre", "Modern upscale stay near Taj Mahal.", "https://images.unsplash.com/photo-1455587734955-081b22074882?w=900&q=80"),
    place("Radisson Hotel Agra", "Premium hotel with rooftop Taj views.", "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?w=900&q=80"),
    place("DoubleTree by Hilton", "Elegant business and leisure hotel.", "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=900&q=80"),
    place("Courtyard by Marriott", "Modern luxury accommodation with city comfort.", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=900&q=80"),
    place("Holiday Inn Agra", "Comfortable upscale hotel for travelers.", "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=900&q=80"),
    place("Crystal Sarovar Premiere", "Stylish Taj-area hotel with rooftop dining.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
    place("Hotel Clarks Shiraz", "Classic Agra hotel known for heritage charm.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
    place("Jaypee Palace Hotel", "Grand resort-style luxury property.", "https://images.unsplash.com/photo-1455587734955-081b22074882?w=900&q=80"),
    place("Trident Agra", "Elegant hotel with peaceful gardens and pools.", "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?w=900&q=80"),
    place("Howard Plaza", "Popular Taj-facing stay with rooftop atmosphere.", "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=900&q=80"),
    place("Hotel Taj Resorts", "Comfortable stay within walking distance of Taj Mahal.", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=900&q=80"),
    place("Grand Mercure Agra", "Modern premium hotel with rooftop pool.", "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=900&q=80"),
    place("Hotel Atulyaa Taj", "Budget-friendly Taj-view accommodation.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
    place("Hotel Alleviate", "Modern hotel with rooftop dining.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
    place("Hotel Taj Vilas", "Comfortable tourist-focused hotel near Taj Mahal.", "https://images.unsplash.com/photo-1455587734955-081b22074882?w=900&q=80"),
    place("Hotel Parador", "Affordable stay with relaxing poolside atmosphere.", "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?w=900&q=80"),
    place("Hotel Seven Hills", "Modern city hotel with premium facilities.", "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=900&q=80"),
    place("Hotel Light House", "Budget-friendly Taj-area accommodation.", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=900&q=80"),
    place("The Retreat", "Comfortable family-oriented hotel.", "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=900&q=80"),
    place("Hotel Royale Residency", "Mid-range hotel near major tourist attractions.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
    place("Hotel Kamal", "Budget traveler hotel with Taj proximity.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
    place("Hotel Sidhartha", "Classic tourist stay near eastern gate.", "https://images.unsplash.com/photo-1455587734955-081b22074882?w=900&q=80"),
    place("Hotel The Grand Imperial", "Colonial-style heritage hotel experience.", "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?w=900&q=80"),
    place("Hotel Amar", "Reliable and comfortable Agra accommodation.", "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=900&q=80"),
    place("Hotel East Gate", "Affordable Taj Mahal stay option.", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=900&q=80"),
    place("Hotel Orbit Inn", "Simple modern stay with easy city access.", "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=900&q=80"),
    place("Hotel Maple Grand", "Contemporary hotel with polished interiors.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
    place("Hotel Grace Agra", "Tourist-friendly hotel with rooftop restaurant.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80")
),
                // =======================
// AGRA - TOURIST PLACES
// =======================
List.of(
    place("Taj Mahal", "World-famous marble monument and symbol of love.", "https://images.unsplash.com/photo-1564507592333-c60657eea523?w=900&q=80"),
    place("Agra Fort", "Historic Mughal fort packed with royal architecture.", "https://images.unsplash.com/photo-1585135497273-1a86b09fe70e?w=900&q=80"),
    place("Fatehpur Sikri", "UNESCO-listed Mughal city near Agra.", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=900&q=80"),
    place("Itimad-ud-Daulah", "Elegant marble tomb known as Baby Taj.", "https://images.unsplash.com/photo-1576485290814-1c72aa4bbb8e?w=900&q=80"),
    place("Mehtab Bagh", "Garden offering beautiful Taj Mahal sunset views.", "https://images.unsplash.com/photo-1465146344425-f00d5f5c8f07?w=900&q=80"),
    place("Akbar's Tomb", "Historic Mughal emperor burial site in Sikandra.", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=900&q=80"),
    place("Jama Masjid Agra", "Historic Mughal mosque in old Agra.", "https://images.unsplash.com/photo-1584285405426-3b4c6bcd5c5d?w=900&q=80"),
    place("Chini Ka Rauza", "Persian-style Mughal monument with tile work.", "https://images.unsplash.com/photo-1576485290814-1c72aa4bbb8e?w=900&q=80"),
    place("Guru Ka Taal", "Historic Sikh pilgrimage site in Agra.", "https://images.unsplash.com/photo-1524499982521-1ffd58dd89ea?w=900&q=80"),
    place("Soami Bagh", "Peaceful marble temple under construction.", "https://images.unsplash.com/photo-1609947017136-9daf32a5eb16?w=900&q=80"),
    place("Taj Nature Walk", "Green walking area with hidden Taj viewpoints.", "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=900&q=80"),
    place("Keetham Lake", "Nature-focused picnic and birdwatching spot.", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=900&q=80"),
    place("Sur Sarovar Bird Sanctuary", "Nature destination filled with migratory birds.", "https://images.unsplash.com/photo-1444464666168-49d633b86797?w=900&q=80"),
    place("Ram Bagh", "Historic Mughal garden beside Yamuna River.", "https://images.unsplash.com/photo-1465146344425-f00d5f5c8f07?w=900&q=80"),
    place("Mughal Heritage Walk", "Walking trail exploring old Agra culture.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80"),
    place("Anguri Bagh", "Beautiful garden area inside Agra Fort.", "https://images.unsplash.com/photo-1465146344425-f00d5f5c8f07?w=900&q=80"),
    place("Shilpgram", "Craft village showcasing regional culture and art.", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=900&q=80"),
    place("Taj Museum", "Museum displaying Mughal artifacts and history.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
    place("Wildlife SOS", "Elephant conservation and rescue center.", "https://images.unsplash.com/photo-1508672019048-805c876b67e2?w=900&q=80"),
    place("Bateshwar Temples", "Historic temple complex near Agra.", "https://images.unsplash.com/photo-1609947017136-9daf32a5eb16?w=900&q=80"),
    place("Gyarah Sidi", "Hidden Mughal-era astronomical structure.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80"),
    place("Yamuna Riverfront", "Relaxed riverside views near the Taj Mahal.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80"),
    place("Kalakriti Cultural Show", "Evening cultural performance inspired by Mughal history.", "https://images.unsplash.com/photo-1516280440614-37939bbacd81?w=900&q=80"),
    place("Dolphin Water Park", "Family-friendly amusement and water park.", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=900&q=80"),
    place("Paliwal Park", "Relaxed green park for evening outings.", "https://images.unsplash.com/photo-1493246507139-91e8fad9978e?w=900&q=80"),
    place("Mankameshwar Temple", "Popular spiritual temple near Agra Fort.", "https://images.unsplash.com/photo-1609947017136-9daf32a5eb16?w=900&q=80"),
    place("Subhash Bazaar", "Traditional shopping market with local crafts.", "https://images.unsplash.com/photo-1521334884684-d80222895322?w=900&q=80"),
    place("Kinari Bazaar", "Busy local shopping market with Agra street energy.", "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=900&q=80"),
    place("Sadar Bazaar", "Popular evening market for food and shopping.", "https://images.unsplash.com/photo-1517244683847-7456b63c5969?w=900&q=80"),
    place("TDI Mall", "Modern shopping and entertainment destination.", "https://images.unsplash.com/photo-1519567241046-7f570eee3ce6?w=900&q=80")
),

List.of(
    place("Agra College", "Historic educational institution established in the colonial era.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
    place("St. John's College", "Well-known academic campus in Agra.", "https://images.unsplash.com/photo-1523240795612-9a054b0db644?w=900&q=80"),
    place("Dayalbagh Educational Institute", "Recognized university and research institution.", "https://images.unsplash.com/photo-1498243691581-b145c3f54a5a?w=900&q=80"),
    place("Dr. B.R. Ambedkar University", "Major higher-education institution in Agra.", "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=900&q=80"),
    place("Central Hindi Institute", "Important language and literature learning center.", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=900&q=80"),
    place("Taj Museum", "Museum showcasing Mughal artifacts and architecture.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
    place("Kalakriti Cultural Center", "Museum-style storytelling and cultural performances.", "https://images.unsplash.com/photo-1516280440614-37939bbacd81?w=900&q=80"),
    place("Wildlife SOS", "Educational conservation center for rescued elephants.", "https://images.unsplash.com/photo-1508672019048-805c876b67e2?w=900&q=80"),
    place("Sur Sarovar Bird Sanctuary", "Eco-learning and birdwatching destination.", "https://images.unsplash.com/photo-1444464666168-49d633b86797?w=900&q=80"),
    place("Mughal Heritage Walk", "Historic learning trail through old Agra.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80"),
    place("Shilpgram", "Cultural village promoting regional crafts and traditions.", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=900&q=80"),
    place("Army Public School Agra", "Recognized educational institution in Agra.", "https://images.unsplash.com/photo-1509062522246-3755977927d7?w=900&q=80"),
    place("Delhi Public School Agra", "Popular CBSE educational campus.", "https://images.unsplash.com/photo-1509062522246-3755977927d7?w=900&q=80"),
    place("Allen Agra", "Competitive exam coaching institute.", "https://images.unsplash.com/photo-1523240795612-9a054b0db644?w=900&q=80"),
    place("Aakash Institute Agra", "Medical and engineering coaching center.", "https://images.unsplash.com/photo-1498243691581-b145c3f54a5a?w=900&q=80"),
    place("FIITJEE Agra", "Engineering entrance preparation institute.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
    place("Kendriya Vidyalaya Agra", "Government-run educational institution.", "https://images.unsplash.com/photo-1509062522246-3755977927d7?w=900&q=80"),
    place("St. Clare's Senior Secondary School", "Well-known school with academic focus.", "https://images.unsplash.com/photo-1509062522246-3755977927d7?w=900&q=80"),
    place("Prelude Public School", "Recognized private school in Agra.", "https://images.unsplash.com/photo-1509062522246-3755977927d7?w=900&q=80"),
    place("The International School Agra", "Modern educational campus.", "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=900&q=80"),
    place("Agra Public School", "Popular local academic institution.", "https://images.unsplash.com/photo-1523240795612-9a054b0db644?w=900&q=80"),
    place("Holy Public School", "Educational institution with strong student culture.", "https://images.unsplash.com/photo-1498243691581-b145c3f54a5a?w=900&q=80"),
    place("Raja Balwant Singh College", "Historic higher-education institution.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
    place("Institute of Mental Health and Hospital", "Medical education and research institution.", "https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?w=900&q=80"),
    place("Government Museum Agra", "Museum preserving regional history and culture.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
    place("Agra University Library", "Academic reading and research space.", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=900&q=80"),
    place("Hindi Sahitya Sammelan", "Language and literature learning center.", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=900&q=80"),
    place("Agra Cantonment Library", "Public reading and educational facility.", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=900&q=80"),
    place("Engineering Institute Agra", "Technical education and innovation campus.", "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=900&q=80"),
    place("Science Learning Center Agra", "Interactive science-focused educational destination.", "https://images.unsplash.com/photo-1507413245164-6160d8298b31?w=900&q=80")
),
                List.of("World-class monument tourism", "Strong luxury hotel presence", "Perfect for heritage-focused itineraries")));

        data.put("varanasi", curatedCity(
                "varanasi", "Varanasi", "Sacred ghats, spiritual rituals, old lanes, temple trails, and riverfront stays.",
                "https://theorionhotels.com/_next/image?url=https%3A%2F%2Fassets.theasar.com%2Fblogs%2F1768561498185_top_10_places_to_visit_in_varanasi.webp&w=3840&q=75", "Uttar Pradesh",
                List.of(new QuickFact("Ghats", "Riverfront"), new QuickFact("Spiritual", "Pilgrimage"), new QuickFact("Ancient", "City")),
               // =======================
// VARANASI - POPULAR PLACES
// =======================
List.of(
    place("Kashi Vishwanath Temple", "One of India's most sacred Shiva temples.", "https://images.unsplash.com/photo-1609947017136-9daf32a5eb16?w=900&q=80"),
    place("Dashashwamedh Ghat", "Most famous ghat for evening Ganga Aarti.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80"),
    place("Assi Ghat", "Student-friendly riverside hangout with calm sunrise views.", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=900&q=80"),
    place("Manikarnika Ghat", "Historic spiritual cremation ghat of Varanasi.", "https://images.unsplash.com/photo-1516280440614-37939bbacd81?w=900&q=80"),
    place("Sarnath", "Buddhist heritage destination where Buddha gave his first sermon.", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=900&q=80"),
    place("Ramnagar Fort", "Historic riverside fort with royal museum.", "https://images.unsplash.com/photo-1585135497273-1a86b09fe70e?w=900&q=80"),
    place("Banaras Hindu University", "Massive educational campus with cultural significance.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
    place("Tulsi Manas Temple", "Peaceful marble temple linked to Ramcharitmanas.", "https://images.unsplash.com/photo-1609947017136-9daf32a5eb16?w=900&q=80"),
    place("Durga Kund Temple", "Historic red-colored temple near Assi area.", "https://images.unsplash.com/photo-1524499982521-1ffd58dd89ea?w=900&q=80"),
    place("New Vishwanath Temple", "BHU temple known for peaceful architecture.", "https://images.unsplash.com/photo-1609947017136-9daf32a5eb16?w=900&q=80"),
    place("Ganga River Cruise", "Evening boat ride with glowing ghat views.", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=900&q=80"),
    place("Godowlia Market", "Busy market area full of Banarasi street life.", "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=900&q=80"),
    place("Kedar Ghat", "Peaceful riverside ghat with spiritual atmosphere.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80"),
    place("Scindia Ghat", "Historic tilted Shiva temple beside the river.", "https://images.unsplash.com/photo-1516280440614-37939bbacd81?w=900&q=80"),
    place("Bharat Kala Bhavan", "Museum preserving Banaras art and history.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
    place("Chunar Fort", "Ancient riverside fort near Varanasi.", "https://images.unsplash.com/photo-1585135497273-1a86b09fe70e?w=900&q=80"),
    place("Rajdari Waterfalls", "Nature getaway near Varanasi.", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=900&q=80"),
    place("Vindham Waterfalls", "Scenic waterfall destination for short trips.", "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=900&q=80"),
    place("Nepali Temple", "Wooden temple inspired by Kathmandu architecture.", "https://images.unsplash.com/photo-1609947017136-9daf32a5eb16?w=900&q=80"),
    place("Alamgir Mosque", "Historic Mughal-era riverside mosque.", "https://images.unsplash.com/photo-1584285405426-3b4c6bcd5c5d?w=900&q=80"),
    place("Panchganga Ghat", "Historic meeting point of sacred rivers.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80"),
    place("Banaras Ghats Walk", "Classic heritage walk through old Varanasi lanes.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80"),
    place("Ravidas Ghat", "Modern riverside ghat with wide open views.", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=900&q=80"),
    place("Chet Singh Fort", "Historic fort overlooking the Ganga.", "https://images.unsplash.com/photo-1585135497273-1a86b09fe70e?w=900&q=80"),
    place("Batuk Bhairav Temple", "Ancient spiritual temple with local importance.", "https://images.unsplash.com/photo-1524499982521-1ffd58dd89ea?w=900&q=80"),
    place("Banaras Silk Market", "Traditional shopping area for Banarasi sarees.", "https://images.unsplash.com/photo-1521334884684-d80222895322?w=900&q=80"),
    place("Lal Bahadur Shastri Ghat", "Peaceful riverside location for evening walks.", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=900&q=80"),
    place("Ahilyabai Ghat", "Historic ghat linked with Maratha architecture.", "https://images.unsplash.com/photo-1516280440614-37939bbacd81?w=900&q=80"),
    place("Beniya Park", "Green local park for casual relaxation.", "https://images.unsplash.com/photo-1493246507139-91e8fad9978e?w=900&q=80"),
    place("Namo Ghat", "Modern redeveloped ghat with evening lights and cafes.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80")
),

// =======================
// VARANASI - CAFES
// =======================
List.of(
    place("Pizzeria Vaatika Cafe", "Popular riverside cafe with Assi Ghat views.", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"),
    place("Brown Bread Bakery", "Backpacker-favorite bakery and breakfast spot.", "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=900&q=80"),
    place("Aum Cafe", "Peaceful rooftop cafe near Assi Ghat.", "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=900&q=80"),
    place("Terracotta Cafe", "Artistic cafe with calm Banaras atmosphere.", "https://images.unsplash.com/photo-1453614512568-c4024d13c247?w=900&q=80"),
    place("Mona Lisa Cafe", "Traveler-friendly rooftop cafe near ghats.", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=900&q=80"),
    place("Open Hand Cafe", "Minimal cafe known for coffee and desserts.", "https://images.unsplash.com/photo-1559925393-8be08e16738e?w=900&q=80"),
    place("Kashi Chat Bhandar", "Famous local snack and chai stop.", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=900&q=80"),
    place("The 3rd Floor Bar Stock Exchange", "Modern rooftop cafe and lounge.", "https://images.unsplash.com/photo-1528605248644-14dd04022da1?w=900&q=80"),
    place("Cafe De Coop", "Youth-focused cafe with artistic vibe.", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"),
    place("I:BA Cafe", "Modern Banaras cafe with calm interiors.", "https://images.unsplash.com/photo-1453614512568-c4024d13c247?w=900&q=80"),
    place("TeaQuila Cafe", "Casual cafe for snacks and conversations.", "https://images.unsplash.com/photo-1511920170033-f8396924c348?w=900&q=80"),
    place("Mangi Ferra Cafe", "Creative rooftop cafe near ghats.", "https://images.unsplash.com/photo-1466978913421-dad2ebd01d17?w=900&q=80"),
    place("Flavours Cafe", "Student-friendly cafe near BHU.", "https://images.unsplash.com/photo-1528605105345-5344ea20e269?w=900&q=80"),
    place("Cafe Ability", "Inclusive social cafe space.", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"),
    place("Shiva Cafe", "Simple rooftop hangout near Assi Ghat.", "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=900&q=80"),
    place("Cafe D Benaras", "Modern aesthetic cafe with rooftop seating.", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=900&q=80"),
    place("The Chillout Lounge", "Cafe-lounge with relaxed evening vibe.", "https://images.unsplash.com/photo-1528605248644-14dd04022da1?w=900&q=80"),
    place("Sparrow Cafe", "Minimal peaceful cafe for coffee breaks.", "https://images.unsplash.com/photo-1559925393-8be08e16738e?w=900&q=80"),
    place("Bunny Cafe", "Cute modern cafe with desserts and coffee.", "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=900&q=80"),
    place("Vegan & Raw Cafe", "Healthy-food cafe near Assi area.", "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=900&q=80"),
    place("Tandoor Villa Cafe", "Rooftop dining with city views.", "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=900&q=80"),
    place("Cafe Blue Lassi", "Famous Banarasi lassi destination.", "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?w=900&q=80"),
    place("Varanasi Cafe", "Casual cafe space for tourists and students.", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=900&q=80"),
    place("Holy Chopsticks Cafe", "Fusion cafe with modern interiors.", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=900&q=80"),
    place("Cafe Zaika", "Budget-friendly local cafe and snack spot.", "https://images.unsplash.com/photo-1528605105345-5344ea20e269?w=900&q=80"),
    place("Ashiyana Cafe", "Rooftop cafe popular among travelers.", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"),
    place("Taste King Cafe", "Popular local coffee and fast-food stop.", "https://images.unsplash.com/photo-1511920170033-f8396924c348?w=900&q=80"),
    place("Cafebility", "Creative cafe space for students and creators.", "https://images.unsplash.com/photo-1453614512568-c4024d13c247?w=900&q=80"),
    place("Street Cafe Varanasi", "Affordable cafe with youth crowd.", "https://images.unsplash.com/photo-1528605248644-14dd04022da1?w=900&q=80"),
    place("The Green Terrace Cafe", "Open-air rooftop cafe with evening vibes.", "https://images.unsplash.com/photo-1466978913421-dad2ebd01d17?w=900&q=80")
),
             // Hotels
List.of(
        place("BrijRama Palace", "Signature riverfront heritage luxury stay.", "https://images.unsplash.com/photo-1445019980597-93fa8acb246c?w=900&q=80"),
        place("Taj Ganges", "Reliable upscale hotel with spacious grounds.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
        place("Guleria Kothi", "Boutique riverside property with intimate atmosphere.", "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=900&q=80"),
        place("Radisson Hotel Varanasi", "Modern premium hotel with comfortable business facilities.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
        place("Hotel Alka", "Popular ghat-side stay with scenic Ganges views.", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=900&q=80"),
        place("Hotel Surya", "Heritage-style property featuring gardens and pool.", "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?w=900&q=80"),
        place("Rivatas by Ideal", "Luxury city hotel known for elegant interiors.", "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=900&q=80"),
        place("Hotel Clarks Varanasi", "Classic luxury hotel with peaceful ambiance.", "https://images.unsplash.com/photo-1564501049412-61c2a3083791?w=900&q=80"),
        place("Costa Riviera Hotel", "Modern stay option close to city attractions.", "https://images.unsplash.com/photo-1455587734955-081b22074882?w=900&q=80"),
        place("The Amayaa", "Comfortable upscale hotel near cantonment area.", "https://images.unsplash.com/photo-1521783988139-89397d761dce?w=900&q=80"),
        place("Hotel Madin", "Contemporary hotel featuring rooftop dining.", "https://images.unsplash.com/photo-1496417263034-38ec4f0b665a?w=900&q=80"),
        place("Palace on Ganges", "Riverside boutique stay inspired by royal decor.", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=900&q=80"),
        place("Dwivedi Hotels Palace", "Traditional riverside hotel with old Banaras charm.", "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=900&q=80"),
        place("Hotel Heritage Inn", "Affordable hotel with cozy accommodations.", "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?w=900&q=80"),
        place("Hotel Buddha", "Budget-friendly stay preferred by backpackers.", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=900&q=80"),
        place("Tree of Life Resort", "Luxury retreat with peaceful surroundings.", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=900&q=80"),
        place("Hotel Zeeras", "Well-known city hotel with rooftop restaurant.", "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=900&q=80"),
        place("Stay Banaras", "Stylish boutique property loved by young travelers.", "https://images.unsplash.com/photo-1521783988139-89397d761dce?w=900&q=80"),
        place("Hotel Varuna", "Comfortable hotel near transportation hubs.", "https://images.unsplash.com/photo-1455587734955-081b22074882?w=900&q=80"),
        place("Ganpati Guest House", "Budget riverside guest house with rooftop cafe.", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=900&q=80"),
        place("Bhadra Kali Guest House", "Simple and peaceful riverside accommodation.", "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=900&q=80"),
        place("Hotel Temple on Ganges", "Scenic hotel offering direct riverfront access.", "https://images.unsplash.com/photo-1496417263034-38ec4f0b665a?w=900&q=80"),
        place("Hotel Sahu", "Affordable stay with traditional Banarasi feel.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
        place("Hotel Tridev", "Modern hotel close to key attractions.", "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?w=900&q=80"),
        place("Hotel Divine Destination", "Elegant stay with contemporary rooms.", "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=900&q=80"),
        place("Hotel New Temple's Town", "Comfortable tourist-friendly accommodation.", "https://images.unsplash.com/photo-1564501049412-61c2a3083791?w=900&q=80"),
        place("Diamond Hotel", "Trusted city hotel with quality services.", "https://images.unsplash.com/photo-1455587734955-081b22074882?w=900&q=80"),
        place("Hotel Varanasi Inn", "Convenient stay option near railway station.", "https://images.unsplash.com/photo-1496417263034-38ec4f0b665a?w=900&q=80"),
        place("Ramada Plaza JHV", "Premium luxury hotel with modern amenities.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
        place("The Fern Residency", "Contemporary eco-friendly upscale hotel.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80")
),

// Tourist Places
List.of(
        place("Boat Ride on the Ganges", "Best way to absorb the full riverfront panorama.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80"),
        place("Sarnath", "Important Buddhist heritage site near the city.", "https://images.unsplash.com/photo-1542601906990-b4d3fb7780b9?w=900&q=80"),
        place("Old City Lanes", "Dense maze of shops, shrines, and local food stops.", "https://images.unsplash.com/photo-1518509562904-e7ef99cdcc86?w=900&q=80"),
        place("Kashi Vishwanath Corridor", "Grand temple corridor connecting ghats and temple.", "https://images.unsplash.com/photo-1561361058-c24cecae35ca?w=900&q=80"),
        place("Dashashwamedh Ghat Aarti", "World-famous evening spiritual ceremony.", "https://images.unsplash.com/photo-1565354785697-7e2f0c0b5c3d?w=900&q=80"),
        place("Assi Ghat Sunrise", "Beautiful morning destination for yoga and tea.", "https://images.unsplash.com/photo-1571679654681-ba01b9e1e117?w=900&q=80"),
        place("Ramnagar Fort Tour", "Historic fort showcasing royal artifacts.", "https://images.unsplash.com/photo-1548013146-72479768bada?w=900&q=80"),
        place("Banaras Hindu University Campus", "Large educational campus with iconic gateways.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
        place("Bharat Kala Bhavan", "Museum housing sculptures and paintings.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
        place("Durga Kund Temple", "Red temple beside sacred kund area.", "https://images.unsplash.com/photo-1514222134-b57cbb8ce073?w=900&q=80"),
        place("Tulsi Manas Temple", "Temple associated with Ramcharitmanas writings.", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=900&q=80"),
        place("Sankat Mochan Temple", "Popular Hanuman temple attracting devotees daily.", "https://images.unsplash.com/photo-1482192596544-9eb780fc7f66?w=900&q=80"),
        place("Manikarnika Ghat", "Sacred cremation ghat filled with history.", "https://images.unsplash.com/photo-1524499982521-1ffd58dd89ea?w=900&q=80"),
        place("Nepali Temple", "Wood-crafted temple inspired by Kathmandu style.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80"),
        place("Scindia Ghat", "Peaceful riverside location with tilted temple.", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=900&q=80"),
        place("Darbhanga Ghat", "Heritage riverside area perfect for photography.", "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?w=900&q=80"),
        place("Chet Singh Fort", "Historic riverside fort with old architecture.", "https://images.unsplash.com/photo-1521295121783-8a321d551ad2?w=900&q=80"),
        place("Alamgir Mosque", "Historic Mughal structure overlooking Ganga.", "https://images.unsplash.com/photo-1548013146-72479768bada?w=900&q=80"),
        place("Panchganga Ghat", "Spiritual riverside point with mythology importance.", "https://images.unsplash.com/photo-1502082553048-f009c37129b9?w=900&q=80"),
        place("Ganga River Cruise", "Luxury cruise offering evening city views.", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=900&q=80"),
        place("Reewa Ghat", "Less crowded riverside spot for calm exploration.", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=900&q=80"),
        place("Kedar Ghat", "Colorful riverside attraction with temple vibes.", "https://images.unsplash.com/photo-1493246507139-91e8fad9978e?w=900&q=80"),
        place("Shivala Ghat", "Historic riverside stretch with peaceful surroundings.", "https://images.unsplash.com/photo-1516483638261-f4dbaf036963?w=900&q=80"),
        place("Raja Ghat", "Authentic Banaras atmosphere away from crowds.", "https://images.unsplash.com/photo-1501594907352-04cda38ebc29?w=900&q=80"),
        place("Bharat Mata Temple", "Unique marble map temple dedicated to India.", "https://images.unsplash.com/photo-1473116763249-2faaef81ccda?w=900&q=80"),
        place("Ahilyabai Ghat", "Historic ghat renovated during Maratha rule.", "https://images.unsplash.com/photo-1501594907352-04cda38ebc29?w=900&q=80"),
        place("Gyanvapi Well", "Ancient religious structure near Vishwanath Temple.", "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=900&q=80"),
        place("Lalita Ghat", "Scenic riverside area with Nepali architecture.", "https://images.unsplash.com/photo-1494526585095-c41746248156?w=900&q=80"),
        place("Vishalakshi Temple", "Important Shakti Peeth attracting pilgrims.", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=900&q=80"),
        place("Chunar Fort Excursion", "Historic day-trip destination near Varanasi.", "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=900&q=80")
),

                List.of("Deep spiritual and cultural draw", "Strong ghat-side tourism experience", "Best explored with river rides and walking routes")));

        data.put("delhi", curatedCity(
                "delhi", "Delhi", "Capital energy, food lanes, monuments, neighborhoods, and nonstop city movement.",
                "https://deih43ym53wif.cloudfront.net/large_Rajpath-delhi-shutterstock_1195751923.jpg_7647e1aad2.jpg", "National Capital Territory",
                List.of(new QuickFact("Capital", "India"), new QuickFact("Monuments", "Historic"), new QuickFact("Food", "Legendary")),
               // =======================
// DELHI - POPULAR PLACES
// =======================
List.of(
    place("India Gate", "Delhi's iconic war memorial and evening hangout landmark.", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=900&q=80"),
    place("Connaught Place", "The social and shopping heart of central Delhi.", "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=900&q=80"),
    place("Red Fort", "Historic Mughal fort and one of Delhi's most recognized attractions.", "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=900&q=80"),
    place("Lotus Temple", "Peaceful white-marble temple with stunning architecture.", "https://images.unsplash.com/photo-1598091383021-15ddea10925d?w=900&q=80"),
    place("Qutub Minar", "UNESCO-listed tower filled with Indo-Islamic history.", "https://images.unsplash.com/photo-1564507592333-c60657eea523?w=900&q=80"),
    place("Humayun's Tomb", "Beautiful Mughal-era monument and garden complex.", "https://images.unsplash.com/photo-1576485290814-1c72aa4bbb8e?w=900&q=80"),
    place("Lodhi Garden", "Green escape loved for walks, picnics, and sunsets.", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=900&q=80"),
    place("Akshardham", "Massive spiritual and cultural temple complex.", "https://images.unsplash.com/photo-1609947017136-9daf32a5eb16?w=900&q=80"),
    place("Chandni Chowk", "Historic market packed with food and old Delhi energy.", "https://images.unsplash.com/photo-1517244683847-7456b63c5969?w=900&q=80"),
    place("Hauz Khas Village", "Trendy mix of cafes, nightlife, and lake views.", "https://images.unsplash.com/photo-1517457373958-b7bdd4587205?w=900&q=80"),
    place("Select Citywalk", "Popular shopping and entertainment destination.", "https://images.unsplash.com/photo-1519567241046-7f570eee3ce6?w=900&q=80"),
    place("DLF Promenade", "Luxury shopping mall with modern city vibe.", "https://images.unsplash.com/photo-1481437156560-3205f6a55735?w=900&q=80"),
    place("Garden of Five Senses", "Aesthetic garden space for calm evening outings.", "https://images.unsplash.com/photo-1493246507139-91e8fad9978e?w=900&q=80"),
    place("National Zoological Park", "Popular zoo and family attraction.", "https://images.unsplash.com/photo-1501706362039-c6e13b4b2b5d?w=900&q=80"),
    place("Agrasen Ki Baoli", "Historic stepwell hidden between modern buildings.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80"),
    place("Purana Qila", "Ancient fort complex with boating and history.", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=900&q=80"),
    place("Jama Masjid", "One of India's largest and most historic mosques.", "https://images.unsplash.com/photo-1584285405426-3b4c6bcd5c5d?w=900&q=80"),
    place("Sarojini Nagar Market", "Street-shopping hotspot loved by students.", "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=900&q=80"),
    place("Janpath Market", "Affordable fashion and accessory shopping street.", "https://images.unsplash.com/photo-1521334884684-d80222895322?w=900&q=80"),
    place("Raj Ghat", "Peaceful memorial dedicated to Mahatma Gandhi.", "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?w=900&q=80"),
    place("Nehru Planetarium", "Fun educational destination for astronomy lovers.", "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=900&q=80"),
    place("Adventure Island", "Popular amusement and water park in Delhi.", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=900&q=80"),
    place("Waste to Wonder Park", "Unique park featuring monuments made from scrap.", "https://images.unsplash.com/photo-1490750967868-88aa4486c946?w=900&q=80"),
    place("Deer Park", "Relaxed green area near Hauz Khas.", "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=900&q=80"),
    place("National Rail Museum", "Interactive museum showcasing railway history.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
    place("Dilli Haat", "Craft and food market representing Indian states.", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=900&q=80"),
    place("Majnu Ka Tila", "Tibetan-style neighborhood with cafes and food.", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=900&q=80"),
    place("Kartavya Path", "Wide ceremonial boulevard near India Gate.", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=900&q=80"),
    place("ISKCON Temple", "Spiritual and cultural temple destination.", "https://images.unsplash.com/photo-1609947017136-9daf32a5eb16?w=900&q=80"),
    place("Yamuna Ghat", "Sunrise photography and peaceful riverside views.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80")
),

// =======================
// DELHI - CAFES
// =======================
List.of(
    place("Diggin Cafe", "Fairy-light cafe famous for aesthetic brunch dates.", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"),
    place("Cafe Delhi Heights", "Casual cafe popular for burgers and group outings.", "https://images.unsplash.com/photo-1528605248644-14dd04022da1?w=900&q=80"),
    place("Blue Tokai", "Specialty coffee chain loved by students and creators.", "https://images.unsplash.com/photo-1559925393-8be08e16738e?w=900&q=80"),
    place("AMA Cafe", "Majnu Ka Tila favorite for coffee and desserts.", "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=900&q=80"),
    place("Social Hauz Khas", "Youth hotspot mixing work, food, and nightlife.", "https://images.unsplash.com/photo-1528605105345-5344ea20e269?w=900&q=80"),
    place("Rose Cafe", "Soft pink interiors and relaxed brunch vibe.", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=900&q=80"),
    place("Jugmug Thela", "Cozy indie cafe with artistic atmosphere.", "https://images.unsplash.com/photo-1453614512568-c4024d13c247?w=900&q=80"),
    place("Cafe Tesu", "Minimal cafe with modern food menu.", "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=900&q=80"),
    place("Perch Wine & Coffee Bar", "Elegant coffee and conversation spot.", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"),
    place("The Big Chill", "Legendary comfort-food cafe in Delhi.", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=900&q=80"),
    place("United Coffee House", "Classic Connaught Place cafe with heritage feel.", "https://images.unsplash.com/photo-1511920170033-f8396924c348?w=900&q=80"),
    place("Cha Bar", "Bookstore cafe perfect for reading sessions.", "https://images.unsplash.com/photo-1521017432531-fbd92d768814?w=900&q=80"),
    place("Cafe Lota", "Culture-inspired cafe near museum district.", "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=900&q=80"),
    place("Elma's Bakery", "Cute bakery cafe with desserts and tea.", "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=900&q=80"),
    place("Triveni Terrace Cafe", "Quiet artistic cafe loved by creatives.", "https://images.unsplash.com/photo-1466978913421-dad2ebd01d17?w=900&q=80"),
    place("Coffee Bond", "Minimal coffee spot with calm interiors.", "https://images.unsplash.com/photo-1559925393-8be08e16738e?w=900&q=80"),
    place("Cafe Wink", "Youth-focused aesthetic cafe space.", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=900&q=80"),
    place("The Grammar Room", "Stylish brunch cafe in South Delhi.", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"),
    place("Qahwa Cafe", "Kashmiri-inspired cafe and tea spot.", "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=900&q=80"),
    place("Colocal", "Chocolate-focused cafe and dessert destination.", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=900&q=80"),
    place("Spezia Bistro", "Italian-inspired cafe with rooftop vibe.", "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=900&q=80"),
    place("AMA Bistro", "Casual Tibetan cafe with cozy seating.", "https://images.unsplash.com/photo-1528605248644-14dd04022da1?w=900&q=80"),
    place("Cafe Dori", "Modern industrial cafe with premium vibe.", "https://images.unsplash.com/photo-1453614512568-c4024d13c247?w=900&q=80"),
    place("The Hudson Cafe", "Student-favorite cafe with comfort food.", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=900&q=80"),
    place("Big Yellow Door", "Popular student hangout cafe near colleges.", "https://images.unsplash.com/photo-1528605105345-5344ea20e269?w=900&q=80"),
    place("Woodbox Cafe", "Affordable cafe for college crowds.", "https://images.unsplash.com/photo-1511920170033-f8396924c348?w=900&q=80"),
    place("Echoes Cafe", "Cafe run with inclusive social concept.", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"),
    place("Music & Mountains", "Mountain-themed cozy cafe space.", "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=900&q=80"),
    place("Cafe Soul Garden", "Garden-style cafe with chill atmosphere.", "https://images.unsplash.com/photo-1466978913421-dad2ebd01d17?w=900&q=80"),
    place("Roastery Coffee House", "Premium coffee destination with calm workspace vibe.", "https://images.unsplash.com/photo-1559925393-8be08e16738e?w=900&q=80")
),
                // =======================
// DELHI - HOTELS
// =======================
List.of(
    place("The Leela Palace", "Luxury Delhi hotel with royal interiors and premium hospitality.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
    place("Taj Palace", "Iconic luxury hotel popular for business and leisure stays.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
    place("ITC Maurya", "Famous five-star property with elegant hospitality.", "https://images.unsplash.com/photo-1455587734955-081b22074882?w=900&q=80"),
    place("The Oberoi New Delhi", "Premium hotel with modern luxury and city views.", "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?w=900&q=80"),
    place("Shangri-La Eros", "Luxury central Delhi hotel with polished interiors.", "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=900&q=80"),
    place("Hyatt Regency Delhi", "Business-friendly luxury hotel with modern amenities.", "https://images.unsplash.com/photo-1445019980597-93fa8acb246c?w=900&q=80"),
    place("The Imperial", "Historic colonial-style luxury property.", "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=900&q=80"),
    place("Roseate House", "Modern airport hotel with stylish rooms.", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=900&q=80"),
    place("Andaz Delhi", "Contemporary luxury hotel near Aerocity.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
    place("Radisson Blu Plaza", "Premium airport-area accommodation.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
    place("Holiday Inn Aerocity", "Comfortable upscale stay near airport.", "https://images.unsplash.com/photo-1455587734955-081b22074882?w=900&q=80"),
    place("Novotel New Delhi", "Modern hotel with polished business facilities.", "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?w=900&q=80"),
    place("Le Meridien", "Luxury hotel close to Connaught Place.", "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=900&q=80"),
    place("The Lalit New Delhi", "Popular luxury hotel with nightlife and dining.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
    place("Vivanta Dwarka", "Modern upscale hotel with spacious rooms.", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=900&q=80"),
    place("Welcomhotel Dwarka", "Elegant stay option for families and travelers.", "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=900&q=80"),
    place("Park Plaza Shahdara", "Business-class hotel with modern facilities.", "https://images.unsplash.com/photo-1455587734955-081b22074882?w=900&q=80"),
    place("The Suryaa", "Luxury South Delhi stay with rooftop views.", "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?w=900&q=80"),
    place("Eros Hotel", "Premium Nehru Place accommodation.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
    place("Jaypee Siddharth", "Classic hotel known for business stays.", "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=900&q=80"),
    place("Hotel City Star", "Affordable stay near New Delhi Railway Station.", "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=900&q=80"),
    place("Bloomrooms", "Minimal and modern budget-friendly hotel.", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=900&q=80"),
    place("Hotel Ajanta", "Popular traveler stay near railway station.", "https://images.unsplash.com/photo-1455587734955-081b22074882?w=900&q=80"),
    place("Hotel Hari Piorko", "Budget hotel popular with backpackers.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
    place("The Prime Delhi", "Modern boutique-style accommodation.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
    place("Hotel Godwin Deluxe", "Comfortable hotel in Paharganj area.", "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?w=900&q=80"),
    place("Rosemallow Hotel", "Trendy modern hotel with aesthetic interiors.", "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=900&q=80"),
    place("Maidens Hotel", "Historic luxury property with colonial charm.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
    place("Haveli Dharampura", "Heritage haveli stay in Old Delhi.", "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=900&q=80"),
    place("Pullman New Delhi", "Upscale hotel with business and leisure comfort.", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=900&q=80")
),

// =======================
// DELHI - TOURIST PLACES
// =======================
List.of(
    place("India Gate", "Delhi's iconic national monument and evening attraction.", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=900&q=80"),
    place("Red Fort", "Historic Mughal fort and UNESCO World Heritage Site.", "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=900&q=80"),
    place("Lotus Temple", "Famous white-marble BahÃ¡Ê¼Ã­ House of Worship.", "https://images.unsplash.com/photo-1598091383021-15ddea10925d?w=900&q=80"),
    place("Qutub Minar", "Tall Indo-Islamic tower and major heritage site.", "https://images.unsplash.com/photo-1564507592333-c60657eea523?w=900&q=80"),
    place("Humayun's Tomb", "Beautiful Mughal architecture and gardens.", "https://images.unsplash.com/photo-1576485290814-1c72aa4bbb8e?w=900&q=80"),
    place("Akshardham", "Massive spiritual and cultural complex.", "https://images.unsplash.com/photo-1609947017136-9daf32a5eb16?w=900&q=80"),
    place("Jama Masjid", "Historic mosque with grand architecture.", "https://images.unsplash.com/photo-1584285405426-3b4c6bcd5c5d?w=900&q=80"),
    place("Chandni Chowk", "Old Delhi market famous for food and shopping.", "https://images.unsplash.com/photo-1517244683847-7456b63c5969?w=900&q=80"),
    place("Agrasen Ki Baoli", "Ancient stepwell hidden in central Delhi.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80"),
    place("Purana Qila", "Historic fort complex with lake boating.", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=900&q=80"),
    place("Lodhi Garden", "Green heritage garden perfect for walks.", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=900&q=80"),
    place("Garden of Five Senses", "Beautiful themed garden for peaceful outings.", "https://images.unsplash.com/photo-1493246507139-91e8fad9978e?w=900&q=80"),
    place("Connaught Place", "Colonial-era commercial and nightlife district.", "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=900&q=80"),
    place("Raj Ghat", "Memorial dedicated to Mahatma Gandhi.", "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?w=900&q=80"),
    place("National Rail Museum", "Interactive railway museum for all ages.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
    place("Dilli Haat", "Craft and food market showcasing Indian culture.", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=900&q=80"),
    place("National Zoological Park", "Popular wildlife and family attraction.", "https://images.unsplash.com/photo-1501706362039-c6e13b4b2b5d?w=900&q=80"),
    place("Nehru Planetarium", "Educational astronomy and science destination.", "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=900&q=80"),
    place("Hauz Khas Fort", "Historic ruins beside a scenic lake.", "https://images.unsplash.com/photo-1517457373958-b7bdd4587205?w=900&q=80"),
    place("Safdarjung Tomb", "Elegant Mughal-style mausoleum.", "https://images.unsplash.com/photo-1576485290814-1c72aa4bbb8e?w=900&q=80"),
    place("Waste to Wonder Park", "Monuments recreated using industrial scrap.", "https://images.unsplash.com/photo-1490750967868-88aa4486c946?w=900&q=80"),
    place("Adventure Island", "Amusement park with rides and attractions.", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=900&q=80"),
    place("ISKCON Temple", "Spiritual and cultural tourist destination.", "https://images.unsplash.com/photo-1609947017136-9daf32a5eb16?w=900&q=80"),
    place("Majnu Ka Tila", "Tibetan-inspired cultural and food district.", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=900&q=80"),
    place("Kartavya Path", "Ceremonial boulevard near Rashtrapati Bhavan.", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=900&q=80"),
    place("National Museum", "Historic museum with Indian art and artifacts.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
    place("Rail Museum Toy Train", "Family-friendly railway attraction.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
    place("Deer Park", "Nature park near Hauz Khas Village.", "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=900&q=80"),
    place("Yamuna Ghat", "Peaceful sunrise photography location.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80"),
    place("Feroz Shah Kotla Fort", "Historic ruins with rich medieval history.", "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=900&q=80")
),

// =======================
// DELHI - EDUCATIONAL +
// MUSEUM + LEARNING PLACES
// =======================
List.of(
    place("Delhi University", "One of India's most recognized university systems.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
    place("Jawaharlal Nehru University", "Leading university known for academics and research.", "https://images.unsplash.com/photo-1523240795612-9a054b0db644?w=900&q=80"),
    place("Jamia Millia Islamia", "Historic central university with strong academic culture.", "https://images.unsplash.com/photo-1498243691581-b145c3f54a5a?w=900&q=80"),
    place("IIT Delhi", "Premier engineering and innovation institute.", "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=900&q=80"),
    place("AIIMS Delhi", "Top medical and healthcare research institution.", "https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?w=900&q=80"),
    place("National Museum", "Educational museum filled with Indian history and artifacts.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
    place("National Science Centre", "Interactive science museum for students and families.", "https://images.unsplash.com/photo-1507413245164-6160d8298b31?w=900&q=80"),
    place("Nehru Planetarium", "Astronomy and space-science learning center.", "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=900&q=80"),
    place("National Rail Museum", "Museum exploring Indian railway history.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
    place("Crafts Museum", "Traditional Indian art and craft museum.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
    place("Indira Gandhi Memorial Museum", "Museum dedicated to India's former Prime Minister.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
    place("Gandhi Smriti", "Historic learning space connected to Mahatma Gandhi.", "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?w=900&q=80"),
    place("Teen Murti Bhavan", "Museum and library dedicated to Jawaharlal Nehru.", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=900&q=80"),
    place("National Gallery of Modern Art", "Art museum with modern Indian masterpieces.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
    place("Delhi Public Library", "Public learning and reading center.", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=900&q=80"),
    place("British Council Library", "International reading and educational resource center.", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=900&q=80"),
    place("Miranda House", "Prestigious women's college under Delhi University.", "https://images.unsplash.com/photo-1509062522246-3755977927d7?w=900&q=80"),
    place("Hansraj College", "Popular DU college with strong student culture.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
    place("SRCC", "India's top commerce college.", "https://images.unsplash.com/photo-1523240795612-9a054b0db644?w=900&q=80"),
    place("Lady Shri Ram College", "Leading arts and commerce college for women.", "https://images.unsplash.com/photo-1498243691581-b145c3f54a5a?w=900&q=80"),
    place("St. Stephen's College", "Historic and academically elite institution.", "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=900&q=80"),
    place("National Law University Delhi", "Premier law and policy university.", "https://images.unsplash.com/photo-1523240795612-9a054b0db644?w=900&q=80"),
    place("Jamia Hamdard", "Research-focused university campus.", "https://images.unsplash.com/photo-1498243691581-b145c3f54a5a?w=900&q=80"),
    place("IGNOU", "India's largest open-learning university.", "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=900&q=80"),
    place("National Philatelic Museum", "Museum exploring Indian postal history.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
    place("Sulabh International Museum of Toilets", "Unique educational museum with global sanitation history.", "https://images.unsplash.com/photo-1507413245164-6160d8298b31?w=900&q=80"),
    place("Shankar's International Dolls Museum", "Museum with dolls from around the world.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
    place("Allen Delhi", "Popular coaching institute for competitive exams.", "https://images.unsplash.com/photo-1523240795612-9a054b0db644?w=900&q=80"),
    place("Aakash Institute Delhi", "Medical and engineering preparation coaching center.", "https://images.unsplash.com/photo-1498243691581-b145c3f54a5a?w=900&q=80"),
    place("FIITJEE Delhi", "Engineering entrance coaching institute.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80")
),
                List.of("Best mix of heritage and urban lifestyle", "Excellent hotel and cafe variety", "Great for food, shopping, and monument circuits")));

        data.put("chandigarh", curatedCity(
                "chandigarh", "Chandigarh", "The city beautiful with planned sectors, lakes, cafes, gardens, and clean design.",
                "https://s7ap1.scene7.com/is/image/incredibleindia/sukhna-lake-chandigarh-chandigarh-2-attr-hero?qlt=82&ts=1742194189957", "Punjab and Haryana",
                List.of(new QuickFact("47+", "Sectors"), new QuickFact("1M+", "Residents"), new QuickFact("UNESCO", "Landmarks")),
              // 30 Popular Places
List.of(
    place("Sukhna Lake", "Chandigarh's signature waterfront for sunrise walks and boating.", "https://s7ap1.scene7.com/is/image/incredibleindia/sukhna-lake-chandigarh-chandigarh-2-attr-hero?qlt=82&ts=1742194189957"),
    place("Rock Garden", "Iconic recycled-art attraction full of sculptures and pathways.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSOiZzq9t31UkjaCU6UiMbNIZasEeBXLzEmYQ&s"),
    place("Rose Garden", "Massive flower garden known for peaceful evening walks.", "https://www.citywoofer.com/blog/wp-content/uploads/2023/02/WhatsApp-Image-2023-02-08-at-8.33.25-PM-756x400.jpeg"),
    place("Sector 17 Plaza", "Open-air shopping and social hotspot.", "https://s7ap1.scene7.com/is/image/incredibleindia/sector-17-chandigarh-punjab-blog-sho-exp-cit-pop?qlt=82&ts=1742181972831"),
    place("Elante Mall", "Modern entertainment and shopping destination.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/13/81/80/07/elante-mall.jpg?w=1200&h=-1&s=1"),
    place("Japanese Garden", "Quiet landscaped park with bridges and greenery.", "https://chandigarhtourism.gov.in/uploads/_1581998967.jpg"),
    place("Leisure Valley", "Green stretch running through the city.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQwGGp9dq0u5jtxz2R7rinqNHY69ABAXy2Tmg&s"),
    place("Terraced Garden", "Popular flower and fountain garden.", "https://www.citywoofer.com/blog/wp-content/uploads/2023/03/WhatsApp-Image-2023-03-14-at-7.13.50-PM-711x400.jpeg"),
    place("Garden of Silence", "Peaceful Buddha statue area near Sukhna Lake.", "https://www.trawell.in/admin/images/upload/472763425Chandigarh_Garden_of_Silence_Main.jpg"),
    place("Open Hand Monument", "Famous architectural symbol of Chandigarh.", "https://www.mapsofindia.com/ci-moi-images/my-india//chandigarh.jpg"),
    place("Capitol Complex", "UNESCO-recognized architecture landmark.", "https://upload.wikimedia.org/wikipedia/commons/1/1a/Palace_of_Assembly_Chandigarh_2006.jpg"),
    place("Bougainvillea Garden", "Aesthetic floral garden with colorful paths.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/13/59/0e/a3/bougainvillea-super-gazebo.jpg?w=1200&h=1200&s=1"),
    place("Shanti Kunj", "Relaxed nature park with streams and greenery.", "https://www.holidify.com/images/cmsuploads/compressed/shanti-kunj-park-chandigarh-tourism-entry-fee-timings-holidays-reviews-header_20220117140331.jpeg"),
    place("Butterfly Park", "Nature-focused spot filled with butterflies and plants.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcS5lcjCxeRMm1tbnX1OVQS89X1fzomfNMBLAQ&s"),
    place("Topiary Park", "Creative garden with animal-shaped plants.", "https://www.shoutlo.com/assets/images/merchant_images/merchant-181055-610e7f574f4fb.jpg"),
    place("Musical Fountain", "Evening attraction with lights and music.", "https://upload.wikimedia.org/wikipedia/commons/b/b8/Musical_Fountain%2C_Sector_17%2C_Chandigarh.jpg"),
    place("Timber Trail", "Nearby hill escape with cable-car views.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/0a/8f/ef/f0/timber-trail.jpg?w=1200&h=-1&s=1"),
    place("Morni Hills", "Quick mountain getaway near Chandigarh.", "https://images.travelandleisureasia.com/wp-content/uploads/sites/2/2024/06/26153443/HIFI-Ariel-view-of-Tikkar-Taal-from-Morni-Hills-The-Himalyan-Lens-Shutterstock-1600x900.jpg"),
    place("Siswan Dam", "Short-drive sunset and cycling spot.", "https://i.ytimg.com/vi/q6fWzkmV9Xk/hq720.jpg?sqp=-oaymwEhCK4FEIIDSFryq4qpAxMIARUAAAAAGAElAADIQj0AgKJD&rs=AOn4CLBmsWkRIzvcijxWocbidYj9TLd5_Q"),
    place("Nada Sahib Gurudwara", "Popular spiritual and riverside destination.", "https://haryanatourism.gov.in/wp-content/uploads/2024/07/nada_pic1-1.jpg"),
    place("Pinjore Gardens", "Historic Mughal-style garden near Chandigarh.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/03/d4/a2/2a/pinjore-yadavindra-gardens.jpg?w=900&h=500&s=1"),
    place("Thunder Zone", "Fun amusement and water park.", "https://hblimg.mmtcdn.com/content/hubble/img/ttd_images/mmt/activities/m_Mohali_Thunder_zone_amusement_park_1_l_425_601.jpg"),
    place("Funcity", "Family-friendly rides and water attractions.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSYXAHp2UJ-8PPjzdEJ0z6mlZl-a2jhKb5VTg&s"),
    place("VR Punjab Mall", "Large nearby mall with shopping and cinema.", "https://hblimg.mmtcdn.com/content/hubble/img/ttd_images/mmt/activities/m_Mohali_Vr_punjab_mall_1_l_480_640.jpg"),
    place("ChhatBir Zoo", "Popular wildlife and safari attraction.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcS1_YOAPE53nrCiNJ4r3RuIpsy7SNtmeuTSgQ&s")
),
            // 30 Cafes
List.of(
    place("The Willow Cafe", "Rustic brunch cafe with cozy greenery.", "https://media-cdn.tripadvisor.com/media/photo-s/0a/51/e3/13/20160213-204436-largejpg.jpg"),
    place("Third Wave Coffee", "Work-friendly specialty coffee stop.", "https://b.zmtcdn.com/data/pictures/8/20271708/e75d4156c1e49292635842bc035f1574.jpg?fit=around|750:500&crop=750:500;*,*"),
    place("Books N Brew", "Reading cafe with relaxed student vibe.", "https://b.zmtcdn.com/data/pictures/4/120554/abf83d3dd8ae17614f28c89be3dc32f5.jpg"),
    place("Back to Source", "Nature-inspired cafe for comfort food.", "https://media-cdn.tripadvisor.com/media/photo-m/1280/25/84/e4/7b/cafe.jpg"),
    place("Nik Baker's", "Popular bakery cafe with desserts and coffee.", "https://content.jdmagicbox.com/v2/comp/chandigarh/s3/0172px172.x172.231005181655.k3s3/catalogue/nik-baker-s-jal-vayu-vihar-chandigarh-bakeries-gtk559x616.jpg"),
    place("Cafe JC's", "Classic Chandigarh cafe for casual meetups.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/11/9a/3a/c4/indoor-setting-also-available.jpg?w=900&h=500&s=1"),
    place("Virgin Courtyard", "Elegant European-style dining cafe.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/05/55/01/c9/virgin-courtyard.jpg?w=900&h=500&s=1"),
    place("Brooklyn Central", "Urban cafe with lively food and music vibe.", "https://b.zmtcdn.com/data/pictures/2/121552/f4126c9b81d636ccc24bf9505afd384c_featured_v2.jpg?fit=around|960:500&crop=960:500;*,*"),
    place("Cafe Delhi Heights", "Popular food spot for groups and outings.", "https://media-cdn.tripadvisor.com/media/photo-m/1280/2f/d5/52/02/restaurant-images.jpg"),
    place("Ovenfresh", "Bakery cafe known for pizzas and desserts.", "https://b.zmtcdn.com/data/pictures/1/122051/261b03672693264c4f9641f37f26e9f0.jpg?fit=around|960:500&crop=960:500;*,*"),
    place("Hedgehog Cafe", "Cute themed cafe with fun interiors.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/18/ee/23/94/img-20190803-220928-largejpg.jpg?w=900&h=500&s=1"),
    place("Indian Coffee House", "Old-school coffee stop with classic charm.", "https://www.shoutlo.com/uploads/articles/header-img-1691129765-indian-coffee-house-chandigarh.jpg"),
    place("The Brew Estate", "Microbrewery cafe with rooftop seating.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/28/c9/9b/b2/the-brew-estate-26.jpg?w=900&h=500&s=1"),
    place("Social Chandigarh", "Trendy youth hangout with food and music.", "https://www.joonsquare.com/usermanage/image/business/sector-7-social-chandigarh-23157/sector-7-social-chandigarh-img_2518.jpg"),
    place("Casa Bella Vista", "Italian cafe with rooftop dining.", "https://dineout-media-assets.swiggy.com/swiggy/image/upload/fl_lossy,f_auto,q_auto/DINEOUT_ALL_RESTAURANTS/IMAGES/RESTAURANT_IMAGE_SERVICE/2025/4/2/dcd92d87-cec0-4347-970b-5df448dfdd80_image110de6ba73c4b740aba18569e7f5c280a3.JPG"),
    place("Cafe Nomad", "Creative cafe popular among students.", "https://b.zmtcdn.com/data/pictures/7/120557/ef3a9f00ae571442a4c38fa319716bee.jpg?fit=around|750:500&crop=750:500;*,*"),
    place("Hot Millions", "Iconic Chandigarh fast-food cafe.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/09/d9/5f/13/hot-millions.jpg?w=900&h=500&s=1"),
    place("Cup and Kitaab", "Book-themed cafe for peaceful evenings.", "https://b.zmtcdn.com/data/reviews_photos/a57/55d47b3b81481492a2284e41bdd9da57_1580194475.jpg?fit=around|750:500&crop=750:500;*,*"),
    place("Cafe Olio", "Minimal cafe with premium food options.", "https://b.zmtcdn.com/data/pictures/1/20376971/20205cb060f7fff1096dd4f5e736cc2b_featured_v2.jpg"),
    place("Tulum Cafe", "Boho-style cafe trending for photos.", "https://b.zmtcdn.com/data/pictures/5/19584185/868e89e1523f574d82f4ad51203afcc3.jpg?fit=around|750:500&crop=750:500;*,*"),
    place("The Coffee Bean", "Simple and cozy coffee-shop escape.", "https://b.zmtcdn.com/data/pictures/chains/4/120934/eaa7e5c9b2e1964315279fc48f396339_featured_v2.jpg"),
    place("Cafe Sicily", "Italian-inspired Chandigarh cafe.", "https://www.shoutlo.com/assets/images/merchant_images/merchant-173502-5de64f6e78bc5.jpg"),
    place("Brew Bros", "Coffee and conversation-focused cafe.", "https://www.shoutlo.com/assets/images/merchant_images/merchant-162722-677bb712631bd.jpg"),
    place("Tan Coffee", "Modern cafe popular for work sessions.", "https://lh3.googleusercontent.com/gps-cs-s/APNQkAGI3xIh6z78JABveDeQsaif7F8pJYPD1VeKhdDVDw_iJ4fApMcxVgdYWE5B6O9Afns9CDJxqcGvfsiraJeDyWINfkYG1gPxlu2luBZ70CjvTU2TWtecyKQwan3mFXemdffu0Esh=s680-w680-h510-rw"),
    place("Mocha Chandigarh", "Stylish cafe with desserts and shakes.", "https://i.pinimg.com/736x/39/d6/ff/39d6ffd0765e3b2e58adcf0f6c389e83.jpg")
),

// 30 Hotels
List.of(
    place("Hyatt Regency", "Luxury stay with polished rooms and dining.", "https://media-cdn.tripadvisor.com/media/photo-s/30/4d/d8/e8/exterior.jpg"),
    place("JW Marriott", "Premium business and leisure hotel.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/33/27/0e/44/hotel-facade.jpg?w=900&h=500&s=1"),
    place("Hotel Icon", "Comfortable boutique hotel in central Chandigarh.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/06/f3/a8/71/hotel-icon.jpg?w=900&h=500&s=1"),
    place("Taj Chandigarh", "Luxury hospitality in a prime location.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSYQ_UiI7mjlmPCUtYJqTZ0Ls3Qq-EGNckXzg&s"),
    place("Hotel Mountview", "Classic government-run luxury property.", "https://pix10.agoda.net/hotelImages/1020737/-1/127991be9e13c6e50ab18d0a52a3c543.jpg?ca=7&ce=1&s=414x232"),
    place("Radisson Zirakpur", "Modern hotel near Chandigarh airport.", "https://media.radissonhotels.net/image/radisson-hotel-chandigarh-zirakpur/exteriorview/16256-114073-f65618606_3xl.jpg?impolicy=HomeHero"),
    place("Novotel Chandigarh", "Contemporary hotel with upscale facilities.", "https://media-cdn.tripadvisor.com/media/photo-s/1e/40/c4/d0/novotel-chandigarh-tribune.jpg"),
    place("Holiday Inn", "Reliable premium stay for travelers.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ7gfz1ZUvNmiH71B-nOxoF17Ks1qrr-uKyPw&s"),
    place("Lemon Tree Hotel", "Comfortable hotel with modern interiors.", "https://media-cdn.tripadvisor.com/media/photo-s/1c/25/c4/1b/lemon-tree-hotel-chandigarh.jpg"),
    place("Ramada Plaza", "Elegant hotel popular for events and stays.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/0a/89/f6/d1/exterior.jpg?w=900&h=500&s=1"),
    place("Park Plaza", "Business-friendly luxury accommodation.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/05/c1/0e/61/park-plaza-zirakpur.jpg?w=900&h=500&s=1"),
    place("The Lalit Chandigarh", "High-end hotel with premium amenities.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/06/20/30/be/the-lalit-chandigarh.jpg?w=900&h=500&s=1"),
    place("Hotel Shivalikview", "Government-owned stay with spacious rooms.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSu82b5zWvo9kbQV-oG-U6K6fhs8KlaW8Dq2A&s"),
    place("Hotel Emerald", "Comfortable mid-range accommodation.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/12/96/ab/39/family-room.jpg?w=900&h=500&s=1"),
    place("Hotel Orbit", "Popular city hotel with modern decor.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTYFHnpm9mHZNPDwL1edIoqw9Fb5iHBG5lArQ&c 1s"),
    place("Hotel Western Court", "Business-class stay in Chandigarh.", "https://gos3.ibcdn.com/5b345688-4c3a-42b4-8d0e-73481b7cb556.jpeg"),
    place("Hotel City Heart", "Budget-friendly city-center hotel.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/33/25/7e/c5/caption.jpg?w=900&h=-1&s=1"),
    place("Velvet Clarks Exotica", "Luxury-style modern property.", "https://pix10.agoda.net/hotelImages/735299/0/158db6dc9dff64b8c3d69cbe75c4c54f.jpeg?ce=0&s=414x232"),
    place("Glades Hotel", "Stylish Zirakpur hotel with premium rooms.", "https://cf.bstatic.com/xdata/images/hotel/max1024x768/92964560.jpg?k=f8bcdf0030d4f6811571dccf7370c6ff937af553a56ae41c812d0270b9c49761&o="),
    place("Best Western Maryland", "Elegant hotel with business facilities.", "https://images.bestwestern.com/bwi/brochures/76559/photos/1024/11806967.jpg"),
    place("Hotel Aquamarine", "Modern interiors and central location.", "https://gos3.ibcdn.com/141258e8c9e611ebb0520242ac110002.jpeg"),
    place("Hotel KLG", "Affordable luxury hotel option.", "https://cf.bstatic.com/xdata/images/hotel/max1024x768/505963839.jpg?k=dbb9d27b77358fb95a5e66b763011858110ae7fdada44a09b9bce81b296f961f&o="),
    place("Hotel Palacio", "Comfortable boutique-style accommodation.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/28/a9/85/d6/hotel-palacio.jpg?w=900&h=-1&s=1"),
    place("Hotel Turquoise", "Popular hotel near transport hubs.", "https://media-cdn.tripadvisor.com/media/photo-s/2b/cf/85/57/very-passionately-crafted.jpg"),
    place("The Fern Residency", "Eco-friendly hotel with modern services.", "https://gos3.ibcdn.com/15b24b328f7211ee91e90a58a9feac02.jpg"),
    place("Hotel Diamond Plaza", "Budget stay with convenient access.", "https://ik.imgkit.net/3vlqs5axxjf/external/ik-seo/https://media.iceportal.com/142772/photos/85344772_XL/Hyatt-Centric-Sector-17-Chandigarh.jpg?tr=w-300%2Ch-180%2Cfo-auto"),
    place("Hotel Royal Park", "Elegant rooms with classic interiors.", "https://gos3.ibcdn.com/c302d6da453611eda5430a58a9feac02.jpeg")
   
),
              // 30 Tourist Places
List.of(
    place("Capitol Complex", "UNESCO-listed architectural landmark of Chandigarh.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTpm42T3STzaQbaxz-U98Mp9g7iI5wTUCl8Yw&s"),
    place("Open Hand Monument", "Le Corbusier's symbolic masterpiece.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/12/28/64/f3/open-hand-monument.jpg?w=1200&h=-1&s=1"),
    place("Government Museum and Art Gallery", "Museum with Gandharan and Indian art collections.", "https://chdmuseum.gov.in/images/govt-museum-and-art-gallery.jpg"),
    place("Leisure Valley", "Green urban landscape perfect for walks.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQwGGp9dq0u5jtxz2R7rinqNHY69ABAXy2Tmg&s"),
    place("Rock Garden", "Famous sculpture garden made from recycled materials.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/09/4c/43/64/the-rock-garden-of-chandigarh.jpg?w=900&h=500&s=1"),
    place("Sukhna Lake", "Popular lake destination for boating and sunsets.", "https://d34vm3j4h7f97z.cloudfront.net/optimized/4X/a/b/e/abef7c523b1454bf02d2c9b7b8f976c918342d90_2_690x460.jpeg"),
    place("Rose Garden", "Asia's largest rose garden with seasonal blooms.", "https://www.citywoofer.com/blog/wp-content/uploads/2023/02/WhatsApp-Image-2023-02-08-at-8.33.25-PM-756x400.jpeg"),
    place("Japanese Garden", "Beautiful Japanese-inspired landscaped park.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQBlEEt1UynoNzUzsqm_kJ97FYe6h1afmbGzA&s"),
    place("Garden of Silence", "Peaceful Buddha-themed tourist attraction.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSIQ-vXQzTNal2KDJWHlv0AyfK3Te_7VxXCcg&s"),
    place("Terraced Garden", "Floral garden with fountains and greenery.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRjDVvBWUIPwvLKIzPtTomON_mxx6s4hxLM-g&s"),
    place("Pinjore Gardens", "Historic Mughal-style garden near Chandigarh.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRaaMgwUDbSTk5BqM0_wY1or8UZBIsM07_K2A&s"),
    place("Nada Sahib Gurudwara", "Popular riverside Sikh pilgrimage site.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/14/80/4b/25/majestic-entrance-of.jpg?w=900&h=500&s=1"),
    place("Morni Hills", "Nearby hill station escape with scenic views.", "https://images.travelandleisureasia.com/wp-content/uploads/sites/2/2024/06/26153443/HIFI-Ariel-view-of-Tikkar-Taal-from-Morni-Hills-The-Himalyan-Lens-Shutterstock-1600x900.jpg"),
    place("Timber Trail", "Cable-car hill destination near Chandigarh.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/0a/8f/ef/f0/timber-trail.jpg?w=1200&h=-1&s=1"),
    place("ChhatBir Zoo", "Wildlife park and safari attraction.", "https://s7ap1.scene7.com/is/image/incredibleindia/chhatbir-zoo-chandigarh-2-attr-hero?qlt=82&ts=1742184872126"),
    place("Cactus Garden", "Largest cactus garden in Asia.", "https://hblimg.mmtcdn.com/content/hubble/img/panchkulatiowimages/mmt/activities/m_Cactus_Garden_1_l_480_640.jpg"),
    place("Butterfly Park", "Nature park filled with butterflies and flowers.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTFcWvbiPLf2abG6x16Q19QfwAZyot7m_Ub8A&s"),
    place("Shanti Kunj", "Green peaceful garden space in the city.", "https://www.holidify.com/images/cmsuploads/compressed/shanti-kunj-park-chandigarh-tourism-entry-fee-timings-holidays-reviews-header_20220117140331.jpeg"),
    place("Topiary Park", "Garden with creatively shaped bushes and plants.", "https://www.shoutlo.com/assets/images/merchant_images/merchant-181055-610e7f574f4fb.jpg"),
    place("Sector 17 Plaza", "Open-air plaza and shopping destination.", "https://upload.wikimedia.org/wikipedia/commons/7/75/Sector-17_chandigarh.jpg"),
    place("Elante Mall", "Entertainment and shopping hotspot.", "https://images.unsplash.com/photo-1519567241046-7f570eee3ce6?w=900&q=80https://images.unsplash.com/photo-1519567241046-7f570eee3ce6?w=900&q=80"),
    place("VR Punjab Mall", "Large mall destination near Chandigarh.", "https://hblimg.mmtcdn.com/content/hubble/img/ttd_images/mmt/activities/m_Mohali_Vr_punjab_mall_1_l_480_640.jpg"),

    place("Funcity", "Family-friendly amusement park.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/07/89/42/2e/funcity.jpg?w=1200&h=1200&s=1"),
    place("Bougainvillea Garden", "Colorful floral tourist attraction.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/13/59/0e/a3/bougainvillea-super-gazebo.jpg?w=1200&h=1200&s=1"),
    place("International Dolls Museum", "Museum displaying dolls from around the world.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/13/59/0e/a3/bougainvillea-super-gazebo.jpg?w=1200&h=1200&s=1"),
    place("Children Traffic Park", "Educational outdoor activity park.", "https://vushii.com/uploads/922439253_Children%20Traffic%20Park%20Chandigarh.jpg"),
    place("Siswan Dam", "Scenic short-drive tourist spot.", "https://gos3.ibcdn.com/96e8f768-edd0-4a41-b0c8-470db652319d.jpg"),
    place("Lake Club", "Lakeside recreational tourist attraction.", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=900&q=80https://www.rozanaspokesman.com/cover/prev/dakdu0qotvsbvec4tvkqndb9f7-20181206220303.Medi.jpeg")
),

              // 30 Educational + Learning + Museum Places
List.of(
    place("Panjab University", "Prestigious university known for academics and modernist architecture.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/09/9c/5f/d4/stuc-pu-largejpg.jpg?w=1200&h=-1&s=1"),
    place("PGIMER", "Top medical and research institute in India.", "https://image-static.collegedunia.com/public/college_data/images/appImage/10842_MEDICAL_NEW.jpg"),
    place("Government Museum and Art Gallery", "Educational stop with archaeology, miniatures, and art collections.", "https://image-static.collegedunia.com/public/college_data/images/appImage/10842_MEDICAL_NEW.jpg"),
    place("Chandigarh College of Architecture", "Renowned institution for architecture and urban design.", "https://images.shiksha.com/mediadata/images/1563372146phpzQJC7r.jpeg"),
    place("PEC Chandigarh", "Leading engineering and technology institute.", "https://media.collegedekho.com/media/img/institute/crawled_images/PEC3.png"),
    place("NIPER Mohali", "Premier pharmaceutical and scientific research institute.", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRR5Mgfe5HBM-k4tOCd315hHZyCP0pzrf_uSw&s"),
    place("ISB Mohali", "Globally recognized business school campus.", "https://live.staticflickr.com/65535/52630469653_769d68d57d.jpg"),
    place("British Library Chandigarh", "Learning and reading resource center.", "https://www.shoutlo.com/assets/images/merchant_images/merchant-175609-5d6a6761eb1c5.jpg"),
    place("State Library Chandigarh", "Quiet study and reading environment.", "https://www.shoutlo.com/assets/images/merchant_images/merchant-130852-5d6e188ccf14f.jpg"),
    place("International Dolls Museum", "Unique museum with global doll collections.", "https://www.shoutlo.com/assets/images/merchant_images/merchant-130852-5d6e188ccf14f.jpg"),
    place("Natural History Museum", "Educational museum focused on wildlife and evolution.", "https://chandigarhtourism.gov.in/uploads/nhm.jpg"),
    place("Architecture Museum Chandigarh", "Learning-focused museum about city planning and design.", "https://chandigarhtourism.gov.in/uploads/_1580985224.jpg"),
    place("Le Corbusier Centre", "Exhibition space exploring Chandigarh's architectural story.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/10/bd/10/3c/displays-of-rare-images.jpg?w=1200&h=-1&s=1"),
    place("Science Museum Chandigarh", "Interactive science and innovation exhibits.", "https://content.jdmagicbox.com/comp/def_content_category/science-museums/b4edc8b24d-science-museums-3-qa81e.jpg"),
    place("Museum of Evolution of Life", "Educational attraction focused on prehistoric life and science.", "https://www.shoutlo.com/assets/images/merchant_images/merchant-124149-64a7bab5cf79e.jpg")
),
                List.of("Strong urban planning and clean-city feel", "Excellent short-break city for food and walks", "Good mix of architecture, gardens, and modern cafes")));

        data.put("udaipur", curatedCity(
                "udaipur", "Udaipur", "Lake palaces, romantic views, old-city lanes, rooftop dining, and heritage hotels.",
                "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/30/8e/f1/66/hotel-facade.jpg?w=900&h=-1&s=1", "Rajasthan",
                List.of(new QuickFact("Lake City", "Identity"), new QuickFact("Palaces", "Royal"), new QuickFact("Sunset", "Views")),
                List.of(
                        place("City Palace", "The defining royal complex above Lake Pichola.", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=900&q=80"),
                        place("Lake Pichola", "Boat rides and reflective evening views.", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=900&q=80"),
                        place("Jagdish Temple", "Historic temple at the center of the old city.", "https://images.unsplash.com/photo-1548013146-72479768bada?w=900&q=80"),
                        place("Saheliyon Ki Bari", "Garden retreat with fountains, marble pavilions, and quiet royal charm.", "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=900&q=80"),
                        place("Jagmandir", "Lake island palace known for boat rides and elegant evening views.", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=900&q=80"),
                        place("Ambrai Ghat", "Popular lakeside sunset point facing the palaces and old city.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80")
                ),
                List.of(
                        place("Jheel's Ginger Coffee Bar", "Traveler-favorite cafe with lake-city charm.", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"),
                        place("Cafe Edelweiss", "Relaxed coffee and dessert stop in the old town.", "https://images.unsplash.com/photo-1541167760496-1628856ab772?w=900&q=80"),
                        place("Millets of Mewar", "Health-focused cafe with local popularity.", "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=900&q=80"),
                        place("Upre by 1559 AD", "Rooftop dining spot with lake views and relaxed evening appeal.", "https://images.unsplash.com/photo-1559925393-8be08e16738e?w=900&q=80"),
                        place("Tribute Restaurant", "Lakeside restaurant known for Rajasthani food and calm ambience.", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=900&q=80"),
                        place("Sun and Moon Rooftop Cafe", "Old-city rooftop cafe for snacks, coffee, and palace-facing views.", "https://images.unsplash.com/photo-1521017432531-fbd92d768814?w=900&q=80")
                ),
                List.of(
                        place("Taj Lake Palace", "Legendary luxury hotel set within the lake.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
                        place("The Oberoi Udaivilas", "Top-tier heritage-inspired luxury stay.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
                        place("Trident Udaipur", "Comfortable premium stay near major attractions.", "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=900&q=80"),
                        place("The Leela Palace Udaipur", "Luxury lakefront property with polished palace-style service.", "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?w=900&q=80"),
                        place("Fateh Prakash Palace", "Heritage stay inside the palace complex with regal interiors.", "https://images.unsplash.com/photo-1564501049412-61c2a3083791?w=900&q=80"),
                        place("Amet Haveli", "Boutique heritage stay close to Ambrai Ghat and lake views.", "https://images.unsplash.com/photo-1590490360182-c33d57733427?w=900&q=80")
                ),
                List.of(
                        place("Sajjangarh Monsoon Palace", "Sunset point above the city's lakes.", "https://images.unsplash.com/photo-1524492514790-831f5b49c3dd?w=900&q=80"),
                        place("Bagore Ki Haveli", "Cultural venue with evening performances.", "https://images.unsplash.com/photo-1518709268805-4e9042af2176?w=900&q=80"),
                        place("Fateh Sagar Lake", "Popular local leisure and boating zone.", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=900&q=80"),
                        place("Shilpgram", "Craft village for folk art, local shopping, and cultural events.", "https://images.unsplash.com/photo-1481437156560-3205f6a55735?w=900&q=80"),
                        place("Sajjangarh Biological Park", "Wildlife park near the Monsoon Palace for family outings.", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=900&q=80"),
                        place("Badi Lake", "Quieter scenic lake escape with open hills and sunset views.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80")
                ),
                List.of(
                        place("Mohanlal Sukhadia University", "Major higher-education campus serving southern Rajasthan.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
                        place("Maharana Pratap University of Agriculture and Technology", "Important academic institution focused on agriculture and engineering.", "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=900&q=80"),
                        place("Pacific University", "Private university campus with professional programs.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
                        place("City Palace Museum", "Museum section preserving royal collections and Mewar history.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
                        place("Bharatiya Lok Kala Mandal", "Cultural museum and performance center for folk traditions.", "https://images.unsplash.com/photo-1518709268805-4e9042af2176?w=900&q=80"),
                        place("Ahar Museum", "Archaeological museum and cenotaph complex for regional history.", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=900&q=80")
                ),
                List.of("High-value heritage and romance destination", "Excellent palace hotels and roof dining", "Great for photography and relaxed sightseeing")));

        data.put("jaisalmer", curatedCity(
                "jaisalmer", "Jaisalmer", "Golden fort city, desert camps, sandstone havelis, and sunset dune escapes.",
                "https://www.thepalaceonwheels.org/storage/jaislamer_fort_night_view_1914%20(1).jpg", "Rajasthan",
                List.of(new QuickFact("Golden Fort", "Identity"), new QuickFact("Desert", "Adventure"), new QuickFact("Havelis", "Craft")),
                List.of(
                        place("Jaisalmer Fort", "Living fort packed with homes, shops, and temples.", "https://images.unsplash.com/photo-1577083552431-6e5fd01aa342?w=900&q=80"),
                        place("Patwon Ki Haveli", "Detailed sandstone mansion cluster and photo stop.", "https://images.unsplash.com/photo-1595815771614-ade9d652a65d?w=900&q=80"),
                        place("Gadisar Lake", "Calm waterbody and traditional architecture stop.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80"),
                        place("Nathmal Ki Haveli", "Ornate merchant mansion known for delicate sandstone craft.", "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=900&q=80"),
                        place("Salim Singh Ki Haveli", "Historic haveli with distinctive balconies and old-city character.", "https://images.unsplash.com/photo-1518709268805-4e9042af2176?w=900&q=80"),
                        place("Jain Temples", "Intricately carved temple cluster inside Jaisalmer Fort.", "https://images.unsplash.com/photo-1548013146-72479768bada?w=900&q=80")
                ),
                List.of(
                        place("The Traveler's Cup", "Comfort cafe for coffee, snacks, and rooftop breaks.", "https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=900&q=80"),
                        place("Cafe The Kaku", "Popular old-city cafe with fort views.", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"),
                        place("Golden Roof Cafe", "Easygoing meal stop for tourists exploring the fort.", "https://images.unsplash.com/photo-1541167760496-1628856ab772?w=900&q=80"),
                        place("KB Cafe", "Rooftop cafe inside the fort with easy sightseeing breaks.", "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=900&q=80"),
                        place("German Bakery and Coffee Shop", "Reliable casual stop for breakfast, coffee, and baked snacks.", "https://images.unsplash.com/photo-1521017432531-fbd92d768814?w=900&q=80"),
                        place("Desert Boy's Dhani", "Traditional dining space with local food and folk ambience.", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=900&q=80")
                ),
                List.of(
                        place("Suryagarh", "Luxury desert-edge property with immersive ambiance.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
                        place("Fort Rajwada", "Premium stay with traditional design language.", "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?w=900&q=80"),
                        place("Desert Camp Sam", "Classic dune-side stay for evening cultural shows.", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=900&q=80"),
                        place("Jaisalmer Marriott Resort and Spa", "Modern premium hotel with comfortable resort facilities.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
                        place("WelcomHeritage Mandir Palace", "Heritage palace hotel close to the city center.", "https://images.unsplash.com/photo-1564501049412-61c2a3083791?w=900&q=80"),
                        place("Hotel Garh Jaisal Haveli", "Characterful haveli stay near the fort with city views.", "https://images.unsplash.com/photo-1590490360182-c33d57733427?w=900&q=80")
                ),
                List.of(
                        place("Sam Sand Dunes", "Desert safari and sunset experience outside the city.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80"),
                        place("Kuldhara", "Abandoned village with local legend appeal.", "https://images.unsplash.com/photo-1518709268805-4e9042af2176?w=900&q=80"),
                        place("Bada Bagh", "Chhatri cenotaphs with dramatic golden-hour views.", "https://images.unsplash.com/photo-1524492514790-831f5b49c3dd?w=900&q=80"),
                        place("Desert National Park", "Protected desert landscape for wildlife and dune scenery.", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=900&q=80"),
                        place("Tanot Mata Temple", "Border-area temple often paired with desert road trips.", "https://images.unsplash.com/photo-1548013146-72479768bada?w=900&q=80"),
                        place("Longewala War Memorial", "Historic military memorial on the Jaisalmer excursion circuit.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80")
                ),
                List.of(
                        place("Government College Jaisalmer", "Key local college serving higher education in the district.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
                        place("Jaisalmer War Museum", "Military history museum explaining regional border heritage.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
                        place("Desert Culture Centre and Museum", "Museum focused on desert life, crafts, and local traditions.", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=900&q=80"),
                        place("Folklore Museum", "Compact cultural museum preserving folk objects and stories.", "https://images.unsplash.com/photo-1518709268805-4e9042af2176?w=900&q=80"),
                        place("Jaisalmer Fort Palace Museum", "Heritage museum inside the fort complex.", "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=900&q=80"),
                        place("Amar Sagar Jain Temple", "Historic temple site useful for architecture and heritage learning.", "https://images.unsplash.com/photo-1548013146-72479768bada?w=900&q=80")
                ),
                List.of("Fort plus desert tourism in one trip", "Strong heritage and camp-stay appeal", "Best for sunsets, photos, and atmospheric stays")));

        data.put("goa", curatedCity(
                "goa", "Goa", "Beach days, nightlife, cafes, Portuguese lanes, resorts, and easy coastal escapes.",
                "https://www.tourmyindia.com/states/goa/image/beaches-goa.webp", "Goa",
                List.of(new QuickFact("Beaches", "Famous"), new QuickFact("Nightlife", "Strong"), new QuickFact("Resorts", "Popular")),
                List.of(
                        place("Baga Beach", "Busy beach with nightlife and water sports.", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=900&q=80"),
                        place("Calangute Beach", "Classic Goa stretch with broad visitor appeal.", "https://images.unsplash.com/photo-1473116763249-2faaef81ccda?w=900&q=80"),
                        place("Fontainhas", "Colorful heritage quarter with Portuguese character.", "https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?w=900&q=80"),
                        place("Basilica of Bom Jesus", "UNESCO-listed church and one of Goa's major heritage sites.", "https://images.unsplash.com/photo-1548013146-72479768bada?w=900&q=80"),
                        place("Fort Aguada", "Seafront fort and lighthouse with strong coastal views.", "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=900&q=80"),
                        place("Palolem Beach", "Scenic South Goa beach known for a calmer coastal vibe.", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=900&q=80")
                ),
                List.of(
                        place("Artjuna", "Goa cafe staple for brunch, coffee, and design-led vibes.", "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=900&q=80"),
                        place("Baba Au Rhum", "Beloved breakfast and bakery stop in North Goa.", "https://images.unsplash.com/photo-1521017432531-fbd92d768814?w=900&q=80"),
                        place("Mojigao", "Green and stylish cafe for slow mornings.", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"),
                        place("Cafe Chocolatti", "Garden cafe known for breakfast, desserts, and relaxed service.", "https://images.unsplash.com/photo-1541167760496-1628856ab772?w=900&q=80"),
                        place("Eva Cafe", "Beachside cafe with sunset views and Mediterranean-style bites.", "https://images.unsplash.com/photo-1559925393-8be08e16738e?w=900&q=80"),
                        place("Thalassa", "Popular cliffside dining spot with Greek food and party energy.", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=900&q=80")
                ),
                List.of(
                        place("Taj Exotica", "Luxury beach resort with broad leisure appeal.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
                        place("W Goa", "Design-forward premium stay with nightlife energy.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
                        place("The Leela Goa", "High-end resort experience with large grounds.", "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=900&q=80"),
                        place("Cidade de Goa", "Well-known beachfront resort close to Panaji.", "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?w=900&q=80"),
                        place("ITC Grand Goa", "Luxury resort option with beach access and large landscaped grounds.", "https://images.unsplash.com/photo-1564501049412-61c2a3083791?w=900&q=80"),
                        place("Alila Diwa Goa", "Premium South Goa resort with quiet design-led atmosphere.", "https://images.unsplash.com/photo-1590490360182-c33d57733427?w=900&q=80")
                ),
                List.of(
                        place("Dudhsagar Falls", "Popular day trip for a different side of Goa.", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=900&q=80"),
                        place("Fort Aguada", "Seafront fort and historic lookout.", "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=900&q=80"),
                        place("Anjuna Flea Market", "Shopping and boho coastal culture stop.", "https://images.unsplash.com/photo-1481437156560-3205f6a55735?w=900&q=80"),
                        place("Chapora Fort", "Popular sunset viewpoint above Vagator and the coastline.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80"),
                        place("Reis Magos Fort", "Restored riverside fort with heritage exhibits and views.", "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=900&q=80"),
                        place("Sahakari Spice Farm", "Plantation visit for food, spice trails, and a greener Goa day.", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=900&q=80")
                ),
                List.of(
                        place("Goa University", "Primary public university campus serving the state.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
                        place("Goa State Museum", "Museum covering state history, art, and cultural objects.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
                        place("Museum of Christian Art", "Specialized museum focused on Indo-Portuguese sacred art.", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=900&q=80"),
                        place("Naval Aviation Museum", "Aviation museum with aircraft displays and naval history.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
                        place("Houses of Goa Museum", "Architecture museum explaining Goan homes and design identity.", "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=900&q=80"),
                        place("Goa Science Centre", "Interactive science museum suited for families and students.", "https://images.unsplash.com/photo-1507413245164-6160d8298b31?w=900&q=80")
                ),
                List.of("Easy blend of beaches, cafes, and nightlife", "Good hotel range from boutique to luxury", "Works well for both relaxed and high-energy trips")));

        data.put("mumbai", curatedCity(
                "mumbai", "Mumbai", "Sea-facing landmarks, cinema energy, luxury hotels, local trains, and nonstop city pace.",
                "https://media-cdn.tripadvisor.com/media/attractions-splice-spp-674x446/07/1b/c8/df.jpg", "Maharashtra",
                List.of(new QuickFact("Maximum", "City"), new QuickFact("Marine Drive", "Icon"), new QuickFact("Film", "Capital")),
                List.of(
                        place("Gateway of India", "One of the city's defining waterfront landmarks.", "https://images.unsplash.com/photo-1566554273541-37a9ca77b91f?w=900&q=80"),
                        place("Marine Drive", "Best long urban promenade for sunsets and skyline views.", "https://images.unsplash.com/photo-1526481280695-3c4691f3f22e?w=900&q=80"),
                        place("Bandra Bandstand", "Popular sea-facing walk with celebrity-neighborhood appeal.", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=900&q=80"),
                        place("Chhatrapati Shivaji Maharaj Terminus", "Historic railway landmark with striking Gothic architecture.", "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=900&q=80"),
                        place("Haji Ali Dargah", "Sea-linked shrine and one of Mumbai's most recognizable spiritual sites.", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=900&q=80"),
                        place("Siddhivinayak Temple", "Major devotional landmark visited by locals and travelers.", "https://images.unsplash.com/photo-1548013146-72479768bada?w=900&q=80")
                ),
                List.of(
                        place("Kala Ghoda Cafe", "Good stop near the art district and heritage core.", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"),
                        place("Leaping Windows", "Well-known Bandra cafe for books and comfort food.", "https://images.unsplash.com/photo-1521017432531-fbd92d768814?w=900&q=80"),
                        place("Subko", "Strong local favorite for specialty coffee and baked goods.", "https://images.unsplash.com/photo-1559925393-8be08e16738e?w=900&q=80"),
                        place("Prithvi Cafe", "Iconic Juhu cafe connected to theatre and creative circles.", "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=900&q=80"),
                        place("Britannia and Co", "Classic heritage eatery famous for Parsi food and old Bombay charm.", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=900&q=80"),
                        place("Candies", "Casual Bandra favorite for snacks, coffee, and colorful interiors.", "https://images.unsplash.com/photo-1541167760496-1628856ab772?w=900&q=80")
                ),
                List.of(
                        place("Taj Mahal Palace", "Historic luxury icon overlooking the harbor.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
                        place("The St. Regis Mumbai", "High-rise premium stay with central connectivity.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
                        place("The Oberoi Mumbai", "Luxury stay with Marine Drive access.", "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=900&q=80"),
                        place("Trident Nariman Point", "Business and leisure hotel with strong sea-facing access.", "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?w=900&q=80"),
                        place("JW Marriott Mumbai Juhu", "Beachside premium stay popular for leisure trips.", "https://images.unsplash.com/photo-1564501049412-61c2a3083791?w=900&q=80"),
                        place("ITC Maratha", "Luxury airport-area hotel with polished hospitality.", "https://images.unsplash.com/photo-1590490360182-c33d57733427?w=900&q=80")
                ),
                List.of(
                        place("Elephanta Caves", "Harbor day trip with major heritage interest.", "https://images.unsplash.com/photo-1542601906990-b4d3fb7780b9?w=900&q=80"),
                        place("Colaba Causeway", "Shopping and street culture around South Mumbai.", "https://images.unsplash.com/photo-1481437156560-3205f6a55735?w=900&q=80"),
                        place("Juhu Beach", "Classic open-air local hangout with snacks and sunset energy.", "https://images.unsplash.com/photo-1473116763249-2faaef81ccda?w=900&q=80"),
                        place("Sanjay Gandhi National Park", "Large urban forest escape with trails and nature breaks.", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=900&q=80"),
                        place("Kanheri Caves", "Ancient Buddhist cave complex inside the national park.", "https://images.unsplash.com/photo-1518709268805-4e9042af2176?w=900&q=80"),
                        place("Chor Bazaar", "Historic market known for antiques, curios, and old-city energy.", "https://images.unsplash.com/photo-1481437156560-3205f6a55735?w=900&q=80")
                ),
                List.of(
                        place("University of Mumbai", "Historic university with landmark architecture and major academic presence.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
                        place("IIT Bombay", "Premier technology institute with a large green campus.", "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=900&q=80"),
                        place("Chhatrapati Shivaji Maharaj Vastu Sangrahalaya", "Major museum for art, archaeology, and natural history.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
                        place("Nehru Science Centre", "Interactive science center for students and family visits.", "https://images.unsplash.com/photo-1507413245164-6160d8298b31?w=900&q=80"),
                        place("Dr Bhau Daji Lad Museum", "Restored museum focused on Mumbai's industrial and cultural history.", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=900&q=80"),
                        place("Asiatic Society Library", "Historic library and research landmark in South Mumbai.", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=900&q=80")
                ),
                List.of("Fast-paced city with strong sea-facing experiences", "Excellent luxury hospitality base", "Best for food, neighborhoods, and iconic urban sightseeing")));

        data.put("maharashtra", curatedCity(
                "maharashtra", "Maharashtra", "A broad state guide covering cities, coasts, hill escapes, forts, and cultural circuits.",
                "https://images.unsplash.com/photo-1473116763249-2faaef81ccda?w=1400&q=80", "Maharashtra State",
                List.of(new QuickFact("Mumbai", "Hub"), new QuickFact("Hill Stations", "Getaways"), new QuickFact("Forts", "Legacy")),
                List.of(
                        place("Ajanta Caves", "World-famous rock-cut cave complex with historic art.", "https://images.unsplash.com/photo-1542601906990-b4d3fb7780b9?w=900&q=80"),
                        place("Ellora Caves", "Monument-rich heritage site with extraordinary carvings.", "https://images.unsplash.com/photo-1518709268805-4e9042af2176?w=900&q=80"),
                        place("Mahabaleshwar", "Popular hill destination for cool weather and valley views.", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=900&q=80"),
                        place("Shirdi", "Major pilgrimage town centered around Sai Baba's shrine.", "https://images.unsplash.com/photo-1548013146-72479768bada?w=900&q=80"),
                        place("Lonavala", "Classic hill-station getaway with monsoon viewpoints.", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=900&q=80"),
                        place("Ganpatipule", "Coastal temple town with a quieter Konkan beach setting.", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=900&q=80")
                ),
                List.of(
                        place("Leopold Cafe", "Classic Maharashtra trip stop if based in Mumbai.", "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=900&q=80"),
                        place("Kayani Bakery", "Pune favorite for a state-level food detour.", "https://images.unsplash.com/photo-1541167760496-1628856ab772?w=900&q=80"),
                        place("Little Italy Lonavala", "Family-friendly hill-station dining and cafe break.", "https://images.unsplash.com/photo-1521017432531-fbd92d768814?w=900&q=80"),
                        place("German Bakery Pune", "Known cafe stop for brunch, bakery items, and relaxed seating.", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"),
                        place("Cafe Goodluck", "Historic Pune cafe famous for bun maska, tea, and casual meals.", "https://images.unsplash.com/photo-1559925393-8be08e16738e?w=900&q=80"),
                        place("Mapro Garden", "Mahabaleshwar cafe and retail stop known for strawberries.", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=900&q=80")
                ),
                List.of(
                        place("Taj Mahal Palace", "Premier luxury base within the state capital.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
                        place("Fariyas Lonavala", "Resort option for hill-station stays.", "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?w=900&q=80"),
                        place("The Westin Pune Koregaon Park", "Strong business and leisure option in Pune.", "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=900&q=80"),
                        place("Radisson Blu Resort Alibaug", "Coastal resort option for beach-side state itineraries.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
                        place("Le Meridien Mahabaleshwar", "Premium forested hill resort with valley access.", "https://images.unsplash.com/photo-1564501049412-61c2a3083791?w=900&q=80"),
                        place("The Corinthians Resort Pune", "Large resort-style stay suited for Pune leisure breaks.", "https://images.unsplash.com/photo-1590490360182-c33d57733427?w=900&q=80")
                ),
                List.of(
                        place("Lonavala", "Easy getaway with monsoon appeal and viewpoints.", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=900&q=80"),
                        place("Raigad Fort", "Strong Maratha history destination.", "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=900&q=80"),
                        place("Alibaug", "Coastal break for beaches and weekend escapes.", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=900&q=80"),
                        place("Tadoba Andhari Tiger Reserve", "Major wildlife destination for tiger-focused safaris.", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=900&q=80"),
                        place("Panchgani", "Hill destination known for viewpoints, table land, and boarding-school charm.", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=900&q=80"),
                        place("Elephanta Caves", "Harbor heritage site often combined with Mumbai travel.", "https://images.unsplash.com/photo-1542601906990-b4d3fb7780b9?w=900&q=80")
                ),
                List.of(
                        place("Savitribai Phule Pune University", "Major public university with a landmark campus.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
                        place("Deccan College", "Historic research institute known for archaeology and linguistics.", "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=900&q=80"),
                        place("Dr Babasaheb Ambedkar Marathwada University", "Important higher-education institution in Aurangabad.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
                        place("Symbiosis International University", "Well-known private university network based in Pune.", "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=900&q=80"),
                        place("Chhatrapati Shivaji Maharaj Vastu Sangrahalaya", "Major museum for art, history, and archaeology.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
                        place("Raja Dinkar Kelkar Museum", "Pune museum with decorative arts and historic collections.", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=900&q=80")
                ),
                List.of("Useful state-wide starter page for broader trip planning", "Includes heritage, coasts, cities, and hills", "Good complement to the dedicated Mumbai page")));

        data.put("kochi", curatedCity(
                "kochi", "Kochi", "Harbor heritage, art spaces, seafood, cafes, colonial streets, and backwater access.",
                "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/0d/de/f0/eb/backwater-tourism.jpg?w=700&h=-1&s=1", "Kerala",
                List.of(new QuickFact("Fort Kochi", "Historic"), new QuickFact("Backwaters", "Nearby"), new QuickFact("Art", "Growing")),
                List.of(
                        place("Fort Kochi", "The best entry point to Kochi's layered coastal history.", "https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?w=900&q=80"),
                        place("Chinese Fishing Nets", "The city's most recognizable waterfront image.", "https://images.unsplash.com/photo-1524492514790-831f5b49c3dd?w=900&q=80"),
                        place("Mattancherry Palace", "Important heritage stop with local royal history.", "https://images.unsplash.com/photo-1548013146-72479768bada?w=900&q=80"),
                        place("St Francis Church", "Historic church tied to Kochi's colonial past.", "https://images.unsplash.com/photo-1518709268805-4e9042af2176?w=900&q=80"),
                        place("Paradesi Synagogue", "Important heritage synagogue in the Mattancherry area.", "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=900&q=80"),
                        place("Hill Palace Museum", "Large palace museum complex with royal collections.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80")
                ),
                List.of(
                        place("Kashi Art Cafe", "One of Kochi's signature traveler-friendly cafes.", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"),
                        place("Loafers Corner", "Easy cafe stop near the sea and old streets.", "https://images.unsplash.com/photo-1521017432531-fbd92d768814?w=900&q=80"),
                        place("David Hall Cafe", "Heritage setting mixed with food and art energy.", "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=900&q=80"),
                        place("Pepper House Cafe", "Art-forward cafe in a heritage warehouse setting.", "https://images.unsplash.com/photo-1559925393-8be08e16738e?w=900&q=80"),
                        place("Qissa Cafe", "Popular Fort Kochi cafe for brunch, coffee, and relaxed meals.", "https://images.unsplash.com/photo-1541167760496-1628856ab772?w=900&q=80"),
                        place("Mocha Art Cafe", "Casual cafe stop with local art and coffeehouse comfort.", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=900&q=80")
                ),
                List.of(
                        place("Brunton Boatyard", "Heritage luxury stay in Fort Kochi.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
                        place("Taj Malabar Resort and Spa", "Harbor-facing upscale stay.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
                        place("Fragrant Nature Kochi", "Boutique comfort for heritage exploration.", "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=900&q=80"),
                        place("Grand Hyatt Kochi Bolgatty", "Large waterfront luxury hotel with resort facilities.", "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?w=900&q=80"),
                        place("Old Harbour Hotel", "Heritage boutique stay in the Fort Kochi core.", "https://images.unsplash.com/photo-1564501049412-61c2a3083791?w=900&q=80"),
                        place("Forte Kochi", "Restored boutique hotel with colonial-era character.", "https://images.unsplash.com/photo-1590490360182-c33d57733427?w=900&q=80")
                ),
                List.of(
                        place("Jew Town", "Antiques, spice stores, and old-market character.", "https://images.unsplash.com/photo-1481437156560-3205f6a55735?w=900&q=80"),
                        place("Marine Drive Kochi", "Simple urban waterfront walk in the newer city area.", "https://images.unsplash.com/photo-1526481280695-3c4691f3f22e?w=900&q=80"),
                        place("Kerala Kathakali Centre", "Strong cultural performance stop.", "https://images.unsplash.com/photo-1518709268805-4e9042af2176?w=900&q=80"),
                        place("Cherai Beach", "Accessible beach escape from Kochi with calm coastal appeal.", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=900&q=80"),
                        place("Bolgatty Palace", "Historic island palace area with harbor views.", "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=900&q=80"),
                        place("Wonderla Kochi", "Amusement park option for family-friendly day plans.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80")
                ),
                List.of(
                        place("Cochin University of Science and Technology", "Major science and technology university in Kochi.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
                        place("Kerala Folklore Museum", "Museum focused on Kerala art, costumes, and cultural history.", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=900&q=80"),
                        place("Hill Palace Museum", "Educational palace museum with archaeology and royal collections.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
                        place("Kerala Museum", "Cultural museum covering Kerala history and visual art.", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=900&q=80"),
                        place("Maharaja's College", "Historic college campus in the heart of Ernakulam.", "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=900&q=80"),
                        place("Sree Sankaracharya University of Sanskrit", "Nearby university focused on language, arts, and culture.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80")
                ),
                List.of("Excellent culture-meets-coast destination", "Strong cafe and boutique hotel scene", "Good base for backwaters and heritage walking")));

        data.put("darjeeling", curatedCity(
                "darjeeling", "Darjeeling", "Tea gardens, toy train views, mountain weather, colonial stays, and Himalayan scenery.",
                "https://s7ap1.scene7.com/is/image/incredibleindia/2-summer-capital-of-India-darjeeling-west-bengal-city-ff?qlt=82&ts=1726643695016", "West Bengal",
                List.of(new QuickFact("Tea", "World-famous"), new QuickFact("Toy Train", "Heritage"), new QuickFact("Kanchenjunga", "Views")),
                List.of(
                        place("Tiger Hill", "Best-known sunrise point for mountain panoramas.", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=900&q=80"),
                        place("Darjeeling Himalayan Railway", "Iconic narrow-gauge rail experience.", "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=900&q=80"),
                        place("Batasia Loop", "Rail heritage stop with open mountain views.", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=900&q=80"),
                        place("Padmaja Naidu Himalayan Zoological Park", "High-altitude zoo known for red pandas and snow leopards.", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=900&q=80"),
                        place("Japanese Peace Pagoda", "Quiet landmark with spiritual atmosphere and valley views.", "https://images.unsplash.com/photo-1542601906990-b4d3fb7780b9?w=900&q=80"),
                        place("Observatory Hill", "Hilltop sacred area with views and old-town character.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80")
                ),
                List.of(
                        place("Glenary's", "Classic bakery cafe and one of the town's staples.", "https://images.unsplash.com/photo-1541167760496-1628856ab772?w=900&q=80"),
                        place("Nathmull's Tea Room", "Tea-first stop for the full Darjeeling mood.", "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=900&q=80"),
                        place("Sonam's Kitchen", "Small, friendly breakfast stop popular with travelers.", "https://images.unsplash.com/photo-1521017432531-fbd92d768814?w=900&q=80"),
                        place("Keventer's", "Classic breakfast and rooftop-view stop on the Mall.", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=900&q=80"),
                        place("Tom and Jerry's", "Small cafe known for pancakes, coffee, and traveler energy.", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"),
                        place("Himalayan Java", "Modern coffee stop for a relaxed hill-town break.", "https://images.unsplash.com/photo-1559925393-8be08e16738e?w=900&q=80")
                ),
                List.of(
                        place("Mayfair Darjeeling", "Known heritage-style stay in the hill town.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
                        place("Windamere Hotel", "Colonial-era charm with strong location value.", "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=900&q=80"),
                        place("Summit Swiss Heritage", "Comfortable heritage-style lodging option.", "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?w=900&q=80"),
                        place("The Elgin Darjeeling", "Elegant heritage hotel with classic hill-station character.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
                        place("Cedar Inn", "Comfortable stay known for views and quiet hill ambience.", "https://images.unsplash.com/photo-1564501049412-61c2a3083791?w=900&q=80"),
                        place("Ramada Darjeeling", "Central hotel option close to the Mall and town walks.", "https://images.unsplash.com/photo-1590490360182-c33d57733427?w=900&q=80")
                ),
                List.of(
                        place("Happy Valley Tea Estate", "Tea-garden stop tied closely to local identity.", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=900&q=80"),
                        place("Peace Pagoda", "Quiet viewpoint and meditation-friendly landmark.", "https://images.unsplash.com/photo-1542601906990-b4d3fb7780b9?w=900&q=80"),
                        place("Chowrasta", "Town-center promenade for local atmosphere.", "https://images.unsplash.com/photo-1481437156560-3205f6a55735?w=900&q=80"),
                        place("Rock Garden", "Terraced garden and waterfall area for short local outings.", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=900&q=80"),
                        place("Ghoom Monastery", "Important monastery near the toy-train route.", "https://images.unsplash.com/photo-1542601906990-b4d3fb7780b9?w=900&q=80"),
                        place("Tinchuley", "Nearby village escape known for quiet views and homestays.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80")
                ),
                List.of(
                        place("St Joseph's College", "Historic college campus with strong academic presence.", "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=900&q=80"),
                        place("Loreto College", "Well-known educational institution in the hill town.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
                        place("Himalayan Mountaineering Institute", "Training institute and museum focused on mountaineering history.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
                        place("Darjeeling Government College", "Public college serving higher education in the region.", "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=900&q=80"),
                        place("Darjeeling Natural History Museum", "Museum with regional natural-history collections.", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=900&q=80"),
                        place("Tibetan Refugee Self Help Centre", "Cultural learning stop for crafts, history, and community work.", "https://images.unsplash.com/photo-1481437156560-3205f6a55735?w=900&q=80")
                ),
                List.of("Strong mountain-town identity", "Easy mix of heritage and scenery", "Excellent for tea, views, and soft-paced travel")));

        data.put("leh", curatedCity(
                "leh", "Leh", "High-altitude monasteries, dramatic roads, mountain cafes, and Ladakh adventure circuits.",
                "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/29/d7/5d/55/caption.jpg?w=800&h=800&s=1", "Ladakh",
                List.of(new QuickFact("High Altitude", "Adventure"), new QuickFact("Monasteries", "Culture"), new QuickFact("Road Trips", "Famous")),
                List.of(
                        place("Shanti Stupa", "Key panoramic viewpoint above Leh town.", "https://images.unsplash.com/photo-1524492514790-831f5b49c3dd?w=900&q=80"),
                        place("Leh Palace", "Historic hilltop palace and cultural marker.", "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=900&q=80"),
                        place("Thiksey Monastery", "Large monastery complex outside the city.", "https://images.unsplash.com/photo-1542601906990-b4d3fb7780b9?w=900&q=80"),
                        place("Hemis Monastery", "Major Ladakhi monastery known for festivals and heritage.", "https://images.unsplash.com/photo-1518709268805-4e9042af2176?w=900&q=80"),
                        place("Shey Palace", "Historic palace and monastery complex on the Leh route.", "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=900&q=80"),
                        place("Hall of Fame", "Museum and memorial focused on Ladakh and the Indian Army.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80")
                ),
                List.of(
                        place("Bon Appetit", "Popular Leh cafe with mountain-facing setting.", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"),
                        place("The Tibetan Kitchen", "Well-liked stop for simple meals and local feel.", "https://images.unsplash.com/photo-1521017432531-fbd92d768814?w=900&q=80"),
                        place("Lehvenda Cafe", "Good casual stop after town walks.", "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=900&q=80"),
                        place("German Bakery Leh", "Traveler-friendly bakery for coffee, breakfast, and quick snacks.", "https://images.unsplash.com/photo-1541167760496-1628856ab772?w=900&q=80"),
                        place("OpenHand Cafe", "Calm cafe and craft space with ethical-shopping appeal.", "https://images.unsplash.com/photo-1559925393-8be08e16738e?w=900&q=80"),
                        place("Lamayuru Restaurant", "Popular town restaurant for Tibetan and Ladakhi comfort meals.", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=900&q=80")
                ),
                List.of(
                        place("The Grand Dragon Ladakh", "One of Leh's strongest premium stays.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
                        place("Ladakh Sarai", "Atmospheric stay with a wider landscape feel.", "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=900&q=80"),
                        place("Hotel Sten-Del", "Dependable central hotel option.", "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?w=900&q=80"),
                        place("The Zen Ladakh", "Resort-style Leh stay with gardens and premium facilities.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
                        place("Chamba Camp Thiksey", "Luxury tented stay near monastery landscapes.", "https://images.unsplash.com/photo-1564501049412-61c2a3083791?w=900&q=80"),
                        place("Gomang Boutique Hotel", "Warm boutique hotel option with mountain-town comfort.", "https://images.unsplash.com/photo-1590490360182-c33d57733427?w=900&q=80")
                ),
                List.of(
                        place("Magnetic Hill", "Popular roadside attraction on the Leh circuit.", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=900&q=80"),
                        place("Nubra Valley", "Major day or overnight trip from Leh.", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=900&q=80"),
                        place("Pangong Lake", "One of the most sought-after scenic excursions.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80"),
                        place("Khardung La", "Famous high mountain pass on the Nubra route.", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=900&q=80"),
                        place("Tso Moriri", "Remote high-altitude lake for longer Ladakh itineraries.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80"),
                        place("Zanskar Valley", "Adventure-heavy region known for dramatic landscapes and road trips.", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=900&q=80")
                ),
                List.of(
                        place("Central Institute of Buddhist Studies", "Important institute for Buddhist studies and Himalayan learning.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
                        place("University of Ladakh", "Key higher-education institution for the Ladakh region.", "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=900&q=80"),
                        place("Ladakh Arts and Media Organisation", "Cultural space supporting heritage, art, and community learning.", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=900&q=80"),
                        place("Hall of Fame Museum", "Museum presenting Ladakh history, terrain, and military stories.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
                        place("Lamdon Model Senior Secondary School", "Known school campus serving local students in Leh.", "https://images.unsplash.com/photo-1509062522246-3755977927d7?w=900&q=80"),
                        place("SECMOL Campus", "Alternative education campus known for sustainability and youth learning.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80")
                ),
                List.of("Best for mountain adventure and monastery circuits", "Needs slow acclimatization planning", "Excellent for road-trip itineraries")));

        data.put("srinagar", curatedCity(
                "srinagar", "Srinagar", "Lakes, gardens, houseboats, mountain views, and calm Kashmiri hospitality.",
                "https://images.unsplash.com/photo-1598091383021-15ddea10925d?w=1400&q=80", "Jammu and Kashmir",
                List.of(new QuickFact("Dal Lake", "Icon"), new QuickFact("Houseboats", "Stay"), new QuickFact("Gardens", "Mughal")),
                List.of(
                        place("Dal Lake", "The signature Srinagar experience with shikaras and reflections.", "https://images.unsplash.com/photo-1598091383021-15ddea10925d?w=900&q=80"),
                        place("Shalimar Bagh", "One of the city's best-known Mughal gardens.", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=900&q=80"),
                        place("Nishat Bagh", "Terraced garden with lake and mountain framing.", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=900&q=80"),
                        place("Hazratbal Shrine", "Important lakeside shrine with strong local significance.", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=900&q=80"),
                        place("Chashme Shahi", "Compact Mughal garden known for spring water and views.", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=900&q=80"),
                        place("Jamia Masjid", "Historic old-city mosque with striking wooden architecture.", "https://images.unsplash.com/photo-1518709268805-4e9042af2176?w=900&q=80")
                ),
                List.of(
                        place("Winterfell Cafe", "Popular modern cafe in Srinagar's urban zone.", "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=900&q=80"),
                        place("Le Delice", "Comfort cafe and bakery option for travelers.", "https://images.unsplash.com/photo-1541167760496-1628856ab772?w=900&q=80"),
                        place("Cafe Liberty", "Easy stop for snacks and coffee during city touring.", "https://images.unsplash.com/photo-1521017432531-fbd92d768814?w=900&q=80"),
                        place("Books and Bricks Cafe", "Cozy cafe with reading-friendly ambience and comfort food.", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"),
                        place("Chai Jaai", "Tea-focused cafe with Kashmiri charm and old-city appeal.", "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=900&q=80"),
                        place("Gulshan Books Cafe", "Bookstore cafe on Dal Lake with a calm, literary mood.", "https://images.unsplash.com/photo-1559925393-8be08e16738e?w=900&q=80")
                ),
                List.of(
                        place("The Lalit Grand Palace", "Luxury stay with strong grounds and views.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
                        place("Vivanta Dal View", "Premium hillside hotel with lake outlooks.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
                        place("Houseboat Stay", "Classic Srinagar lodging experience on Dal Lake.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80"),
                        place("Radisson Srinagar", "Central full-service hotel for city sightseeing.", "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=900&q=80"),
                        place("Four Points by Sheraton Srinagar", "Modern hotel option for comfortable urban stays.", "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?w=900&q=80"),
                        place("WelcomHeritage Gurkha Houseboats", "Heritage-style houseboat stay with traditional interiors.", "https://images.unsplash.com/photo-1590490360182-c33d57733427?w=900&q=80")
                ),
                List.of(
                        place("Pari Mahal", "Scenic viewpoint and gardened monument.", "https://images.unsplash.com/photo-1524492514790-831f5b49c3dd?w=900&q=80"),
                        place("Old Srinagar", "Markets, mosques, and layered local culture.", "https://images.unsplash.com/photo-1518546305927-5a555bb7020d?w=900&q=80"),
                        place("Gulmarg Day Trip", "Major excursion option from the city.", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=900&q=80"),
                        place("Shankaracharya Temple", "Hilltop temple with panoramic views over Srinagar.", "https://images.unsplash.com/photo-1548013146-72479768bada?w=900&q=80"),
                        place("Dachigam National Park", "Protected wildlife area known for alpine scenery and hangul habitat.", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=900&q=80"),
                        place("Tulip Garden", "Seasonal garden famous for spring tulip displays.", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=900&q=80")
                ),
                List.of(
                        place("University of Kashmir", "Major public university near the Hazratbal side of Dal Lake.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
                        place("NIT Srinagar", "Important engineering institute with a lakeside campus.", "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=900&q=80"),
                        place("SPS Museum", "Museum preserving Kashmir's art, archaeology, and cultural history.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
                        place("Kashmir Government Arts Emporium", "Learning stop for regional crafts, carpets, and papier-mache work.", "https://images.unsplash.com/photo-1481437156560-3205f6a55735?w=900&q=80"),
                        place("Government Medical College Srinagar", "Major medical education institution in the city.", "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=900&q=80"),
                        place("Central University of Kashmir", "Regional higher-education institution connected to Kashmir studies.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80")
                ),
                List.of("Calm scenic destination with strong visual identity", "Best known for lake stays and gardens", "Excellent for slow-paced leisure travel")));

        data.put("manali", curatedCity(
                "manali", "Manali", "Mountain cafes, pine valleys, adventure routes, riverside stays, and snow-season tourism.",
                "https://static.toiimg.com/thumb/msid-115938847,width-1070,height-580,resizemode-75/115938847,pt-32,y_pad-40/115938847.jpg", "Himachal Pradesh",
                List.of(new QuickFact("Snow", "Seasonal"), new QuickFact("Adventure", "Strong"), new QuickFact("Cafe Scene", "Popular")),
                List.of(
                        place("Hadimba Temple", "The town's best-known heritage and forest stop.", "https://images.unsplash.com/photo-1548013146-72479768bada?w=900&q=80"),
                        place("Solang Valley", "Adventure and mountain-view hotspot near Manali.", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=900&q=80"),
                        place("Old Manali", "Cafe-heavy neighborhood with laid-back traveler appeal.", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=900&q=80"),
                        place("Manu Temple", "Historic temple in Old Manali tied to local legend.", "https://images.unsplash.com/photo-1548013146-72479768bada?w=900&q=80"),
                        place("Mall Road Manali", "Main shopping and walking stretch in central Manali.", "https://images.unsplash.com/photo-1481437156560-3205f6a55735?w=900&q=80"),
                        place("Van Vihar", "Deodar forest park for short walks near the town center.", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=900&q=80")
                ),
                List.of(
                        place("Cafe 1947", "Well-known riverside cafe for live music and atmosphere.", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"),
                        place("Johnson's Cafe", "Comfortable mountain-town dining and cafe stop.", "https://images.unsplash.com/photo-1521017432531-fbd92d768814?w=900&q=80"),
                        place("Drifters Cafe", "Backpacker-friendly cafe in Old Manali.", "https://images.unsplash.com/photo-1559925393-8be08e16738e?w=900&q=80"),
                        place("Dylan's Toasted and Roasted", "Cozy cafe known for coffee, cookies, and Old Manali mood.", "https://images.unsplash.com/photo-1541167760496-1628856ab772?w=900&q=80"),
                        place("The Lazy Dog", "Riverside cafe and bar with music and relaxed seating.", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=900&q=80"),
                        place("Renaissance Manali", "Popular cafe stop for continental food and mountain-town comfort.", "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=900&q=80")
                ),
                List.of(
                        place("The Himalayan", "Distinctive premium stay with mountain ambience.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
                        place("Span Resort and Spa", "Upscale riverside resort outside central town.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
                        place("ManuAllaya Resort", "Comfortable premium option near central Manali.", "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=900&q=80"),
                        place("Johnson Lodge", "Well-located boutique stay close to cafes and town walks.", "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?w=900&q=80"),
                        place("Snow Valley Resorts", "Family-friendly stay with mountain views and easy access.", "https://images.unsplash.com/photo-1564501049412-61c2a3083791?w=900&q=80"),
                        place("Larisa Resort Manali", "Premium resort option with forested surroundings.", "https://images.unsplash.com/photo-1590490360182-c33d57733427?w=900&q=80")
                ),
                List.of(
                        place("Rohtang Pass", "Seasonal high-altitude excursion route.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80"),
                        place("Vashisht", "Hot springs and old-village atmosphere near Manali.", "https://images.unsplash.com/photo-1518709268805-4e9042af2176?w=900&q=80"),
                        place("Jogini Falls", "Short trekking option with strong local appeal.", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=900&q=80"),
                        place("Atal Tunnel", "Major engineering landmark and gateway to Lahaul routes.", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=900&q=80"),
                        place("Naggar Castle", "Historic castle and day trip near the Kullu valley.", "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=900&q=80"),
                        place("Beas River", "Scenic riverside belt for walks, cafes, and adventure activity bases.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80")
                ),
                List.of(
                        place("Atal Bihari Vajpayee Institute of Mountaineering", "Training institute for adventure sports and mountain skills.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
                        place("Nicholas Roerich Art Gallery", "Art and heritage stop in nearby Naggar.", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=900&q=80"),
                        place("Urusvati Himalayan Folk Art Museum", "Cultural museum focused on Himalayan art and traditions.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
                        place("Government College Kullu", "Regional college serving higher education in the valley.", "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=900&q=80"),
                        place("Himalayan Nyinmapa Buddhist Monastery", "Cultural learning stop with Tibetan Buddhist architecture.", "https://images.unsplash.com/photo-1542601906990-b4d3fb7780b9?w=900&q=80"),
                        place("Siyali Mahadev Temple", "Historic wooden temple useful for local architecture learning.", "https://images.unsplash.com/photo-1548013146-72479768bada?w=900&q=80")
                ),
                List.of("High-demand hill destination for mixed-age groups", "Strong blend of adventure and cafe culture", "Best for short scenic getaways and snow-season travel")));

        data.put("shimla", curatedCity(
                "shimla", "Shimla", "Colonial hill capital with ridge walks, cafes, viewpoints, and easy mountain stays.",
                "https://www.honeymoonpackagesmanali.in/wp-content/uploads/2024/04/SUMMER-HILL.jpg", "Himachal Pradesh",
                List.of(new QuickFact("Ridge", "Walks"), new QuickFact("Colonial", "Legacy"), new QuickFact("Hill Stay", "Classic")),
                List.of(
                        place("The Ridge", "Shimla's signature public promenade and city center.", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=900&q=80"),
                        place("Mall Road", "Main shopping and strolling stretch in the town core.", "https://images.unsplash.com/photo-1481437156560-3205f6a55735?w=900&q=80"),
                        place("Jakhoo Temple", "Hilltop temple with broad views over the town.", "https://images.unsplash.com/photo-1548013146-72479768bada?w=900&q=80"),
                        place("Christ Church", "Historic church and one of Shimla's most visible landmarks.", "https://images.unsplash.com/photo-1518709268805-4e9042af2176?w=900&q=80"),
                        place("Viceregal Lodge", "Grand colonial-era building with major historical value.", "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=900&q=80"),
                        place("Lakkar Bazaar", "Market lane known for wooden crafts and hill-town shopping.", "https://images.unsplash.com/photo-1481437156560-3205f6a55735?w=900&q=80")
                ),
                List.of(
                        place("Cafe Simla Times", "Well-liked cafe with youth-friendly style.", "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=900&q=80"),
                        place("Wake and Bake", "Traveler-favorite breakfast and coffee stop.", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"),
                        place("Honey Hut", "Popular quick-stop cafe for honey-based products and drinks.", "https://images.unsplash.com/photo-1559925393-8be08e16738e?w=900&q=80"),
                        place("Indian Coffee House", "Old-school cafe institution with simple food and nostalgia.", "https://images.unsplash.com/photo-1521017432531-fbd92d768814?w=900&q=80"),
                        place("Cafe Sol", "Bright cafe and restaurant stop near the Mall.", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=900&q=80"),
                        place("Eighteen71 Cookhouse", "Modern restaurant-cafe option for polished meals in town.", "https://images.unsplash.com/photo-1541167760496-1628856ab772?w=900&q=80")
                ),
                List.of(
                        place("Wildflower Hall", "Luxury retreat outside central Shimla.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
                        place("The Oberoi Cecil", "Classic heritage luxury property in town.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
                        place("Clarkes Hotel", "Well-situated heritage-style Shimla stay.", "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=900&q=80"),
                        place("Radisson Hotel Shimla", "Full-service hotel with good access to town viewpoints.", "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?w=900&q=80"),
                        place("Woodville Palace", "Heritage hotel with old-world charm and quiet grounds.", "https://images.unsplash.com/photo-1564501049412-61c2a3083791?w=900&q=80"),
                        place("Chapslee", "Characterful heritage stay connected to Shimla's royal past.", "https://images.unsplash.com/photo-1590490360182-c33d57733427?w=900&q=80")
                ),
                List.of(
                        place("Christ Church", "One of the town's most visible landmarks.", "https://images.unsplash.com/photo-1518709268805-4e9042af2176?w=900&q=80"),
                        place("Kufri", "Easy excursion for views and outdoor fun.", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=900&q=80"),
                        place("Indian Institute of Advanced Study", "Architectural and historical campus worth visiting.", "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=900&q=80"),
                        place("Chadwick Falls", "Seasonal waterfall and nature stop near Summer Hill.", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=900&q=80"),
                        place("Annandale", "Open meadow area with army heritage and valley views.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80"),
                        place("Tara Devi Temple", "Hilltop temple and scenic excursion outside the main town.", "https://images.unsplash.com/photo-1548013146-72479768bada?w=900&q=80")
                ),
                List.of(
                        place("Himachal Pradesh University", "Major university campus serving the state capital.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
                        place("Indian Institute of Advanced Study", "Research institute housed in the historic Viceregal Lodge.", "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=900&q=80"),
                        place("Gaiety Heritage Cultural Complex", "Cultural venue and heritage theatre on the Mall.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
                        place("Army Heritage Museum", "Museum at Annandale covering military history and displays.", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=900&q=80"),
                        place("Himachal State Museum", "Museum focused on regional art, sculpture, and history.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
                        place("Bishop Cotton School", "Historic school campus with colonial-era educational legacy.", "https://images.unsplash.com/photo-1509062522246-3755977927d7?w=900&q=80")
                ),
                List.of("Strong beginner-friendly hill destination", "Good blend of heritage and scenic walking", "Works well for family holidays and relaxed weekends")));

        data.put("rishikesh", curatedCity(
                "rishikesh", "Rishikesh", "Yoga town riverfronts, suspension bridges, cafes, rafting trips, and Himalayan calm.",
                "https://hblimg.mmtcdn.com/content/hubble/img/desttvimg/mmt/destination/m_Rishikesh_tv_destination_img_2_l_664_996.jpg", "Uttarakhand",
                List.of(new QuickFact("Yoga", "Capital"), new QuickFact("Ganga", "Riverfront"), new QuickFact("Adventure", "Rafting")),
                List.of(
                        place("Laxman Jhula", "One of the town's defining bridge landmarks.", "https://images.unsplash.com/photo-1548013146-72479768bada?w=900&q=80"),
                        place("Triveni Ghat", "Evening aarti and devotional riverfront atmosphere.", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=900&q=80"),
                        place("Ram Jhula", "Busy crossing that links key temple and ashram areas.", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=900&q=80"),
                        place("Parmarth Niketan", "Major ashram known for riverfront spirituality and evening aarti.", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=900&q=80"),
                        place("Beatles Ashram", "Graffiti-filled former ashram with strong cultural appeal.", "https://images.unsplash.com/photo-1518709268805-4e9042af2176?w=900&q=80"),
                        place("Neelkanth Mahadev Temple", "Important temple excursion through forested hill roads.", "https://images.unsplash.com/photo-1548013146-72479768bada?w=900&q=80")
                ),
                List.of(
                        place("Little Buddha Cafe", "Traveler-favorite river-view cafe in the bridge area.", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"),
                        place("Bistro Nirvana", "Relaxed cafe for coffee and long hangouts.", "https://images.unsplash.com/photo-1521017432531-fbd92d768814?w=900&q=80"),
                        place("The 60's Cafe", "Popular cafe stop for backpackers and riverside vibes.", "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=900&q=80"),
                        place("Beatles Cafe", "Laid-back cafe themed around the town's music history.", "https://images.unsplash.com/photo-1559925393-8be08e16738e?w=900&q=80"),
                        place("Ganga View Cafe", "Simple river-facing cafe for slow meals and views.", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=900&q=80"),
                        place("Pure Soul Cafe", "Wellness-friendly cafe with light food and calm seating.", "https://images.unsplash.com/photo-1541167760496-1628856ab772?w=900&q=80")
                ),
                List.of(
                        place("Aloha on the Ganges", "Premium stay with river-facing leisure value.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
                        place("Taj Rishikesh Resort and Spa", "Luxury retreat beyond the busy center.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
                        place("EllBee Ganga View", "Convenient city-side hotel near the ghats.", "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=900&q=80"),
                        place("Divine Resort", "River-facing stay close to the Tapovan side.", "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?w=900&q=80"),
                        place("Glasshouse on the Ganges", "Boutique retreat set by the river outside the town rush.", "https://images.unsplash.com/photo-1564501049412-61c2a3083791?w=900&q=80"),
                        place("Sterling Palm Bliss", "Comfortable wellness-focused stay for relaxed trips.", "https://images.unsplash.com/photo-1590490360182-c33d57733427?w=900&q=80")
                ),
                List.of(
                        place("Rafting on the Ganga", "The adventure activity most visitors come for.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80"),
                        place("Neer Garh Waterfall", "Short escape for nature and walking.", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=900&q=80"),
                        place("Beatles Ashram", "Cultural and visual landmark with graffiti-rich ruins.", "https://images.unsplash.com/photo-1518709268805-4e9042af2176?w=900&q=80"),
                        place("Kunjapuri Temple", "Sunrise viewpoint and temple excursion above Rishikesh.", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=900&q=80"),
                        place("Rajaji National Park", "Wildlife and forest excursion accessible from the region.", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=900&q=80"),
                        place("Shivpuri", "Adventure base for rafting camps and riverside breaks.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80")
                ),
                List.of(
                        place("Parmarth Niketan", "Major ashram campus for yoga, spirituality, and cultural learning.", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=900&q=80"),
                        place("Sivananda Ashram", "Known spiritual learning center in the Rishikesh tradition.", "https://images.unsplash.com/photo-1548013146-72479768bada?w=900&q=80"),
                        place("Yoga Niketan", "Established yoga ashram for structured practice and learning.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
                        place("Patanjali International Yoga Foundation", "Yoga training center popular with long-stay learners.", "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=900&q=80"),
                        place("AIIMS Rishikesh", "Major medical institute and academic hospital in the city.", "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=900&q=80"),
                        place("Swami Rama Sadhaka Grama", "Meditation and yoga learning campus near Rishikesh.", "https://images.unsplash.com/photo-1507413245164-6160d8298b31?w=900&q=80")
                ),
                List.of("Strong spiritual and adventure dual appeal", "Easy cafe-rich riverfront experience", "Best for short healing trips or activity breaks")));

        data.put("leh", data.get("leh"));

        data.put("srinagar", data.get("srinagar"));

        data.put("kochi", data.get("kochi"));

        data.put("mumbai", data.get("mumbai"));

        data.put("darjeeling", data.get("darjeeling"));

        data.put("shimla", data.get("shimla"));

        data.put("manali", data.get("manali"));

        data.put("goa", data.get("goa"));

        data.put("jaisalmer", data.get("jaisalmer"));

        data.put("agra", data.get("agra"));

        data.put("varanasi", data.get("varanasi"));

        data.put("jaipur", data.get("jaipur"));

        data.put("chamba", curatedCity(
                "chamba", "Chamba", "Quiet Himachali valleys, temples, mountain scenery, and slower hill travel.",
                "https://site.outlookindia.com/traveller/wp-content/uploads/2017/08/chamba-ravi-river_FI.jpg", "Himachal Pradesh",
                List.of(new QuickFact("Valleys", "Scenic"), new QuickFact("Temples", "Historic"), new QuickFact("Relaxed", "Pace")),
                List.of(
                        place("Chamba Town", "Starting point for local history and hillside views.", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=900&q=80"),
                        place("Lakshmi Narayan Temple", "Important old temple complex in the district center.", "https://images.unsplash.com/photo-1548013146-72479768bada?w=900&q=80"),
                        place("Khajjiar", "Popular meadow destination often paired with Chamba.", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=900&q=80"),
                        place("Bhuri Singh Museum", "Key cultural museum for Chamba art and regional history.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
                        place("Chaugan", "Large town meadow used for gatherings, strolls, and local events.", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=900&q=80"),
                        place("Champavati Temple", "Historic temple known for Chamba's traditional architecture.", "https://images.unsplash.com/photo-1548013146-72479768bada?w=900&q=80")
                ),
                List.of(
                        place("Cafe Ravi View", "Simple local stop with scenic break value.", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"),
                        place("Mountain Brew Chamba", "Casual cafe option for travelers.", "https://images.unsplash.com/photo-1521017432531-fbd92d768814?w=900&q=80"),
                        place("Town Corner Cafe", "Basic refreshments in the town center.", "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=900&q=80"),
                        place("Cafe Kalatop", "Hill-route cafe stop suited for forest and meadow day trips.", "https://images.unsplash.com/photo-1559925393-8be08e16738e?w=900&q=80"),
                        place("Khajjiar Cafe", "Simple cafe stop for snacks during Khajjiar sightseeing.", "https://images.unsplash.com/photo-1541167760496-1628856ab772?w=900&q=80"),
                        place("Himalayan Trout Cafe", "Relaxed regional food stop with mountain-trip character.", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=900&q=80")
                ),
                List.of(
                        place("Aroha Resort", "Comfort hill-stay option with mountain outlooks.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
                        place("Hotel Iravati", "Central accommodation for district touring.", "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=900&q=80"),
                        place("Khajjiar Retreat", "Useful stay choice if combining meadow visits.", "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?w=900&q=80"),
                        place("Classic Hill Top Resort", "Hill-stay option for travelers exploring Chamba and Khajjiar.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
                        place("Hotel Aroma Palace", "Town hotel option for simple, central stays.", "https://images.unsplash.com/photo-1564501049412-61c2a3083791?w=900&q=80"),
                        place("H2O House Chamba", "Boutique-style stay suited for quieter valley travel.", "https://images.unsplash.com/photo-1590490360182-c33d57733427?w=900&q=80")
                ),
                List.of(
                        place("Bhuri Singh Museum", "Good stop for regional history and art.", "https://images.unsplash.com/photo-1518709268805-4e9042af2176?w=900&q=80"),
                        place("Rang Mahal", "Historic palace site in Chamba town.", "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=900&q=80"),
                        place("Khajjiar Lake", "Short outing inside the Khajjiar meadow zone.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80"),
                        place("Kalatop Wildlife Sanctuary", "Forest escape near Dalhousie and Khajjiar with walking trails.", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=900&q=80"),
                        place("Sach Pass", "High-altitude road adventure for experienced mountain travelers.", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=900&q=80"),
                        place("Chamunda Devi Temple", "Temple and viewpoint area with spiritual and scenic appeal.", "https://images.unsplash.com/photo-1548013146-72479768bada?w=900&q=80")
                ),
                List.of(
                        place("Bhuri Singh Museum", "Educational museum for Chamba miniature paintings and local heritage.", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=900&q=80"),
                        place("Government College Chamba", "Main higher-education institution in the district town.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
                        place("Lakshmi Narayan Temple Complex", "Historic temple complex useful for architecture and heritage learning.", "https://images.unsplash.com/photo-1548013146-72479768bada?w=900&q=80"),
                        place("Rang Mahal", "Historic palace site connected to Chamba's royal past.", "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=900&q=80"),
                        place("Chamba Library", "Local learning resource and quiet civic education space.", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=900&q=80"),
                        place("Akhand Chandi Palace", "Royal-era landmark associated with Chamba's political history.", "https://images.unsplash.com/photo-1518709268805-4e9042af2176?w=900&q=80")
                ),
                List.of("Good for quieter Himachal travel", "Pairs well with Khajjiar and Dalhousie", "A slower alternative to crowded hill stations")));

        return data;
    }

    private Map<String, CityPage> loadEditableCityData() {
        InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("data/cities.json");
        if (input == null) {
            return Map.of();
        }

        try (input) {
            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            CityDataFile file = mapper.readValue(input, new TypeReference<>() {});
            if (file.cities() == null || file.cities().isEmpty()) {
                return Map.of();
            }

            Map<String, CityPage> loaded = new LinkedHashMap<>();
            for (EditableCity city : file.cities()) {
                if (city.slug() == null || city.slug().isBlank()) {
                    continue;
                }
                loaded.put(city.slug(), toCityPage(city));
            }
            return loaded;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load editable city data from src/main/resources/data/cities.json", ex);
        }
    }

    private CityPage toCityPage(EditableCity city) {
        List<QuickFact> facts = city.facts() == null
                ? List.of()
                : city.facts().stream()
                .map(fact -> new QuickFact(fact.value(), fact.label()))
                .toList();

        List<CategoryPage> categories = city.categories() == null
                ? List.of()
                : city.categories().stream()
                .map(category -> new CategoryPage(
                        category.slug(),
                        category.name(),
                        category.summary(),
                        category.heroImage(),
                        toPlaceCards(category)))
                .toList();

        return new CityPage(
                city.slug(),
                city.name(),
                city.tagline(),
                city.heroImage(),
                city.region(),
                facts,
                categories,
                city.highlights() == null ? List.of() : city.highlights());
    }

    private List<PlaceCard> toPlaceCards(EditableCategory category) {
        if (category.places() == null) {
            return List.of();
        }
        return category.places().stream()
                .map(place -> place(place.name(), place.description(), place.imageUrl()))
                .toList();
    }

    private Map<String, String> buildAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        for (CityPage city : cities.values()) {
            aliases.put(normalize(city.name()), city.slug());
        }
        aliases.put("varanassi", "varanasi");
        aliases.put("banaras", "varanasi");
        aliases.put("kashi", "varanasi");
        aliases.put("shirinagar", "srinagar");
        aliases.put("srinager", "srinagar");
        aliases.put("jaislmair", "jaisalmer");
        aliases.put("jaiselmer", "jaisalmer");
        aliases.put("mharastra", "maharashtra");
        aliases.put("maharastra", "maharashtra");
        aliases.put("bombay", "mumbai");
        aliases.put("cochin", "kochi");
        aliases.put("darjling", "darjeeling");
        aliases.put("chd", "chandigarh");
        aliases.put("goa beaches", "goa");
        return aliases;
    }

    private CityPage toCityPage(ManagedCity city, String citySlug) {
        List<ManagedPlace> managedPlaces = getManagedPlacesForCity(citySlug);
        Map<String, List<ManagedPlace>> grouped = managedPlaces.stream()
                .collect(Collectors.groupingBy(ManagedPlace::getCategorySlug, LinkedHashMap::new, Collectors.toList()));

        List<CategoryPage> categories = getAdminCategoryOptions().stream()
                .map(categorySlug -> new CategoryPage(
                        categorySlug,
                        labelForCategory(categorySlug),
                        summaryForCategory(categorySlug, city.getName()),
                        categoryHeroImage(categorySlug),
                        grouped.getOrDefault(categorySlug, List.of()).stream()
                                .map(this::toManagedPlaceCard)
                                .toList()
                ))
                .toList();

        long hotelCount = managedPlaces.stream().filter(place -> "hotels".equals(place.getCategorySlug())).count();
        long cafeCount = managedPlaces.stream().filter(place -> "cafes".equals(place.getCategorySlug())).count();
        long sightCount = managedPlaces.stream().filter(place ->
                "popular-places".equals(place.getCategorySlug())
                        || "tourist-places".equals(place.getCategorySlug())
                        || "educational-places".equals(place.getCategorySlug())).count();

        List<QuickFact> facts = new ArrayList<>(List.of(
                new QuickFact(String.valueOf(sightCount), "Places"),
                new QuickFact(String.valueOf(hotelCount), "Hotels"),
                new QuickFact(String.valueOf(cafeCount), "Cafes")
        ));
        if (city.getBestSeason() != null && !city.getBestSeason().isBlank()) {
            facts.add(new QuickFact(city.getBestSeason().trim(), "Best Season"));
        }
        if (city.getIdealDuration() != null && !city.getIdealDuration().isBlank()) {
            facts.add(new QuickFact(city.getIdealDuration().trim(), "Ideal Stay"));
        }

        List<String> highlights = parseMultiline(city.getCityHighlights());
        if (highlights.isEmpty()) {
            highlights = List.of(
                    "Admin-managed destination page",
                    "Browse places, cafes, hotels, and tourist spots",
                    "Post reviews and expand content over time"
            );
        }

        return new CityPage(
                city.getSlug(),
                city.getName(),
                city.getTagline(),
                city.getHeroImage(),
                city.getRegion(),
                facts,
                categories,
                highlights
        );
    }

    private String labelForCategory(String categorySlug) {
        return switch (categorySlug) {
            case "popular-places" -> "Popular Places";
            case "tourist-places" -> "Tourist Places";
            case "hotels" -> "Hotels";
            case "cafes" -> "Cafes";
            case "educational-places" -> "Educational Places";
            case "emergency-services" -> "Emergency Services";
            default -> "Places";
        };
    }

    private String summaryForCategory(String categorySlug, String cityName) {
        return switch (categorySlug) {
            case "popular-places" -> "Popular landmarks and must-see spots in " + cityName + ".";
            case "tourist-places" -> "Extra tourist spots and visitor favorites in " + cityName + ".";
            case "hotels" -> "Hotels and stay recommendations in " + cityName + ".";
            case "cafes" -> "Cafe picks and food-stop insights in " + cityName + ".";
            case "educational-places" -> "Educational institutes, museums, learning hubs, and knowledge stops in " + cityName + ".";
            case "emergency-services" -> "Hospitals, helplines, police, fire, pharmacies, and urgent support in " + cityName + ".";
            default -> "Explore " + cityName + ".";
        };
    }

    private String buildUniqueSlug(String value) {
        String base = normalize(value);
        String candidate = base;
        int count = 2;
        while (cities.containsKey(candidate) || managedCityRepository.existsBySlug(candidate)) {
            candidate = base + count;
            count++;
        }
        return candidate;
    }

    private CityPage curatedCity(String slug,
                                 String name,
                                 String tagline,
                                 String heroImage,
                                 String region,
                                 List<QuickFact> facts,
                                 List<PlaceCard> popularPlaces,
                                 List<PlaceCard> cafes,
                                 List<PlaceCard> hotels,
                                 List<PlaceCard> touristPlaces,
                                 List<String> highlights) {
        return curatedCity(
                slug,
                name,
                tagline,
                heroImage,
                region,
                facts,
                popularPlaces,
                cafes,
                hotels,
                touristPlaces,
                List.of(),
                highlights);
    }

    private CityPage curatedCity(String slug,
                                 String name,
                                 String tagline,
                                 String heroImage,
                                 String region,
                                 List<QuickFact> facts,
                                 List<PlaceCard> popularPlaces,
                                 List<PlaceCard> cafes,
                                 List<PlaceCard> hotels,
                                 List<PlaceCard> touristPlaces,
                                 List<PlaceCard> educationalPlaces,
                                 List<String> highlights) {
        return new CityPage(
                slug,
                name,
                tagline,
                heroImage,
                region,
                facts,
                curatedCategories(slug, name, popularPlaces, cafes, hotels, touristPlaces, educationalPlaces),
                highlights
        );
    }

    private List<CategoryPage> curatedCategories(String slug,
                                                 String name,
                                                 List<PlaceCard> popularPlaces,
                                                 List<PlaceCard> cafes,
                                                 List<PlaceCard> hotels,
                                                 List<PlaceCard> touristPlaces,
                                                 List<PlaceCard> educationalPlaces) {
        int minimumPlaces = minimumCategoryPlaces(slug);
        List<PlaceCard> completedPopularPlaces = completePlaces(slug, "popular-places", popularPlaces, minimumPlaces);
        List<PlaceCard> completedTouristPlaces = completePlaces(slug, "tourist-places", touristPlaces, minimumPlaces);
        List<PlaceCard> completedCafes = completePlaces(slug, "cafes", cafes, minimumPlaces);
        List<PlaceCard> completedHotels = completePlaces(slug, "hotels", hotels, minimumPlaces);
        List<PlaceCard> completedEducationalPlaces = completePlaces(slug, "educational-places", educationalPlaces, minimumPlaces);

        List<CategoryPage> categories = new ArrayList<>();
        categories.add(category("popular-places", "Popular Places", "The biggest must-see spots in " + name + ".", categoryHeroImage("popular-places"), completedPopularPlaces));
        categories.add(category("tourist-places", "Tourist Places", "Extra sightseeing stops and visitor favorites in " + name + ".", categoryHeroImage("tourist-places"), completedTouristPlaces));
        categories.add(category("cafes", "Cafes", "Good coffee, brunch, and local cafe picks across " + name + ".", categoryHeroImage("cafes"), completedCafes));
        categories.add(category("hotels", "Hotels", "Recommended places to stay in and around " + name + ".", categoryHeroImage("hotels"), completedHotels));
        categories.add(category("educational-places", "Educational Places", "Museums, universities, galleries, and learning spaces in " + name + ".", categoryHeroImage("educational-places"), completedEducationalPlaces));
        categories.add(category("emergency-services", "Emergency Services", "Hospitals, ambulance help, police, fire, pharmacies, and urgent support in " + name + ".", categoryHeroImage("emergency-services"), emergencyServicesForCity(slug, name)));
        return categories;
    }

    private int minimumCategoryPlaces(String citySlug) {
        return List.of("jaipur", "chandigarh").contains(citySlug)
                ? BASE_CATEGORY_PLACE_COUNT
                : ENRICHED_CATEGORY_PLACE_COUNT;
    }

    private List<PlaceCard> completePlaces(String citySlug, String categorySlug, List<PlaceCard> existing, int minimum) {
        List<PlaceCard> completed = joinPlaceCards(existing, supplementalPlaces(citySlug, categorySlug));
        if (completed.size() >= minimum) {
            return completed;
        }
        return joinPlaceCards(completed, fallbackPlaces(citySlug, categorySlug, minimum - completed.size()));
    }

    private List<PlaceCard> supplementalPlaces(String citySlug, String categorySlug) {
        return switch (citySlug + ":" + categorySlug) {
            case "udaipur:popular-places" -> List.of(
                    place("Lake Pichola", "Udaipur's signature lake with palaces, boat rides, and old-city views.", "https://images.unsplash.com/photo-1599661046827-dacde6976549?w=900&q=80"),
                    place("City Palace Udaipur", "Grand palace complex overlooking the lake and historic old city.", "https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?w=900&q=80"),
                    place("Fateh Sagar Lake", "Relaxed lakeside stretch for sunsets, boating, and evening food plans.", "https://images.unsplash.com/photo-1599661046289-e31897846e41?w=900&q=80"));
            case "udaipur:cafes" -> List.of(
                    place("Jheel's Ginger Coffee Bar", "Lake-facing cafe lane stop for coffee, snacks, and easy old-city pauses.", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"),
                    place("Millets of Mewar", "Health-forward cafe popular for slow meals and lake-town traveller energy.", "https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=900&q=80"),
                    place("Upre Rooftop Cafe", "Rooftop dining spot with palace views and polished evening ambience.", "https://images.unsplash.com/photo-1514933651103-005eec06c04b?w=900&q=80"));
            case "udaipur:hotels" -> List.of(
                    place("Taj Lake Palace", "Iconic luxury hotel set on Lake Pichola for a royal stay experience.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
                    place("The Leela Palace Udaipur", "Premium lakefront hotel with grand courtyards and refined service.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
                    place("Trident Udaipur", "Comfortable resort-style stay close to the lakeside circuit.", "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=900&q=80"));
            case "udaipur:tourist-places" -> List.of(
                    place("Sajjangarh Monsoon Palace", "Hilltop palace known for sunset views over lakes and Aravalli ridges.", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=900&q=80"),
                    place("Saheliyon Ki Bari", "Garden with fountains, marble kiosks, and a quieter heritage feel.", "https://images.unsplash.com/photo-1518002054494-3a6f94352e9d?w=900&q=80"),
                    place("Bagore Ki Haveli", "Cultural haveli museum and evening folk performance venue by the lake.", "https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?w=900&q=80"));
            case "udaipur:educational-places" -> List.of(
                    place("Mohanlal Sukhadia University", "Major university campus anchoring higher education in Udaipur.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
                    place("Bharatiya Lok Kala Mandal", "Folk arts museum and cultural learning space for regional traditions.", "https://images.unsplash.com/photo-1544967082-d9d25d867d66?w=900&q=80"),
                    place("Udaipur City Palace Museum", "Museum-led learning stop for Mewar history, architecture, and royal archives.", "https://images.unsplash.com/photo-1582555172866-f73bb12a2ab3?w=900&q=80"),
                    place("Sajjangarh Biological Park Learning Trail", "Nature education stop for families, students, and wildlife orientation.", "https://images.unsplash.com/photo-1542601906990-b4d3fb7780b9?w=900&q=80"),
                    place("Shilpgram Craft Village", "Living craft campus for folk art, textiles, workshops, and cultural demos.", "https://images.unsplash.com/photo-1520697222868-1c245460d3cd?w=900&q=80"),
                    place("Pratap Gaurav Kendra", "Interpretive museum space focused on Maharana Pratap and regional history.", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=900&q=80"));

            case "jaisalmer:popular-places" -> List.of(
                    place("Jaisalmer Fort", "Living golden sandstone fort with lanes, temples, homes, and viewpoints.", "https://images.unsplash.com/photo-1477587458883-47145ed94245?w=900&q=80"),
                    place("Patwon Ki Haveli", "Detailed merchant haveli cluster known for carved facades and heritage rooms.", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=900&q=80"),
                    place("Gadisar Lake", "Historic lakefront for boats, temples, and golden-hour photographs.", "https://images.unsplash.com/photo-1609947017136-9daf32a5eb16?w=900&q=80"));
            case "jaisalmer:cafes" -> List.of(
                    place("Kuku Coffee Shop", "Small fort-side coffee stop with rooftop views and traveller conversations.", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"),
                    place("The Traveler's Cup", "Easy cafe for espresso, breakfast, and slow old-city planning.", "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=900&q=80"),
                    place("Cafe The Kaku", "Rooftop cafe known for fort views, meals, and sunset ambience.", "https://images.unsplash.com/photo-1514933651103-005eec06c04b?w=900&q=80"));
            case "jaisalmer:hotels" -> List.of(
                    place("Suryagarh Jaisalmer", "Luxury desert hotel with grand architecture and curated desert experiences.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
                    place("Jaisalmer Marriott Resort", "Modern resort-style stay with pool, dining, and fort access.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
                    place("Fort Rajwada", "Heritage-style hotel base for city touring and desert excursions.", "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=900&q=80"));
            case "jaisalmer:tourist-places" -> List.of(
                    place("Sam Sand Dunes", "Classic desert excursion for dunes, camel rides, and evening camps.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80"),
                    place("Kuldhara Village", "Abandoned heritage village with desert folklore and photography appeal.", "https://images.unsplash.com/photo-1509316785289-025f5b846b35?w=900&q=80"),
                    place("Bada Bagh", "Royal cenotaph garden set against open desert scenery.", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=900&q=80"));
            case "jaisalmer:educational-places" -> List.of(
                    place("Desert Culture Centre", "Museum space for puppetry, manuscripts, music, and desert community stories.", "https://images.unsplash.com/photo-1544967082-d9d25d867d66?w=900&q=80"),
                    place("Jaisalmer War Museum", "Interpretive military museum covering desert warfare and regional history.", "https://images.unsplash.com/photo-1582555172866-f73bb12a2ab3?w=900&q=80"),
                    place("Government Museum Jaisalmer", "Archaeology and local heritage collection for student-friendly learning.", "https://images.unsplash.com/photo-1566127992631-137a642a90f4?w=900&q=80"),
                    place("Fort Palace Museum", "Historic palace museum with royal rooms, balconies, and city context.", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=900&q=80"),
                    place("Akal Wood Fossil Park", "Geology learning site with fossilized wood from ancient landscapes.", "https://images.unsplash.com/photo-1473773508845-188df298d2d1?w=900&q=80"),
                    place("Jaisalmer Public Library", "Useful local reading and reference stop for students and residents.", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=900&q=80"));

            case "goa:popular-places" -> List.of(
                    place("Baga Beach", "High-energy beach stretch with shacks, water sports, and nightlife access.", "https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?w=900&q=80"),
                    place("Fort Aguada", "Sea-facing Portuguese fort with lighthouse views and heritage value.", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=900&q=80"),
                    place("Basilica of Bom Jesus", "UNESCO-listed church and one of Old Goa's most important landmarks.", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=900&q=80"));
            case "goa:cafes" -> List.of(
                    place("Artjuna Anjuna", "Aesthetic garden cafe for brunch, shopping, coffee, and slow afternoons.", "https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=900&q=80"),
                    place("Baba Au Rhum", "Popular bakery cafe with breakfast plates and relaxed Goa energy.", "https://images.unsplash.com/photo-1541167760496-1628856ab772?w=900&q=80"),
                    place("Eva Cafe", "Beachside cafe for coffee, breakfast, and soft sea-view mornings.", "https://images.unsplash.com/photo-1514933651103-005eec06c04b?w=900&q=80"));
            case "goa:hotels" -> List.of(
                    place("Taj Fort Aguada Resort", "Cliffside luxury resort with sea views and heritage surroundings.", "https://images.unsplash.com/photo-1540541338287-41700207dee6?w=900&q=80"),
                    place("W Goa", "Trendy resort near Vagator with nightlife, design, and beach access.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
                    place("The Leela Goa", "Premium South Goa resort with lagoons, gardens, and beach frontage.", "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=900&q=80"));
            case "goa:tourist-places" -> List.of(
                    place("Dudhsagar Falls", "Major waterfall excursion for monsoon scenery and nature plans.", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=900&q=80"),
                    place("Fontainhas", "Latin Quarter with colorful lanes, galleries, cafes, and walking routes.", "https://images.unsplash.com/photo-1518002054494-3a6f94352e9d?w=900&q=80"),
                    place("Chapora Fort", "Popular sunset fort overlooking Vagator and the coastline.", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=900&q=80"));
            case "goa:educational-places" -> List.of(
                    place("Goa University", "Primary higher education campus with research and student activity.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
                    place("Museum of Goa", "Contemporary art museum and cultural learning space near Pilerne.", "https://images.unsplash.com/photo-1544967082-d9d25d867d66?w=900&q=80"),
                    place("Goa Science Centre", "Family-friendly science learning space with interactive exhibits.", "https://images.unsplash.com/photo-1532094349884-543bc11b234d?w=900&q=80"),
                    place("Central Library Panaji", "Large public library and knowledge resource in the capital.", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=900&q=80"),
                    place("Houses of Goa Museum", "Architecture-focused museum explaining Goan homes and design history.", "https://images.unsplash.com/photo-1582555172866-f73bb12a2ab3?w=900&q=80"),
                    place("Kala Academy Goa", "Cultural learning venue for music, theatre, art, and public events.", "https://images.unsplash.com/photo-1499364615650-ec38552f4f34?w=900&q=80"));

            case "mumbai:popular-places" -> List.of(
                    place("Gateway of India", "Mumbai's defining waterfront monument and a starting point for city photos.", "https://images.unsplash.com/photo-1529253355930-ddbe423a2ac7?w=900&q=80"),
                    place("Marine Drive", "Curving seafront promenade famous for sunsets and night lights.", "https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=900&q=80"),
                    place("Chhatrapati Shivaji Maharaj Terminus", "UNESCO railway landmark with Gothic architecture and daily city rhythm.", "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=900&q=80"));
            case "mumbai:cafes" -> List.of(
                    place("Prithvi Cafe", "Culture-heavy Juhu cafe beside theatre shows and creative crowds.", "https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=900&q=80"),
                    place("Leaping Windows", "Comic-book cafe in Andheri with comfort food and relaxed seating.", "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=900&q=80"),
                    place("Kala Ghoda Cafe", "Compact arts-district cafe for coffee, brunch, and gallery walks.", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"));
            case "mumbai:hotels" -> List.of(
                    place("The Taj Mahal Palace", "Historic luxury hotel beside the Gateway with iconic sea-facing presence.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
                    place("Trident Nariman Point", "Business and leisure hotel with Marine Drive and harbour access.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
                    place("JW Marriott Mumbai Juhu", "Beachside luxury stay close to Juhu, dining, and airport routes.", "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=900&q=80"));
            case "mumbai:tourist-places" -> List.of(
                    place("Elephanta Caves", "UNESCO cave-temple excursion reached by ferry from the harbour.", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=900&q=80"),
                    place("Bandra Bandstand", "Sea-facing promenade for sunset walks and celebrity-home spotting.", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=900&q=80"),
                    place("Sanjay Gandhi National Park", "Large urban forest with caves, trails, and nature breaks.", "https://images.unsplash.com/photo-1542601906990-b4d3fb7780b9?w=900&q=80"));
            case "mumbai:educational-places" -> List.of(
                    place("University of Mumbai Fort Campus", "Historic university precinct with strong architecture and academic legacy.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
                    place("Chhatrapati Shivaji Maharaj Vastu Sangrahalaya", "Major museum for art, archaeology, natural history, and culture.", "https://images.unsplash.com/photo-1544967082-d9d25d867d66?w=900&q=80"),
                    place("Nehru Science Centre", "Interactive science museum popular with families and students.", "https://images.unsplash.com/photo-1532094349884-543bc11b234d?w=900&q=80"),
                    place("Asiatic Society Library", "Historic reading room and public knowledge landmark in South Mumbai.", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=900&q=80"),
                    place("Dr Bhau Daji Lad Museum", "City museum covering Mumbai's industrial, cultural, and design history.", "https://images.unsplash.com/photo-1582555172866-f73bb12a2ab3?w=900&q=80"),
                    place("National Gallery of Modern Art Mumbai", "Art learning stop for exhibitions, talks, and modern Indian works.", "https://images.unsplash.com/photo-1499364615650-ec38552f4f34?w=900&q=80"));

            default -> List.of();
        };
    }

    private List<PlaceCard> fallbackPlaces(String citySlug, String categorySlug, int count) {
        String cityName = titleFromSlug(citySlug);
        List<PlaceCard> fallback = new ArrayList<>();
        List<String> names = supplementalNames(citySlug, categorySlug);
        if (!names.isEmpty()) {
            fallback.addAll(names.stream()
                    .map(name -> place(name, fallbackDescription(name, cityName, categorySlug), fallbackImage(categorySlug)))
                    .toList());
        }
        List<PlaceCard> baseFallback = switch (categorySlug) {
            case "popular-places" -> List.of(
                    place(cityName + " Heritage Walk", "A practical city route for landmark photos, old streets, and first-time orientation.", "https://images.unsplash.com/photo-1518002054494-3a6f94352e9d?w=900&q=80"),
                    place(cityName + " Central Promenade", "A recognizable public stretch for evening walks, food stops, and quick city browsing.", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=900&q=80"),
                    place(cityName + " Viewpoint", "A scenic city lookout that works well for golden-hour photos and relaxed plans.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=80"));
            case "tourist-places" -> List.of(
                    place(cityName + " Local Museum Route", "A visitor-friendly route for culture, regional stories, and slow exploration.", "https://images.unsplash.com/photo-1544967082-d9d25d867d66?w=900&q=80"),
                    place(cityName + " Nature Escape", "A green or scenic outing for families, couples, and calmer travel days.", "https://images.unsplash.com/photo-1542601906990-b4d3fb7780b9?w=900&q=80"),
                    place(cityName + " Market Trail", "A market-led sightseeing plan with snacks, shopping, and local street energy.", "https://images.unsplash.com/photo-1555529771-835f59fc5efe?w=900&q=80"));
            case "cafes" -> List.of(
                    place(cityName + " Roastery Cafe", "A warm coffee stop for breakfast, work breaks, and casual meetups.", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80"),
                    place(cityName + " Garden Cafe", "A calmer cafe plan with open seating, snacks, and relaxed conversation.", "https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=900&q=80"),
                    place(cityName + " Rooftop Cafe", "An easy evening cafe option for views, quick bites, and group plans.", "https://images.unsplash.com/photo-1514933651103-005eec06c04b?w=900&q=80"));
            case "hotels" -> List.of(
                    place(cityName + " Grand Hotel", "Comfortable full-service stay for families, business travelers, and weekend visitors.", "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=900&q=80"),
                    place(cityName + " Heritage Stay", "Character-led stay option with local design cues and central access.", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=900&q=80"),
                    place(cityName + " Riverside Resort", "Relaxed resort-style base for slower travel days and scenic downtime.", "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=900&q=80"));
            case "educational-places" -> List.of(
                    place(cityName + " University Campus", "Higher education hub for academic visits, student activity, and local learning culture.", "https://images.unsplash.com/photo-1562774053-701939374585?w=900&q=80"),
                    place(cityName + " Public Library", "Quiet reading and reference space for students, locals, and slow-travel visitors.", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=900&q=80"),
                    place(cityName + " Science and Culture Centre", "Learning stop for exhibits, workshops, events, and family-friendly discovery.", "https://images.unsplash.com/photo-1532094349884-543bc11b234d?w=900&q=80"),
                    place(cityName + " Regional Museum", "Museum stop for history, art, archives, and cultural context.", "https://images.unsplash.com/photo-1582555172866-f73bb12a2ab3?w=900&q=80"),
                    place(cityName + " Coaching Hub", "Student-focused area for exam prep, classes, bookstores, and quick food breaks.", "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=900&q=80"),
                    place(cityName + " Art Learning Studio", "Creative learning space for art, craft, music, and cultural workshops.", "https://images.unsplash.com/photo-1499364615650-ec38552f4f34?w=900&q=80"));
            default -> List.of();
        };
        fallback.addAll(baseFallback);
        fallback.addAll(generatedFallbackPlaces(cityName, categorySlug, count));
        return dedupePlaceCards(fallback).stream().limit(count).toList();
    }

    private List<PlaceCard> generatedFallbackPlaces(String cityName, String categorySlug, int count) {
        List<String> names = generatedFallbackNames(cityName, categorySlug);
        return names.stream()
                .limit(count)
                .map(name -> place(name, fallbackDescription(name, cityName, categorySlug), fallbackImage(categorySlug)))
                .toList();
    }

    private List<String> generatedFallbackNames(String cityName, String categorySlug) {
        return switch (categorySlug) {
            case "popular-places" -> List.of(
                    cityName + " Old City Quarter",
                    cityName + " Clock Tower Market",
                    cityName + " Riverfront Walk",
                    cityName + " Sunset Point",
                    cityName + " Heritage Gate",
                    cityName + " Main Bazaar",
                    cityName + " Palace Road",
                    cityName + " Temple Street",
                    cityName + " City Lakefront",
                    cityName + " Craft Market",
                    cityName + " Town Hall Square",
                    cityName + " Garden Promenade",
                    cityName + " View Deck",
                    cityName + " Culture Street",
                    cityName + " Food Lane",
                    cityName + " Arts Quarter",
                    cityName + " Heritage Garden",
                    cityName + " Civic Square",
                    cityName + " Fort Road",
                    cityName + " Riverside Steps",
                    cityName + " Memorial Park",
                    cityName + " Photography Point",
                    cityName + " Local Shopping Arcade",
                    cityName + " Traditional Market",
                    cityName + " Weekend Plaza",
                    cityName + " Scenic Drive",
                    cityName + " Community Park",
                    cityName + " Old Bridge",
                    cityName + " Cultural Circle",
                    cityName + " Tourist Information Plaza");
            case "tourist-places" -> List.of(
                    cityName + " Heritage Museum",
                    cityName + " Nature Trail",
                    cityName + " Art Street",
                    cityName + " Riverside Park",
                    cityName + " Fort Viewpoint",
                    cityName + " Local Food Trail",
                    cityName + " Botanical Garden",
                    cityName + " Historic Courtyard",
                    cityName + " Cultural Museum",
                    cityName + " Waterfall Route",
                    cityName + " Village Excursion",
                    cityName + " Hill View Road",
                    cityName + " Handicraft Centre",
                    cityName + " Old Temple Route",
                    cityName + " Evening Market",
                    cityName + " Heritage Haveli",
                    cityName + " Lake Circuit",
                    cityName + " Forest Walk",
                    cityName + " Sculpture Garden",
                    cityName + " Panorama Point",
                    cityName + " Folk Art Centre",
                    cityName + " Archaeology Stop",
                    cityName + " Garden Walk",
                    cityName + " Adventure Base",
                    cityName + " Cultural Performance Hall",
                    cityName + " Old Palace Ruins",
                    cityName + " Local Craft Trail",
                    cityName + " Photography Trail",
                    cityName + " History Walk",
                    cityName + " Scenic Picnic Spot");
            case "cafes" -> List.of(
                    cityName + " Coffee House",
                    cityName + " Old Town Cafe",
                    cityName + " Rooftop Brew",
                    cityName + " Garden Brunch Cafe",
                    cityName + " Artisan Bakehouse",
                    cityName + " Lakeview Cafe",
                    cityName + " Market Street Cafe",
                    cityName + " Book Cafe",
                    cityName + " Courtyard Cafe",
                    cityName + " Breakfast Bar",
                    cityName + " Heritage Espresso",
                    cityName + " Terrace Tea Room",
                    cityName + " Indie Coffee Lab",
                    cityName + " Traveller's Cafe",
                    cityName + " Sunset Bistro",
                    cityName + " Local Roastery",
                    cityName + " Dessert Studio",
                    cityName + " Chai Lounge",
                    cityName + " Riverside Cafe",
                    cityName + " Bakery Corner",
                    cityName + " Community Cafe",
                    cityName + " Greenhouse Cafe",
                    cityName + " Culture Cafe",
                    cityName + " Work and Brew",
                    cityName + " Street Coffee Bar",
                    cityName + " Family Cafe",
                    cityName + " Tapri Stop",
                    cityName + " Pancake House",
                    cityName + " Evening Brew Cafe",
                    cityName + " Slow Table Cafe");
            case "hotels" -> List.of(
                    cityName + " Grand Hotel",
                    cityName + " Heritage Residency",
                    cityName + " Central Inn",
                    cityName + " Palace View Hotel",
                    cityName + " Riverside Retreat",
                    cityName + " Boutique Stay",
                    cityName + " City Centre Hotel",
                    cityName + " Comfort Suites",
                    cityName + " Royal Haveli Stay",
                    cityName + " Lake View Resort",
                    cityName + " Business Hotel",
                    cityName + " Family Resort",
                    cityName + " Hill View Lodge",
                    cityName + " Garden Retreat",
                    cityName + " Premium Residency",
                    cityName + " Traveller's Hostel",
                    cityName + " Courtyard Hotel",
                    cityName + " Gateway Resort",
                    cityName + " Urban Stay",
                    cityName + " Boutique Haveli",
                    cityName + " Scenic Resort",
                    cityName + " Club Hotel",
                    cityName + " Homestay Collective",
                    cityName + " Spa Resort",
                    cityName + " Airport Road Hotel",
                    cityName + " Old Town Guest House",
                    cityName + " Executive Residency",
                    cityName + " Nature Camp",
                    cityName + " Weekend Resort",
                    cityName + " Heritage Lodge");
            case "educational-places" -> List.of(
                    cityName + " Public Library",
                    cityName + " Regional Museum",
                    cityName + " Science Centre",
                    cityName + " Art Gallery",
                    cityName + " Government College",
                    cityName + " Cultural Research Centre",
                    cityName + " Heritage Archive",
                    cityName + " Craft Training Centre",
                    cityName + " Music Academy",
                    cityName + " History Resource Centre",
                    cityName + " Open Learning Centre",
                    cityName + " Language Institute",
                    cityName + " Design Studio",
                    cityName + " Fine Arts School",
                    cityName + " Nature Interpretation Centre",
                    cityName + " Archaeology Museum",
                    cityName + " Community Learning Hall",
                    cityName + " Teacher Training College",
                    cityName + " Technical Institute",
                    cityName + " Youth Skill Centre",
                    cityName + " Performing Arts Centre",
                    cityName + " Digital Learning Lab",
                    cityName + " Children's Discovery Centre",
                    cityName + " Cultural Workshop Space",
                    cityName + " University Extension Centre",
                    cityName + " District Education Office",
                    cityName + " Literature Forum",
                    cityName + " Environmental Learning Trail",
                    cityName + " Museum Learning Gallery",
                    cityName + " Civil Services Study Hub");
            default -> List.of();
        };
    }

    private List<String> supplementalNames(String citySlug, String categorySlug) {
        return switch (citySlug + ":" + categorySlug) {
            case "agra:popular-places" -> List.of("Mehtab Bagh", "Itmad-ud-Daulah", "Akbar's Tomb", "Sadar Bazaar", "Taj Nature Walk", "Kinari Bazaar");
            case "agra:cafes" -> List.of("Sheroes Hangout", "Tea'se Me", "Cafe Turquoise Cottage", "Chapter 1 Cafe", "Good Vibes Cafe Agra", "Joney's Place");
            case "agra:hotels" -> List.of("The Oberoi Amarvilas", "ITC Mughal", "Taj Hotel and Convention Centre Agra", "Trident Agra", "Courtyard by Marriott Agra", "Hotel Clarks Shiraz");
            case "agra:tourist-places" -> List.of("Fatehpur Sikri", "Mariam's Tomb", "Chini Ka Rauza", "Ram Bagh", "Wildlife SOS Bear Rescue Facility", "Korai Village");
            case "agra:educational-places" -> List.of("Dr Bhimrao Ambedkar University", "St John's College", "Agra College", "Taj Museum", "Agra Public Library", "Sanskriti Museum");

            case "varanasi:popular-places" -> List.of("Dashashwamedh Ghat", "Kashi Vishwanath Temple", "Assi Ghat", "Sarnath", "Manikarnika Ghat", "Ramnagar Fort");
            case "varanasi:cafes" -> List.of("Pizzeria Vaatika Cafe", "Brown Bread Bakery", "Blue Lassi", "Aum Cafe", "Terracotta Cafe", "Open Hand Cafe");
            case "varanasi:hotels" -> List.of("BrijRama Palace", "Taj Ganges Varanasi", "The Gateway Hotel Ganges", "Hotel Surya Kaiser Palace", "Amritara Suryauday Haveli", "Ramada Plaza JHV Varanasi");
            case "varanasi:tourist-places" -> List.of("Banaras Hindu University", "Bharat Kala Bhavan", "Tulsi Manas Mandir", "Durga Kund Temple", "Alamgir Mosque", "Chaukhandi Stupa");
            case "varanasi:educational-places" -> List.of("Banaras Hindu University", "Bharat Kala Bhavan Museum", "Sampurnanand Sanskrit University", "Central Hindu School", "Jnana Pravaha Cultural Centre", "Ramnagar Fort Museum");

            case "delhi:popular-places" -> List.of("India Gate", "Red Fort", "Qutub Minar", "Humayun's Tomb", "Lotus Temple", "Connaught Place");
            case "delhi:cafes" -> List.of("AMA Cafe", "The Big Chill Cafe", "Diggin Cafe", "Cafe Lota", "Blue Tokai Khan Market", "Triveni Terrace Cafe");
            case "delhi:hotels" -> List.of("The Imperial New Delhi", "Taj Mahal New Delhi", "The Lodhi", "ITC Maurya", "The Leela Palace New Delhi", "Andaz Delhi");
            case "delhi:tourist-places" -> List.of("Akshardham Temple", "National Museum", "Lodhi Garden", "Agrasen Ki Baoli", "Dilli Haat", "Sunder Nursery");
            case "delhi:educational-places" -> List.of("Delhi University North Campus", "Jawaharlal Nehru University", "National Science Centre", "National Gallery of Modern Art", "India Habitat Centre", "American Center Library");

            case "chandigarh:popular-places" -> List.of("Rock Garden", "Sukhna Lake", "Rose Garden", "Capitol Complex", "Sector 17 Plaza", "Le Corbusier Centre");
            case "chandigarh:cafes" -> List.of("Backpackers Cafe", "Indian Coffee House", "Brooklyn Central", "Cafe JC's", "Nik Baker's", "Books N Brew");
            case "chandigarh:hotels" -> List.of("JW Marriott Chandigarh", "Hyatt Regency Chandigarh", "Taj Chandigarh", "The Lalit Chandigarh", "Hotel Mountview", "Lemon Tree Hotel Chandigarh");
            case "chandigarh:tourist-places" -> List.of("Terraced Garden", "Japanese Garden", "Government Museum and Art Gallery", "Chandigarh Bird Park", "Elante Mall", "Pinjore Gardens");
            case "chandigarh:educational-places" -> List.of("Panjab University", "Government Museum and Art Gallery", "Le Corbusier Centre", "Chandigarh Architecture Museum", "State Library Chandigarh", "PEC Chandigarh");

            case "maharashtra:popular-places" -> List.of("Ajanta Caves", "Ellora Caves", "Shaniwar Wada", "Gateway of India", "Mahabaleshwar Viewpoints", "Elephanta Caves");
            case "maharashtra:cafes" -> List.of("German Bakery Pune", "Vaishali Pune", "Cafe Goodluck", "Kala Ghoda Cafe", "Leaping Windows", "Prithvi Cafe");
            case "maharashtra:hotels" -> List.of("The Taj Mahal Palace Mumbai", "The Westin Pune Koregaon Park", "Le Meridien Mahabaleshwar", "Vivanta Aurangabad", "Radisson Blu Resort Alibaug", "Fariyas Resort Lonavala");
            case "maharashtra:tourist-places" -> List.of("Lonavala", "Alibaug Beach", "Nashik Vineyards", "Matheran", "Sinhagad Fort", "Bhimashankar");
            case "maharashtra:educational-places" -> List.of("Savitribai Phule Pune University", "Deccan College", "Raja Dinkar Kelkar Museum", "Nehru Science Centre Mumbai", "Asiatic Society Library", "Film and Television Institute of India");

            case "kochi:popular-places" -> List.of("Fort Kochi", "Chinese Fishing Nets", "Mattancherry Palace", "Jew Town", "Marine Drive Kochi", "St Francis Church");
            case "kochi:cafes" -> List.of("Kashi Art Cafe", "Qissa Cafe", "Loafers Corner Cafe", "Mocha Art Cafe", "Pepper House Cafe", "Teapot Cafe");
            case "kochi:hotels" -> List.of("Brunton Boatyard", "Grand Hyatt Kochi Bolgatty", "Taj Malabar Resort", "Fragrant Nature Kochi", "Old Harbour Hotel", "Le Meridien Kochi");
            case "kochi:tourist-places" -> List.of("Kerala Folklore Museum", "Bolgatty Palace", "Willingdon Island", "Cherai Beach", "Hill Palace Museum", "Paradesi Synagogue");
            case "kochi:educational-places" -> List.of("Cochin University of Science and Technology", "Kerala Folklore Museum", "Hill Palace Museum", "David Hall Art Gallery", "Ernakulam Public Library", "Kerala Museum");

            case "darjeeling:popular-places" -> List.of("Tiger Hill", "Batasia Loop", "Darjeeling Himalayan Railway", "Mall Road Darjeeling", "Peace Pagoda", "Happy Valley Tea Estate");
            case "darjeeling:cafes" -> List.of("Glenary's", "Keventer's", "Tom and Jerry's Cafe", "Nathmulls Tea Room", "Sonam's Kitchen", "Himalayan Coffee");
            case "darjeeling:hotels" -> List.of("Mayfair Darjeeling", "Windamere Hotel", "The Elgin Darjeeling", "Cedar Inn", "Ramada Darjeeling", "Summit Grace Hotel");
            case "darjeeling:tourist-places" -> List.of("Padmaja Naidu Himalayan Zoological Park", "Himalayan Mountaineering Institute", "Rock Garden Darjeeling", "Tinchuley", "Lamahatta", "Observatory Hill");
            case "darjeeling:educational-places" -> List.of("Himalayan Mountaineering Institute", "St Paul's School Darjeeling", "Loreto College Darjeeling", "Darjeeling Government College", "Tibetan Refugee Self Help Centre", "Darjeeling District Library");

            case "leh:popular-places" -> List.of("Leh Palace", "Shanti Stupa", "Thiksey Monastery", "Hall of Fame Leh", "Main Bazaar Leh", "Sangam Point");
            case "leh:cafes" -> List.of("Bon Appetit Leh", "The Tibetan Kitchen", "Lehvenda Cafe", "OpenHand Cafe Leh", "Brazil Cafe Leh", "Yama Coffee House");
            case "leh:hotels" -> List.of("The Grand Dragon Ladakh", "Ladakh Sarai", "The Indus Valley", "Hotel Sten-Del", "The Zen Ladakh", "Chamba Camp Thiksey");
            case "leh:tourist-places" -> List.of("Magnetic Hill", "Nubra Valley", "Pangong Lake", "Khardung La", "Hemis Monastery", "Alchi Monastery");
            case "leh:educational-places" -> List.of("Central Institute of Buddhist Studies", "Students' Educational and Cultural Movement of Ladakh", "Hall of Fame Museum", "Ladakh Arts and Media Organisation", "Munshi Aziz Bhat Museum", "District Library Leh");

            case "srinagar:popular-places" -> List.of("Dal Lake", "Shalimar Bagh", "Nishat Bagh", "Hazratbal Shrine", "Pari Mahal", "Lal Chowk");
            case "srinagar:cafes" -> List.of("Winterfell Cafe", "Le Delice Srinagar", "Cafe Liberty", "Books and Bricks Cafe", "Chai Jaai", "14th Avenue Cafe");
            case "srinagar:hotels" -> List.of("The Lalit Grand Palace", "Vivanta Dal View", "Radisson Srinagar", "Fortune Resort Heevan", "RK Sarovar Portico", "Houseboat Sukoon");
            case "srinagar:tourist-places" -> List.of("Chashme Shahi", "Indira Gandhi Memorial Tulip Garden", "Shankaracharya Temple", "Old Srinagar", "Gulmarg Day Trip", "Dachigam National Park");
            case "srinagar:educational-places" -> List.of("University of Kashmir", "Sri Pratap Singh Museum", "Kashmir Government Arts Emporium", "Srinagar Central Library", "NIT Srinagar", "Institute of Music and Fine Arts Kashmir");

            case "manali:popular-places" -> List.of("Hadimba Temple", "Solang Valley", "Old Manali", "Manu Temple", "Mall Road Manali", "Vashisht Hot Springs");
            case "manali:cafes" -> List.of("Cafe 1947", "Johnson's Cafe", "Drifters Cafe", "The Lazy Dog", "Renaissance Manali", "Dylan's Toasted and Roasted");
            case "manali:hotels" -> List.of("The Himalayan", "Span Resort and Spa", "ManuAllaya Resort", "Apple Country Resort", "Johnson Lodge", "Welcomhotel by ITC Hotels Hamsa Manali");
            case "manali:tourist-places" -> List.of("Rohtang Pass", "Jogini Falls", "Atal Tunnel", "Naggar Castle", "Hampta Pass", "Beas River Viewpoint");
            case "manali:educational-places" -> List.of("Himalayan Nyinmapa Buddhist Monastery", "Museum of Himachal Culture and Folk Art", "Government College Haripur Manali", "Manali Public Library", "Mountaineering Institute Aleo", "Nicholas Roerich Art Gallery Naggar");

            case "shimla:popular-places" -> List.of("The Ridge", "Mall Road Shimla", "Jakhoo Temple", "Christ Church", "Scandal Point", "Viceregal Lodge");
            case "shimla:cafes" -> List.of("Cafe Simla Times", "Wake and Bake", "Honey Hut", "Indian Coffee House Shimla", "Cafe Sol", "The Devicos Cafe");
            case "shimla:hotels" -> List.of("Wildflower Hall", "The Oberoi Cecil", "Clarkes Hotel", "Radisson Hotel Shimla", "The Willow Banks", "Woodville Palace Hotel");
            case "shimla:tourist-places" -> List.of("Kufri", "Indian Institute of Advanced Study", "Annandale", "Chadwick Falls", "Mashobra", "Tara Devi Temple");
            case "shimla:educational-places" -> List.of("Indian Institute of Advanced Study", "Himachal Pradesh University", "State Library Shimla", "Gaiety Heritage Cultural Complex", "Army Heritage Museum Annandale", "Himachal State Museum");

            case "rishikesh:popular-places" -> List.of("Laxman Jhula", "Triveni Ghat", "Ram Jhula", "Parmarth Niketan", "Beatles Ashram", "Neer Garh Waterfall");
            case "rishikesh:cafes" -> List.of("Little Buddha Cafe", "Bistro Nirvana", "The 60's Cafe", "Ganga View Cafe", "Freedom Cafe", "Shambala Cafe");
            case "rishikesh:hotels" -> List.of("Aloha on the Ganges", "Taj Rishikesh Resort and Spa", "EllBee Ganga View", "Ganga Kinare", "Divine Resort Rishikesh", "Lemon Tree Premier Rishikesh");
            case "rishikesh:tourist-places" -> List.of("Rafting on the Ganga", "Neelkanth Mahadev Temple", "Kunjapuri Temple", "Rajaji National Park", "Patna Waterfall", "Vashishta Cave");
            case "rishikesh:educational-places" -> List.of("Parmarth Niketan Yoga Centre", "Yoga Niketan Ashram", "AIIMS Rishikesh Campus", "Swami Rama Sadhaka Grama", "Rishikesh Public Library", "Beatles Ashram Art Trail");

            case "chamba:popular-places" -> List.of("Chamba Town", "Lakshmi Narayan Temple", "Khajjiar", "Chamunda Devi Temple", "Chaugan", "Akhand Chandi Palace");
            case "chamba:cafes" -> List.of("Cafe Ravi View", "Mountain Brew Chamba", "Town Corner Cafe", "Cafe Aroma Chamba", "The Kettle House Chamba", "Khajjiar Meadow Cafe");
            case "chamba:hotels" -> List.of("Aroha Resort", "Hotel Iravati", "Khajjiar Retreat", "HPTDC Hotel Champak", "Hotel Ashiana Regency", "NotOnMap H2O House");
            case "chamba:tourist-places" -> List.of("Bhuri Singh Museum", "Rang Mahal", "Khajjiar Lake", "Manimahesh Lake", "Kalatop Wildlife Sanctuary", "Sui Mata Temple");
            case "chamba:educational-places" -> List.of("Bhuri Singh Museum", "Government College Chamba", "Chamba Public Library", "Rang Mahal Craft Centre", "District Museum Learning Gallery", "Minjar Fair Cultural Grounds");

            default -> List.of();
        };
    }

    private String fallbackDescription(String placeName, String cityName, String categorySlug) {
        return switch (categorySlug) {
            case "popular-places" -> placeName + " is a recognizable " + cityName + " stop that works well for first-time sightseeing, photos, and route planning.";
            case "tourist-places" -> placeName + " adds a deeper visitor experience in " + cityName + " with local context, scenery, or cultural value.";
            case "cafes" -> placeName + " is a practical cafe pick in " + cityName + " for coffee, snacks, relaxed meetups, and short breaks.";
            case "hotels" -> placeName + " is a reliable stay option in " + cityName + " for comfort, access, and a smoother travel base.";
            case "educational-places" -> placeName + " gives " + cityName + " a learning-focused stop through academics, museums, libraries, culture, or public knowledge spaces.";
            default -> placeName + " is a useful " + cityName + " listing for planning a complete city route.";
        };
    }

    private String fallbackImage(String categorySlug) {
        return switch (categorySlug) {
          case "popular-places" -> "https://website-bf3e422f.hey.ayf.mybluehost.me/wp-content/uploads/2013/01/gwalior-fort-madhya-pradesh.jpg.jpg";
            case "cafes" -> "https://dt4l9bx31tioh.cloudfront.net/eazymedia/eazytrendz/4912/trend20250707110937.jpg";
            case "hotels" -> "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/30/b0/1b/8e/caption.jpg?w=1200&h=-1&s=1";
            case "tourist-places" -> "https://www.flamingotravels.co.in/blog/wp-content/uploads/2025/10/best-places-to-visit-in-India.jpg";
            case "educational-places" -> "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/18/0f/81/51/advance-study-shimla.jpg?w=500&h=500&s=1";
            case "emergency-services" -> "https://amcarehospital.com/wp-content/uploads/2023/12/Are-Hospitals-Making-as-Much-Money-as-You-Think1.jpg";
            default -> "https://hblimg.mmtcdn.com/content/hubble/img/delhi/mmt/activities/m_activities_delhi_red_fort_l_341_817.jpg";
        };
    }

    private String titleFromSlug(String slug) {
        return List.of(slug.split("-")).stream()
                .filter(part -> !part.isBlank())
                .map(part -> part.substring(0, 1).toUpperCase(Locale.ENGLISH) + part.substring(1))
                .collect(Collectors.joining(" "));
    }

    private CityPage mergeCityWithManagedContent(CityPage city, String citySlug) {
        Map<String, List<PlaceCard>> managedByCategory = getManagedPlacesForCity(citySlug).stream()
                .collect(Collectors.groupingBy(
                        ManagedPlace::getCategorySlug,
                        LinkedHashMap::new,
                        Collectors.mapping(this::toManagedPlaceCard, Collectors.toList())));

        List<CategoryPage> mergedCategories = new ArrayList<>();
        for (CategoryPage category : city.categories()) {
            List<PlaceCard> mergedPlaces = new ArrayList<>(category.places());
            mergedPlaces.addAll(managedByCategory.getOrDefault(category.slug(), List.of()));
            mergedCategories.add(new CategoryPage(
                    category.slug(),
                    category.name(),
                    category.summary(),
                    category.heroImage(),
                    mergedPlaces));
        }

        for (String categorySlug : getAdminCategoryOptions()) {
            boolean exists = mergedCategories.stream().anyMatch(category -> category.slug().equals(categorySlug));
            if (!exists) {
                mergedCategories.add(new CategoryPage(
                        categorySlug,
                        labelForCategory(categorySlug),
                        summaryForCategory(categorySlug, city.name()),
                        categoryHeroImage(categorySlug),
                        managedByCategory.getOrDefault(categorySlug, List.of())));
            }
        }

        List<QuickFact> facts = new ArrayList<>(city.facts());
        long managedCount = getManagedPlacesForCity(citySlug).size();
        if (managedCount > 0) {
            facts.add(new QuickFact(String.valueOf(managedCount), "Admin Adds"));
        }

        List<String> highlights = new ArrayList<>(city.highlights());
        if (managedCount > 0) {
            highlights.add("Includes admin-managed updates for cafes, hotels, tourist places, and educational spots.");
        }

        return new CityPage(
                city.slug(),
                city.name(),
                city.tagline(),
                city.heroImage(),
                city.region(),
                facts,
                mergedCategories,
                highlights);
    }

    private List<ManagedPlace> getManagedPlacesForCity(String citySlug) {
        return managedPlaceRepository.findByCitySlugOrderByNameAsc(citySlug);
    }

    private CategoryPage category(String slug, String name, String summary, String heroImage, List<PlaceCard> places) {
        return new CategoryPage(slug, name, summary, heroImage, places);
    }

    private String categoryHeroImage(String categorySlug) {
        return switch (categorySlug) {
            case "popular-places" -> "https://website-bf3e422f.hey.ayf.mybluehost.me/wp-content/uploads/2013/01/gwalior-fort-madhya-pradesh.jpg.jpg";
            case "cafes" -> "https://dt4l9bx31tioh.cloudfront.net/eazymedia/eazytrendz/4912/trend20250707110937.jpg";
            case "hotels" -> "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/30/b0/1b/8e/caption.jpg?w=1200&h=-1&s=1";
            case "tourist-places" -> "https://www.flamingotravels.co.in/blog/wp-content/uploads/2025/10/best-places-to-visit-in-India.jpg";
            case "educational-places" -> "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/18/0f/81/51/advance-study-shimla.jpg?w=500&h=500&s=1";
            case "emergency-services" -> "https://amcarehospital.com/wp-content/uploads/2023/12/Are-Hospitals-Making-as-Much-Money-as-You-Think1.jpg";
            default -> "https://hblimg.mmtcdn.com/content/hubble/img/delhi/mmt/activities/m_activities_delhi_red_fort_l_341_817.jpg";
        };
    }

    @SafeVarargs
    private List<PlaceCard> joinPlaceCards(List<PlaceCard>... groups) {
        List<PlaceCard> places = new ArrayList<>();
        for (List<PlaceCard> group : groups) {
            places.addAll(group);
        }
        return dedupePlaceCards(places);
    }

    private List<PlaceCard> dedupePlaceCards(List<PlaceCard> places) {
        Map<String, PlaceCard> unique = new LinkedHashMap<>();
        for (PlaceCard place : places) {
            unique.putIfAbsent(place.slug(), place);
        }
        return new ArrayList<>(unique.values());
    }

    private List<PlaceCard> adaptPlaces(List<PlaceCard> source, String cityName, String categorySlug, int limit) {
        return source.stream()
                .limit(limit)
                .map(place -> new PlaceCard(
                        place.slug(),
                        place.name(),
                        adaptedCategoryDescription(place.name(), cityName, categorySlug, place.description()),
                        place.image()))
                .toList();
    }

    private String adaptedCategoryDescription(String placeName, String cityName, String categorySlug, String original) {
        return switch (categorySlug) {
            case "famous-spots" -> placeName + " is one of the recognizable " + cityName + " spots visitors usually associate with the city. " + original;
            case "attractions" -> placeName + " works well as an attraction-style stop for photos, sightseeing, and route planning in " + cityName + ". " + original;
            case "hidden-gems" -> placeName + " gives a softer, less-rushed side of " + cityName + " for visitors who want something beyond the obvious checklist. " + original;
            case "restaurants" -> placeName + " can fit a food-led plan around " + cityName + ", especially when paired with nearby markets, cafes, or sightseeing stops. " + original;
            default -> original;
        };
    }

    private List<PlaceCard> hiddenGemPlaces(String cityName,
                                            List<PlaceCard> popularPlaces,
                                            List<PlaceCard> touristPlaces,
                                            List<PlaceCard> educationalPlaces) {
        List<PlaceCard> pool = joinPlaceCards(touristPlaces, educationalPlaces, popularPlaces);
        int skip = Math.min(3, pool.size());
        return adaptPlaces(pool.stream().skip(skip).toList(), cityName, "hidden-gems", 30);
    }

    private List<PlaceCard> restaurantPlaces(String cityName, List<PlaceCard> cafes) {
        List<PlaceCard> restaurants = adaptPlaces(cafes, cityName, "restaurants", 30);
        if (!restaurants.isEmpty()) {
            return restaurants;
        }
        return List.of(
                place(cityName + " Local Food Street", "A city-specific dining route for snacks, quick meals, and casual food exploration.", categoryHeroImage("restaurants")),
                place(cityName + " Central Dining Hub", "A practical restaurant area for lunch, dinner, and group food plans.", categoryHeroImage("restaurants")),
                place(cityName + " Traditional Thali Stop", "A starter food card for regional meals and local dining experiences.", categoryHeroImage("restaurants")));
    }

    private List<PlaceCard> mallPlaces(String citySlug, String cityName) {
        return switch (citySlug) {
            case "jaipur" -> List.of(
                    place("World Trade Park Jaipur", "Large shopping, dining, entertainment, and movie hub in Jaipur.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/15/bc/3a/e8/world-trade-park.jpg?w=900&h=500&s=1"),
                    place("Triton Mall Jaipur", "Popular mall for shopping, food court plans, and casual family outings.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/0f/4d/50/79/triton-mall.jpg?w=900&h=500&s=1"),
                    place("GT Central Jaipur", "Shopping and entertainment mall close to Malviya Nagar and Jawahar Circle.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/10/0f/72/0d/gt-central.jpg?w=900&h=500&s=1"));
            case "chandigarh" -> List.of(
                    place("Elante Mall", "Major Chandigarh mall for shopping, movies, dining, and weekend plans.", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/11/d5/50/25/elante-mall.jpg?w=900&h=500&s=1"),
                    place("DLF City Centre Mall Chandigarh", "Shopping and food stop around IT Park and central Chandigarh routes.", "https://images.unsplash.com/photo-1519567241046-7f570eee3ce6?w=900&q=80"),
                    place("VR Punjab", "Large Mohali mall with retail, food, and entertainment near Chandigarh.", "https://images.unsplash.com/photo-1519567241046-7f570eee3ce6?w=900&q=80"));
            case "goa" -> List.of(
                    place("Mall de Goa", "Goa shopping mall with stores, food options, and cinema plans.", "https://images.unsplash.com/photo-1519567241046-7f570eee3ce6?w=900&q=80"),
                    place("Caculo Mall Panaji", "Central Panaji shopping stop with food and retail options.", "https://images.unsplash.com/photo-1519567241046-7f570eee3ce6?w=900&q=80"),
                    place("18th June Road Shopping", "Panaji shopping route for local stores, cafes, and quick city browsing.", "https://images.unsplash.com/photo-1555529771-835f59fc5efe?w=900&q=80"));
            default -> List.of(
                    place(cityName + " City Mall", "Shopping, food court, movies, and easy group plans in " + cityName + ".", categoryHeroImage("malls")),
                    place(cityName + " Central Market", "Retail, street shopping, snacks, and local browsing in " + cityName + ".", "https://images.unsplash.com/photo-1555529771-835f59fc5efe?w=900&q=80"),
                    place(cityName + " Entertainment Hub", "A mall-style stop for shopping, cafes, and casual outings in " + cityName + ".", categoryHeroImage("malls")));
        };
    }

    private List<PlaceCard> nightlifePlaces(String citySlug, String cityName, List<PlaceCard> cafes) {
        List<PlaceCard> base = switch (citySlug) {
            case "jaipur" -> List.of(
                    place("Blackout Jaipur", "Rooftop nightlife spot known for DJ nights and group plans.", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=900&q=80"),
                    place("House of People Jaipur", "Dance, music, dining, and weekend crowd energy in Jaipur.", "https://images.unsplash.com/photo-1566737236500-c8ac43014a8e?w=900&q=80"),
                    place("Townsend Jaipur", "Cafe-bar and restaurant setting for dinner and evening plans.", "https://images.unsplash.com/photo-1514933651103-005eec06c04b?w=900&q=80"));
            case "chandigarh" -> List.of(
                    place("Peddlers Chandigarh", "Live music, pub food, and energetic weekend nightlife.", "https://images.unsplash.com/photo-1514933651103-005eec06c04b?w=900&q=80"),
                    place("Paara Chandigarh", "Rooftop lounge-style nightlife with group dinner energy.", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=900&q=80"),
                    place("The Back Room Chandigarh", "Late-night music, drinks, and social crowd plans.", "https://images.unsplash.com/photo-1566737236500-c8ac43014a8e?w=900&q=80"));
            case "goa" -> List.of(
                    place("Tito's Lane", "Famous Goa nightlife stretch with clubs, music, and late-night energy.", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=900&q=80"),
                    place("Curlies Anjuna", "Beach-shack nightlife and party mood near Anjuna.", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=900&q=80"),
                    place("Club Cubana", "Goa club experience with music, lights, and party crowd.", "https://images.unsplash.com/photo-1566737236500-c8ac43014a8e?w=900&q=80"));
            default -> List.of();
        };
        List<PlaceCard> cafeEvening = cafes.stream()
                .limit(4)
                .map(cafe -> new PlaceCard(cafe.slug(), cafe.name(), cafe.name() + " can work as an evening hangout or late cafe plan in " + cityName + ". " + cafe.description(), cafe.image()))
                .toList();
        return joinPlaceCards(base, cafeEvening, List.of(
                place(cityName + " Late Food Street", "A city-specific late food and snack route for group-night plans.", categoryHeroImage("nightlife")),
                place(cityName + " Music Lounge Area", "Starter nightlife card for lounges, music, and social evenings in " + cityName + ".", categoryHeroImage("nightlife"))));
    }

    private GenZModeData buildGenZModeData(CityPage city) {
        Optional<GenZModeData> customGenZData = customGenZModeData(city);
        if (customGenZData.isPresent()) {
            return customGenZData.get();
        }

        List<GenZPlace> cafes = genZPlacesFromCategory(city, "cafes", "cafe");
        List<GenZPlace> restaurants = genZPlacesFromCategory(city, "restaurants", "cafe");
        List<GenZPlace> popular = genZPlacesFromCategory(city, "popular-places", "place");
        List<GenZPlace> famous = genZPlacesFromCategory(city, "famous-spots", "place");
        List<GenZPlace> attractions = genZPlacesFromCategory(city, "attractions", "activity");
        List<GenZPlace> tourist = genZPlacesFromCategory(city, "tourist-places", "place");
        List<GenZPlace> hidden = genZPlacesFromCategory(city, "hidden-gems", "place");
        List<GenZPlace> malls = genZPlacesFromCategory(city, "malls", "activity");
        List<GenZPlace> nightlife = genZPlacesFromCategory(city, "nightlife", "club");
        List<GenZPlace> educational = genZPlacesFromCategory(city, "educational-places", "place");

        List<GenZPlace> cafePool = ensureGenZPlaces(joinGenZ(cafes, restaurants), city, "cafe");
        List<GenZPlace> placePool = ensureGenZPlaces(joinGenZ(popular, famous, attractions, tourist, hidden, malls, educational), city, "place");

        List<GenZPlace> bunkSpots = takeGenZ(joinGenZ(placePool, cafePool), 8);
        List<GenZPlace> hiddenGems = takeGenZ(joinGenZ(hidden, tourist, educational, popular), 8);
        List<GenZPlace> trending = takeGenZ(joinGenZ(famous, popular, attractions, cafePool, tourist), 8);
        List<GenZPlace> surprise = takeGenZ(joinGenZ(cafePool, placePool, nightlife, cityActivityIdeas(city)), 10);
        List<GenZPlace> clubs = takeGenZ(joinGenZ(nightlife, genZNightlife(city)), 10);

        Map<String, List<GenZPlace>> vibe = new LinkedHashMap<>();
        vibe.put("chill", takeGenZ(joinGenZ(cafePool, educational, popular), 10));
        vibe.put("party", takeGenZ(joinGenZ(clubs, cafePool, cityActivityIdeas(city)), 10));
        vibe.put("romantic", takeGenZ(joinGenZ(popular, cafePool, tourist), 10));
        vibe.put("solo", takeGenZ(joinGenZ(educational, tourist, popular, cafePool), 10));

        return new GenZModeData(
                bunkSpots,
                hiddenGems,
                buildGenZPairs(cafePool, placePool, clubs),
                surprise,
                vibe,
                trending,
                clubs);
    }

    private Optional<GenZModeData> customGenZModeData(CityPage city) {
        return switch (city.slug()) {
          case "jaipur" -> Optional.of(genZCustomCity(city,

  // Main Highlights
        List.of(
                genZCustomPlace(city, "Amer sunrise ride", "Fort-side morning ride for friends, reels and calm Jaipur views.", "activity", "https://www.rajasthantourdriver.com/wp-content/uploads/2026/03/amber-fort-jaipur-boat.webp"),
                genZCustomPlace(city, "Nahargarh sunset crew", "Golden-hour fort viewpoint perfect for reels and friend-group photos.", "place", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT8C0X3MAYR55cyyZ6KJ2iRn-i5r1tENQmqdg&s"),
                genZCustomPlace(city, "Sambhar Lake roadtrip", "One-day bunk escape with open roads and surreal salt-lake views.", "place", "https://s7ap1.scene7.com/is/image/incredibleindia/sambhar-lake-jaipur-rajasthan-1-attr-hero?qlt=82&ts=1742161079901"),
                genZCustomPlace(city, "Jal Mahal night drive", "Late-evening city drive with music, lights and calm lake scenes.", "activity", "https://s7ap1.scene7.com/is/image/incredibleindia/jal-mahal-jaipur-rajasthan-2-attr-hero?qlt=82&ts=1742162626393"),
                genZCustomPlace(city, "Chokhi Dhani fun plan", "Rajasthani-night vibe with food, music and chaotic friend energy.", "activity", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/16/64/14/90/chokhi-dhani-resort.jpg?w=900&h=500&s=1"),
                genZCustomPlace(city, "Jaigarh cannon trail", "Fort-side bunk plan with viewpoints and cinematic backgrounds.", "place", "https://india-custom-tours.com/wp-content/uploads/2025/06/cannon1-300x240.png"),
                genZCustomPlace(city, "Pink City scooter ride", "Vintage scooter ride through aesthetic Jaipur streets.", "activity", "https://i.ytimg.com/vi/_4W29zWUrUs/sddefault.jpg"),
                genZCustomPlace(city, "Dagla-The Rooftop", "Rainy rooftop vibe with pakoras and skyline views.", "restaurant", "https://lh3.googleusercontent.com/gps-cs-s/APNQkAF0BKjBLsuI9fm5WYllauDMf15mm6quqvLepR7Iiirlt20OTJ25V8NkYFDF0W4QSHxFQbnq6NsdBP_MwW9WJXh6sgq_XajZeTqtu2E42-epiMKJ9fr7GoQhdnf2pcZBUCYg3aszuw=s680-w680-h510-rw"), // Representative aesthetic
                genZCustomPlace(city, "Stepwell picnic stop", "Cute lowkey picnic scene with aesthetic heritage backdrop.", "activity", "https://images.travelandleisureasia.com/wp-content/uploads/sites/2/2019/12/Iconic-step-wells-of-India-feature.jpg"),
                genZCustomPlace(city, "Leopard safari mini escape", "Adventure-style half-day wildlife and hill-road plan.", "activity", "https://xperienceadventure.com/wp-content/uploads/2023/09/3_1647583150-870x555.jpg")
        ),

        // Hidden Gems
        List.of(
                genZCustomPlace(city, "Panna Meena photo drop", "Quiet stepwell stop with aesthetic frames and less tourist rush.", "place", "https://s7ap1.scene7.com/is/image/incredibleindia/panna-meena-ka-kund-jaipur-rajasthan-2?qlt=82&ts=1742190826164"),
                genZCustomPlace(city, "Hidden haveli rooftop", "Underrated rooftop view with old-city sunset vibe.", "place", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/05/21/70/a6/khatu-haveli.jpg?w=900&h=500&s=1"),
                genZCustomPlace(city, "Vintage bookstore cafe", "Cozy café for coffee, books and rainy-day mood.", "cafe", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/30/f5/79/39/ambience.jpg?w=900&h=-1&s=1"),
                genZCustomPlace(city, "Elefanjoy Elephant Sanctuary", "Experience the magic of Elefanjoy, Jaipur's premier ethical elephant sanctuary", "place", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/29/01/15/d5/caption.jpg?w=700&h=400&s=1"),
                genZCustomPlace(city, "Quiet Jal Mahal corner", "Low-crowd lake side spot for soft evening conversations.", "place", "https://jaipurthrumylens.com/wp-content/uploads/2022/09/badal-mahal-tibari-jal-mahal-jaipur-architecture.jpg"),
                genZCustomPlace(city, "Hidden pottery workshop", "Cute creative pottery session with indie Gen-Z vibe.", "activity", "https://media-cdn.tripadvisor.com/media/attractions-splice-spp-674x446/06/6b/6a/14.jpg"),
                genZCustomPlace(city, "Monkey Temple", "Monkey Temple is worth visiting for the amazing views and the playful monkeys.", "place", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/0a/02/8c/12/monkey-temple-galta-ji.jpg?w=700&h=400&s=1"),
                genZCustomPlace(city, "Old-city thrift lane", "Underrated shopping lane for silver jewellery and tote bags.", "shopping", "https://ilovejaipur.city/media/posts/2025/11/shopping-jaipur.webp"),
                genZCustomPlace(city, "Hawk View Restaurant & Bar", "The Restaurant is on The Roof of a Heritage built Hotel Royal Sheraton and is located near by the City Palace . ", "food", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/11/d0/71/d9/exclusive-view-of-nahar.jpg?w=900&h=500&s=1"),
                genZCustomPlace(city, "Rusirani Village", "Rusirani is a village near Jaipur which is about 2500 years old. ", "place", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/16/3c/9b/25/discussion-with-locals.jpg?w=700&h=400&s=1")
        ),

        // Trending & Surprise
        List.of(
                genZCustomPlace(city, "Patrika Gate reel stop", "Most viral Jaipur photo-dump location for Gen-Z creators.", "place", "https://patrikagate.org/wp-content/uploads/2025/01/Patrika_gate_Jaipur-banner.jpg"),
                genZCustomPlace(city, "Hawa Mahal blue-hour view", "Soft evening city vibe for dates and aesthetic photos.", "place", "https://miro.medium.com/1*fYA-b-KA9UUqPL2OsDYkQw.png"),
                genZCustomPlace(city, "Pink City night walk", "Night aesthetic route with lights and old-city charm.", "place", "https://media-cdn.tripadvisor.com/media/attractions-splice-spp-674x446/09/76/47/5a.jpg"),
                genZCustomPlace(city, "Vintage car photoshoot", "Classic Jaipur luxury vibe for aesthetic content shoots.", "activity", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTxzbPWCtNDE2K7mBdjgWHcmj_FfN_4TBZ9FQ&s"),
                genZCustomPlace(city, "Drone-shot fort viewpoint", "Wide city-view point perfect for cinematic reels.", "place", "https://i.ytimg.com/vi/W0TsF1M1ppo/hq720.jpg?sqp=-oaymwEhCK4FEIIDSFryq4qpAxMIARUAAAAAGAElAADIQj0AgKJD&rs=AOn4CLBoaftAyTp7oIKe4fpadeuJDTq1_w")
        
        ),       
        // clubs / nightlife
        List.of(
                  genZCustomPlace(city, "Blackout Club Jaipur", "High-energy nightlife spot with EDM beats, neon vibes, and packed weekend parties.", "club", "https://images.jdmagicbox.com/comp/jaipur/h2/0141px141.x141.140813175149.y3h2/catalogue/blackout-restaurant-c-scheme-jaipur-north-indian-restaurants-9rjmctt2iz.jpg"),
genZCustomPlace(city, "House of People", "One of Jaipur’s most popular Gen-Z party hubs with rooftop music and dance nights.", "club", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/0c/3d/63/8c/house-of-people.jpg?w=900&h=500&s=1"),
genZCustomPlace(city, "Club Trove", "Stylish nightlife destination known for DJs, luxury interiors, and trendy crowd.", "club", "https://b.zmtcdn.com/data/pictures/1/18954411/49f94e1ce35cb0bad7601fd299d45332.jpg?fit=around|750:500&crop=750:500;*,*"),
genZCustomPlace(city, "Native Cocktail Room", "Modern Jaipur party lounge with aesthetic interiors, cocktails, and live music vibes.", "club", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/2f/5d/e9/c8/caption.jpg?w=900&h=500&s=1"),
genZCustomPlace(city, "Rosado Jaipur", "Luxury rooftop club famous for sunset parties, live DJs, and Instagram-worthy ambience.", "club", "https://b.zmtcdn.com/data/pictures/9/20417119/327bf87e3430c6e983274779ecf620d6.jpg")

        )));

case "agra" -> Optional.of(genZCustomCity(city,

        // bunk spots
        List.of(
                genZCustomPlace(city, "Taj sunrise walk", "Early-morning calm Taj vibe with friends and soft sunlight.", "activity", "https://images.unsplash.com/photo-1564507592333-c60657eea523?w=1200&q=80"),
                genZCustomPlace(city, "Fatehpur Sikri escape", "One-day bunk trip with red architecture and cinematic roads.", "place", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=1200&q=80"),
                genZCustomPlace(city, "Yamuna riverside drive", "Lowkey sunset-drive plan with peaceful river views.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Tajganj cafe crawl", "Café hopping with rooftop views and Gen-Z vibe.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Late-night petha run", "Sweet-snack night drive with chaotic friend energy.", "food", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Mehtab sunset picnic", "Soft riverside picnic with aesthetic Taj backdrop.", "activity", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),
                genZCustomPlace(city, "Keetham Lake ride", "Mini escape with lakeside roads and calm weather.", "place", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80"),
                genZCustomPlace(city, "Old Agra scooter ride", "Vintage scooter reels through heritage streets.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Street-food challenge night", "Friends trying random Agra food spots together.", "food", "https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?w=1200&q=80"),
                genZCustomPlace(city, "Mini rooftop brunch", "Slow brunch plan with skyline and coffee vibe.", "cafe", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=1200&q=80")
        ),

        // hidden gems
        List.of(
                genZCustomPlace(city, "Mehtab Bagh quiet frame", "Peaceful Taj-view garden for aesthetic sunset photos.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),
                genZCustomPlace(city, "Hidden Mughal lane", "Quiet heritage streets for reels and vintage edits.", "place", "https://images.unsplash.com/photo-1480714378408-67cf0d13bc1b?w=1200&q=80"),
                genZCustomPlace(city, "Indie rooftop chai spot", "Underrated rooftop tea stop with skyline vibes.", "cafe", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Silent riverside point", "Calm riverside aesthetic away from tourist noise.", "place", "https://images.unsplash.com/photo-1500534623283-312aade485b7?w=1200&q=80"),
                genZCustomPlace(city, "Hidden art cafe", "Minimal indie café with music and aesthetic interiors.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),
                genZCustomPlace(city, "Vintage book corner cafe", "Bookstore café with cozy rainy-day energy.", "cafe", "https://images.unsplash.com/photo-1526243741027-444d633d7365?w=1200&q=80"),
                genZCustomPlace(city, "Secret rooftop sunset", "Minimal-crowd skyline spot for calm evening photos.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Old-city graffiti lane", "Street-art walls for Gen-Z outfit reels.", "place", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=1200&q=80"),
                genZCustomPlace(city, "Quiet tea terrace", "Lowkey tea place with fairy lights and soft music.", "food", "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=1200&q=80"),
                genZCustomPlace(city, "Hidden pottery studio", "Creative pottery stop with indie aesthetic vibe.", "activity", "https://images.unsplash.com/photo-1517048676732-d65bc937f952?w=1200&q=80")
        ),

        // surprise
        List.of(
                genZCustomPlace(city, "Yamuna river photo loop", "Camera-ready riverside route for cinematic reels.", "place", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80"),
                genZCustomPlace(city, "Rooftop Taj dinner", "Soft evening dinner with skyline views and lights.", "place", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight Taj viewpoint", "Night aesthetic Taj-view vibe with calm atmosphere.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Open-air movie cafe", "Movie-night setup with snacks and fairy lights.", "activity", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&q=80"),
                genZCustomPlace(city, "Vintage scooter reels", "Old-school Agra streets for aesthetic content shoots.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Late-night kulhad pizza", "Chaotic Gen-Z food vibe with late-night energy.", "food", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Sunrise yoga garden", "Peaceful early-morning aesthetic with calm vibes.", "activity", "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=1200&q=80"),
                genZCustomPlace(city, "DIY picnic rooftop", "Cute rooftop picnic with fairy lights and reels.", "activity", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic rooftop cafe", "Indie songs and aesthetic rooftop dinner vibe.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Monsoon chai drive", "Rain-drive playlist vibe through Agra streets.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80")
        ),

        // trending
        List.of(
                genZCustomPlace(city, "Taj aesthetic sunrise reels", "Most viral Agra reel vibe with golden sunlight.", "place", "https://images.unsplash.com/photo-1564507592333-c60657eea523?w=1200&q=80"),
                genZCustomPlace(city, "Luxury dessert cafe", "Pinterest-style café interiors and dessert shots.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),
                genZCustomPlace(city, "Streetwear market lane", "Indie shopping spot with accessories and Gen-Z fits.", "shopping", "https://images.unsplash.com/photo-1521334884684-d80222895322?w=1200&q=80"),
                genZCustomPlace(city, "Drone-shot Taj backdrop", "Wide cinematic city-view reel location.", "place", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Live rooftop music scene", "Late-evening acoustic café with aesthetic lights.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Mirror aesthetic cafe", "Pinterest-core café interiors and soft lighting.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Night skyline reels", "Night city visuals for cinematic transition edits.", "place", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Thrift jewellery lane", "Cute rings, tote bags and indie-fashion shopping.", "shopping", "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1200&q=80"),
                genZCustomPlace(city, "Vintage jeep photoshoot", "Luxury jeep setup for aesthetic reels.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Neon gaming cafe", "Gaming plus snacks plus loud Gen-Z vibe.", "activity", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80")
        ),

        // clubs / nightlife
        List.of(
                genZCustomPlace(city, "Sadar Bazaar snack night", "Late-night food street chaos and group energy.", "club", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Rooftop DJ setup", "Music-heavy rooftop night with skyline atmosphere.", "club", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&q=80"),
                genZCustomPlace(city, "Midnight chai terrace", "Late-night tea scene with aesthetic city lights.", "club", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Open mic indie night", "Poetry and acoustic music with cozy Gen-Z crowd.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "After-dark rooftop lounge", "Skyline lounge vibe with music and mocktails.", "club", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight terrace party", "Open-air dance setup with fairy lights.", "club", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic campfire jam", "Night guitar sessions and chill rooftop energy.", "club", "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=1200&q=80"),
                genZCustomPlace(city, "Weekend neon rave", "Heavy DJ beats and Gen-Z dance-floor chaos.", "club", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=1200&q=80"),
                genZCustomPlace(city, "Late-night dessert adda", "Midnight dessert scene with friends and music.", "club", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Afterparty gaming zone", "Gaming consoles and post-club Gen-Z vibe.", "club", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80")
)));
// ======================= VARANASI =======================

case "varanasi" -> Optional.of(genZCustomCity(city,

        // bunk spots
        List.of(
                genZCustomPlace(city, "Assi Ghat morning scene", "Sunrise chai, boat vibes and aesthetic ghat mornings.", "activity", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Sarnath mini escape", "One-day bunk trip with peaceful roads and calm vibe.", "place", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1200&q=80"),
                genZCustomPlace(city, "Boat-ride reel hour", "Golden-light river ride perfect for cinematic reels.", "activity", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80"),
                genZCustomPlace(city, "Lanka cafe lane", "Student-style café hopping with conversations and food.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Riverside maggi stop", "Simple food stop with calm Ganga-side atmosphere.", "food", "https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?w=1200&q=80"),
                genZCustomPlace(city, "Ramnagar bridge drive", "Soft evening bridge-drive vibe with river breeze.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Sunrise cycling ghat route", "Morning cycle route with chai and aesthetic river views.", "activity", "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=1200&q=80"),
                genZCustomPlace(city, "Hidden chai rooftop", "Lowkey rooftop tea spot for long conversations.", "food", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Monsoon boat ride", "Rainy-weather river ride with cinematic sky vibe.", "activity", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=1200&q=80"),
                genZCustomPlace(city, "Street-food crawl night", "Friends trying random food stalls across ghats.", "food", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80")
        ),

        // hidden gems
        List.of(
                genZCustomPlace(city, "Ramnagar Fort slow stop", "Calmer heritage spot away from heavy tourist rush.", "place", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=1200&q=80"),
                genZCustomPlace(city, "Hidden ghat sunset point", "Quiet riverside location for aesthetic evening photos.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),
                genZCustomPlace(city, "Indie rooftop chai cafe", "Lowkey tea spot with live music and river breeze.", "cafe", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Old alley photo route", "Vintage Varanasi streets made for cinematic edits.", "place", "https://images.unsplash.com/photo-1480714378408-67cf0d13bc1b?w=1200&q=80"),
                genZCustomPlace(city, "Quiet boat dock corner", "Minimal crowd riverside vibe for slow conversations.", "place", "https://images.unsplash.com/photo-1500534623283-312aade485b7?w=1200&q=80"),
                genZCustomPlace(city, "Bookstore jazz cafe", "Books, jazz music and cozy lighting aesthetic.", "cafe", "https://images.unsplash.com/photo-1526243741027-444d633d7365?w=1200&q=80"),
                genZCustomPlace(city, "Hidden art mural lane", "Street-art walls for fashion reels and edits.", "place", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=1200&q=80"),
                genZCustomPlace(city, "Silent riverside terrace", "Underrated rooftop point with peaceful skyline vibe.", "place", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Local pottery workshop", "Cute pottery-making stop with indie creative vibe.", "activity", "https://images.unsplash.com/photo-1517048676732-d65bc937f952?w=1200&q=80"),
                genZCustomPlace(city, "Hidden acoustic cafe", "Fairy lights, indie songs and calm night atmosphere.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80")
        ),

        // surprise
        List.of(
                genZCustomPlace(city, "Ganga aarti date plan", "Lights, music and riverside evening mood together.", "place", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Night boat lantern ride", "Soft glowing river experience for aesthetic reels.", "activity", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=1200&q=80"),
                genZCustomPlace(city, "Open mic riverside cafe", "Poetry nights and acoustic music near the ghats.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "Street-food chaos night", "Friends, chaat and loud Varanasi-night energy.", "food", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Sunrise yoga ghat scene", "Peaceful aesthetic morning with calm river atmosphere.", "activity", "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=1200&q=80"),
                genZCustomPlace(city, "DIY riverside picnic", "Cute picnic setup with fairy lights and snacks.", "activity", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),
                genZCustomPlace(city, "Late-night kulhad coffee", "Midnight coffee and skyline conversations together.", "food", "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=1200&q=80"),
                genZCustomPlace(city, "Vintage scooter reel ride", "Old-school Varanasi street aesthetic for content shoots.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight rooftop dinner", "Fairy-light dinner setup with calm river breeze.", "club", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=1200&q=80"),
                genZCustomPlace(city, "Rain-drive riverside loop", "Monsoon playlist drive through riverside roads.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80")
        ),

        // trending
        List.of(
                genZCustomPlace(city, "Aesthetic ghat reel walk", "Most viral Varanasi reel route with river views.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Vintage cafe interiors", "Pinterest-core café vibe with cozy lighting.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),
                genZCustomPlace(city, "Streetwear thrift lane", "Oversized fits, indie jewellery and tote bags.", "shopping", "https://images.unsplash.com/photo-1521334884684-d80222895322?w=1200&q=80"),
                genZCustomPlace(city, "Drone-shot river viewpoint", "Wide cinematic river reel location.", "place", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Live-music rooftop cafe", "Late-night café crowd with fairy lights and songs.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Korean dessert cafe", "Trending Gen-Z dessert spot with aesthetic interiors.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Sunset skyline reels", "Golden-hour skyline vibe for transition edits.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),
                genZCustomPlace(city, "Indie jewellery market", "Cute accessories and handmade fashion finds.", "shopping", "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1200&q=80"),
                genZCustomPlace(city, "Neon gaming cafe", "Gaming setup with loud crowd and Gen-Z vibe.", "activity", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Vintage jeep reel shoot", "Classic vehicle aesthetic for cinematic content.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80")
        ),

        // clubs / nightlife
        List.of(
                genZCustomPlace(city, "Kashi chaat night", "Classic late-night food-street vibe with friends.", "club", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Riverside acoustic jam", "Night guitar sessions with calm river breeze.", "club", "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight chai rooftop", "Late-night terrace tea with aesthetic skyline.", "club", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Open mic poetry night", "Poetry and indie songs with Gen-Z crowd vibe.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "After-dark rooftop lounge", "Night skyline vibe with music and mocktails.", "club", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Neon terrace party", "Dance-floor setup with fairy lights and DJs.", "club", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&q=80"),
                genZCustomPlace(city, "Silent disco riverside", "Wireless-headphone dance vibe near the ghats.", "club", "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=1200&q=80"),
                genZCustomPlace(city, "Midnight dessert adda", "Late-night desserts and rooftop conversations.", "club", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Live indie cafe night", "Acoustic live music and aesthetic crowd energy.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Afterparty gaming lounge", "Gaming consoles and post-club Gen-Z chaos.", "club", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80")
)));

case "delhi" -> Optional.of(genZCustomCity(city,

        // bunk spots
        List.of(
                genZCustomPlace(city, "India Gate late drive", "Night-drive plan with friends, music and cool Delhi air.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Murthal food run", "One-day bunk trip for parathas and highway chaos.", "food", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Damdama Lake escape", "Quick lake-side outing with boating and sunset scenes.", "place", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80"),
                genZCustomPlace(city, "Hauz Khas cafe hop", "Food, cafés and Gen-Z hangout vibe in one lane.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Lodhi Garden picnic", "Cute aesthetic picnic setup with reels and snacks.", "activity", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),
                genZCustomPlace(city, "Majnu ka Tila ramen stop", "Tibetan food and anime-core café vibe for friend groups.", "food", "https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?w=1200&q=80"),
                genZCustomPlace(city, "Aravalli hills ride", "Morning bike ride with chill roads and soft weather.", "activity", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=1200&q=80"),
                genZCustomPlace(city, "Cyber City sunset plan", "After-college skyline hangout with cafés and reels.", "place", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Sultanpur birding escape", "Nature escape for peaceful walks and aesthetic photos.", "place", "https://images.unsplash.com/photo-1474511320723-9a56873867b5?w=1200&q=80"),
                genZCustomPlace(city, "Noida gaming arena", "Full gaming and bowling day with loud Gen-Z energy.", "activity", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80")
        ),

        // hidden gems
        List.of(
                genZCustomPlace(city, "Sunder Nursery reset", "Quiet greenery and calm walks away from city chaos.", "place", "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=1200&q=80"),
                genZCustomPlace(city, "Hidden bookstore cafe", "Cozy reading café with soft music and coffee vibe.", "cafe", "https://images.unsplash.com/photo-1526243741027-444d633d7365?w=1200&q=80"),
                genZCustomPlace(city, "Secret graffiti lane", "Street-art walls made for fashion shoots and reels.", "place", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=1200&q=80"),
                genZCustomPlace(city, "Lowkey rooftop sunset", "Underrated skyline point for evening conversations.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),
                genZCustomPlace(city, "Vintage indie cafe", "Retro-style café interiors and acoustic music nights.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),
                genZCustomPlace(city, "Hidden art warehouse", "Industrial aesthetic art space with Gen-Z vibe.", "place", "https://images.unsplash.com/photo-1480714378408-67cf0d13bc1b?w=1200&q=80"),
                genZCustomPlace(city, "Silent terrace chai point", "Minimal crowd terrace tea stop for night talks.", "food", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Old Delhi rooftop frame", "Chaotic old-city skyline perfect for cinematic reels.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Indie vinyl cafe", "Lo-fi music café with cozy lighting and retro vibe.", "cafe", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Hidden skate corner", "Street-skating and BMX vibe for Gen-Z edits.", "activity", "https://images.unsplash.com/photo-1517649763962-0c623066013b?w=1200&q=80")
        ),

        // surprise
        List.of(
                genZCustomPlace(city, "Lodhi art walk", "Wall-art route perfect for reels and outfit dumps.", "place", "https://images.unsplash.com/photo-1480714378408-67cf0d13bc1b?w=1200&q=80"),
                genZCustomPlace(city, "Qutub golden-hour plan", "Soft sunset monument vibe for dates and photos.", "place", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=1200&q=80"),
                genZCustomPlace(city, "Drive-in movie night", "Movie setup with snacks and aesthetic night vibe.", "activity", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&q=80"),
                genZCustomPlace(city, "Late-night momo run", "Classic Delhi friend-group food chaos after dark.", "food", "https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?w=1200&q=80"),
                genZCustomPlace(city, "Rooftop live-music dinner", "Skyline dinner vibe with acoustic music and lights.", "club", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Monsoon chai drive", "Rain-drive playlist vibe with roadside chai stops.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "DIY picnic aesthetic", "Pinterest-style picnic with fairy lights and snacks.", "activity", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),
                genZCustomPlace(city, "Late-night waffle cafe", "Dessert and coffee vibe with city-light atmosphere.", "cafe", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Neon arcade zone", "Retro arcade gaming and Gen-Z chaos together.", "activity", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Sunrise cycling route", "Morning cycling vibe with calm roads and coffee stops.", "activity", "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=1200&q=80")
        ),

        // trending
        List.of(
                genZCustomPlace(city, "Cyber Hub aesthetic night", "Most viral Gen-Z nightlife and food scene in NCR.", "club", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&q=80"),
                genZCustomPlace(city, "Luxury brunch cafe", "Pinterest-style café interiors with dessert reels.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Streetwear shopping hub", "Oversized fits, sneakers and Gen-Z fashion vibe.", "shopping", "https://images.unsplash.com/photo-1521334884684-d80222895322?w=1200&q=80"),
                genZCustomPlace(city, "Drone-shot skyline point", "Wide city-view reel location for cinematic edits.", "place", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Gaming and neon cafe", "Gaming plus snacks plus late-night Gen-Z crowd.", "activity", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Aesthetic korean cafe", "Trending Seoul-style interiors and dessert shots.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),
                genZCustomPlace(city, "Vintage car shoot", "Luxury vintage-car aesthetic for transition reels.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Sunset rooftop reels", "Soft skyline visuals for trending reel edits.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),
                genZCustomPlace(city, "Indie thrift market", "Cute tote bags, rings and oversized fashion finds.", "shopping", "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1200&q=80"),
                genZCustomPlace(city, "Live DJ rooftop cafe", "Dance music, skyline and loud crowd energy.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80")
        ),

        // clubs / nightlife
        List.of(
                genZCustomPlace(city, "Cyber Hub night plan", "Food, music and nightlife energy all together.", "club", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&q=80"),
                genZCustomPlace(city, "Silent disco terrace", "Wireless-headphone dance setup with neon lights.", "club", "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=1200&q=80"),
                genZCustomPlace(city, "After-dark rooftop lounge", "Late-night skyline lounge with music and mocktails.", "club", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Open mic indie night", "Poetry, songs and cozy Gen-Z crowd atmosphere.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "Neon warehouse party", "Underground dance-floor vibe with lasers and DJs.", "club", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=1200&q=80"),
                genZCustomPlace(city, "Late-night tea adda", "Midnight tea scene with chill terrace conversations.", "club", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic rooftop jam", "Night guitar sessions and fairy-light vibe.", "club", "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=1200&q=80"),
                genZCustomPlace(city, "Weekend techno scene", "Heavy beats and Gen-Z rave atmosphere.", "club", "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight skyline party", "Open-air rooftop dance setup with skyline views.", "club", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&q=80"),
                genZCustomPlace(city, "Afterparty gaming lounge", "Gaming consoles, music and post-club chaos.", "club", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80")
)));
                 
case "chandigarh" -> Optional.of(genZCustomCity(city,
    // bunk spots
    List.of(
            genZCustomPlace(city, "Kasauli quick escape", "Perfect college-bunk hill drive with cafés, clouds and soft mountain weather.", "place", "https://i.redd.it/kasoli-himachal-pradesh-v0-1e9pjfwfebzd1.jpg?width=1080&format=pjpg&auto=webp&s=535e89d7b2aa8b29b53f34b8dd8070253f7dcea5"),
            genZCustomPlace(city, "Morni Hills bike ride", "Morning ride plan with curves, lake stops and mountain-view reels.", "activity", "https://www.progressivetourtravels.com/images/other3.jpg"),
            genZCustomPlace(city, "Siswan dam sunset drive", "One-hour drive for sunset scenes, music and peaceful evening vibe.", "place", "https://i.ytimg.com/vi/q6fWzkmV9Xk/hq720.jpg?sqp=-oaymwEhCK4FEIIDSFryq4qpAxMIARUAAAAAGAElAADIQj0AgKJD&rs=AOn4CLBmsWkRIzvcijxWocbidYj9TLd5_Q"),
            genZCustomPlace(city, "Sector 26 cafe hopping", "Full Gen-Z café crawl with desserts, gossip and aesthetic interiors.", "cafe", "https://media-assets.swiggy.com/swiggy/image/upload/fl_lossy,f_auto,q_auto/DINEOUT_ALL_RESTAURANTS/IMAGES/RESTAURANT_IMAGE_SERVICE/2024/7/11/c7a7ddb3-b36a-4f9e-aea9-e03b5835cd0a_20240711T083016537.jpg"),
            genZCustomPlace(city, "Sukhna sunrise cycling", "Early-morning cycling scene with calm lake energy and coffee stops.", "activity", "https://i.redd.it/0ec4jlgv9axe1.jpeg"),
            genZCustomPlace(city, "Timber Trail ropeway plan", "Mountain ropeway trip with cinematic valley-view photos.", "activity", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/0a/8f/ef/f0/timber-trail.jpg?w=1200&h=1200&s=1"),
            genZCustomPlace(city, "Pinjore garden evening", "Soft aesthetic garden date plan with lights and peaceful walks.", "place", "https://haryanatourism.gov.in/wp-content/uploads/2024/07/pic-13-scaled.jpg"),
            genZCustomPlace(city, "Elante weekday escape", "Shopping, movies and food-court chaos with friends.", "shopping", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/13/81/80/07/elante-mall.jpg?w=1200&h=1200&s=1"),
            genZCustomPlace(city, "Lake-side maggi stop", "Rainy-weather maggi and chai vibe near hidden lake roads.", "food", "https://static2.tripoto.com/media/filter/tst/img/830945/Image/1656738834_maggie_80291_pixahive_1.jpg.webp"),
            genZCustomPlace(city, "Nada Sahib riverside chill", "Peaceful riverside stop for calm evenings and lowkey talks.", "place", "https://www.mappls.com/explore/images/user_photos/review/original/131d223d9353d440.jpg")
    ),

    // hidden gems
    List.of(
            genZCustomPlace(city, "Secret forest trail walk", "Underrated walking trail with greenery and soft aesthetic vibe.", "place", "https://chandigarhtourism.gov.in/uploads/nature-2.jpg"),
            genZCustomPlace(city, "The wildhood", "Awesome rustic ambience, and have nice farm animals.", "cafe", "https://b.zmtcdn.com/data/pictures/0/21474560/c328c031cf405c3eab8a50dffcddca98.jpg?fit=around|960:500&crop=960:500;*,*"),
            genZCustomPlace(city, "Sector 7 rooftop corner", "Quiet rooftop café for night talks and city-light photos.", "cafe", "https://b.zmtcdn.com/data/pictures/0/124170/28230f083adb03db40f477ff26a4b625.jpg?fit=around|750:500&crop=750:500;*,*"),
            genZCustomPlace(city, "Underpass skate spot", "Street-style skating area for Gen-Z edits and reels.", "activity", "https://pbs.twimg.com/media/EjytvAeU8AA5kzs.jpg"),
            genZCustomPlace(city, "SOCIAL", "SOCIAL is designed to take you offline while keeping you connected, striking the perfect balance between work x play. ", "cafe", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/12/56/f4/35/sector-7-social.jpg?w=900&h=-1&s=1"),
            genZCustomPlace(city, "Open-air pottery studio", "Cute creative workshop plan with artsy Gen-Z vibe.", "activity", "https://static.wixstatic.com/media/c9a0fe_84438c08f5584cc6b4a5e847e8ca0c6b~mv2.jpg/v1/fill/w_320,h_200,al_c,q_80,usm_0.66_1.00_0.01,enc_avif,quality_auto/IMG_1080_edited.jpg"),
            genZCustomPlace(city, "Calm lake bench point", "Low-crowd lake corner perfect for sunset conversations.", "place", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/11/15/e8/ac/sukhna-lake.jpg?w=600&h=400&s=1"),
            genZCustomPlace(city, "Aesthetic bookstore cafe", "Coffee plus books plus cozy rainy-day energy.", "cafe", "https://b.zmtcdn.com/data/pictures/4/120554/abf83d3dd8ae17614f28c89be3dc32f5.jpg"),
            genZCustomPlace(city, "Hidden graffiti lane", "Colorful wall-art area for streetwear shoots and reels.", "place", "https://i.ytimg.com/vi/7qPfpIfL7V4/hqdefault.jpg"),
            genZCustomPlace(city, "Quiet rooftop sunset point", "Minimal crowd sunset spot with skyline photography vibe.", "place", "https://www.shoutlo.com/uploads/articles/item-header-img-sky-grill4.jpg")
    ),

    // surprise
    List.of(
            genZCustomPlace(city, "Drive-in movie night", "Car movie-night plan with snacks and aesthetic lights.", "activity", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&q=80"),

            genZCustomPlace(city, "Midnight chai adda", "Late-night tea scene with chaotic friend-group energy.", "food", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),

            genZCustomPlace(city, "Open mic poetry cafe", "Indie poetry nights with acoustic music and cozy crowd.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),

            genZCustomPlace(city, "Sunset rooftop dinner", "Fairy-light rooftop plan with mocktails and skyline views.", "cafe", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=1200&q=80"),

            genZCustomPlace(city, "Sector 17 night stroll", "Classic Chandigarh night-walk vibe with music and food.", "place", "https://images.unsplash.com/photo-1480714378408-67cf0d13bc1b?w=1200&q=80"),

            genZCustomPlace(city, "Hidden bowling night", "Fun bowling hangout with loud music and Gen-Z energy.", "activity", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),

            genZCustomPlace(city, "Late-night waffle run", "Dessert run with soft city-light aesthetic.", "food", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),

            genZCustomPlace(city, "Rain-drive music session", "Monsoon roadtrip with playlists and chai breaks.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),

            genZCustomPlace(city, "DIY picnic setup", "Cute Gen-Z picnic aesthetic with fairy lights and snacks.", "activity", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),

            genZCustomPlace(city, "Night cycling loop", "Late-evening cycling with neon streets and cool breeze.", "activity", "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=1200&q=80")
    ),

    // trending
    List.of(
            genZCustomPlace(city, "Tubu", "Trending aesthetic café known for Gen-Z photo dumps.", "cafe", "https://assets.architecturaldigest.in/photos/672b301e2152d183f8e71af0/master/w_1600%2Cc_limit/TUBU-08.jpg"),

            genZCustomPlace(city, "Sukhna golden-hour walk", "Sunset walk spot with soft skies and reel-worthy frames.", "place", "https://d34vm3j4h7f97z.cloudfront.net/optimized/4X/a/b/e/abef7c523b1454bf02d2c9b7b8f976c918342d90_2_690x460.jpeg"),

            genZCustomPlace(city, "Streetwear shopping lane", "Oversized fits, sneakers and indie-style shopping vibe.", "shopping", "https://chdlife.com/wp-content/uploads/2024/04/Shastri-Market-Sector-22-Chandigarh.webp"),

            genZCustomPlace(city, "Live-music rooftop cafe", "Acoustic night vibe with fairy lights and crowd energy.", "club", "https://www.shoutlo.com/uploads/articles/header-img-rooftop-bars-in-chandigarh.jpg"),

            genZCustomPlace(city, "Underground gaming cafe", "Gaming + food + Gen-Z group-hang atmosphere.", "activity", "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/26/2a/0e/64/common-room.jpg?w=1000&h=-1&s=1"),

            genZCustomPlace(city, "Luxury dessert cafe", "Pinterest dessert spot with aesthetic interiors.", "cafe", "https://prinkled.com/wp-content/uploads/2023/06/Must-Try-Desserts-In-Chandigarh-1-1024x893.webp"),

            genZCustomPlace(city, "City-light rooftop reels", "Night skyline spot for cinematic transition videos.", "place", "https://b.zmtcdn.com/data/pictures/5/121175/1eb0fb183a6b027307cc9004a63fb31b.jpeg?fit=around|750:500&crop=750:500;*,*"),
            genZCustomPlace(city, "Hidden sunset basketball court", "Lowkey Gen-Z hangout for sports and evening chill.", "activity", "https://pmlsdpublicschool32.ac.in/wp-content/uploads/2022/11/basket-1.jpg"),
            genZCustomPlace(city, "Jannaat", "luxury clubbing vibe with neon interiors and influencer-style party atmosphere.", "club", "https://b.zmtcdn.com/data/pictures/0/21038580/84e6817a031b3e5b1470159c3921937c.jpeg?fit=around|960:500&crop=960:500;*,*")

    ),

    // clubs / nightlife
    List.of(
            genZCustomPlace(city, "Rooftop DJ night", "Skyline rooftop music scene with dance-floor energy.", "club", "https://www.shoutlo.com/assets/images/merchant_images/merchant-153823-67ed0c975ed95.jpg"),

            genZCustomPlace(city, "Silent disco party", "Wireless headphone dance setup and neon-night aesthetic.", "club", "https://lh3.googleusercontent.com/HPxN-t8XP5YmuVovfI4Cmni77dAzEh8NIL0MotrrCPvcCWQ2aFRH-ED_HkIYwwpEbsW3Wh8k_eeBRmntD4eDojQjt4E=w1600-rw"),

            genZCustomPlace(city, "After-dark hookah lounge", "Late-night chill lounge with music and soft lights.", "club", "https://content3.jdmagicbox.com/comp/chandigarh/w2/0172px172.x172.170224070648.d1w2/catalogue/cowboy-rodeo-cafe-and-lounge-chandigarh-sector-9d-chandigarh-night-clubs-id29y.jpg"),
            genZCustomPlace(city, "Kala Ghoda", "Kala Ghoda is a vibrant and stylish party destination in Chandigarh.", "club", "https://lh3.googleusercontent.com/gps-cs-s/APNQkAHgf_lMXq_zfia9p0NGYnkTeRMeAq17drQWktAqr2QpdxaTA5dWya3v2IzZGEIWcH40PZdaiIAQOPp4Ti01_x1_GXetxpxJKmwjdFB8RKBhLUldFDTXZkoczR_-wz1nIS67M3tj9A=s680-w680-h510-rw"),

            genZCustomPlace(city, "Open-air terrace party", "Weekend terrace party with fairy lights and live DJs.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80")
    )));// ======================= UDAIPUR =======================

case "udaipur" -> Optional.of(genZCustomCity(city,

        // bunk spots
        List.of(
                genZCustomPlace(city, "Fateh Sagar sunrise ride", "Morning ride plan with lake breeze and calm aesthetic views.", "activity", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80"),
                genZCustomPlace(city, "Badi Lake picnic escape", "One-day bunk plan with friends, snacks and peaceful water views.", "place", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1200&q=80"),
                genZCustomPlace(city, "Bahubali Hills trek", "Short hike with cinematic lake viewpoints and reel-worthy sunsets.", "activity", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=1200&q=80"),
                genZCustomPlace(city, "Ambrai lakeside walk", "Soft evening lakeside stroll with music and coffee vibe.", "place", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Old City scooter ride", "Vintage scooter reels through aesthetic white streets.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Monsoon Palace sunset run", "Golden-hour palace view with clouds and skyline energy.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Cafe rooftop hopping", "Lake-view cafés and Gen-Z brunch vibe together.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Lake-side maggi stop", "Rainy-weather maggi and chai with peaceful lake backdrop.", "food", "https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?w=1200&q=80"),
                genZCustomPlace(city, "Neemach Mata quick hike", "Mini hill hike with friends and aesthetic skyline views.", "activity", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=1200&q=80"),
                genZCustomPlace(city, "Hidden village roadtrip", "Quiet countryside escape with open roads and reels.", "place", "https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=1200&q=80")
        ),

        // hidden gems
        List.of(
                genZCustomPlace(city, "Secret lakeside bench", "Quiet sunset corner for calm conversations and soft photos.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),
                genZCustomPlace(city, "Hidden haveli rooftop", "Underrated rooftop with old-city lake aesthetic.", "place", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Vintage bookstore cafe", "Books, coffee and rainy-day Gen-Z vibe together.", "cafe", "https://images.unsplash.com/photo-1526243741027-444d633d7365?w=1200&q=80"),
                genZCustomPlace(city, "Silent ghat evening", "Minimal-crowd lakeside vibe with fairy-light reflections.", "place", "https://images.unsplash.com/photo-1500534623283-312aade485b7?w=1200&q=80"),
                genZCustomPlace(city, "Hidden pottery studio", "Creative pottery stop with indie aesthetic atmosphere.", "activity", "https://images.unsplash.com/photo-1517048676732-d65bc937f952?w=1200&q=80"),
                genZCustomPlace(city, "Art mural alley", "Street-art walls made for outfit shoots and transition reels.", "place", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic rooftop cafe", "Indie songs, fairy lights and chill crowd energy.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Hidden tea terrace", "Late-evening tea vibe with calm skyline views.", "food", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Quiet bridge viewpoint", "Underrated city-light viewpoint for aesthetic reels.", "place", "https://images.unsplash.com/photo-1480714378408-67cf0d13bc1b?w=1200&q=80"),
                genZCustomPlace(city, "Indie vinyl cafe", "Retro-style music café with cozy Gen-Z atmosphere.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80")
        ),

        // surprise
        List.of(
                genZCustomPlace(city, "Lake Pichola boat ride", "Golden-light boat ride with cinematic city reflections.", "activity", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80"),
                genZCustomPlace(city, "Rooftop dinner by the lake", "Soft dinner vibe with fairy lights and calm water views.", "club", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight lakeside walk", "Late-night calm vibe with reflections and skyline lights.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Open-air movie cafe", "Movie-night setup with snacks and aesthetic decor.", "activity", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&q=80"),
                genZCustomPlace(city, "Vintage jeep reel shoot", "Classic jeep setup with palace-view aesthetic.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "DIY rooftop picnic", "Cute rooftop picnic with reels and fairy lights.", "activity", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),
                genZCustomPlace(city, "Monsoon chai drive", "Rain-drive playlist vibe through lake roads.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Late-night dessert cafe", "Dessert and coffee scenes with Gen-Z crowd.", "cafe", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Sunrise yoga viewpoint", "Peaceful hilltop yoga and aesthetic sunrise atmosphere.", "activity", "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=1200&q=80"),
                genZCustomPlace(city, "Hidden jazz terrace", "Lowkey rooftop with soft jazz and cozy lighting.", "club", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=1200&q=80")
        ),

        // trending
        List.of(
                genZCustomPlace(city, "City Palace reel stop", "Most viral Udaipur photo-dump location for creators.", "place", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=1200&q=80"),
                genZCustomPlace(city, "Luxury brunch cafe", "Pinterest-style café interiors with lake views.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Streetwear shopping lane", "Oversized fits, silver jewellery and tote-bag shopping.", "shopping", "https://images.unsplash.com/photo-1521334884684-d80222895322?w=1200&q=80"),
                genZCustomPlace(city, "Drone-shot lake viewpoint", "Wide cinematic lake reel location with skyline.", "place", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Neon gaming cafe", "Gaming plus snacks plus loud Gen-Z crowd vibe.", "activity", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Mirror aesthetic cafe", "Pinterest-core dessert spot with soft lighting.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),
                genZCustomPlace(city, "Sunset skyline reels", "Golden-hour rooftop visuals for transition edits.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),
                genZCustomPlace(city, "Vintage car photoshoot", "Luxury-car setup for cinematic reels and edits.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Indie jewellery market", "Cute rings, tote bags and handmade accessories.", "shopping", "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1200&q=80"),
                genZCustomPlace(city, "Live DJ rooftop cafe", "Dance music, skyline and Gen-Z nightlife energy.", "club", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&q=80")
        ),

        // clubs / nightlife
        List.of(
                genZCustomPlace(city, "Lakeview rooftop party", "Open-air rooftop party with fairy lights and DJs.", "club", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&q=80"),
                genZCustomPlace(city, "Silent disco terrace", "Wireless-headphone dance setup with neon aesthetic.", "club", "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=1200&q=80"),
                genZCustomPlace(city, "After-dark rooftop lounge", "Late-night skyline lounge with music and mocktails.", "club", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Open mic indie night", "Poetry and acoustic songs with cozy Gen-Z crowd.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight lakeside adda", "Late-night tea and calm lake-view conversations.", "club", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Weekend neon rave", "Heavy DJ beats and chaotic dance-floor energy.", "club", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic campfire jam", "Night guitar sessions with fairy lights and chill vibe.", "club", "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=1200&q=80"),
                genZCustomPlace(city, "Afterparty gaming lounge", "Gaming consoles and loud post-club crowd vibe.", "club", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Late-night dessert adda", "Dessert and skyline conversations with friends.", "club", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Live-music rooftop scene", "Acoustic live songs with aesthetic rooftop atmosphere.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80")
)));
         

   case "jaisalmer" -> Optional.of(genZCustomCity(city,
                    List.of(
            genZCustomPlace(city, "Kuldhara ghost village escape", "Haunted abandoned village road-trip plan perfect for bunk-day thrill and sunset reels.", "place", "https://images.unsplash.com/photo-1477587458883-47145ed94245?w=1200&q=80"),

            genZCustomPlace(city, "Sam dunes bike ride", "Friends trip for jeep rides, chai stops, dunes and full desert-core vibe.", "activity", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),

            genZCustomPlace(city, "Longewala border mini roadtrip", "Long-drive style bunk plan with desert roads, army-history stop and sunset scenes.", "place", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1200&q=80"),

            genZCustomPlace(city, "Desert highway chai break", "Random chai-stop drive with loud music and open-road aesthetic.", "cafe", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),

            genZCustomPlace(city, "Khuri dunes sunset plan", "Less-crowded dunes vibe for reels, bike shots and chill evening scenes.", "activity", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=1200&q=80"),

            genZCustomPlace(city, "Abandoned desert road photos", "Empty road cinematic photo plan with desert wind and golden-hour edits.", "place", "https://images.unsplash.com/photo-1500534623283-312aade485b7?w=1200&q=80"),

            genZCustomPlace(city, "Early-morning fort breakfast ride", "Quick bunk-trip for breakfast cafés and calm fort streets.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),

            genZCustomPlace(city, "Desert picnic escape", "Mini picnic setup with friends and sunset playlist energy.", "activity", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),

            genZCustomPlace(city, "Village-side camel trail", "Local desert trail plan for reels and calm offbeat moments.", "activity", "https://images.unsplash.com/photo-1516483638261-f4dbaf036963?w=1200&q=80"),

            genZCustomPlace(city, "Roadtrip to fossil park", "Underrated desert-side stop for unique landscape shots.", "place", "https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=1200&q=80")
    ),

    // hidden gems
    List.of(
            genZCustomPlace(city, "Khaba Fort quiet ruin", "Offbeat fort ruins with calm views and peaceful desert energy.", "place", "https://images.unsplash.com/photo-1467269204594-9661b134dd2b?w=1200&q=80"),

            genZCustomPlace(city, "Bada Bagh golden-hour shoot", "Warm golden light spot for aesthetic photos and cinematic edits.", "place", "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=1200&q=80"),

            genZCustomPlace(city, "Hidden fort alley photoshoot", "Golden stone lanes perfect for vintage outfits and aesthetic reels.", "place", "https://images.unsplash.com/photo-1494526585095-c41746248156?w=1200&q=80"),

            genZCustomPlace(city, "Secret rooftop sunset point", "Quiet rooftop fort view for chill evenings and coffee scenes.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),

            genZCustomPlace(city, "Old city thrift lane", "Hidden shopping lane for rings, silver jewellery and indie fits.", "shopping", "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1200&q=80"),

            genZCustomPlace(city, "Desert chai point evening", "Roadside chai stop with soft skies and slow conversations.", "cafe", "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=1200&q=80"),

            genZCustomPlace(city, "Quiet haveli balcony spot", "Underrated heritage balcony views with old-school aesthetic.", "place", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=1200&q=80"),

            genZCustomPlace(city, "Hidden local music courtyard", "Acoustic folk music corner with cozy late-evening vibes.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),

            genZCustomPlace(city, "Silent desert moonlight walk", "Night desert walk plan with stars and calm open-air vibe.", "place", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),

            genZCustomPlace(city, "Offbeat pottery workshop stop", "Cute hands-on pottery experience with local artist vibe.", "activity", "https://images.unsplash.com/photo-1517048676732-d65bc937f952?w=1200&q=80")
    ),

    // surprise
    List.of(
            genZCustomPlace(city, "Fort rooftop cafe hop", "Cafe-to-cafe hopping with fort views and mocktail scenes.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),

            genZCustomPlace(city, "Gadisar Lake soft evening", "Slow evening walk spot for boating and sunset frames.", "place", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80"),

            genZCustomPlace(city, "Desert ATV ride session", "Dust trails, adrenaline and action-reel vibe.", "activity", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=1200&q=80"),

            genZCustomPlace(city, "Night sky stargazing camp", "Open desert sky experience with music and midnight photos.", "place", "https://images.unsplash.com/photo-1500534623283-312aade485b7?w=1200&q=80"),

            genZCustomPlace(city, "Luxury tent café hangout", "Aesthetic desert tents with fairy lights and food reels.", "cafe", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=1200&q=80"),

            genZCustomPlace(city, "Sunrise dunes meditation", "Peaceful sunrise spot with soft desert wind and calm vibe.", "place", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=1200&q=80"),

            genZCustomPlace(city, "Camel safari reel session", "Fun camel ride content plan with matching desert outfits.", "activity", "https://images.unsplash.com/photo-1516483638261-f4dbaf036963?w=1200&q=80"),

            genZCustomPlace(city, "Late-night maggi desert stop", "Tiny roadside maggi break after dunes and music night.", "food", "https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?w=1200&q=80"),

            genZCustomPlace(city, "Golden fort night walk", "Night fort streets with lights, cafés and aesthetic corners.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),

            genZCustomPlace(city, "Desert movie-night setup", "Projector-style outdoor movie plan with beanbags and snacks.", "activity", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&q=80")
    ),

    // trending
    List.of(
            genZCustomPlace(city, "Patwon Haveli reel walk", "Trending reel location with royal architecture and golden frames.", "place", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=1200&q=80"),

            genZCustomPlace(city, "Sunset point fort side", "Late-evening coffee and aesthetic skyline plan.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),

            genZCustomPlace(city, "Desert thrift market stroll", "Shopping stop for silver jewellery and indie fits.", "shopping", "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1200&q=80"),

            genZCustomPlace(city, "Aesthetic mirror café", "Pinterest-style café interiors and dessert reels.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),

            genZCustomPlace(city, "Desert outfit photoshoot", "Matching outfit shoot plan with cinematic sand backgrounds.", "activity", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=1200&q=80"),

            genZCustomPlace(city, "Fort-view live music cafe", "Gen-Z crowd café with acoustic nights and fairy lights.", "club", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),

            genZCustomPlace(city, "Drone-shot desert viewpoint", "Wide-open dunes perfect for cinematic drone reels.", "place", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),

            genZCustomPlace(city, "Vintage jeep reel ride", "Old jeep desert reels with loud music and sunset scenes.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),

            genZCustomPlace(city, "Boho shopping lane", "Trendy handmade accessories and Gen-Z aesthetic shopping vibe.", "shopping", "https://images.unsplash.com/photo-1521334884684-d80222895322?w=1200&q=80"),

            genZCustomPlace(city, "Night fort photography walk", "Night aesthetic plan for glowing fort shots and stories.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80")
    ),

    // clubs / nightlife
    List.of(
            genZCustomPlace(city, "Desert camp music night", "Bonfire, DJ night and chaotic friend-group energy.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),

            genZCustomPlace(city, "Rooftop live music dinner", "Night rooftop vibe with live singers and skyline views.", "club", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),

            genZCustomPlace(city, "Late-night dunes bonfire", "Music speakers, cold desert air and midnight storytelling.", "club", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),

            genZCustomPlace(city, "Neon desert party setup", "Open-air neon-lit party scene with Gen-Z crowd energy.", "club", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&q=80"),

            genZCustomPlace(city, "Silent disco desert night", "Wireless headphone party experience under the stars.", "club", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=1200&q=80"),

            genZCustomPlace(city, "Campfire acoustic jam", "Late-night guitar sessions and chill aesthetic vibe.", "club", "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=1200&q=80"),

            genZCustomPlace(city, "Open mic rooftop scene", "Poetry, indie songs and Gen-Z café culture energy.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),

            genZCustomPlace(city, "Midnight tea rooftop adda", "Late-night tea conversations with city lights and fort view.", "club", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),

            genZCustomPlace(city, "After-dark desert dance floor", "Night dance setup in dunes with lasers and DJ vibes.", "club", "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=1200&q=80"),

            genZCustomPlace(city, "Moonlight hammock lounge", "Relaxed desert lounge scene with cushions and fairy lights.", "club", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1200&q=80")
    )
));
           // ======================= GOA =======================

case "goa" -> Optional.of(genZCustomCity(city,

        // bunk spots
        List.of(
                genZCustomPlace(city, "Chapora sunset ride", "Scooter ride with ocean views and sunset reel vibes.", "activity", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80"),
                genZCustomPlace(city, "Ashwem beach chill", "Lowkey beach escape with music and soft sunset energy.", "place", "https://images.unsplash.com/photo-1493558103817-58b2924bce98?w=1200&q=80"),
                genZCustomPlace(city, "Anjuna cafe hopping", "Beach cafés and brunch scenes with Gen-Z crowd.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Hidden waterfall roadtrip", "One-day escape with forest roads and waterfall vibes.", "place", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=1200&q=80"),
                genZCustomPlace(city, "Baga midnight food run", "Late-night snacks and beach-road chaos with friends.", "food", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Scooty beach loop", "Full-day beach hopping with reels and playlists.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Fort Aguada golden hour", "Sunset fort vibe made for cinematic photos.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Beachside maggi stop", "Rainy-weather maggi and chai near the waves.", "food", "https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?w=1200&q=80"),
                genZCustomPlace(city, "Vagator sunset picnic", "Cute beach picnic with fairy lights and snacks.", "activity", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),
                genZCustomPlace(city, "Hidden village cafe", "Portuguese-style café with calm aesthetic vibe.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80")
        ),

        // hidden gems
        List.of(
                genZCustomPlace(city, "Secret cliff viewpoint", "Quiet ocean-view point with cinematic sunset energy.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),
                genZCustomPlace(city, "Indie vinyl beach cafe", "Retro café vibe with music and ocean breeze.", "cafe", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Hidden forest lake", "Minimal-crowd lake stop with calm green surroundings.", "place", "https://images.unsplash.com/photo-1500534623283-312aade485b7?w=1200&q=80"),
                genZCustomPlace(city, "Old Portuguese alley", "Vintage Goa streets for cinematic reels and edits.", "place", "https://images.unsplash.com/photo-1480714378408-67cf0d13bc1b?w=1200&q=80"),
                genZCustomPlace(city, "Silent beach bonfire", "Lowkey beach bonfire night with friends and music.", "club", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=1200&q=80"),
                genZCustomPlace(city, "Art mural cafe", "Street-art interiors with Pinterest-style aesthetics.", "cafe", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=1200&q=80"),
                genZCustomPlace(city, "Hidden tea shack", "Minimal beach tea stop with calm vibe.", "food", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight beach walk", "Late-night beach aesthetic with reflections and music.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Secret rooftop jazz cafe", "Soft jazz and fairy lights with ocean views.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "Pottery and art studio", "Creative indie stop for aesthetic content shoots.", "activity", "https://images.unsplash.com/photo-1517048676732-d65bc937f952?w=1200&q=80")
        ),

        // surprise
        List.of(
                genZCustomPlace(city, "Beach movie night", "Open-air movie setup by the waves and fairy lights.", "activity", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight scooty ride", "Late-night ride through calm beach roads.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "DIY beach picnic", "Cute beach picnic setup with reels and snacks.", "activity", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),
                genZCustomPlace(city, "Sunrise yoga beach scene", "Calm ocean-side sunrise vibe with peaceful energy.", "activity", "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=1200&q=80"),
                genZCustomPlace(city, "Late-night waffle cafe", "Dessert, coffee and beach-night atmosphere.", "cafe", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic rooftop dinner", "Live songs and ocean breeze under fairy lights.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Vintage jeep beach shoot", "Classic jeep setup for cinematic reels.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Rain-drive beach route", "Monsoon drive with playlists and ocean roads.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Floating breakfast cafe", "Pinterest-style breakfast setup with aesthetic views.", "cafe", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=1200&q=80"),
                genZCustomPlace(city, "Hidden sunset dock", "Quiet sunset spot with dreamy water reflections.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80")
        ),

        // trending
        List.of(
                genZCustomPlace(city, "Neon beach rave", "Most viral Goa nightlife vibe with DJs and lasers.", "club", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&q=80"),
                genZCustomPlace(city, "Korean beach cafe", "Trending café interiors with desserts and reels.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),
                genZCustomPlace(city, "Drone-shot beach cliff", "Wide cinematic reel viewpoint over the ocean.", "place", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Streetwear flea market", "Oversized fits, tote bags and indie jewellery.", "shopping", "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1200&q=80"),
                genZCustomPlace(city, "Gaming beach lounge", "Gaming plus music plus Gen-Z crowd energy.", "activity", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Mirror aesthetic cafe", "Pinterest-core interiors with pastel aesthetics.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Luxury yacht reels", "Ocean yacht vibe for viral content shoots.", "activity", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80"),
                genZCustomPlace(city, "Sunset rooftop reels", "Golden-hour skyline and ocean visuals together.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),
                genZCustomPlace(city, "Live DJ sunset cafe", "Beachside DJs and sunset dance atmosphere.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Vintage car beach shoot", "Luxury-car beach aesthetic for reels and edits.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80")
        ),

        // clubs / nightlife
        List.of(
                genZCustomPlace(city, "Beach rave night", "Heavy DJ beats and beach-party chaos together.", "club", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&q=80"),
                genZCustomPlace(city, "Silent disco beach", "Wireless-headphone dance setup near the ocean.", "club", "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=1200&q=80"),
                genZCustomPlace(city, "Bonfire music night", "Beach bonfire with guitars and chill Gen-Z crowd.", "club", "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=1200&q=80"),
                genZCustomPlace(city, "After-dark rooftop lounge", "Late-night rooftop vibe with ocean skyline.", "club", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Open mic indie night", "Poetry and acoustic music with aesthetic crowd.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight beach adda", "Late-night beach talks and tea with friends.", "club", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Weekend neon rave", "Underground DJ setup with lasers and loud energy.", "club", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=1200&q=80"),
                genZCustomPlace(city, "Afterparty gaming zone", "Gaming consoles and post-club chaos vibe.", "club", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Late-night dessert shack", "Dessert and ocean-breeze conversations after midnight.", "club", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Live rooftop music scene", "Acoustic live songs with fairy lights and waves.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80")
)));
         // ======================= MUMBAI =======================

case "mumbai" -> Optional.of(genZCustomCity(city,

        // bunk spots
        List.of(
                genZCustomPlace(city, "Marine Drive sunset sit", "Classic sea-face hangout with music and skyline sunsets.", "place", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Bandra cafe hopping", "Pinterest-style cafés and Gen-Z brunch energy.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Versova beach chai run", "Rainy-weather chai and beach-walk vibe together.", "food", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Lonavala quick escape", "One-day bunk trip with hills, waterfalls and reels.", "place", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=1200&q=80"),
                genZCustomPlace(city, "Sea-link midnight drive", "Late-night playlist drive with skyline reflections.", "activity", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Juhu food crawl", "Street-food chaos and beach-night Gen-Z energy.", "food", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Colaba vintage walk", "Old-school aesthetic streets made for photo dumps.", "place", "https://images.unsplash.com/photo-1480714378408-67cf0d13bc1b?w=1200&q=80"),
                genZCustomPlace(city, "Bandra fort sunset", "Golden-hour fort view with sea breeze and reels.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),
                genZCustomPlace(city, "Hidden rooftop brunch", "Lowkey skyline brunch setup with soft aesthetics.", "cafe", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=1200&q=80"),
                genZCustomPlace(city, "Monsoon local-train ride", "Rainy Mumbai vibe with playlists and window views.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80")
        ),

        // hidden gems
        List.of(
                genZCustomPlace(city, "Secret sea-view point", "Quiet ocean-view corner away from heavy crowds.", "place", "https://images.unsplash.com/photo-1500534623283-312aade485b7?w=1200&q=80"),
                genZCustomPlace(city, "Indie bookstore cafe", "Books, coffee and cozy rainy-day atmosphere.", "cafe", "https://images.unsplash.com/photo-1526243741027-444d633d7365?w=1200&q=80"),
                genZCustomPlace(city, "Graffiti art lane", "Street-art walls for aesthetic outfit reels.", "place", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=1200&q=80"),
                genZCustomPlace(city, "Hidden jazz rooftop", "Soft jazz and skyline lights with indie crowd.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "Vintage Irani cafe stop", "Retro Mumbai café vibe with old-school aesthetic.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),
                genZCustomPlace(city, "Silent dock sunset", "Minimal-crowd sea dock with dreamy reflections.", "place", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1200&q=80"),
                genZCustomPlace(city, "Art studio loft", "Creative indie loft with aesthetic content vibe.", "activity", "https://images.unsplash.com/photo-1517048676732-d65bc937f952?w=1200&q=80"),
                genZCustomPlace(city, "Hidden tea terrace", "Late-night tea spot with skyline conversations.", "food", "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight sea walk", "Late-night calm walk with ocean breeze and lights.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic indie cafe", "Fairy lights and acoustic music with Gen-Z crowd.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80")
        ),

        // surprise
        List.of(
                genZCustomPlace(city, "Drive-in movie night", "Open-air movie setup with snacks and skyline views.", "activity", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight Marine Drive", "Late-night sea-face vibe with friends and music.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "DIY rooftop picnic", "Cute rooftop picnic with fairy lights and reels.", "activity", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),
                genZCustomPlace(city, "Sunrise yoga beach scene", "Peaceful beach sunrise with calm aesthetic vibe.", "activity", "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=1200&q=80"),
                genZCustomPlace(city, "Late-night waffle stop", "Dessert and coffee vibe with Mumbai-night atmosphere.", "food", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Vintage taxi reel ride", "Classic yellow-black taxi aesthetic for edits.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic rooftop dinner", "Live songs and skyline under fairy lights.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Rain-drive sea route", "Monsoon drive with playlists and sea-link lights.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Floating brunch vibe", "Pinterest-style brunch with aesthetic interiors.", "cafe", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=1200&q=80"),
                genZCustomPlace(city, "Secret skyline rooftop", "Hidden rooftop point for cinematic night reels.", "place", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80")
        ),

        // trending
        List.of(
                genZCustomPlace(city, "Bandra reel streets", "Most viral Mumbai aesthetic location for creators.", "place", "https://images.unsplash.com/photo-1480714378408-67cf0d13bc1b?w=1200&q=80"),
                genZCustomPlace(city, "Luxury dessert cafe", "Pinterest-core dessert place with pastel aesthetics.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Streetwear thrift market", "Oversized fits, rings and tote-bag shopping.", "shopping", "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1200&q=80"),
                genZCustomPlace(city, "Drone-shot skyline point", "Wide cinematic skyline reel location.", "place", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Gaming and neon cafe", "Gaming plus snacks plus loud Gen-Z vibe.", "activity", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Mirror aesthetic cafe", "Soft-light interiors and Gen-Z dessert reels.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),
                genZCustomPlace(city, "Luxury yacht sunset", "Yacht-party vibe for cinematic ocean reels.", "activity", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80"),
                genZCustomPlace(city, "Sunset rooftop edits", "Golden-hour skyline visuals for transition edits.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),
                genZCustomPlace(city, "Live DJ rooftop cafe", "Dance music and skyline crowd energy together.", "club", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&q=80"),
                genZCustomPlace(city, "Vintage car photoshoot", "Luxury-car setup for cinematic content shoots.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80")
        ),

        // clubs / nightlife
        List.of(
                genZCustomPlace(city, "Marine Drive night adda", "Late-night sea-face talks with friends and music.", "club", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Silent disco rooftop", "Wireless-headphone dance setup with neon vibe.", "club", "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=1200&q=80"),
                genZCustomPlace(city, "After-dark skyline lounge", "Late-night rooftop atmosphere with DJs and lights.", "club", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Open mic indie night", "Poetry and acoustic music with cozy Gen-Z crowd.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "Neon warehouse rave", "Underground dance-floor chaos with lasers and DJs.", "club", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic campfire jam", "Night guitar sessions and chill terrace vibe.", "club", "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=1200&q=80"),
                genZCustomPlace(city, "Late-night dessert adda", "Midnight dessert vibe with skyline conversations.", "club", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Weekend techno scene", "Heavy beats and Gen-Z rave energy together.", "club", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&q=80"),
                genZCustomPlace(city, "Afterparty gaming lounge", "Gaming consoles and post-club crowd energy.", "club", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Live rooftop music scene", "Acoustic live songs with skyline and fairy lights.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80")
)));
           // ======================= MAHARASHTRA =======================

case "maharashtra" -> Optional.of(genZCustomCity(city,

        // bunk spots
        List.of(
                genZCustomPlace(city, "Lonavala rain drive", "Monsoon roadtrip vibe with waterfalls and playlists.", "activity", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=1200&q=80"),
                genZCustomPlace(city, "Lavasa quick escape", "One-day bunk trip with colorful streets and lake views.", "place", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1200&q=80"),
                genZCustomPlace(city, "Panchgani strawberry stop", "Hill-town café and strawberry dessert vibe.", "food", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Alibaug beach ride", "Scooter and ferry trip with beach sunsets and reels.", "activity", "https://images.unsplash.com/photo-1493558103817-58b2924bce98?w=1200&q=80"),
                genZCustomPlace(city, "Matheran toy-train vibe", "Cute hill-station aesthetic with foggy views.", "place", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=1200&q=80"),
                genZCustomPlace(city, "Hidden fort sunrise", "Morning fort trek with cinematic cloud views.", "activity", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Pune cafe hopping", "Gen-Z cafés and aesthetic brunch scenes together.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Mahabaleshwar lake picnic", "Cute picnic setup with fog and calm weather.", "activity", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),
                genZCustomPlace(city, "Night highway chai run", "Late-night tea stops with music and friends.", "food", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Secret waterfall route", "Rainy hidden waterfall escape with aesthetic vibe.", "place", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80")
        ),

        // hidden gems
        List.of(
                genZCustomPlace(city, "Hidden lake viewpoint", "Quiet sunset spot with dreamy reflections.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),
                genZCustomPlace(city, "Vintage hill cafe", "Retro-style café with foggy mountain atmosphere.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),
                genZCustomPlace(city, "Silent fort corner", "Minimal-crowd fort stop with panoramic valley views.", "place", "https://images.unsplash.com/photo-1500534623283-312aade485b7?w=1200&q=80"),
                genZCustomPlace(city, "Art mural alley", "Street-art lane perfect for outfit and reel shoots.", "place", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic rooftop cafe", "Indie music and fairy-light vibe with chill crowd.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Pottery art workshop", "Creative indie stop with aesthetic interiors.", "activity", "https://images.unsplash.com/photo-1517048676732-d65bc937f952?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight valley point", "Late-night valley skyline with calm atmosphere.", "place", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Bookstore jazz cafe", "Books, jazz and cozy rainy-day energy.", "cafe", "https://images.unsplash.com/photo-1526243741027-444d633d7365?w=1200&q=80"),
                genZCustomPlace(city, "Tea terrace hideout", "Lowkey tea spot with aesthetic sunset vibe.", "food", "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=1200&q=80"),
                genZCustomPlace(city, "Forest river trail", "Quiet nature trail with cinematic greenery.", "place", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=1200&q=80")
        ),

        // surprise
        List.of(
                genZCustomPlace(city, "Drive-in movie setup", "Open-air movie vibe with snacks and fairy lights.", "activity", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&q=80"),
                genZCustomPlace(city, "Vintage jeep hill shoot", "Classic jeep setup for cinematic travel reels.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "DIY lakeside picnic", "Pinterest-style picnic by the water and hills.", "activity", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),
                genZCustomPlace(city, "Sunrise yoga viewpoint", "Peaceful sunrise hill vibe with clouds and calm air.", "activity", "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=1200&q=80"),
                genZCustomPlace(city, "Late-night waffle cafe", "Desserts and coffee with Gen-Z crowd energy.", "cafe", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Monsoon playlist drive", "Rain-drive atmosphere through hill roads and fog.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Floating brunch cafe", "Pinterest-style brunch setup with lake views.", "cafe", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight fort walk", "Late-night fort-view aesthetic with city lights.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic campfire dinner", "Live songs and bonfire vibe with friends.", "club", "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=1200&q=80"),
                genZCustomPlace(city, "Hidden sunset dock", "Dreamy sunset reflections and cinematic reels.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80")
        ),

        // trending
        List.of(
                genZCustomPlace(city, "Drone-shot hill viewpoint", "Wide cinematic valley reel location.", "place", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Luxury brunch cafe", "Pinterest-core café with aesthetic interiors.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Streetwear thrift market", "Oversized fits, rings and indie accessories.", "shopping", "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1200&q=80"),
                genZCustomPlace(city, "Gaming and neon cafe", "Gaming setup with loud Gen-Z crowd vibe.", "activity", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Mirror aesthetic dessert cafe", "Pastel interiors and viral dessert reels.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),
                genZCustomPlace(city, "Luxury yacht sunset", "Sunset yacht-party visuals for cinematic edits.", "activity", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80"),
                genZCustomPlace(city, "Sunset rooftop reels", "Golden-hour skyline vibe for transition edits.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),
                genZCustomPlace(city, "Vintage car photoshoot", "Luxury-car setup for aesthetic content shoots.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Indie jewellery lane", "Cute tote bags and handmade jewellery shopping.", "shopping", "https://images.unsplash.com/photo-1521334884684-d80222895322?w=1200&q=80"),
                genZCustomPlace(city, "Live DJ rooftop scene", "Dance music and Gen-Z nightlife atmosphere.", "club", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&q=80")
        ),

        // clubs / nightlife
        List.of(
                genZCustomPlace(city, "Hilltop bonfire party", "Bonfire, music and chill Gen-Z night vibe.", "club", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=1200&q=80"),
                genZCustomPlace(city, "Silent disco terrace", "Wireless-headphone dance setup with neon lights.", "club", "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=1200&q=80"),
                genZCustomPlace(city, "After-dark rooftop lounge", "Late-night skyline atmosphere with DJs.", "club", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Open mic indie night", "Poetry and acoustic songs with cozy crowd.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "Weekend neon rave", "Underground rave with lasers and heavy beats.", "club", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight tea adda", "Late-night tea and conversations under the stars.", "club", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic rooftop jam", "Night guitar sessions with fairy-light atmosphere.", "club", "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=1200&q=80"),
                genZCustomPlace(city, "Afterparty gaming lounge", "Gaming consoles and post-club chaos vibe.", "club", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Late-night dessert adda", "Midnight desserts and skyline conversations.", "club", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Live rooftop music scene", "Acoustic live music with skyline and fairy lights.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80")
)));// ======================= KOCHI =======================

case "kochi" -> Optional.of(genZCustomCity(city,

        // bunk spots
        List.of(
                genZCustomPlace(city, "Fort Kochi sunset cycle", "Beachside cycling with sunset skies and calm coastal vibe.", "activity", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80"),
                genZCustomPlace(city, "Cherai beach one-day trip", "Quick beach escape with friends and aesthetic waves.", "place", "https://images.unsplash.com/photo-1493558103817-58b2924bce98?w=1200&q=80"),
                genZCustomPlace(city, "Mattancherry cafe hopping", "Vintage cafés and indie Gen-Z hangout vibe.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Backwater ferry ride", "Relaxed ferry ride with cinematic water reflections.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Rainy chai waterfront stop", "Monsoon chai and calm backwater atmosphere.", "food", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Hidden art alley walk", "Street-art and colorful alleyways made for reels.", "place", "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=1200&q=80"),
                genZCustomPlace(city, "Scooter coastal roadtrip", "Ocean-road playlists and aesthetic coastal views.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Waterfront maggi spot", "Simple maggi-and-rain vibe beside the water.", "food", "https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?w=1200&q=80"),
                genZCustomPlace(city, "Kumbalangi village escape", "One-day peaceful village trip with boat vibes.", "place", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1200&q=80"),
                genZCustomPlace(city, "Sunrise fishing harbor walk", "Morning harbor atmosphere with soft golden light.", "activity", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80")
        ),

        // hidden gems
        List.of(
                genZCustomPlace(city, "Secret waterfront bench", "Quiet backwater corner for calm conversations and reels.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),
                genZCustomPlace(city, "Indie bookstore cafe", "Books, coffee and cozy rainy-day atmosphere.", "cafe", "https://images.unsplash.com/photo-1526243741027-444d633d7365?w=1200&q=80"),
                genZCustomPlace(city, "Vintage spice warehouse lane", "Old Kochi aesthetic streets with cinematic vibe.", "place", "https://images.unsplash.com/photo-1480714378408-67cf0d13bc1b?w=1200&q=80"),
                genZCustomPlace(city, "Hidden jazz rooftop", "Soft jazz and fairy lights with sea breeze.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "Art gallery loft", "Creative indie space with aesthetic content vibe.", "activity", "https://images.unsplash.com/photo-1517048676732-d65bc937f952?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight harbor walk", "Late-night harbor reflections with calm vibe.", "place", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Tea terrace hideout", "Lowkey tea spot with aesthetic skyline atmosphere.", "food", "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=1200&q=80"),
                genZCustomPlace(city, "Pottery art studio", "Creative pottery workshop with indie interiors.", "activity", "https://images.unsplash.com/photo-1517048676732-d65bc937f952?w=1200&q=80"),
                genZCustomPlace(city, "Silent ferry dock", "Minimal-crowd dock with dreamy water reflections.", "place", "https://images.unsplash.com/photo-1500534623283-312aade485b7?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic indie cafe", "Live acoustic songs with chill Gen-Z crowd.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80")
        ),

        // surprise
        List.of(
                genZCustomPlace(city, "Backwater movie night", "Open-air movie setup beside calm water views.", "activity", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight ferry ride", "Late-night ferry ride with skyline reflections.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "DIY beach picnic", "Cute beach picnic with fairy lights and reels.", "activity", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),
                genZCustomPlace(city, "Sunrise yoga beach scene", "Peaceful sunrise and ocean-breeze aesthetic vibe.", "activity", "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=1200&q=80"),
                genZCustomPlace(city, "Late-night dessert cafe", "Desserts and coffee with cozy Gen-Z crowd.", "cafe", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Vintage jeep coastal shoot", "Classic jeep setup for cinematic beach reels.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic rooftop dinner", "Live songs and fairy lights beside the sea.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Monsoon coastal drive", "Rain-drive vibe with ocean roads and playlists.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Floating brunch setup", "Pinterest-style brunch vibe over the water.", "cafe", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=1200&q=80"),
                genZCustomPlace(city, "Hidden sunset dock", "Dreamy dock-side sunset for aesthetic edits.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80")
        ),

        // trending
        List.of(
                genZCustomPlace(city, "Fort Kochi reel streets", "Most viral Kochi aesthetic streets for creators.", "place", "https://images.unsplash.com/photo-1480714378408-67cf0d13bc1b?w=1200&q=80"),
                genZCustomPlace(city, "Luxury waterfront cafe", "Pinterest-core café with calm water views.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Streetwear flea market", "Oversized fits, indie jewellery and tote bags.", "shopping", "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1200&q=80"),
                genZCustomPlace(city, "Drone-shot harbor viewpoint", "Wide cinematic reel viewpoint over the water.", "place", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Gaming and neon cafe", "Gaming plus snacks plus loud Gen-Z energy.", "activity", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Mirror aesthetic dessert cafe", "Pastel interiors and viral dessert reels.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),
                genZCustomPlace(city, "Luxury yacht sunset", "Yacht-party vibe with cinematic ocean visuals.", "activity", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80"),
                genZCustomPlace(city, "Sunset rooftop reels", "Golden-hour skyline visuals for transition edits.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),
                genZCustomPlace(city, "Live DJ waterfront cafe", "Dance music and backwater crowd energy together.", "club", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&q=80"),
                genZCustomPlace(city, "Vintage car photoshoot", "Luxury-car setup for cinematic content shoots.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80")
        ),

        // clubs / nightlife
        List.of(
                genZCustomPlace(city, "Beach rave night", "Heavy DJ beats and beach-party chaos together.", "club", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&q=80"),
                genZCustomPlace(city, "Silent disco rooftop", "Wireless-headphone dance setup with neon vibe.", "club", "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=1200&q=80"),
                genZCustomPlace(city, "After-dark harbor lounge", "Late-night waterfront atmosphere with DJs.", "club", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Open mic indie night", "Poetry and acoustic songs with cozy crowd.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "Weekend neon rave", "Underground rave with lasers and loud beats.", "club", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight tea adda", "Late-night tea and sea-breeze conversations.", "club", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic rooftop jam", "Night guitar sessions with fairy-light vibe.", "club", "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=1200&q=80"),
                genZCustomPlace(city, "Afterparty gaming lounge", "Gaming consoles and post-club crowd energy.", "club", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Late-night dessert adda", "Midnight desserts and skyline conversations.", "club", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Live rooftop music scene", "Acoustic live songs with fairy lights and water views.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80")
)));
                  // ======================= DARJEELING =======================

case "darjeeling" -> Optional.of(genZCustomCity(city,

        // bunk spots
        List.of(
                genZCustomPlace(city, "Tiger Hill sunrise run", "Cloudy sunrise vibe with cinematic mountain views.", "activity", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=1200&q=80"),
                genZCustomPlace(city, "Toy train aesthetic ride", "Vintage train reels and foggy hill atmosphere.", "activity", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=1200&q=80"),
                genZCustomPlace(city, "Tea garden picnic", "Cute hill picnic with tea-estate scenery and reels.", "place", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1200&q=80"),
                genZCustomPlace(city, "Hidden cafe hopping", "Cozy cafés with mountain fog and Gen-Z vibe.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Monastery calm stop", "Peaceful one-day escape with aesthetic mountain silence.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Rainy momo chai run", "Foggy-weather momos and hot chai scenes.", "food", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Mountain-road scooter ride", "Playlist ride through foggy roads and pine forests.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Hidden valley viewpoint", "Underrated mountain viewpoint with dreamy clouds.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),
                genZCustomPlace(city, "Sunset rooftop tea stop", "Late-evening tea and skyline mountain vibe.", "food", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Forest trail escape", "One-day nature trail with cinematic greenery.", "activity", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=1200&q=80")
        ),

        // hidden gems
        List.of(
                genZCustomPlace(city, "Secret tea estate corner", "Quiet tea-garden vibe with dreamy photo spots.", "place", "https://images.unsplash.com/photo-1500534623283-312aade485b7?w=1200&q=80"),
                genZCustomPlace(city, "Vintage bookstore cafe", "Books, coffee and cozy foggy atmosphere.", "cafe", "https://images.unsplash.com/photo-1526243741027-444d633d7365?w=1200&q=80"),
                genZCustomPlace(city, "Hidden monastery lane", "Minimal-crowd peaceful route with aesthetic vibes.", "place", "https://images.unsplash.com/photo-1480714378408-67cf0d13bc1b?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic hill cafe", "Indie music and fairy-light mountain vibe.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight fog viewpoint", "Late-night cloud aesthetic with calm silence.", "place", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Hidden momo rooftop", "Lowkey momo spot with valley skyline views.", "food", "https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?w=1200&q=80"),
                genZCustomPlace(city, "Tea art workshop", "Creative tea-making and pottery vibe together.", "activity", "https://images.unsplash.com/photo-1517048676732-d65bc937f952?w=1200&q=80"),
                genZCustomPlace(city, "Foggy pine road", "Cinematic pine-tree road for aesthetic reels.", "place", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Jazz rooftop hideout", "Soft jazz with mountain fog and warm lights.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "Tea terrace adda", "Late-night tea talks and mountain breeze vibe.", "food", "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=1200&q=80")
        ),

        // surprise
        List.of(
                genZCustomPlace(city, "Cloud picnic setup", "Pinterest-style mountain picnic with foggy vibes.", "activity", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),
                genZCustomPlace(city, "Sunrise yoga viewpoint", "Peaceful sunrise mountain atmosphere and calm air.", "activity", "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=1200&q=80"),
                genZCustomPlace(city, "Open-air movie hills", "Movie-night setup with blankets and mountain cold.", "activity", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&q=80"),
                genZCustomPlace(city, "Vintage jeep fog ride", "Classic jeep setup for cinematic mountain reels.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Late-night waffle cafe", "Dessert and coffee with cozy hill vibe.", "cafe", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic rooftop dinner", "Live songs and fairy lights in mountain weather.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Rain-drive mountain road", "Foggy rain-drive vibe with playlists and clouds.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Floating breakfast setup", "Pinterest-style breakfast with valley views.", "cafe", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight tea point", "Late-night tea and skyline cloud atmosphere.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Hidden waterfall stop", "Dreamy waterfall spot with cinematic reels.", "place", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80")
        ),

        // trending
        List.of(
                genZCustomPlace(city, "Drone-shot valley point", "Wide cinematic reel viewpoint over the hills.", "place", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Luxury hill cafe", "Pinterest-core café with foggy aesthetics.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Streetwear thrift lane", "Oversized fits and indie jewellery shopping.", "shopping", "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1200&q=80"),
                genZCustomPlace(city, "Gaming and neon cafe", "Gaming setup with loud Gen-Z crowd vibe.", "activity", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Mirror dessert cafe", "Pastel interiors and viral dessert reels.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),
                genZCustomPlace(city, "Sunset rooftop reels", "Golden-hour mountain skyline edits.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),
                genZCustomPlace(city, "Vintage car mountain shoot", "Luxury-car setup for cinematic content.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Indie jewellery market", "Cute rings, tote bags and handmade accessories.", "shopping", "https://images.unsplash.com/photo-1521334884684-d80222895322?w=1200&q=80"),
                genZCustomPlace(city, "Live DJ rooftop cafe", "Dance music and Gen-Z hill-night energy.", "club", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&q=80"),
                genZCustomPlace(city, "Aesthetic fog reels", "Cloudy cinematic edits and Pinterest-style visuals.", "place", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=1200&q=80")
        ),

        // clubs / nightlife
        List.of(
                genZCustomPlace(city, "Bonfire music night", "Bonfire, guitars and chill Gen-Z hill vibe.", "club", "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=1200&q=80"),
                genZCustomPlace(city, "Silent disco terrace", "Wireless-headphone dance setup in mountain cold.", "club", "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=1200&q=80"),
                genZCustomPlace(city, "After-dark hill lounge", "Late-night skyline atmosphere with DJs and lights.", "club", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Open mic indie night", "Poetry and acoustic songs with cozy crowd.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "Weekend neon rave", "Underground hill rave with lasers and DJs.", "club", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight tea adda", "Late-night tea talks under cloudy skies.", "club", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic rooftop jam", "Night guitar sessions with fairy-light vibe.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Afterparty gaming lounge", "Gaming consoles and post-club chaos vibe.", "club", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Late-night dessert adda", "Midnight desserts and mountain conversations.", "club", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Live rooftop music scene", "Acoustic live music with mountain skyline.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80")
)));
                   // ======================= LEH =======================

case "leh" -> Optional.of(genZCustomCity(city,

        // bunk spots
        List.of(
                genZCustomPlace(city, "Khardung La sunrise ride", "High-altitude roadtrip vibe with cinematic mountain roads.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Pangong one-day escape", "Lake-view roadtrip with dreamy blue-water aesthetics.", "place", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1200&q=80"),
                genZCustomPlace(city, "Leh market cafe hopping", "Mountain cafés and Gen-Z coffee-break vibe.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Magnetic Hill drive", "Playlist drive through surreal Leh landscapes.", "activity", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=1200&q=80"),
                genZCustomPlace(city, "Monastery peace stop", "Calm monastery views with mountain silence.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Nubra Valley picnic", "Cute valley picnic with cold desert aesthetics.", "activity", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),
                genZCustomPlace(city, "Hilltop maggi point", "Cold-weather maggi and chai with skyline views.", "food", "https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?w=1200&q=80"),
                genZCustomPlace(city, "Scooter mountain loop", "Foggy road reels and cinematic valley views.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Hidden glacier stop", "Underrated glacier viewpoint for dreamy edits.", "place", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80"),
                genZCustomPlace(city, "Sunset rooftop tea stop", "Golden-hour tea with mountain skyline atmosphere.", "food", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80")
        ),

        // hidden gems
        List.of(
                genZCustomPlace(city, "Secret valley viewpoint", "Quiet valley corner with cinematic cloud scenery.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),
                genZCustomPlace(city, "Vintage mountain cafe", "Retro-style café with cozy mountain vibe.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),
                genZCustomPlace(city, "Hidden monastery lane", "Peaceful route away from heavy tourist rush.", "place", "https://images.unsplash.com/photo-1480714378408-67cf0d13bc1b?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic campfire cafe", "Indie songs and bonfire vibe in mountain cold.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight desert point", "Late-night cold-desert skyline and star views.", "place", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Tea terrace hideout", "Lowkey tea spot with calm Leh skyline.", "food", "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=1200&q=80"),
                genZCustomPlace(city, "Pottery and art studio", "Creative indie workshop with aesthetic interiors.", "activity", "https://images.unsplash.com/photo-1517048676732-d65bc937f952?w=1200&q=80"),
                genZCustomPlace(city, "Foggy mountain trail", "Minimal-crowd trekking route with dreamy views.", "activity", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=1200&q=80"),
                genZCustomPlace(city, "Jazz rooftop hideout", "Soft jazz and fairy lights with snowy mountains.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "Hidden frozen lake stop", "Dreamy frozen-lake aesthetic for cinematic reels.", "place", "https://images.unsplash.com/photo-1500534623283-312aade485b7?w=1200&q=80")
        ),

        // surprise
        List.of(
                genZCustomPlace(city, "Open-air movie hills", "Movie-night setup under the stars and mountains.", "activity", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&q=80"),
                genZCustomPlace(city, "Cloud picnic setup", "Pinterest-style picnic with snowy mountain backdrop.", "activity", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),
                genZCustomPlace(city, "Sunrise yoga viewpoint", "Peaceful sunrise air with snowy valley views.", "activity", "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=1200&q=80"),
                genZCustomPlace(city, "Vintage jeep snow ride", "Classic jeep reels through mountain roads.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Late-night waffle cafe", "Dessert and coffee with cozy hill atmosphere.", "cafe", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic rooftop dinner", "Live songs and fairy lights under the cold sky.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Snow-drive playlist route", "Cinematic snow-road drive with playlists.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Floating breakfast setup", "Luxury breakfast vibe with snowy mountain views.", "cafe", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight glacier point", "Late-night icy skyline aesthetic and star vibes.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Frozen waterfall stop", "Dreamy waterfall visuals made for aesthetic edits.", "place", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80")
        ),

        // trending
        List.of(
                genZCustomPlace(city, "Drone-shot valley reels", "Wide cinematic mountain visuals for creators.", "place", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Luxury mountain cafe", "Pinterest-core café with snowy aesthetics.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Streetwear winter market", "Oversized jackets and indie accessories shopping.", "shopping", "https://images.unsplash.com/photo-1521334884684-d80222895322?w=1200&q=80"),
                genZCustomPlace(city, "Gaming and neon cafe", "Gaming setup with loud Gen-Z winter vibe.", "activity", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Mirror dessert cafe", "Pastel interiors and viral dessert reels.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),
                genZCustomPlace(city, "Snowy sunset reels", "Golden-hour snowy skyline edits.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),
                genZCustomPlace(city, "Vintage car mountain shoot", "Luxury-car setup for cinematic snow edits.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Indie jewellery market", "Cute rings and handmade winter accessories.", "shopping", "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1200&q=80"),
                genZCustomPlace(city, "Live DJ rooftop cafe", "Dance music and snowy-night crowd energy.", "club", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&q=80"),
                genZCustomPlace(city, "Cloud aesthetic reels", "Foggy cinematic edits with snowy backgrounds.", "place", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=1200&q=80")
        ),

        // clubs / nightlife
        List.of(
                genZCustomPlace(city, "Bonfire music night", "Bonfire, guitars and chill snowy-night vibe.", "club", "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=1200&q=80"),
                genZCustomPlace(city, "Silent disco terrace", "Wireless-headphone dance setup in mountain cold.", "club", "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=1200&q=80"),
                genZCustomPlace(city, "After-dark hill lounge", "Late-night snowy skyline atmosphere with DJs.", "club", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Open mic indie night", "Poetry and acoustic songs with cozy crowd.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "Weekend neon rave", "Underground snow-rave with lasers and DJs.", "club", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight tea adda", "Late-night tea talks under starry skies.", "club", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic rooftop jam", "Night guitar sessions with fairy-light vibe.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Afterparty gaming lounge", "Gaming consoles and post-club crowd energy.", "club", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Late-night dessert adda", "Midnight desserts and snowy conversations.", "club", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Live rooftop music scene", "Acoustic live music with snowy skyline.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80")
)));
                 // ======================= SRINAGAR =======================

case "srinagar" -> Optional.of(genZCustomCity(city,

        // bunk spots
        List.of(
                genZCustomPlace(city, "Dal Lake shikara morning", "Calm shikara ride with dreamy Kashmir reflections.", "activity", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1200&q=80"),
                genZCustomPlace(city, "Gulmarg one-day snow run", "Quick snow-trip vibe with reels and cold mountain air.", "place", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=1200&q=80"),
                genZCustomPlace(city, "Boulevard Road cafe hopping", "Lake-view cafés and cozy Gen-Z coffee scenes.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Houseboat sunset adda", "Golden-hour lake atmosphere with calm conversations.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Rainy kahwa chai stop", "Cold-weather kahwa vibe beside mountain views.", "food", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Pahalgam picnic escape", "Cute valley picnic with cinematic pine forests.", "activity", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),
                genZCustomPlace(city, "Hidden valley scooter ride", "Playlist ride through foggy Kashmir roads.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Snow maggi viewpoint", "Hot maggi and snowy skyline together.", "food", "https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?w=1200&q=80"),
                genZCustomPlace(city, "Tulip garden aesthetic walk", "Pinterest-style flower reels and dreamy colors.", "place", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=1200&q=80"),
                genZCustomPlace(city, "Sunset rooftop kahwa stop", "Late-evening tea and mountain skyline vibe.", "food", "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=1200&q=80")
        ),

        // hidden gems
        List.of(
                genZCustomPlace(city, "Secret lake-view corner", "Quiet Dal Lake viewpoint away from tourist rush.", "place", "https://images.unsplash.com/photo-1500534623283-312aade485b7?w=1200&q=80"),
                genZCustomPlace(city, "Vintage Kashmiri cafe", "Warm wooden interiors and cozy winter atmosphere.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),
                genZCustomPlace(city, "Hidden pine forest trail", "Minimal-crowd snowy trail with dreamy mountain vibe.", "activity", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic lakeside cafe", "Indie songs and fairy lights beside the water.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight snow viewpoint", "Late-night snowy skyline with cinematic silence.", "place", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Hidden kahwa terrace", "Lowkey tea stop with lake and mountain views.", "food", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Art and carpet studio", "Creative Kashmiri craft setup with aesthetic vibe.", "activity", "https://images.unsplash.com/photo-1517048676732-d65bc937f952?w=1200&q=80"),
                genZCustomPlace(city, "Foggy valley road", "Cinematic pine-road reels with mountain clouds.", "place", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Jazz rooftop hideout", "Soft jazz and snowy skyline with warm lights.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "Frozen stream stop", "Dreamy icy-water aesthetic for cinematic edits.", "place", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80")
        ),

        // surprise
        List.of(
                genZCustomPlace(city, "Open-air movie snow setup", "Movie-night vibe under snowy skies and blankets.", "activity", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&q=80"),
                genZCustomPlace(city, "Cloud picnic setup", "Pinterest-style picnic with snowy valley scenery.", "activity", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),
                genZCustomPlace(city, "Sunrise yoga lake point", "Peaceful sunrise and cold mountain air together.", "activity", "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=1200&q=80"),
                genZCustomPlace(city, "Vintage jeep snow ride", "Classic jeep reels through snowy Kashmir roads.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Late-night waffle cafe", "Desserts and coffee with cozy hill atmosphere.", "cafe", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic rooftop dinner", "Live songs and fairy lights under snowy skies.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Snow-drive playlist route", "Cinematic snow-road drive with playlists.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Floating breakfast setup", "Luxury breakfast vibe with lake reflections.", "cafe", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight glacier point", "Late-night icy skyline and dreamy stars.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Frozen waterfall stop", "Dreamy frozen-waterfall visuals for edits.", "place", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80")
        ),

        // trending
        List.of(
                genZCustomPlace(city, "Drone-shot valley reels", "Wide cinematic Kashmir visuals for creators.", "place", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Luxury lake-view cafe", "Pinterest-core café with snowy aesthetics.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Streetwear winter market", "Oversized jackets and indie shopping vibe.", "shopping", "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1200&q=80"),
                genZCustomPlace(city, "Gaming and neon cafe", "Gaming setup with loud Gen-Z winter vibe.", "activity", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Mirror dessert cafe", "Pastel interiors and viral dessert reels.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),
                genZCustomPlace(city, "Snowy sunset reels", "Golden-hour snowy skyline edits.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),
                genZCustomPlace(city, "Vintage car mountain shoot", "Luxury-car setup for cinematic snow edits.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Indie jewellery market", "Cute rings and handmade winter accessories.", "shopping", "https://images.unsplash.com/photo-1521334884684-d80222895322?w=1200&q=80"),
                genZCustomPlace(city, "Live DJ rooftop cafe", "Dance music and snowy-night crowd energy.", "club", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&q=80"),
                genZCustomPlace(city, "Cloud aesthetic reels", "Foggy cinematic edits with snowy backgrounds.", "place", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=1200&q=80")
        ),

        // clubs / nightlife
        List.of(
                genZCustomPlace(city, "Bonfire music night", "Bonfire, guitars and chill snowy-night vibe.", "club", "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=1200&q=80"),
                genZCustomPlace(city, "Silent disco terrace", "Wireless-headphone dance setup in mountain cold.", "club", "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=1200&q=80"),
                genZCustomPlace(city, "After-dark hill lounge", "Late-night snowy skyline atmosphere with DJs.", "club", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Open mic indie night", "Poetry and acoustic songs with cozy crowd.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "Weekend neon rave", "Underground snow-rave with lasers and DJs.", "club", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight kahwa adda", "Late-night kahwa talks under snowy skies.", "club", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic rooftop jam", "Night guitar sessions with fairy-light vibe.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Afterparty gaming lounge", "Gaming consoles and post-club crowd energy.", "club", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Late-night dessert adda", "Midnight desserts and snowy conversations.", "club", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Live rooftop music scene", "Acoustic live music with snowy skyline.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80")
)));
                  // ======================= MANALI =======================

case "manali" -> Optional.of(genZCustomCity(city,

        // bunk spots
        List.of(
                genZCustomPlace(city, "Solang Valley ride", "Snow-view roadtrip vibe with reels and mountain air.", "activity", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=1200&q=80"),
                genZCustomPlace(city, "Old Manali cafe hopping", "Indie cafés and Gen-Z mountain crowd vibe.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Jogini waterfall trek", "One-day waterfall escape with cinematic forest views.", "activity", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80"),
                genZCustomPlace(city, "Snow maggi point", "Hot maggi and snowy valley skyline together.", "food", "https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?w=1200&q=80"),
                genZCustomPlace(city, "Mountain-road scooter ride", "Playlist drive through pine forests and fog.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Hidden riverside picnic", "Cute riverside setup with aesthetic cold-weather vibe.", "place", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),
                genZCustomPlace(city, "Sunset tea rooftop", "Golden-hour mountain skyline and chai vibe.", "food", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Kasol quick escape", "One-day hippie-town trip with mountain aesthetics.", "place", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1200&q=80"),
                genZCustomPlace(city, "Pine forest trail", "Foggy trail route with cinematic greenery.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Snowfall late-night walk", "Cold-weather night walk with dreamy snow vibes.", "place", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=1200&q=80")
        ),

        // hidden gems
        List.of(
                genZCustomPlace(city, "Secret valley viewpoint", "Quiet mountain corner with dreamy cloud visuals.", "place", "https://images.unsplash.com/photo-1500534623283-312aade485b7?w=1200&q=80"),
                genZCustomPlace(city, "Vintage hill cafe", "Retro café interiors with warm indie atmosphere.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),
                genZCustomPlace(city, "Hidden forest cabin", "Minimal-crowd cabin vibe surrounded by pine trees.", "place", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic campfire cafe", "Indie songs and bonfire atmosphere together.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight snow viewpoint", "Late-night snowy skyline and cinematic silence.", "place", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Tea terrace hideout", "Lowkey tea stop with calm mountain atmosphere.", "food", "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=1200&q=80"),
                genZCustomPlace(city, "Pottery art workshop", "Creative indie workshop with aesthetic interiors.", "activity", "https://images.unsplash.com/photo-1517048676732-d65bc937f952?w=1200&q=80"),
                genZCustomPlace(city, "Foggy bridge stop", "Cinematic bridge reels with mountain fog.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Jazz rooftop hideout", "Soft jazz and fairy lights with snowy skyline.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "Frozen stream corner", "Dreamy icy-water visuals for aesthetic edits.", "place", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80")
        ),

        // surprise
        List.of(
                genZCustomPlace(city, "Open-air movie hills", "Movie-night setup under snowy mountain skies.", "activity", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&q=80"),
                genZCustomPlace(city, "Cloud picnic setup", "Pinterest-style picnic with valley scenery.", "activity", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),
                genZCustomPlace(city, "Sunrise yoga viewpoint", "Peaceful sunrise mountain atmosphere and cold air.", "activity", "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=1200&q=80"),
                genZCustomPlace(city, "Vintage jeep snow ride", "Classic jeep setup for cinematic mountain reels.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Late-night waffle cafe", "Desserts and coffee with cozy hill vibe.", "cafe", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic rooftop dinner", "Live songs and fairy lights under cold skies.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Snow-drive playlist route", "Cinematic snow-road drive with playlists.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Floating breakfast setup", "Luxury breakfast vibe with snowy valley views.", "cafe", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight glacier point", "Late-night icy skyline aesthetic and stars.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Frozen waterfall stop", "Dreamy waterfall visuals for cinematic edits.", "place", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80")
        ),

        // trending
        List.of(
                genZCustomPlace(city, "Drone-shot valley reels", "Wide cinematic mountain visuals for creators.", "place", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Luxury hill cafe", "Pinterest-core café with snowy aesthetics.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Streetwear winter market", "Oversized jackets and indie accessories shopping.", "shopping", "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1200&q=80"),
                genZCustomPlace(city, "Gaming and neon cafe", "Gaming setup with loud Gen-Z winter vibe.", "activity", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Mirror dessert cafe", "Pastel interiors and viral dessert reels.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),
                genZCustomPlace(city, "Snowy sunset reels", "Golden-hour snowy skyline edits.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),
                genZCustomPlace(city, "Vintage car mountain shoot", "Luxury-car setup for cinematic snow edits.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Indie jewellery market", "Cute rings and handmade winter accessories.", "shopping", "https://images.unsplash.com/photo-1521334884684-d80222895322?w=1200&q=80"),
                genZCustomPlace(city, "Live DJ rooftop cafe", "Dance music and snowy-night crowd energy.", "club", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&q=80"),
                genZCustomPlace(city, "Cloud aesthetic reels", "Foggy cinematic edits with snowy backgrounds.", "place", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=1200&q=80")
        ),

        // clubs / nightlife
        List.of(
                genZCustomPlace(city, "Bonfire music night", "Bonfire, guitars and chill snowy-night vibe.", "club", "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=1200&q=80"),
                genZCustomPlace(city, "Silent disco terrace", "Wireless-headphone dance setup in mountain cold.", "club", "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=1200&q=80"),
                genZCustomPlace(city, "After-dark hill lounge", "Late-night snowy skyline atmosphere with DJs.", "club", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Open mic indie night", "Poetry and acoustic songs with cozy crowd.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "Weekend neon rave", "Underground snow-rave with lasers and DJs.", "club", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight tea adda", "Late-night tea talks under snowy skies.", "club", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic rooftop jam", "Night guitar sessions with fairy-light vibe.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Afterparty gaming lounge", "Gaming consoles and post-club crowd energy.", "club", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Late-night dessert adda", "Midnight desserts and snowy conversations.", "club", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Live rooftop music scene", "Acoustic live music with snowy skyline.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80")
)));// ======================= SHIMLA =======================

case "shimla" -> Optional.of(genZCustomCity(city,

        // bunk spots
        List.of(
                genZCustomPlace(city, "Mall Road evening walk", "Cold-weather walk with cafés, lights and Gen-Z vibe.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Kufri snow escape", "Quick snow-trip plan with reels and mountain views.", "activity", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=1200&q=80"),
                genZCustomPlace(city, "Hidden hill cafe hopping", "Indie cafés and cozy winter atmosphere together.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Toy train aesthetic ride", "Vintage train ride with cinematic hill views.", "activity", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=1200&q=80"),
                genZCustomPlace(city, "Snow maggi viewpoint", "Hot maggi and snowy skyline in one vibe.", "food", "https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?w=1200&q=80"),
                genZCustomPlace(city, "Pine forest trail", "Foggy pine-road walk with cinematic mountain atmosphere.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Sunset tea rooftop", "Golden-hour tea and mountain skyline mood.", "food", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Mashobra quick ride", "One-day mountain escape with calm snowy roads.", "place", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1200&q=80"),
                genZCustomPlace(city, "Hidden riverside picnic", "Cute riverside setup with aesthetic hill vibe.", "place", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),
                genZCustomPlace(city, "Late-night snowfall walk", "Dreamy night walk during soft snowfall scenes.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80")
        ),

        // hidden gems
        List.of(
                genZCustomPlace(city, "Secret valley viewpoint", "Quiet hill corner with dreamy cloud visuals.", "place", "https://images.unsplash.com/photo-1500534623283-312aade485b7?w=1200&q=80"),
                genZCustomPlace(city, "Vintage hill cafe", "Retro café interiors with cozy indie atmosphere.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),
                genZCustomPlace(city, "Hidden forest cabin", "Minimal-crowd snowy cabin with aesthetic vibe.", "place", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic campfire cafe", "Indie songs and bonfire vibe with mountain cold.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight snow viewpoint", "Late-night snowy skyline and cinematic silence.", "place", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Tea terrace hideout", "Lowkey tea stop with calm mountain atmosphere.", "food", "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=1200&q=80"),
                genZCustomPlace(city, "Pottery art workshop", "Creative indie workshop with aesthetic interiors.", "activity", "https://images.unsplash.com/photo-1517048676732-d65bc937f952?w=1200&q=80"),
                genZCustomPlace(city, "Foggy bridge stop", "Cinematic bridge reels with mountain fog.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Jazz rooftop hideout", "Soft jazz and fairy lights with snowy skyline.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "Frozen stream corner", "Dreamy icy-water visuals for aesthetic edits.", "place", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80")
        ),

        // surprise
        List.of(
                genZCustomPlace(city, "Open-air movie hills", "Movie-night setup under snowy mountain skies.", "activity", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&q=80"),
                genZCustomPlace(city, "Cloud picnic setup", "Pinterest-style picnic with valley scenery.", "activity", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),
                genZCustomPlace(city, "Sunrise yoga viewpoint", "Peaceful sunrise mountain atmosphere and cold air.", "activity", "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=1200&q=80"),
                genZCustomPlace(city, "Vintage jeep snow ride", "Classic jeep setup for cinematic mountain reels.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Late-night waffle cafe", "Desserts and coffee with cozy hill vibe.", "cafe", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic rooftop dinner", "Live songs and fairy lights under cold skies.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Snow-drive playlist route", "Cinematic snow-road drive with playlists.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Floating breakfast setup", "Luxury breakfast vibe with snowy valley views.", "cafe", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight glacier point", "Late-night icy skyline aesthetic and stars.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Frozen waterfall stop", "Dreamy waterfall visuals for cinematic edits.", "place", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80")
        ),

        // trending
        List.of(
                genZCustomPlace(city, "Drone-shot valley reels", "Wide cinematic mountain visuals for creators.", "place", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Luxury hill cafe", "Pinterest-core café with snowy aesthetics.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Streetwear winter market", "Oversized jackets and indie accessories shopping.", "shopping", "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1200&q=80"),
                genZCustomPlace(city, "Gaming and neon cafe", "Gaming setup with loud Gen-Z winter vibe.", "activity", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Mirror dessert cafe", "Pastel interiors and viral dessert reels.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),
                genZCustomPlace(city, "Snowy sunset reels", "Golden-hour snowy skyline edits.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),
                genZCustomPlace(city, "Vintage car mountain shoot", "Luxury-car setup for cinematic snow edits.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Indie jewellery market", "Cute rings and handmade winter accessories.", "shopping", "https://images.unsplash.com/photo-1521334884684-d80222895322?w=1200&q=80"),
                genZCustomPlace(city, "Live DJ rooftop cafe", "Dance music and snowy-night crowd energy.", "club", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&q=80"),
                genZCustomPlace(city, "Cloud aesthetic reels", "Foggy cinematic edits with snowy backgrounds.", "place", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=1200&q=80")
        ),

        // clubs / nightlife
        List.of(
                genZCustomPlace(city, "Bonfire music night", "Bonfire, guitars and chill snowy-night vibe.", "club", "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=1200&q=80"),
                genZCustomPlace(city, "Silent disco terrace", "Wireless-headphone dance setup in mountain cold.", "club", "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=1200&q=80"),
                genZCustomPlace(city, "After-dark hill lounge", "Late-night snowy skyline atmosphere with DJs.", "club", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Open mic indie night", "Poetry and acoustic songs with cozy crowd.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "Weekend neon rave", "Underground snow-rave with lasers and DJs.", "club", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight tea adda", "Late-night tea talks under snowy skies.", "club", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic rooftop jam", "Night guitar sessions with fairy-light vibe.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Afterparty gaming lounge", "Gaming consoles and post-club crowd energy.", "club", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Late-night dessert adda", "Midnight desserts and snowy conversations.", "club", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Live rooftop music scene", "Acoustic live music with snowy skyline.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80")
)));
                  // ======================= RISHIKESH =======================

case "rishikesh" -> Optional.of(genZCustomCity(city,

        // bunk spots
        List.of(
                genZCustomPlace(city, "Lakshman Jhula sunrise walk", "Morning river vibe with reels and calm mountain air.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "River rafting escape", "One-day adrenaline plan with Gen-Z group energy.", "activity", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80"),
                genZCustomPlace(city, "Beatles cafe hopping", "Aesthetic cafés and slow riverside conversations.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Neer waterfall trek", "Quick waterfall escape with cinematic greenery.", "activity", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=1200&q=80"),
                genZCustomPlace(city, "Riverside maggi point", "Hot maggi and cold river breeze together.", "food", "https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?w=1200&q=80"),
                genZCustomPlace(city, "Scooter ghat ride", "Playlist ride beside the Ganga and mountains.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Sunset chai rooftop", "Golden-hour tea vibe with river skyline.", "food", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Hidden riverside picnic", "Cute picnic setup with calm water atmosphere.", "place", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),
                genZCustomPlace(city, "Forest yoga trail", "Nature trail and peaceful mountain energy together.", "activity", "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=1200&q=80"),
                genZCustomPlace(city, "Late-night river walk", "Dreamy riverside night walk with cold breeze.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80")
        ),

        // hidden gems
        List.of(
                genZCustomPlace(city, "Secret riverside viewpoint", "Quiet river corner with dreamy mountain visuals.", "place", "https://images.unsplash.com/photo-1500534623283-312aade485b7?w=1200&q=80"),
                genZCustomPlace(city, "Vintage yoga cafe", "Retro café vibe with calm indie atmosphere.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),
                genZCustomPlace(city, "Hidden forest cabin", "Minimal-crowd cabin surrounded by greenery.", "place", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic riverside cafe", "Indie songs and fairy lights beside the river.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight river viewpoint", "Late-night riverside skyline and calm silence.", "place", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Tea terrace hideout", "Lowkey tea stop with river and mountain atmosphere.", "food", "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=1200&q=80"),
                genZCustomPlace(city, "Pottery art workshop", "Creative indie workshop with aesthetic interiors.", "activity", "https://images.unsplash.com/photo-1517048676732-d65bc937f952?w=1200&q=80"),
                genZCustomPlace(city, "Foggy jungle road", "Cinematic jungle-road reels with clouds.", "place", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Jazz rooftop hideout", "Soft jazz and fairy lights with river skyline.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "Hidden waterfall stop", "Dreamy waterfall visuals for aesthetic edits.", "place", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80")
        ),

        // surprise
        List.of(
                genZCustomPlace(city, "Open-air movie riverside", "Movie-night setup beside the river and mountains.", "activity", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&q=80"),
                genZCustomPlace(city, "Cloud picnic setup", "Pinterest-style picnic beside flowing river views.", "activity", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),
                genZCustomPlace(city, "Sunrise yoga viewpoint", "Peaceful sunrise river atmosphere and cold air.", "activity", "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=1200&q=80"),
                genZCustomPlace(city, "Vintage jeep mountain ride", "Classic jeep setup for cinematic reels.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Late-night waffle cafe", "Desserts and coffee with cozy riverside vibe.", "cafe", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic rooftop dinner", "Live songs and fairy lights beside the river.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Rain-drive playlist route", "Cinematic jungle-road drive with playlists.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Floating breakfast setup", "Luxury breakfast vibe with river scenery.", "cafe", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight river point", "Late-night riverside aesthetic and dreamy stars.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Forest waterfall stop", "Dreamy waterfall visuals made for cinematic edits.", "place", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80")
        ),

        // trending
        List.of(
                genZCustomPlace(city, "Drone-shot river reels", "Wide cinematic river visuals for creators.", "place", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Luxury riverside cafe", "Pinterest-core café with dreamy river aesthetics.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Streetwear indie market", "Oversized fits and handmade accessories shopping.", "shopping", "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1200&q=80"),
                genZCustomPlace(city, "Gaming and neon cafe", "Gaming setup with loud Gen-Z crowd vibe.", "activity", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Mirror dessert cafe", "Pastel interiors and viral dessert reels.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),
                genZCustomPlace(city, "Sunset bridge reels", "Golden-hour river skyline edits.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),
                genZCustomPlace(city, "Vintage car riverside shoot", "Luxury-car setup for cinematic reels.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Indie jewellery market", "Cute rings and handmade accessories shopping.", "shopping", "https://images.unsplash.com/photo-1521334884684-d80222895322?w=1200&q=80"),
                genZCustomPlace(city, "Live DJ rooftop cafe", "Dance music and riverside crowd energy.", "club", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&q=80"),
                genZCustomPlace(city, "Cloud aesthetic reels", "Foggy cinematic edits with river backgrounds.", "place", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=1200&q=80")
        ),

        // clubs / nightlife
        List.of(
                genZCustomPlace(city, "Bonfire music night", "Bonfire, guitars and chill riverside-night vibe.", "club", "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=1200&q=80"),
                genZCustomPlace(city, "Silent disco terrace", "Wireless-headphone dance setup in mountain air.", "club", "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=1200&q=80"),
                genZCustomPlace(city, "After-dark river lounge", "Late-night river atmosphere with DJs and lights.", "club", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Open mic indie night", "Poetry and acoustic songs with cozy crowd.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "Weekend neon rave", "Underground riverside rave with lasers and DJs.", "club", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight chai adda", "Late-night chai talks beside the river.", "club", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic rooftop jam", "Night guitar sessions with fairy-light vibe.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Afterparty gaming lounge", "Gaming consoles and post-club crowd energy.", "club", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Late-night dessert adda", "Midnight desserts and riverside conversations.", "club", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Live rooftop music scene", "Acoustic live music with river skyline.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80")
)));
                  // ======================= CHAMBA =======================

case "chamba" -> Optional.of(genZCustomCity(city,

        // bunk spots
        List.of(
                genZCustomPlace(city, "Khajjiar one-day escape", "Mini-Switzerland vibe with reels and mountain air.", "place", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1200&q=80"),
                genZCustomPlace(city, "Pine forest trail walk", "Foggy forest walk with cinematic greenery.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Hidden hill cafe hopping", "Indie cafés and calm mountain conversations.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Riverside maggi point", "Hot maggi and cold mountain breeze together.", "food", "https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?w=1200&q=80"),
                genZCustomPlace(city, "Scooter valley ride", "Playlist ride through foggy hill roads.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Sunset chai rooftop", "Golden-hour tea vibe with valley skyline.", "food", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Hidden riverside picnic", "Cute picnic setup with dreamy mountain atmosphere.", "place", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),
                genZCustomPlace(city, "Foggy bridge stop", "Cinematic bridge reels with pine-forest views.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Mountain waterfall trek", "One-day waterfall escape with cold-air vibes.", "activity", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80"),
                genZCustomPlace(city, "Late-night valley walk", "Dreamy night walk with mountain silence.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80")
        ),

        // hidden gems
        List.of(
                genZCustomPlace(city, "Secret valley viewpoint", "Quiet mountain corner with dreamy cloud visuals.", "place", "https://images.unsplash.com/photo-1500534623283-312aade485b7?w=1200&q=80"),
                genZCustomPlace(city, "Vintage hill cafe", "Retro café interiors with cozy indie atmosphere.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),
                genZCustomPlace(city, "Hidden forest cabin", "Minimal-crowd cabin surrounded by pine trees.", "place", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic campfire cafe", "Indie songs and bonfire mountain vibe.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight hill viewpoint", "Late-night skyline and cinematic silence together.", "place", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Tea terrace hideout", "Lowkey tea stop with calm mountain atmosphere.", "food", "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=1200&q=80"),
                genZCustomPlace(city, "Pottery art workshop", "Creative indie workshop with aesthetic interiors.", "activity", "https://images.unsplash.com/photo-1517048676732-d65bc937f952?w=1200&q=80"),
                genZCustomPlace(city, "Foggy jungle road", "Cinematic jungle-road reels with mountain clouds.", "place", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Jazz rooftop hideout", "Soft jazz and fairy lights with valley skyline.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "Hidden waterfall stop", "Dreamy waterfall visuals made for aesthetic edits.", "place", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80")
        ),

        // surprise
        List.of(
                genZCustomPlace(city, "Open-air movie hills", "Movie-night setup under mountain skies.", "activity", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&q=80"),
                genZCustomPlace(city, "Cloud picnic setup", "Pinterest-style picnic with valley scenery.", "activity", "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?w=1200&q=80"),
                genZCustomPlace(city, "Sunrise yoga viewpoint", "Peaceful sunrise mountain atmosphere and cold air.", "activity", "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=1200&q=80"),
                genZCustomPlace(city, "Vintage jeep hill ride", "Classic jeep setup for cinematic mountain reels.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Late-night waffle cafe", "Desserts and coffee with cozy hill vibe.", "cafe", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic rooftop dinner", "Live songs and fairy lights under cold skies.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Rain-drive playlist route", "Cinematic hill-road drive with playlists.", "activity", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Floating breakfast setup", "Luxury breakfast vibe with mountain views.", "cafe", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight valley point", "Late-night valley aesthetic and dreamy stars.", "place", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=80"),
                genZCustomPlace(city, "Forest waterfall stop", "Dreamy waterfall visuals for cinematic edits.", "place", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80")
        ),

        // trending
        List.of(
                genZCustomPlace(city, "Drone-shot valley reels", "Wide cinematic mountain visuals for creators.", "place", "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=80"),
                genZCustomPlace(city, "Luxury hill cafe", "Pinterest-core café with dreamy mountain aesthetics.", "cafe", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"),
                genZCustomPlace(city, "Streetwear indie market", "Oversized fits and handmade accessories shopping.", "shopping", "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1200&q=80"),
                genZCustomPlace(city, "Gaming and neon cafe", "Gaming setup with loud Gen-Z crowd vibe.", "activity", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Mirror dessert cafe", "Pastel interiors and viral dessert reels.", "cafe", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1200&q=80"),
                genZCustomPlace(city, "Sunset valley reels", "Golden-hour skyline edits with mountain clouds.", "place", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=1200&q=80"),
                genZCustomPlace(city, "Vintage car mountain shoot", "Luxury-car setup for cinematic hill edits.", "activity", "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80"),
                genZCustomPlace(city, "Indie jewellery market", "Cute rings and handmade accessories shopping.", "shopping", "https://images.unsplash.com/photo-1521334884684-d80222895322?w=1200&q=80"),
                genZCustomPlace(city, "Live DJ rooftop cafe", "Dance music and mountain crowd energy.", "club", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&q=80"),
                genZCustomPlace(city, "Cloud aesthetic reels", "Foggy cinematic edits with valley backgrounds.", "place", "https://images.unsplash.com/photo-1472396961693-142e6e269027?w=1200&q=80")
        ),

        // clubs / nightlife
        List.of(
                genZCustomPlace(city, "Bonfire music night", "Bonfire, guitars and chill mountain-night vibe.", "club", "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=1200&q=80"),
                genZCustomPlace(city, "Silent disco terrace", "Wireless-headphone dance setup in mountain air.", "club", "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=1200&q=80"),
                genZCustomPlace(city, "After-dark hill lounge", "Late-night valley atmosphere with DJs and lights.", "club", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&q=80"),
                genZCustomPlace(city, "Open mic indie night", "Poetry and acoustic songs with cozy crowd.", "club", "https://images.unsplash.com/photo-1515169067868-5387ec356754?w=1200&q=80"),
                genZCustomPlace(city, "Weekend neon rave", "Underground hill rave with lasers and DJs.", "club", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=1200&q=80"),
                genZCustomPlace(city, "Moonlight chai adda", "Late-night chai talks beside the hills.", "club", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80"),
                genZCustomPlace(city, "Acoustic rooftop jam", "Night guitar sessions with fairy-light vibe.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80"),
                genZCustomPlace(city, "Afterparty gaming lounge", "Gaming consoles and post-club crowd energy.", "club", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=80"),
                genZCustomPlace(city, "Late-night dessert adda", "Midnight desserts and mountain conversations.", "club", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&q=80"),
                genZCustomPlace(city, "Live rooftop music scene", "Acoustic live music with valley skyline.", "club", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&q=80")
)));
            default -> Optional.empty();
        };
    }

    private GenZModeData genZOnlyCity(
            CityPage city,
            GenZPlace bunk,
            GenZPlace cafe,
            GenZPlace hidden,
            GenZPlace trending,
            GenZPlace romantic,
            GenZPlace party) {
        return genZCustomCity(
                city,
                List.of(bunk, cafe, trending),
                List.of(hidden, romantic),
                List.of(cafe, bunk, hidden, trending, romantic, party),
                List.of(trending, romantic, bunk),
                List.of(party));
    }

    private GenZModeData genZCustomCity(
            CityPage city,
            List<GenZPlace> bunkSpots,
            List<GenZPlace> hiddenGems,
            List<GenZPlace> surpriseAndTrending,
            List<GenZPlace> clubs) {
        return genZCustomCity(city, bunkSpots, hiddenGems, surpriseAndTrending, surpriseAndTrending, clubs);
    }

    private GenZModeData genZCustomCity(
            CityPage city,
            List<GenZPlace> bunkSpots,
            List<GenZPlace> hiddenGems,
            List<GenZPlace> surprise,
            List<GenZPlace> trending,
            List<GenZPlace> clubs) {
        List<GenZPlace> safeBunkSpots = dedupeGenZ(bunkSpots);
        List<GenZPlace> safeHiddenGems = dedupeGenZ(hiddenGems);
        List<GenZPlace> safeSurprise = dedupeGenZ(surprise);
        List<GenZPlace> safeTrending = dedupeGenZ(trending);
        List<GenZPlace> safeClubs = dedupeGenZ(clubs);

        Map<String, List<GenZPlace>> vibe = new LinkedHashMap<>();
        vibe.put("chill", takeGenZ(joinGenZ(safeBunkSpots, safeHiddenGems), 4));
        vibe.put("party", takeGenZ(joinGenZ(safeClubs, safeBunkSpots), 4));
        vibe.put("romantic", takeGenZ(joinGenZ(safeTrending, safeHiddenGems), 4));
        vibe.put("solo", takeGenZ(joinGenZ(safeHiddenGems, safeBunkSpots), 4));

        return new GenZModeData(
                safeBunkSpots,
                safeHiddenGems,
                buildGenZPairs(safeBunkSpots, safeHiddenGems, safeClubs),
                safeSurprise,
                vibe,
                safeTrending,
                safeClubs);
    }

    private GenZPlace genZCustomPlace(CityPage city, String name, String description, String type, String imageUrl) {
        return genZPlaceWithCityMap(name, description, type, genZImageFor(city, name, type, imageUrl), city.name());
    }

    private String genZImageFor(CityPage city, String name, String type, String imageUrl) {
        if (isUsableGenZImageUrl(imageUrl)) {
            return imageUrl.trim();
        }
        return genZPlaceImageFromCityData(city, name, type).orElse(imageUrl);
    }

    private Optional<String> genZPlaceImageFromCityData(CityPage city, String name, String type) {
        List<String> keywords = genZImageKeywords(city.slug(), name, type);
        if (keywords.isEmpty()) {
            return Optional.empty();
        }
        List<PlaceCard> places = city.categories().stream()
                .flatMap(category -> category.places().stream())
                .toList();
        for (String keyword : keywords) {
            String normalizedKeyword = normalize(keyword);
            Optional<String> image = places.stream()
                    .filter(place -> normalize(place.name()).contains(normalizedKeyword))
                    .map(PlaceCard::image)
                    .filter(this::isUsableGenZImageUrl)
                    .findFirst();
            if (image.isPresent()) {
                return image;
            }
        }
        return Optional.empty();
    }

    private List<String> genZImageKeywords(String citySlug, String name, String type) {
        String normalizedName = normalize(name);
        List<String> keywords = new ArrayList<>();

        if (normalizedName.contains("taj")) keywords.add("Taj Mahal");
        if (normalizedName.contains("fatehpur")) keywords.add("Fatehpur Sikri");
        if (normalizedName.contains("mehtab")) keywords.add("Mehtab Bagh");
        if (normalizedName.contains("sarnath")) keywords.add("Sarnath");
        if (normalizedName.contains("assi") || normalizedName.contains("ghat") || normalizedName.contains("ganga") || normalizedName.contains("kashi")) keywords.add("Dashashwamedh Ghat");
        if (normalizedName.contains("indiagate")) keywords.add("India Gate");
        if (normalizedName.contains("qutub")) keywords.add("Qutub Minar");
        if (normalizedName.contains("lodhi")) keywords.add("Lodhi Garden");
        if (normalizedName.contains("hauz")) keywords.add("Hauz Khas");
        if (normalizedName.contains("sukhna")) keywords.add("Sukhna Lake");
        if (normalizedName.contains("rock")) keywords.add("Rock Garden");
        if (normalizedName.contains("rose")) keywords.add("Rose Garden");
        if (normalizedName.contains("elante")) keywords.add("Elante Mall");
        if (normalizedName.contains("pinjore")) keywords.add("Pinjore Garden");
        if (normalizedName.contains("amber") || normalizedName.contains("amer")) keywords.add("Amber Fort");
        if (normalizedName.contains("nahargarh")) keywords.add("Nahargarh Fort");
        if (normalizedName.contains("jaigarh")) keywords.add("Jaigarh Fort");
        if (normalizedName.contains("jalmahal")) keywords.add("Jal Mahal");
        if (normalizedName.contains("panna")) keywords.add("Panna Meena Ka Kund");
        if (normalizedName.contains("patrika")) keywords.add("Patrika Gate");
        if (normalizedName.contains("sambhar")) keywords.add("Sambhar Lake");
        if (normalizedName.contains("chokhi")) keywords.add("Chokhi Dhani");
        if (normalizedName.contains("citypalace")) keywords.add("City Palace");
        if (normalizedName.contains("lakepichola") || normalizedName.contains("pichola")) keywords.add("Lake Pichola");
        if (normalizedName.contains("fatehsagar")) keywords.add("Fateh Sagar Lake");
        if (normalizedName.contains("jaisalmerfort")) keywords.add("Jaisalmer Fort");
        if (normalizedName.contains("sam") || normalizedName.contains("desert") || normalizedName.contains("dune")) keywords.add("Sam Sand Dunes");
        if (normalizedName.contains("baga")) keywords.add("Baga Beach");
        if (normalizedName.contains("anjuna")) keywords.add("Anjuna Beach");
        if (normalizedName.contains("fortaguada")) keywords.add("Fort Aguada");
        if (normalizedName.contains("marinedrive")) keywords.add("Marine Drive");
        if (normalizedName.contains("gateway")) keywords.add("Gateway of India");
        if (normalizedName.contains("elephanta")) keywords.add("Elephanta Caves");
        if (normalizedName.contains("fortkochi")) keywords.add("Fort Kochi");
        if (normalizedName.contains("chinese")) keywords.add("Chinese Fishing Nets");
        if (normalizedName.contains("tea") || normalizedName.contains("tigerhill")) keywords.add("Tiger Hill");
        if (normalizedName.contains("batasia")) keywords.add("Batasia Loop");
        if (normalizedName.contains("pangong")) keywords.add("Pangong Lake");
        if (normalizedName.contains("nubra")) keywords.add("Nubra Valley");
        if (normalizedName.contains("dal")) keywords.add("Dal Lake");
        if (normalizedName.contains("mughal")) keywords.add("Mughal Garden");
        if (normalizedName.contains("solang")) keywords.add("Solang Valley");
        if (normalizedName.contains("hidimba")) keywords.add("Hadimba Temple");
        if (normalizedName.contains("mallroad")) keywords.add("Mall Road");
        if (normalizedName.contains("ridge")) keywords.add("The Ridge");
        if (normalizedName.contains("rafting") || normalizedName.contains("ganga")) keywords.add("Ganga River");
        if (normalizedName.contains("laxman") || normalizedName.contains("lakshman")) keywords.add("Lakshman Jhula");
        if (normalizedName.contains("khajjiar")) keywords.add("Khajjiar");
        if (normalizedName.contains("chamunda")) keywords.add("Chamunda Devi Temple");

        if ("cafe".equals(type) || normalizedName.contains("cafe") || normalizedName.contains("coffee") || normalizedName.contains("chai") || normalizedName.contains("brunch")) {
            keywords.addAll(categoryPlaceNames(citySlug, "cafes"));
        }
        if (normalizedName.contains("food") || normalizedName.contains("snack") || normalizedName.contains("dessert") || normalizedName.contains("dinner")) {
            keywords.addAll(categoryPlaceNames(citySlug, "cafes"));
        }
        if (normalizedName.contains("hotel") || normalizedName.contains("stay") || normalizedName.contains("palace")) {
            keywords.addAll(categoryPlaceNames(citySlug, "hotels"));
        }

        keywords.addAll(categoryPlaceNames(citySlug, "popular-places"));
        keywords.addAll(categoryPlaceNames(citySlug, "tourist-places"));
        keywords.addAll(categoryPlaceNames(citySlug, "educational-places"));
        return keywords.stream().distinct().toList();
    }

    private List<String> categoryPlaceNames(String citySlug, String categorySlug) {
        return cities.getOrDefault(citySlug, new CityPage("", "", "", "", "", List.of(), List.of(), List.of()))
                .categories().stream()
                .filter(category -> categorySlug.equals(category.slug()))
                .flatMap(category -> category.places().stream())
                .map(PlaceCard::name)
                .toList();
    }

    private List<GenZPlace> genZPlacesFromCategory(CityPage city, String categorySlug, String type) {
        return city.categories().stream()
                .filter(category -> categorySlug.equals(category.slug()))
                .flatMap(category -> category.places().stream())
                .map(place -> genZPlaceWithCityMap(place.name(), genZDescription(place.name(), city.name(), type), type, place.image(), city.name()))
                .toList();
    }

    @SafeVarargs
    private List<GenZPlace> joinGenZ(List<GenZPlace>... groups) {
        List<GenZPlace> joined = new ArrayList<>();
        for (List<GenZPlace> group : groups) {
            joined.addAll(group);
        }
        return dedupeGenZ(joined);
    }

    private List<GenZPlace> takeGenZ(List<GenZPlace> places, int limit) {
        return dedupeGenZ(places).stream().limit(limit).toList();
    }

    private List<GenZPlace> dedupeGenZ(List<GenZPlace> places) {
        Map<String, GenZPlace> unique = new LinkedHashMap<>();
        for (GenZPlace place : places) {
            unique.putIfAbsent(normalize(place.name()), place);
        }
        return new ArrayList<>(unique.values());
    }

    private List<GenZPlace> ensureGenZPlaces(List<GenZPlace> places, CityPage city, String type) {
        if (!places.isEmpty()) {
            return places;
        }
        String image = "cafe".equals(type) ? genZCategoryImage("cafes") : city.heroImage();
        return List.of(
                genZPlaceWithCityMap(city.name() + " chill stop", "Easy " + city.name() + " plan for friends, photos, and quick breaks.", type, image, city.name()),
                genZPlaceWithCityMap(city.name() + " local hangout", "A flexible pick for low-effort exploring in " + city.name() + ".", type, image, city.name()),
                genZPlaceWithCityMap(city.name() + " evening plan", "Good for a simple city route when everyone wants something easy.", type, image, city.name()));
    }

    private List<GenZPlace> cityActivityIdeas(CityPage city) {
        return List.of(
                genZPlaceWithCityMap(city.name() + " food crawl", "Try a few local snacks, cafes, or market bites in one casual route.", "activity", genZCategoryImage("cafes"), city.name()),
                genZPlaceWithCityMap(city.name() + " photo walk", "A camera-roll-ready walk through the most visual corners of the city.", "activity", city.heroImage(), city.name()),
                genZPlaceWithCityMap(city.name() + " sunset plan", "Pick a viewpoint, lake, fort, garden, or open public place for golden-hour scenes.", "activity", genZCategoryImage("popular-places"), city.name()));
    }

    private List<GenZPlace> genZNightlife(CityPage city) {
        return List.of(
                genZPlaceWithCityMap(city.name() + " late food street", "Crowd-friendly food, snacks, and after-dark city energy.", "club", genZCategoryImage("nightlife"), city.name()),
                genZPlaceWithCityMap(city.name() + " cafe-bar plan", "Dinner first, music later, and a simple group-night mood.", "club", genZCategoryImage("nightlife"), city.name()),
                genZPlaceWithCityMap(city.name() + " rooftop or lounge night", "City lights, group photos, and relaxed nightlife energy.", "club", genZCategoryImage("nightlife"), city.name()),
                genZPlaceWithCityMap(city.name() + " live music corner", "Good for plans that need sound, crowd, and movement.", "club", genZCategoryImage("nightlife"), city.name()));
    }

    private List<GenZPair> buildGenZPairs(List<GenZPlace> cafes, List<GenZPlace> places, List<GenZPlace> clubs) {
        List<GenZPair> pairs = new ArrayList<>();
        addGenZPair(pairs, cafes, 0, 1);
        addGenZPair(pairs, places, 0, 1);
        addGenZPair(pairs, clubs, 0, 1);
        if (!cafes.isEmpty() && !places.isEmpty()) {
            pairs.add(new GenZPair(cafes.get(0), places.get(0)));
        }
        return pairs;
    }

    private void addGenZPair(List<GenZPair> pairs, List<GenZPlace> places, int left, int right) {
        if (places.size() > right) {
            pairs.add(new GenZPair(places.get(left), places.get(right)));
        }
    }

    private GenZPlace genZPlaceWithCityMap(String name, String desc, String type, String image, String cityName) {
        return genZPlace(name, desc, type, image, mapsSearchUrl(name, cityName));
    }

    private GenZPlace genZPlace(String name, String desc, String type, String image, String googleMapsUrl) {
        return new GenZPlace(name, desc, type, genZSafeImage(name, type, image), googleMapsUrl);
    }

    private String genZSafeImage(String name, String type, String image) {
        if (isUsableGenZImageUrl(image)) {
            return image.trim();
        }
        String normalizedName = normalize(name);
        if (normalizedName.contains("gaming") || normalizedName.contains("neon")) {
            return "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&q=85&auto=format&fit=crop";
        }
        if (normalizedName.contains("rooftop") || normalizedName.contains("dj") || normalizedName.contains("party") || "club".equals(type)) {
            return "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=1200&q=85&auto=format&fit=crop";
        }
        if (normalizedName.contains("river") || normalizedName.contains("beach") || normalizedName.contains("lake")) {
            return "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=85&auto=format&fit=crop";
        }
        if (normalizedName.contains("mountain") || normalizedName.contains("hill") || normalizedName.contains("bonfire")) {
            return "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=85&auto=format&fit=crop";
        }
        if ("cafe".equals(type)) {
            return genZCategoryImage("cafes");
        }
        if ("activity".equals(type)) {
            return "https://images.unsplash.com/photo-1527631746610-bca00a040d60?w=1200&q=85&auto=format&fit=crop";
        }
        return genZCategoryImage("popular-places");
    }

    private boolean isUsableGenZImageUrl(String image) {
        if (image == null) {
            return false;
        }
        String url = image.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return false;
        }
        if (url.contains("source.unsplash.com") || url.contains("example.com")) {
            return false;
        }
        return !url.contains("AF1Qip") || !url.contains("8Q-wS1m0Q8Z7");
    }

    private String mapsSearchUrl(String placeName, String cityName) {
        String query = URLEncoder.encode(destinationQueryForMaps(placeName, cityName), StandardCharsets.UTF_8);
        return "https://www.google.com/maps/dir/?api=1&origin=Current+Location&destination=" + query + "&travelmode=driving";
    }

    private String destinationQueryForMaps(String placeName, String cityName) {
        String cleanedName = Optional.ofNullable(placeName)
                .orElse("")
                .replaceAll("(?i)\\b(quick|mini|one-day|weekday|weekend|perfect|college-bunk|bunk-day)\\b", " ")
                .replaceAll("(?i)\\b(escape|roadtrip|trip|plan|ride|route|stop)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
        String destination = cleanedName.isBlank() ? cityName : cleanedName + " " + cityName;
        return destination.trim();
    }

    private String genZDescription(String placeName, String cityName, String type) {
        return switch (type) {
            case "cafe" -> placeName + " works as a " + cityName + " hangout for coffee, chats, study breaks, and casual plans.";
            case "club" -> placeName + " fits the louder side of " + cityName + " with group-night, music, and late-food energy.";
            case "activity" -> placeName + " is a quick Gen Z-friendly activity idea for a flexible " + cityName + " plan.";
            default -> placeName + " is a Gen Z-friendly " + cityName + " stop for photos, walks, short plans, and easy exploring.";
        };
    }

    private String genZCategoryImage(String categorySlug) {
        return switch (categorySlug) {
            case "cafes" -> "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=85&auto=format&fit=crop";
            case "restaurants" -> "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=1200&q=85&auto=format&fit=crop";
            case "popular-places" -> "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=1200&q=85&auto=format&fit=crop";
            case "tourist-places" -> "https://images.unsplash.com/photo-1527631746610-bca00a040d60?w=1200&q=85&auto=format&fit=crop";
            case "hidden-gems" -> "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=85&auto=format&fit=crop";
            case "nightlife" -> "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=1200&q=85&auto=format&fit=crop";
            default -> "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200&q=85&auto=format&fit=crop";
        };
    }

    private List<PlaceCard> emergencyServicesForCity(String citySlug, String cityName ) {
        return switch (citySlug) {
            case "jaipur" -> List.of(
                    emergencyPlace("Sawai Man Singh Hospital", "Major government hospital for emergency and trauma care in Jaipur.", "hospital", "https://doctorlistingingestionpr.blob.core.windows.net/doctorprofilepic/1670849641636_HospitalProfileImage_5f5081b2-0554-47fc-839b-00e1f9b120cd.png"),
                    emergencyPlace("Jaipur 108 Ambulance Services", "Emergency ambulance support for urgent medical transport across Jaipur.", "ambulance", "https://www.jaipurstuff.com/wp-content/uploads/2019/10/a1_5228031_835x547-m.jpg"),
                    emergencyPlace("Jaipur Police Control Room", "Police assistance and city safety coordination for urgent incidents.", "police", "https://content.jdmagicbox.com/comp/jaipur/x2/0141px141.x141.140607132643.b8x2/catalogue/police-station-amer-jaipur-police-stations-LOSpkV9QtM.jpg"),
                    emergencyPlace("Jaipur Fire Station", "Fire and rescue support for accidents, fire calls, and emergency response.", "fire", "https://static.toiimg.com/thumb/msid-67498735,width-1280,height-720,imgsize-544933,resizemode-72,overlay-toi_sw,pt-32,y_pad-40/photo.jpg"),
                    emergencyPlace("Zanana Hospital Emergency", "Women and child emergency medical support in Jaipur.", "clinic", "https://content.jdmagicbox.com/comp/jaipur/j6/0141px141.x141.180821000203.q2j6/catalogue/zanana-hospital-jaipur-hospitals-tkvvs8fs6m.jpg"),
                    emergencyPlace("Jaipur Blood Bank Network", "Blood bank support for hospitals and urgent transfusion needs.", "blood-bank", "https://content.jdmagicbox.com/comp/jaipur/l6/0141px141.x141.230711172046.x1l6/catalogue/monarch-blood-bank-jagatpura-getor-jaipur-private-hospitals-9nfkaerdt8.jpg"),
                    emergencyPlace("Women Helpline Jaipur", "Emergency support and guidance for women in distress.", "women-helpline", "https://content.jdmagicbox.com/comp/delhi/y8/011pxx11.xx11.121116100238.s7y8/catalogue/women-helpline-number-new-delhi-delhi-helplines-for-women-4exll2h.jpg"),
                    emergencyPlace("Jaipur Disaster Control Room", "Disaster response and civic emergency coordination.", "disaster", " https://www.khaskhabar.com/s3-storage/khaskhabar/khaskhabarimages/img500/jila-collectorate-jaipur-30-1531496274-326701-khaskhabar.jpg"),
                    emergencyPlace("24x7 Pharmacy Jaipur", "Round-the-clock medicine access near major hospitals.", "pharmacy", "https://www.mypunepulse.com/wp-content/uploads/2024/04/WhatsApp-Image-2024-04-30-at-1.12.09-PM-1024x683.jpg"),
                    emergencyPlace("Jaipur Emergency Contacts", "Quick reference for police, fire, ambulance, and civic help.", "contacts", "https://www.mgmch.org/images/24hrs_emergency.webp"));
            case "agra" -> List.of(
                    emergencyPlace("SN Medical College Emergency", "Key emergency hospital support for Agra residents and visitors.", "hospital", "https://example.com/hospital.jpg"),
                    emergencyPlace("Agra 108 Ambulance Services", "Urgent medical transport and ambulance response across Agra.", "ambulance", "https://example.com/ambulance.jpg"),
                    emergencyPlace("Agra Police Control Room", "Police help for safety, lost items, and emergency incidents.", "police", "https://example.com/police.jpg"),
                    emergencyPlace("Agra Fire Station", "Fire and rescue services for city emergencies.", "fire", "https://example.com/fire.jpg"),
                    emergencyPlace("District Hospital Agra Emergency", "Emergency clinic and public hospital support in Agra.", "clinic", "https://example.com/clinic.jpg"),
                    emergencyPlace("Agra Blood Bank", "Blood availability support for hospitals and urgent cases.", "blood-bank", "https://example.com/blood-bank.jpg"),
                    emergencyPlace("Women Helpline Agra", "Immediate assistance for women needing safety support.", "women-helpline", "https://example.com/women-helpline.jpg"),
                    emergencyPlace("Agra Disaster Help Center", "Coordination point for disaster and civic emergencies.", "disaster", "https://example.com/disaster.jpg"),
                    emergencyPlace("24x7 Pharmacy Agra", "Night and emergency pharmacy options around medical hubs.", "pharmacy", "https://example.com/pharmacy.jpg"),
                    emergencyPlace("Agra Emergency Contacts", "Police, fire, ambulance, hospital, and tourist emergency references.", "contacts", "https://example.com/contacts.jpg"));
            case "varanasi" -> List.of(
                    emergencyPlace("BHU Trauma Centre", "Major trauma and emergency care facility serving Varanasi.", "hospital", "https://example.com/hospital.jpg"),
                    emergencyPlace("Varanasi Ambulance Services", "Emergency ambulance access for city and ghat-side medical needs.", "ambulance", "https://example.com/ambulance.jpg"),
                    emergencyPlace("Varanasi Police Control Room", "Police support for public safety and urgent incidents.", "police", "https://example.com/police.jpg"),
                    emergencyPlace("Varanasi Fire Service", "Fire and rescue response across dense city areas and ghats.", "fire", "https://example.com/fire.jpg"),
                    emergencyPlace("Kabir Chaura Hospital Emergency", "Public emergency clinic and hospital support in Varanasi.", "clinic", "https://example.com/clinic.jpg"),
                    emergencyPlace("Varanasi Blood Bank", "Blood bank support for emergency transfusion needs.", "blood-bank", "https://example.com/blood-bank.jpg"),
                    emergencyPlace("Women Helpline Varanasi", "Safety assistance and reporting support for women.", "women-helpline", "https://example.com/women-helpline.jpg"),
                    emergencyPlace("Varanasi Disaster Help Center", "Flood, crowd, and civic emergency coordination.", "disaster", "https://example.com/disaster.jpg"),
                    emergencyPlace("24x7 Pharmacy Varanasi", "Round-the-clock pharmacy access near hospitals and major roads.", "pharmacy", "https://example.com/pharmacy.jpg"),
                    emergencyPlace("Varanasi Emergency Contacts", "Quick help references for medical, police, fire, and civic support.", "contacts", "https://example.com/contacts.jpg"));
            case "delhi" -> List.of(
                    emergencyPlace("AIIMS Delhi Emergency", "Major emergency and trauma care center for Delhi NCR.", "hospital", "https://example.com/hospital.jpg"),
                    emergencyPlace("Delhi CATS Ambulance", "City ambulance network for urgent medical response.", "ambulance", "https://example.com/ambulance.jpg"),
                    emergencyPlace("Delhi Police Control Room", "Police emergency help and public safety coordination.", "police", "https://example.com/police.jpg"),
                    emergencyPlace("Delhi Fire Service", "Fire, rescue, and accident response across Delhi.", "fire", "https://example.com/fire.jpg"),
                    emergencyPlace("Safdarjung Hospital Emergency", "Large public emergency hospital and trauma support.", "clinic", "https://example.com/clinic.jpg"),
                    emergencyPlace("Delhi Blood Bank Network", "Blood bank services across public and private hospitals.", "blood-bank", "https://example.com/blood-bank.jpg"),
                    emergencyPlace("Delhi Women Helpline", "Emergency assistance and safety guidance for women.", "women-helpline", "https://example.com/women-helpline.jpg"),
                    emergencyPlace("Delhi Disaster Management Authority", "Disaster response and city emergency coordination.", "disaster", "https://example.com/disaster.jpg"),
                    emergencyPlace("24x7 Pharmacy Delhi", "All-night medicine access near major hospital districts.", "pharmacy", "https://example.com/pharmacy.jpg"),
                    emergencyPlace("Delhi Emergency Contacts", "Police, fire, ambulance, disaster, and hospital quick contacts.", "contacts", "https://example.com/contacts.jpg"));
            case "chandigarh" -> List.of(
                    emergencyPlace("PGIMER Emergency Chandigarh", "Major emergency and trauma care hospital for the tricity.", "hospital", "https://example.com/hospital.jpg"),
                    emergencyPlace("Chandigarh Ambulance Services", "Urgent medical transport across Chandigarh and nearby sectors.", "ambulance", "https://example.com/ambulance.jpg"),
                    emergencyPlace("Chandigarh Police Control Room", "Police help for safety, reporting, and urgent incidents.", "police", "https://example.com/police.jpg"),
                    emergencyPlace("Chandigarh Fire Station", "Fire and rescue response across city sectors.", "fire", "https://example.com/fire.jpg"),
                    emergencyPlace("GMSH Sector 16 Emergency", "Public hospital emergency services in central Chandigarh.", "clinic", "https://example.com/clinic.jpg"),
                    emergencyPlace("Chandigarh Blood Bank", "Blood support linked to major hospitals and urgent cases.", "blood-bank", "https://example.com/blood-bank.jpg"),
                    emergencyPlace("Women Helpline Chandigarh", "Safety and assistance support for women in distress.", "women-helpline", "https://example.com/women-helpline.jpg"),
                    emergencyPlace("Chandigarh Disaster Help Center", "Civic emergency and disaster response coordination.", "disaster", "https://example.com/disaster.jpg"),
                    emergencyPlace("24x7 Pharmacy Chandigarh", "Round-the-clock pharmacies near PGI, Sector 16, and main markets.", "pharmacy", "https://example.com/pharmacy.jpg"),
                    emergencyPlace("Chandigarh Emergency Contacts", "Quick contacts for ambulance, police, fire, hospitals, and civic help.", "contacts", "https://example.com/contacts.jpg"));
            case "udaipur" -> List.of(
                    emergencyPlace("Maharana Bhupal Hospital Emergency", "Primary public emergency hospital support in Udaipur.", "hospital", "https://example.com/hospital.jpg"),
                    emergencyPlace("Udaipur Ambulance Services", "Emergency medical transport for city and lake-area incidents.", "ambulance", "https://example.com/ambulance.jpg"),
                    emergencyPlace("Udaipur Police Control Room", "Police assistance for safety and urgent reporting.", "police", "https://example.com/police.jpg"),
                    emergencyPlace("Udaipur Fire Station", "Fire and rescue response for city emergencies.", "fire", "https://example.com/fire.jpg"),
                    emergencyPlace("Udaipur Emergency Clinic", "Urgent care support around central Udaipur.", "clinic", "https://example.com/clinic.jpg"),
                    emergencyPlace("Udaipur Blood Bank", "Blood support for hospitals and urgent medical needs.", "blood-bank", "https://example.com/blood-bank.jpg"),
                    emergencyPlace("Women Helpline Udaipur", "Women safety support and emergency guidance.", "women-helpline", "https://example.com/women-helpline.jpg"),
                    emergencyPlace("Udaipur Disaster Help Center", "Coordination for civic and disaster emergencies.", "disaster", "https://example.com/disaster.jpg"),
                    emergencyPlace("24x7 Pharmacy Udaipur", "Emergency medicine access around hospitals and tourist zones.", "pharmacy", "https://example.com/pharmacy.jpg"),
                    emergencyPlace("Udaipur Emergency Contacts", "Quick references for police, ambulance, fire, and medical help.", "contacts", "https://example.com/contacts.jpg"));
            case "jaisalmer" -> List.of(
                    emergencyPlace("Jawahar Hospital Jaisalmer", "District hospital emergency support for Jaisalmer.", "hospital", "https://example.com/hospital.jpg"),
                    emergencyPlace("Jaisalmer Ambulance Services", "Emergency ambulance help for city and desert-route needs.", "ambulance", "https://example.com/ambulance.jpg"),
                    emergencyPlace("Jaisalmer Police Control Room", "Police support for safety and urgent incidents.", "police", "https://example.com/police.jpg"),
                    emergencyPlace("Jaisalmer Fire Station", "Fire and rescue help for city and hospitality areas.", "fire", "https://example.com/fire.jpg"),
                    emergencyPlace("Jaisalmer Emergency Clinic", "Urgent care clinic support near city medical hubs.", "clinic", "https://example.com/clinic.jpg"),
                    emergencyPlace("Jaisalmer Blood Bank", "Blood support for district hospital and urgent cases.", "blood-bank", "https://example.com/blood-bank.jpg"),
                    emergencyPlace("Women Helpline Jaisalmer", "Emergency assistance for women in distress.", "women-helpline", "https://example.com/women-helpline.jpg"),
                    emergencyPlace("Jaisalmer Disaster Help Center", "Heat, desert-route, and civic emergency coordination.", "disaster", "https://example.com/disaster.jpg"),
                    emergencyPlace("24x7 Pharmacy Jaisalmer", "Emergency medicine access near hospital and fort area.", "pharmacy", "https://example.com/pharmacy.jpg"),
                    emergencyPlace("Jaisalmer Emergency Contacts", "Quick help for medical, police, fire, and desert safety needs.", "contacts", "https://example.com/contacts.jpg"));
            case "goa" -> List.of(
                    emergencyPlace("Goa Medical College Emergency", "Major emergency hospital serving North and South Goa.", "hospital", "https://example.com/hospital.jpg"),
                    emergencyPlace("Goa 108 Ambulance Services", "Ambulance support for beaches, towns, and highways.", "ambulance", "https://example.com/ambulance.jpg"),
                    emergencyPlace("Goa Police Control Room", "Police and tourist safety support across Goa.", "police", "https://example.com/police.jpg"),
                    emergencyPlace("Goa Fire and Emergency Services", "Fire, rescue, and beach-area emergency response.", "fire", "https://example.com/fire.jpg"),
                    emergencyPlace("Goa Emergency Clinic Network", "Urgent care clinics around major tourist belts.", "clinic", "https://example.com/clinic.jpg"),
                    emergencyPlace("Goa Blood Bank", "Blood bank support for hospitals and emergency cases.", "blood-bank", "https://example.com/blood-bank.jpg"),
                    emergencyPlace("Women Helpline Goa", "Immediate safety assistance and reporting support for women.", "women-helpline", "https://example.com/women-helpline.jpg"),
                    emergencyPlace("Goa Disaster Management Authority", "Cyclone, flood, and civic emergency coordination.", "disaster", "https://example.com/disaster.jpg"),
                    emergencyPlace("24x7 Pharmacy Goa", "Night pharmacy access around Panaji, Margao, and beach areas.", "pharmacy", "https://example.com/pharmacy.jpg"),
                    emergencyPlace("Goa Emergency Contacts", "Quick references for police, ambulance, fire, coast, and hospitals.", "contacts", "https://example.com/contacts.jpg"));
            case "mumbai", "maharashtra" -> List.of(
                    emergencyPlace("KEM Hospital Emergency", "Major public emergency and trauma care hospital in Mumbai.", "hospital", "https://example.com/hospital.jpg"),
                    emergencyPlace("Mumbai 108 Ambulance Services", "Emergency ambulance response across Mumbai and Maharashtra routes.", "ambulance", "https://example.com/ambulance.jpg"),
                    emergencyPlace("Mumbai Police Control Room", "Police assistance for safety, reporting, and urgent incidents.", "police", "https://example.com/police.jpg"),
                    emergencyPlace("Mumbai Fire Brigade", "Fire, rescue, and high-rise emergency response.", "fire", "https://example.com/fire.jpg"),
                    emergencyPlace("JJ Hospital Emergency", "Large public emergency hospital support in South Mumbai.", "clinic", "https://example.com/clinic.jpg"),
                    emergencyPlace("Mumbai Blood Bank Network", "Blood bank access across major hospitals and donation centers.", "blood-bank", "https://example.com/blood-bank.jpg"),
                    emergencyPlace("Women Helpline Mumbai", "Women safety and urgent assistance support.", "women-helpline", "https://example.com/women-helpline.jpg"),
                    emergencyPlace("Maharashtra Disaster Management Cell", "Monsoon, flood, and disaster response coordination.", "disaster", "https://example.com/disaster.jpg"),
                    emergencyPlace("24x7 Pharmacy Mumbai", "Round-the-clock pharmacy access near major hospitals.", "pharmacy", "https://example.com/pharmacy.jpg"),
                    emergencyPlace("Mumbai Emergency Contacts", "Police, fire, ambulance, disaster, and hospital quick references.", "contacts", "https://example.com/contacts.jpg"));
            case "kochi" -> List.of(
                    emergencyPlace("Ernakulam General Hospital Emergency", "Public emergency care for Kochi and Ernakulam.", "hospital", "https://example.com/hospital.jpg"),
                    emergencyPlace("Kochi Ambulance Services", "Urgent ambulance help across city, island, and highway zones.", "ambulance", "https://example.com/ambulance.jpg"),
                    emergencyPlace("Kochi City Police Control Room", "Police assistance for urgent incidents and public safety.", "police", "https://example.com/police.jpg"),
                    emergencyPlace("Kochi Fire and Rescue Station", "Fire and rescue response across Kochi.", "fire", "https://example.com/fire.jpg"),
                    emergencyPlace("Kochi Emergency Clinic", "Urgent care clinic support near central Kochi.", "clinic", "https://example.com/clinic.jpg"),
                    emergencyPlace("Kochi Blood Bank", "Blood support linked to hospitals and emergency needs.", "blood-bank", "https://example.com/blood-bank.jpg"),
                    emergencyPlace("Women Helpline Kochi", "Safety assistance for women in distress.", "women-helpline", "https://example.com/women-helpline.jpg"),
                    emergencyPlace("Kerala Disaster Help Center Kochi", "Flood, monsoon, and civic emergency coordination.", "disaster", "https://example.com/disaster.jpg"),
                    emergencyPlace("24x7 Pharmacy Kochi", "Emergency medicine access near hospitals and transit hubs.", "pharmacy", "https://example.com/pharmacy.jpg"),
                    emergencyPlace("Kochi Emergency Contacts", "Quick contacts for medical, police, fire, and disaster support.", "contacts", "https://example.com/contacts.jpg"));
            case "darjeeling" -> List.of(
                    emergencyPlace("Darjeeling District Hospital", "Emergency medical care for Darjeeling town and nearby areas.", "hospital", "https://example.com/hospital.jpg"),
                    emergencyPlace("Darjeeling Ambulance Services", "Ambulance support for hill roads and town emergencies.", "ambulance", "https://example.com/ambulance.jpg"),
                    emergencyPlace("Darjeeling Police Station", "Police support for public safety and urgent reporting.", "police", "https://example.com/police.jpg"),
                    emergencyPlace("Darjeeling Fire Station", "Fire and rescue response for hill-town emergencies.", "fire", "https://example.com/fire.jpg"),
                    emergencyPlace("Darjeeling Emergency Clinic", "Urgent care support around town medical points.", "clinic", "https://example.com/clinic.jpg"),
                    emergencyPlace("Darjeeling Blood Bank", "Blood support for district hospital and emergency cases.", "blood-bank", "https://example.com/blood-bank.jpg"),
                    emergencyPlace("Women Helpline Darjeeling", "Emergency assistance and guidance for women.", "women-helpline", "https://example.com/women-helpline.jpg"),
                    emergencyPlace("Darjeeling Disaster Help Center", "Landslide, weather, and civic emergency coordination.", "disaster", "https://example.com/disaster.jpg"),
                    emergencyPlace("24x7 Pharmacy Darjeeling", "Emergency pharmacy access near hospital and town center.", "pharmacy", "https://example.com/pharmacy.jpg"),
                    emergencyPlace("Darjeeling Emergency Contacts", "Quick help for police, medical, fire, and hill-road emergencies.", "contacts", "https://example.com/contacts.jpg"));
            case "leh" -> List.of(
                    emergencyPlace("SNM Hospital Leh Emergency", "Primary emergency hospital for Leh and high-altitude health needs.", "hospital", "https://example.com/hospital.jpg"),
                    emergencyPlace("Leh Ambulance Services", "Ambulance help for altitude sickness, road, and travel emergencies.", "ambulance", "https://example.com/ambulance.jpg"),
                    emergencyPlace("Leh Police Station", "Police support for safety and urgent incidents.", "police", "https://example.com/police.jpg"),
                    emergencyPlace("Leh Fire and Emergency Services", "Fire and rescue response in Leh town.", "fire", "https://example.com/fire.jpg"),
                    emergencyPlace("Leh Emergency Clinic", "Urgent clinic support for altitude and travel-related issues.", "clinic", "https://example.com/clinic.jpg"),
                    emergencyPlace("Leh Blood Bank", "Blood support for hospital emergency cases.", "blood-bank", "https://example.com/blood-bank.jpg"),
                    emergencyPlace("Women Helpline Leh", "Emergency assistance for women in distress.", "women-helpline", "https://example.com/women-helpline.jpg"),
                    emergencyPlace("Ladakh Disaster Help Center", "Mountain-road, weather, and disaster coordination.", "disaster", "https://example.com/disaster.jpg"),
                    emergencyPlace("24x7 Pharmacy Leh", "Emergency medicine access around Leh market and hospital areas.", "pharmacy", ""),
                    emergencyPlace("Leh Emergency Contacts", "Quick contacts for medical, police, fire, weather, and road help.", "contacts", ""));
            case "srinagar" -> List.of(
                    emergencyPlace("SMHS Hospital Emergency", "Major emergency care hospital in Srinagar.", "hospital", ""),
                    emergencyPlace("Srinagar Ambulance Services", "Urgent medical transport around city and valley routes.", "ambulance", ""),
                    emergencyPlace("Srinagar Police Control Room", "Police help for safety and urgent reporting.", "police", ""),
                    emergencyPlace("Srinagar Fire and Emergency Services", "Fire and rescue response for city emergencies.", "fire", ""),
                    emergencyPlace("SKIMS Emergency", "Specialty emergency and hospital care support in Srinagar.", "clinic", ""),
                    emergencyPlace("Srinagar Blood Bank", "Blood bank support for hospitals and urgent needs.", "blood-bank", ""),
                    emergencyPlace("Women Helpline Srinagar", "Safety support and urgent assistance for women.", "women-helpline", ""),
                    emergencyPlace("Srinagar Disaster Help Center", "Weather, flood, and civic emergency coordination.", "disaster", ""),
                    emergencyPlace("24x7 Pharmacy Srinagar", "Emergency medicine access near hospitals and central areas.", "pharmacy", ""),
                    emergencyPlace("Srinagar Emergency Contacts", "Quick references for police, fire, ambulance, and hospitals.", "contacts", ""));
            case "manali" -> List.of(
                    emergencyPlace("Civil Hospital Manali", "Emergency medical care for Manali town and travelers.", "hospital", ""),
                    emergencyPlace("Manali Ambulance Services", "Ambulance support for mountain-road and town emergencies.", "ambulance", ""),
                    emergencyPlace("Manali Police Station", "Police assistance for public safety and urgent incidents.", "police", ""),
                    emergencyPlace("Manali Fire Station", "Fire and rescue help for hotels, markets, and road incidents.", "fire", ""),
                    emergencyPlace("Manali Emergency Clinic", "Urgent clinic support for travelers and residents.", "clinic", ""),
                    emergencyPlace("Manali Blood Bank Support", "Blood assistance through nearby district medical network.", "blood-bank", ""),
                    emergencyPlace("Women Helpline Manali", "Safety assistance and reporting support for women.", "women-helpline", ""),
                    emergencyPlace("Kullu-Manali Disaster Help Center", "Landslide, weather, and road emergency coordination.", "disaster", ""),
                    emergencyPlace("24x7 Pharmacy Manali", "Emergency pharmacy access near Mall Road and hospital area.", "pharmacy", ""),
                    emergencyPlace("Manali Emergency Contacts", "Quick contacts for medical, police, fire, and mountain-route help.", "contacts", ""));
            case "shimla" -> List.of(
                    emergencyPlace("IGMC Shimla Emergency", "Major emergency hospital and trauma care support in Shimla.", "hospital", ""),
                    emergencyPlace("Shimla Ambulance Services", "Urgent ambulance access across hill roads and city areas.", "ambulance", ""),
                    emergencyPlace("Shimla Police Control Room", "Police support for safety and urgent reporting.", "police", ""),
                    emergencyPlace("Shimla Fire Station", "Fire and rescue response for city and hill-area emergencies.", "fire", ""),
                    emergencyPlace("Deen Dayal Upadhyay Hospital Emergency", "Public emergency clinic and hospital support in Shimla.", "clinic", ""),
                    emergencyPlace("Shimla Blood Bank", "Blood support for hospitals and urgent transfusion needs.", "blood-bank", ""),
                    emergencyPlace("Women Helpline Shimla", "Emergency assistance and safety support for women.", "women-helpline", ""),
                    emergencyPlace("Himachal Disaster Help Center", "Snow, landslide, road, and civic emergency coordination.", "disaster", ""),
                    emergencyPlace("24x7 Pharmacy Shimla", "Emergency medicine access around hospital and Mall Road areas.", "pharmacy", ""),
                    emergencyPlace("Shimla Emergency Contacts", "Quick references for police, fire, ambulance, hospital, and road help.", "contacts", ""));
            case "rishikesh" -> List.of(
                    emergencyPlace("AIIMS Rishikesh Emergency", "Major emergency and trauma care hospital in Rishikesh.", "hospital", ""),
                    emergencyPlace("Rishikesh Ambulance Services", "Urgent medical transport for city, highway, and rafting-area incidents.", "ambulance", ""),
                    emergencyPlace("Rishikesh Police Station", "Police support for safety and urgent incidents.", "police", ""),
                    emergencyPlace("Rishikesh Fire Station", "Fire and rescue response for city and hospitality areas.", "fire", ""),
                    emergencyPlace("Rishikesh Emergency Clinic", "Urgent clinic support near city and travel zones.", "clinic", ""),
                    emergencyPlace("Rishikesh Blood Bank", "Blood support through AIIMS and local hospital networks.", "blood-bank", ""),
                    emergencyPlace("Women Helpline Rishikesh", "Emergency assistance and safety support for women.", "women-helpline", ""),
                    emergencyPlace("Uttarakhand Disaster Help Center", "River, landslide, weather, and pilgrimage-route emergency support.", "disaster", ""),
                    emergencyPlace("24x7 Pharmacy Rishikesh", "Emergency medicine access near AIIMS and main market areas.", "pharmacy", ""),
                    emergencyPlace("Rishikesh Emergency Contacts", "Quick contacts for medical, police, fire, river, and road emergencies.", "contacts", ""));
            case "chamba" -> List.of(
                    emergencyPlace("Medical College Chamba Emergency", "Emergency hospital care for Chamba town and nearby valleys.", "hospital", ""),
                    emergencyPlace("Chamba Ambulance Services", "Ambulance support for town and hill-road emergencies.", "ambulance", ""),
                    emergencyPlace("Chamba Police Station", "Police assistance for urgent incidents and public safety.", "police", ""),
                    emergencyPlace("Chamba Fire Station", "Fire and rescue support for town emergencies.", "fire", ""),
                    emergencyPlace("Chamba Emergency Clinic", "Urgent clinic support near medical and market areas.", "clinic", ""),
                    emergencyPlace("Chamba Blood Bank", "Blood support for hospital and emergency needs.", "blood-bank", ""),
                    emergencyPlace("Women Helpline Chamba", "Safety assistance and emergency guidance for women.", "women-helpline", ""),
                    emergencyPlace("Chamba Disaster Help Center", "Landslide, road, and weather emergency coordination.", "disaster", ""),
                    emergencyPlace("24x7 Pharmacy Chamba", "Emergency medicine access around hospital and town center.", "pharmacy", ""),
                    emergencyPlace("Chamba Emergency Contacts", "Quick contacts for police, fire, ambulance, hospital, and road help.", "contacts", ""));
            default -> List.of(
                    emergencyPlace(cityName + " General Hospital Emergency", "Urgent hospital and emergency care support in " + cityName + ".", "hospital", ""),
                    emergencyPlace(cityName + " Ambulance Services", "Emergency ambulance access across " + cityName + ".", "ambulance", ""),
                    emergencyPlace(cityName + " Police Control Room", "Police assistance for safety and urgent incidents in " + cityName + ".", "police", ""),
                    emergencyPlace(cityName + " Fire Station", "Fire and rescue response for emergency calls in " + cityName + ".", "fire", ""),
                    emergencyPlace(cityName + " Emergency Clinic", "Urgent clinic support for residents and visitors.", "clinic", ""),
                    emergencyPlace(cityName + " Blood Bank", "Blood support for hospitals and urgent transfusion needs.", "blood-bank", ""),
                    emergencyPlace(cityName + " Women Helpline", "Emergency safety assistance for women in distress.", "women-helpline", ""),
                    emergencyPlace(cityName + " Disaster Help Center", "Disaster and civic emergency coordination.", "disaster", ""),
                    emergencyPlace(cityName + " 24x7 Pharmacy", "Round-the-clock medicine access near medical hubs.", "pharmacy", ""),
                    emergencyPlace(cityName + " Emergency Contacts", "Quick references for police, ambulance, fire, and hospital help.", "contacts", ""));
        };
    }

    private PlaceCard emergencyPlace(String name, String description, String type) {
        return emergencyPlace(name, description, type, "");
    }

    private PlaceCard emergencyPlace(String name, String description, String type, String imageUrl) {
        return place(name, description, firstNonBlank(imageUrl, emergencyImage(type)));
    }

    private String emergencyImage(String type) {
        return switch (type) {
            case "hospital" -> "https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?w=900&q=80&auto=format&fit=crop";
            case "ambulance" -> "https://images.unsplash.com/photo-1612531386530-97286d97c2d2?w=900&q=80&auto=format&fit=crop";
            case "police" -> "https://images.unsplash.com/photo-1596394723269-b2cbca4e6313?w=900&q=80&auto=format&fit=crop";
            case "fire" -> "https://images.unsplash.com/photo-1505322022379-7c3353ee6291?w=900&q=80&auto=format&fit=crop";
            case "clinic" -> "https://images.unsplash.com/photo-1587351021759-3e566b6af7cc?w=900&q=80&auto=format&fit=crop";
            case "blood-bank" -> "https://images.unsplash.com/photo-1615461066841-6116e61058f4?w=900&q=80&auto=format&fit=crop";
            case "women-helpline" -> "https://images.unsplash.com/photo-1573497491208-6b1acb260507?w=900&q=80&auto=format&fit=crop";
            case "disaster" -> "https://images.unsplash.com/photo-1469571486292-0ba58a3f068b?w=900&q=80&auto=format&fit=crop";
            case "pharmacy" -> "https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=900&q=80&auto=format&fit=crop";
            default -> "https://images.unsplash.com/photo-1584515933487-779824d29309?w=900&q=80&auto=format&fit=crop";
        };
    }

    private PlaceCard place(String name, String description, String image) {
        return new PlaceCard(normalize(name), name, description, image);
    }

    private PlacePage toPlacePage(String citySlug, CategoryPage category, PlaceCard place) {
        Optional<ManagedPlace> managedPlace = findManagedPlace(citySlug, category.slug(), place.slug());
        if (managedPlace.isPresent()) {
            return toManagedPlacePage(managedPlace.get(), citySlug, category);
        }
        String cityName = getCity(citySlug).map(CityPage::name).orElse(citySlug);
        return new PlacePage(
                place.slug(),
                place.name(),
                category.slug(),
                category.name(),
                place.image(),
                place.description(),
                placeInsight(category.slug(), place.name(), cityName, place.description()),
                placeHistory(category.slug(), place.name(), cityName),
                placeDetailItems(category.slug(), place.name(), cityName),
                nearbyMoments(category.slug(), place.name(), cityName),
                List.of());
    }

    private String placeInsight(String categorySlug, String placeName, String cityName, String description) {
        return switch (categorySlug) {
            case "cafes" -> placeName + " stands out in " + cityName + " for its atmosphere-first cafe experience. " + description + " Visitors usually come here for long catch-ups, comfort food, and a setting that feels more personal than a quick coffee stop.";
            case "hotels" -> placeName + " works as a stay experience rather than just a room booking. " + description + " For travelers in " + cityName + ", it fits well for business trips, weekend breaks, and guests who want smoother service and dependable amenities.";
            case "educational-places" -> placeName + " adds depth to the learning side of " + cityName + ". " + description + " It is the kind of place that appeals to students, curious visitors, and anyone wanting context beyond sightseeing.";
            case "tourist-places" -> placeName + " is one of the stronger visitor experiences in " + cityName + ". " + description + " It usually rewards slow exploration, photos, and spending enough time to take in the full setting.";
            default -> placeName + " captures a recognizable part of " + cityName + "'s identity. " + description + " It is a good first-choice stop for visitors who want something memorable, accessible, and tied to the city's character.";
        };
    }

    private String placeHistory(String categorySlug, String placeName, String cityName) {
        return switch (categorySlug) {
            case "cafes" -> placeName + " reflects the newer social culture of " + cityName + ", where cafes have become community spaces for students, creators, remote workers, and casual meetups. Its appeal comes from blending food, design, and lingering time in one venue.";
            case "hotels" -> placeName + " represents how hospitality in " + cityName + " has evolved to serve both city visitors and destination travelers. Properties like this tend to shape guest expectations around comfort, location, dining, and service quality.";
            case "educational-places" -> placeName + " contributes to the academic and knowledge-driven identity of " + cityName + ". Places in this category often grow in importance because they influence student life, research culture, public learning, or civic awareness over time.";
            case "tourist-places" -> placeName + " forms part of the visitor story of " + cityName + ". Over time, landmarks in this category become reference points for memory, local pride, and the way travelers understand the city on first visit.";
            default -> placeName + " has become one of the better-known stops associated with " + cityName + ". Whether through urban design, local memory, or repeat footfall, places like this gradually turn into anchors of the city's public image.";
        };
    }

    private List<DetailItem> placeDetailItems(String categorySlug, String placeName, String cityName) {
        return switch (categorySlug) {
            case "cafes" -> List.of(
                    new DetailItem("Venue", "Designed for conversation, coffee breaks, and casual hangouts within the rhythm of " + cityName + "."),
                    new DetailItem("What They Sell", "Coffee, baked items, comfort plates, desserts, and small meals that support both quick visits and longer stays."),
                    new DetailItem("Popular Dishes", "Signature coffees, breakfast platters, sandwiches, pastries, and dessert-led cafe staples."),
                    new DetailItem("Best For", "Meetups, solo work sessions, date-style cafe visits, and easy evening plans around " + placeName + ".")
            );
            case "hotels" -> List.of(
                    new DetailItem("Stay Style", "Comfort-led hospitality with emphasis on location, room quality, and dependable service flow."),
                    new DetailItem("Amenities", "Guest rooms, dining, front-desk support, and the service touches travelers expect from a strong city stay."),
                    new DetailItem("Who It Suits", "Business travelers, weekend visitors, families, and guests who want a smoother base in " + cityName + "."),
                    new DetailItem("Why It Stands Out", placeName + " is better approached as a full stay experience instead of a simple overnight booking.")
            );
            case "educational-places" -> List.of(
                    new DetailItem("Learning Focus", "Academic, cultural, or research value that adds context to the educational side of " + cityName + "."),
                    new DetailItem("Venue Type", "A campus, museum, institution, or public-learning environment with a knowledge-first identity."),
                    new DetailItem("What Visitors Explore", "Programs, collections, architecture, student energy, or civic knowledge depending on the place."),
                    new DetailItem("Best For", "Students, researchers, parents, and visitors who want more depth than standard tourism.")
            );
            case "tourist-places" -> List.of(
                    new DetailItem("Visitor Experience", "A sightseeing stop that rewards time, observation, and moving through the space slowly."),
                    new DetailItem("What You Notice", "Visual identity, local atmosphere, photo points, and the distinct public character of " + placeName + "."),
                    new DetailItem("Best Time", "Daylight or early evening, when the place is easiest to explore and its atmosphere feels most complete."),
                    new DetailItem("Trip Fit", "Works well as a planned city stop, a walk-through highlight, or part of a longer outing around " + cityName + ".")
            );
            default -> List.of(
                    new DetailItem("Core Identity", placeName + " is one of the places that helps define how visitors experience " + cityName + "."),
                    new DetailItem("Why People Go", "It balances familiarity, visual appeal, and a strong sense of being tied to the city itself."),
                    new DetailItem("What To Expect", "A mix of public energy, recognizable landmarks, and an experience that feels central to the city."),
                    new DetailItem("Best For", "First-time visitors, local revisits, and anyone building a short list of essential stops.")
            );
        };
    }

    private List<String> nearbyMoments(String categorySlug, String placeName, String cityName) {
        return switch (categorySlug) {
            case "cafes" -> List.of(
                    "Arrive for a slower breakfast or mid-evening coffee window.",
                    "Pair " + placeName + " with a nearby market, walk, or photo stop in " + cityName + ".",
                    "Use it as a reset point between shopping, meetings, or sightseeing.");
            case "hotels" -> List.of(
                    "Plan arrivals and departures around the city's peak movement hours.",
                    "Use " + placeName + " as your base for nearby dining and short city circuits.",
                    "Good choice when you want comfort between meetings or day trips.");
            case "educational-places" -> List.of(
                    "Visit when you can spend unhurried time reading, observing, or exploring the campus or collection.",
                    "Pair the stop with another culture or architecture location in " + cityName + ".",
                    "Best experienced with curiosity rather than a rushed checklist approach.");
            case "tourist-places" -> List.of(
                    "Keep extra time for photos and slow exploration.",
                    "Combine " + placeName + " with one food stop and one nearby city landmark.",
                    "Best enjoyed when the weather is mild and movement through the city feels easy.");
            default -> List.of(
                    "Treat it as a cornerstone stop when planning your city route.",
                    "Combine " + placeName + " with cafes, markets, or a sunset point nearby.",
                    "Leave enough time to experience the place, not just pass through it.");
        };
    }

    private void applyManagedCityForm(ManagedCity city, AdminCityForm form) {
        city.setName(form.getName().trim());
        city.setRegion(form.getRegion().trim());
        city.setTagline(form.getTagline().trim());
        city.setHeroImage(form.getHeroImage().trim());
        city.setSearchKeywords(trimToNull(form.getSearchKeywords()));
        city.setBestSeason(trimToNull(form.getBestSeason()));
        city.setIdealDuration(trimToNull(form.getIdealDuration()));
        city.setCityHighlights(trimToNull(form.getCityHighlights()));
    }

    private void applyManagedPlaceForm(ManagedPlace place, AdminPlaceForm form) {
        String citySlug = sanitizeCitySlug(form.getCitySlug());
        String categorySlug = sanitizeCategorySlug(form.getCategorySlug());
        place.setCity(null);
        managedCityRepository.findBySlug(citySlug).ifPresent(place::setCity);
        place.setCitySlug(citySlug);
        place.setCategorySlug(categorySlug);
        place.setName(form.getName().trim());
        place.setDescription(form.getDescription().trim());
        place.setImageUrl(form.getImageUrl().trim());
        place.setGalleryImages(trimToNull(form.getGalleryImages()));
        place.setInsight(trimToNull(form.getInsight()));
        place.setHistory(trimToNull(form.getHistory()));
        place.setAddress(trimToNull(form.getAddress()));
        place.setTimings(trimToNull(form.getTimings()));
        place.setPriceRange(trimToNull(form.getPriceRange()));
        place.setDetailTitleOne(trimToNull(form.getDetailTitleOne()));
        place.setDetailBodyOne(trimToNull(form.getDetailBodyOne()));
        place.setDetailTitleTwo(trimToNull(form.getDetailTitleTwo()));
        place.setDetailBodyTwo(trimToNull(form.getDetailBodyTwo()));
        place.setDetailTitleThree(trimToNull(form.getDetailTitleThree()));
        place.setDetailBodyThree(trimToNull(form.getDetailBodyThree()));
        place.setDetailTitleFour(trimToNull(form.getDetailTitleFour()));
        place.setDetailBodyFour(trimToNull(form.getDetailBodyFour()));
        place.setVisitorNotes(trimToNull(form.getVisitorNotes()));
    }

    private PlaceCard toManagedPlaceCard(ManagedPlace place) {
        return new PlaceCard(normalize(place.getName()), place.getName(), place.getDescription(), place.getImageUrl());
    }

    private Optional<ManagedPlace> findManagedPlace(String citySlug, String categorySlug, String placeSlug) {
        return getManagedPlacesForCity(citySlug).stream()
                .filter(place -> categorySlug.equals(place.getCategorySlug()))
                .filter(place -> normalize(place.getName()).equals(placeSlug))
                .findFirst();
    }

    private PlacePage toManagedPlacePage(ManagedPlace place, String citySlug, CategoryPage category) {
        String cityName = getCity(citySlug).map(CityPage::name).orElse(citySlug);
        return new PlacePage(
                normalize(place.getName()),
                place.getName(),
                category.slug(),
                category.name(),
                place.getImageUrl(),
                place.getDescription(),
                firstNonBlank(place.getInsight(), placeInsight(category.slug(), place.getName(), cityName, place.getDescription())),
                firstNonBlank(place.getHistory(), placeHistory(category.slug(), place.getName(), cityName)),
                managedDetailItems(place, category.slug(), cityName),
                managedVisitorNotes(place, category.slug(), place.getName(), cityName),
                managedGallery(place));
    }

    private List<DetailItem> managedDetailItems(ManagedPlace place, String categorySlug, String cityName) {
        List<DetailItem> items = new ArrayList<>();
        addDetailIfPresent(items, place.getDetailTitleOne(), place.getDetailBodyOne());
        addDetailIfPresent(items, place.getDetailTitleTwo(), place.getDetailBodyTwo());
        addDetailIfPresent(items, place.getDetailTitleThree(), place.getDetailBodyThree());
        addDetailIfPresent(items, place.getDetailTitleFour(), place.getDetailBodyFour());
        addDetailIfPresent(items, "Address", place.getAddress());
        addDetailIfPresent(items, "Timings", place.getTimings());
        addDetailIfPresent(items, "Price Range", place.getPriceRange());
        if (!items.isEmpty()) {
            return items;
        }
        return placeDetailItems(categorySlug, place.getName(), cityName);
    }

    private void addDetailIfPresent(List<DetailItem> items, String title, String description) {
        if (title != null && !title.isBlank() && description != null && !description.isBlank()) {
            items.add(new DetailItem(title.trim(), description.trim()));
        }
    }

    private List<String> managedVisitorNotes(ManagedPlace place, String categorySlug, String placeName, String cityName) {
        List<String> notes = parseMultiline(place.getVisitorNotes());
        if (!notes.isEmpty()) {
            return notes;
        }
        return nearbyMoments(categorySlug, placeName, cityName);
    }

    private List<String> managedGallery(ManagedPlace place) {
        return parseMultiline(place.getGalleryImages());
    }

    private List<String> parseMultiline(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String sanitizeCitySlug(String value) {
        String trimmed = value == null ? "" : value.trim();
        return resolveCitySlug(trimmed).orElse(trimmed);
    }

    private String sanitizeCategorySlug(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (getAdminCategoryOptions().contains(trimmed)) {
            return trimmed;
        }
        return normalize(trimmed);
    }

    private String firstNonBlank(String primary, String fallback) {
        return primary != null && !primary.isBlank() ? primary.trim() : fallback;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ENGLISH).trim().replaceAll("[^a-z0-9]+", "");
    }

    private record CityDataFile(List<EditableCity> cities) {}

    private record EditableCity(
            String slug,
            String name,
            String tagline,
            String heroImage,
            String region,
            List<EditableFact> facts,
            List<String> highlights,
            List<EditableCategory> categories) {}

    private record EditableFact(String value, String label) {}

    private record EditableCategory(
            String slug,
            String name,
            String summary,
            String heroImage,
            List<EditablePlace> places) {}

    private record EditablePlace(
            String name,
            String description,
            String category,
            String imageUrl,
            EditableCoordinates coordinates,
            double rating,
            List<String> tags) {}

    private record EditableCoordinates(double latitude, double longitude) {}

    public record CityCard(String slug, String name, String tagline, String image) {}

    public record CityPage(
            String slug,
            String name,
            String tagline,
            String heroImage,
            String region,
            List<QuickFact> facts,
            List<CategoryPage> categories,
            List<String> highlights) {}

    public record QuickFact(String value, String label) {}

    public record CategoryPage(
            String slug,
            String name,
            String summary,
            String heroImage,
            List<PlaceCard> places) {}

    public record PlaceCard(String slug, String name, String description, String image) {}

    public record GenZModeData(
            List<GenZPlace> bunkSpots,
            List<GenZPlace> hiddenGems,
            List<GenZPair> thisOrThat,
            List<GenZPlace> surprise,
            Map<String, List<GenZPlace>> vibe,
            List<GenZPlace> trending,
            List<GenZPlace> clubs) {}

    public record GenZCategory(
            String slug,
            String name,
            String summary,
            String heroImage,
            List<GenZPlace> places) {}

    public record GenZPlace(String name, String desc, String type, String image, String googleMapsUrl) {}

    public record GenZPair(GenZPlace left, GenZPlace right) {}

    public record PlacePage(
            String slug,
            String name,
            String categorySlug,
            String categoryName,
            String image,
            String description,
            String insight,
            String history,
            List<DetailItem> details,
            List<String> moments,
            List<String> galleryImages) {}

    public record DetailItem(String title, String description) {}

    public record ReviewView(String author, int rating, String comment, String timestamp) {}

    public record FlightOption(String code, String airline, String fromCity, String toCity, String departure, String arrival, int price) {}

    public record HotelOption(String cityName, String hotelName, String overview, String image, long pricePerNight) {}

    public record ManagedPlaceView(
            Long id,
            String citySlug,
            String cityName,
            String categorySlug,
            String categoryName,
            String name,
            String description,
            String imageUrl,
            String galleryImages,
            String insight,
            String history,
            String address,
            String timings,
            String priceRange,
            String detailTitleOne,
            String detailBodyOne,
            String detailTitleTwo,
            String detailBodyTwo,
            String detailTitleThree,
            String detailBodyThree,
            String detailTitleFour,
            String detailBodyFour,
            String visitorNotes,
            int galleryCount) {}
}
