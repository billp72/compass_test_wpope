package com.api;

import io.javalin.Javalin;
import io.javalin.http.Context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.Locale;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class App {

    enum FilterOperator {
        EQUALS,
        GREATER_THAN,
        LESS_THAN
    }

    record Filter(
            String field,
            FilterOperator operator,
            Object value) {
    }

    record Pagination(
            int totalResults,
            int totalPages,
            int page,
            int pageSize) {
    }

    record CombinedResults(
            ArrayNode results,
            Pagination pagination) {
    }

    record Dedupe(
            ArrayNode listings,
            int count) {
    }

    private static String normalizeAddress(String address) {
        return address
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\b(?:apt|apartment|appartment|unit|suite|ste)\\b", "")
                .replaceAll("[^\\p{L}\\p{N}]", "");
    }

    public static void main(String[] args) {
        JsonNode listings = loadListings();

        var app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        });

        app.get("/api/health", ctx -> {
            ctx.json(Map.of(
                    "status", "ok",
                    "message", "Javalin is running"));
        });

        app.get("/api/listings", ctx -> {
            String keyword = ctx.queryParam("keyword");
            int pageParam = Integer.parseInt(ctx.queryParam("page"));
            int pageSizeParam = Integer.parseInt(ctx.queryParam("pageSize"));

            Filter[] filters;

            try {
                filters = loadFilters(ctx);
            } catch (IllegalArgumentException exception) {
                ctx.status(400).json(Map.of("error", exception.getMessage()));
                return;
            }

            if (keyword == null || keyword.isBlank()) {
                ctx.json(paginateResults(filterListings(listings, filters), pageParam, pageSizeParam));
                return;
            }

            if (filters.length > 0) {
                ctx.status(400).json(Map.of(
                        "error", "Cannot use both keyword search and filters at the same time"));
                return;
            }

            ctx.json(paginateResults(searchListings(listings, keyword), pageParam, pageSizeParam));
        });

        app.start(7070);
    }

    private static Dedupe dedupeListings(JsonNode listings, JsonNode originalListings, int pageSize) {
        ArrayNode finalResults = new ObjectMapper().createArrayNode();
        Set<String> seenAddresses = new HashSet<>();
        Set<String> seenZipcodes = new HashSet<>();

        for (JsonNode listing : listings) {
            String normalizedAddress = normalizeAddress(listing.path("address").asText());
            String zipcode = listing.path("zipcode").asText();

            if (!seenAddresses.add(normalizedAddress) && !seenZipcodes.add(zipcode)) {
                finalResults.add(listing);
            }
        }
        for (JsonNode listing : originalListings) {
            if (finalResults.size() >= pageSize) {
                break;
            }

            String address = normalizeAddress(
                    listing.path("address").asText());

            String zipcode = listing.path("zipcode").asText();

            if (!seenAddresses.add(address) && !seenZipcodes.add(zipcode)) {
                finalResults.add(listing);
            }
        }

        return new Dedupe(finalResults, finalResults.size());
    }

    private static CombinedResults paginateResults(ArrayNode results, int page, int pageSize) {
        int totalResults = results.size();
        int totalPages = (int) Math.ceil((double) totalResults / pageSize);

        if (page < 1 || page > totalPages) {
            throw new IllegalArgumentException("Page number out of range");
        }

        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalResults);

        ArrayNode paginatedResults = results.arrayNode();
        for (int i = fromIndex; i < toIndex; i++) {
            paginatedResults.add(results.get(i));
        }

        ArrayNode dedupedResults = dedupeListings(paginatedResults, results, pageSize).listings();

        return new CombinedResults(
                dedupedResults,
                new Pagination(totalResults, totalPages, page, pageSize));
    }

    private static Filter[] loadFilters(Context ctx) {
        List<Filter> filters = new ArrayList<>();

        for (String rawFilter : ctx.queryParams("filter")) {
            String[] parts = rawFilter.split(":", 3);

            if (parts.length != 3) {
                throw new IllegalArgumentException(
                        "Filter must use the format field:operator:value");
            }

            String field = parts[0].trim();
            String operatorText = parts[1].trim().toUpperCase();
            String rawValue = parts[2].trim();

            FilterOperator operator;

            try {
                operator = FilterOperator.valueOf(operatorText);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Unsupported filter operator: " + parts[1], exception);
            }

            Object value = parseFilterValue(field, rawValue);

            filters.add(new Filter(field, operator, value));
        }

        return filters.toArray(new Filter[0]);
    }

    private static Object parseFilterValue(String field, String rawValue) {
        return switch (field) {
            case "price", "bedrooms", "bathrooms", "sqft" -> {
                try {
                    yield Double.parseDouble(rawValue);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException(
                            "Filter value for " + field + " must be numeric", exception);
                }
            }

            case "address", "city", "state", "status", "source", "zip" ->
                rawValue;

            default -> throw new IllegalArgumentException(
                    "Unsupported filter field: " + field);
        };
    }

    private static JsonNode loadListings() {
        try (var input = App.class.getResourceAsStream("/com/api/sample_listings.json")) {
            if (input == null) {
                throw new IllegalStateException("sample_listings.json was not found");
            }
            return new ObjectMapper().readTree(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read sample_listings.json", exception);
        }
    }

    private static ArrayNode filterListings(
            JsonNode listings,
            Filter[] filters) {
        ArrayNode results = new ObjectMapper().createArrayNode();

        for (JsonNode listing : listings) {
            boolean matchesAllFilters = true;

            for (Filter filter : filters) {
                if (!matchesFilter(listing, filter)) {
                    matchesAllFilters = false;
                    break;
                }
            }

            if (matchesAllFilters) {
                results.add(listing);
            }
        }

        return results;
    }

    private static boolean matchesFilter(JsonNode listing, Filter filter) {
        JsonNode actualValue = listing.get(filter.field());

        if (actualValue == null) {
            return false;
        }

        if (actualValue.isTextual() && filter.value() instanceof String expectedValue) {
            return actualValue.asText().equalsIgnoreCase(expectedValue);
        }

        if (actualValue.isNumber() && filter.value() instanceof Number expectedValue) {
            double actualNumber = actualValue.asDouble();
            double expectedNumber = expectedValue.doubleValue();

            return switch (filter.operator()) {
                case EQUALS -> actualNumber == expectedNumber;
                case GREATER_THAN -> actualNumber > expectedNumber;
                case LESS_THAN -> actualNumber < expectedNumber;
            };
        }

        return false;
    }

    private static ArrayNode searchListings(JsonNode listings, String keyword) {
        ArrayNode results = new ObjectMapper().createArrayNode();
        for (JsonNode listing : listings) {
            String description = listing.get("description").asText();
            if (description.toLowerCase().contains(keyword.toLowerCase())) {
                results.add(listing);
            }
        }
        return results;
    }
}