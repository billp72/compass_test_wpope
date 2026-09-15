package com.api;

import io.javalin.Javalin;
import io.javalin.http.Context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

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
            Filter[] filters;

            try {
                filters = loadFilters(ctx);
            } catch (IllegalArgumentException exception) {
                ctx.status(400).json(Map.of("error", exception.getMessage()));
                return;
            }

            if (keyword == null || keyword.isBlank()) {
                ctx.json(filterListings(listings, filters));
                return;
            }

            if (filters.length > 0) {
                ctx.status(400).json(Map.of(
                        "error", "Cannot use both keyword search and filters at the same time"));
                return;
            }

            ctx.json(searchListings(listings, keyword));
        });

        app.start(7070);
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