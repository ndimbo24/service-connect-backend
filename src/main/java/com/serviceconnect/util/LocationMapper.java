package com.serviceconnect.util;

import java.util.List;

/**
 * Maps GPS coordinates (latitude, longitude) to Tanzanian region and district.
 * Uses a predefined bounding-box approach for major regions and districts.
 *
 * <p>This is a lightweight alternative to external reverse-geocoding APIs.
 * For production, consider integrating a real geocoding service.</p>
 */
public class LocationMapper {

    private LocationMapper() {
        // utility class
    }

    /**
     * Result holder for region + district lookup.
     */
    public record RegionDistrict(String region, String district) {
    }

    private static final List<Zone> ZONES = List.of(
        // Dar es Salaam
        new Zone(-7.0, -6.5, 39.0, 39.5, "Dar es Salaam", "Kinondoni"),
        new Zone(-7.0, -6.5, 39.5, 40.0, "Dar es Salaam", "Ilala"),
        new Zone(-7.0, -6.5, 40.0, 40.5, "Dar es Salaam", "Temeke"),
        new Zone(-6.9, -6.7, 39.1, 39.4, "Dar es Salaam", "Ubungo"),
        new Zone(-6.9, -6.7, 39.4, 39.7, "Dar es Salaam", "Kigamboni"),

        // Arusha
        new Zone(-3.5, -3.0, 36.5, 37.0, "Arusha", "Arusha Urban"),
        new Zone(-3.5, -3.0, 37.0, 37.5, "Arusha", "Arusha Rural"),
        new Zone(-2.5, -2.0, 35.5, 36.0, "Arusha", "Monduli"),
        new Zone(-3.0, -2.5, 36.0, 36.5, "Arusha", "Meru"),

        // Mwanza
        new Zone(-2.8, -2.3, 32.5, 33.0, "Mwanza", "Mwanza Urban"),
        new Zone(-2.8, -2.3, 33.0, 33.5, "Mwanza", "Ilemela"),
        new Zone(-2.3, -1.8, 32.5, 33.0, "Mwanza", "Sengerema"),

        // Dodoma
        new Zone(-6.5, -6.0, 35.5, 36.0, "Dodoma", "Dodoma Urban"),
        new Zone(-6.5, -6.0, 36.0, 36.5, "Dodoma", "Dodoma Rural"),
        new Zone(-7.0, -6.5, 35.5, 36.0, "Dodoma", "Chamwino"),

        // Mbeya
        new Zone(-8.5, -8.0, 33.0, 33.5, "Mbeya", "Mbeya Urban"),
        new Zone(-8.5, -8.0, 33.5, 34.0, "Mbeya", "Mbeya Rural"),
        new Zone(-9.0, -8.5, 33.0, 33.5, "Mbeya", "Rungwe"),

        // Mtwara
        new Zone(-11.0, -10.5, 39.5, 40.0, "Mtwara", "Mtwara Urban"),
        new Zone(-11.0, -10.5, 40.0, 40.5, "Mtwara", "Mtwara Rural"),

        // Morogoro
        new Zone(-7.0, -6.5, 37.0, 37.5, "Morogoro", "Morogoro Urban"),
        new Zone(-7.0, -6.5, 37.5, 38.0, "Morogoro", "Morogoro Rural"),
        new Zone(-8.0, -7.5, 36.5, 37.0, "Morogoro", "Kilosa"),

        // Tanga
        new Zone(-5.5, -5.0, 38.5, 39.0, "Tanga", "Tanga Urban"),
        new Zone(-5.5, -5.0, 39.0, 39.5, "Tanga", "Tanga Rural"),

        // Kilimanjaro
        new Zone(-3.5, -3.0, 37.0, 37.5, "Kilimanjaro", "Moshi Urban"),
        new Zone(-3.5, -3.0, 37.5, 38.0, "Kilimanjaro", "Moshi Rural"),
        new Zone(-4.0, -3.5, 37.0, 37.5, "Kilimanjaro", "Hai"),

        // Zanzibar
        new Zone(-6.5, -6.0, 39.0, 39.5, "Zanzibar", "Zanzibar Urban"),
        new Zone(-6.5, -6.0, 39.5, 40.0, "Zanzibar", "Zanzibar North"),

        // Kagera
        new Zone(-1.5, -1.0, 31.0, 31.5, "Kagera", "Bukoba Urban"),
        new Zone(-2.0, -1.5, 30.5, 31.0, "Kagera", "Karagwe"),

        // Kigoma
        new Zone(-5.0, -4.5, 29.5, 30.0, "Kigoma", "Kigoma Urban"),
        new Zone(-5.0, -4.5, 30.0, 30.5, "Kigoma", "Kigoma Rural"),

        // Lindi
        new Zone(-10.5, -10.0, 39.0, 39.5, "Lindi", "Lindi Urban"),
        new Zone(-10.5, -10.0, 39.5, 40.0, "Lindi", "Lindi Rural"),

        // Mara
        new Zone(-1.8, -1.3, 33.5, 34.0, "Mara", "Musoma Urban"),
        new Zone(-1.8, -1.3, 34.0, 34.5, "Mara", "Rorya"),

        // Manyara
        new Zone(-4.0, -3.5, 35.5, 36.0, "Manyara", "Babati Urban"),
        new Zone(-4.0, -3.5, 36.0, 36.5, "Manyara", "Babati Rural"),

        // Njombe
        new Zone(-9.5, -9.0, 34.5, 35.0, "Njombe", "Njombe Urban"),
        new Zone(-9.5, -9.0, 35.0, 35.5, "Njombe", "Njombe Rural"),

        // Pwani
        new Zone(-7.5, -7.0, 38.5, 39.0, "Pwani", "Kibaha"),
        new Zone(-8.0, -7.5, 39.0, 39.5, "Pwani", "Bagamoyo"),

        // Rukwa
        new Zone(-8.0, -7.5, 32.0, 32.5, "Rukwa", "Sumbawanga Urban"),

        // Ruvuma
        new Zone(-11.5, -11.0, 34.5, 35.0, "Ruvuma", "Songea Urban"),
        new Zone(-11.5, -11.0, 35.0, 35.5, "Ruvuma", "Songea Rural"),
        new Zone(-11.0, -10.5, 34.0, 35.0, "Ruvuma", "Mbinga"),

        // Shinyanga
        new Zone(-3.8, -3.3, 33.0, 33.5, "Shinyanga", "Shinyanga Urban"),

        // Simiyu
        new Zone(-3.5, -3.0, 33.5, 34.0, "Simiyu", "Bariadi"),

        // Singida
        new Zone(-5.0, -4.5, 34.0, 34.5, "Singida", "Singida Urban"),

        // Songwe
        new Zone(-9.5, -9.0, 33.0, 33.5, "Songwe", "Vwawa"),

        // Tabora
        new Zone(-5.5, -5.0, 32.5, 33.0, "Tabora", "Tabora Urban"),

        // Geita
        new Zone(-3.0, -2.5, 32.0, 32.5, "Geita", "Geita Urban"),

        // Iringa
        new Zone(-8.0, -7.5, 35.5, 36.0, "Iringa", "Iringa Urban"),
        new Zone(-8.5, -8.0, 35.0, 35.5, "Iringa", "Kilolo"),

        // Katavi
        new Zone(-6.5, -6.0, 31.0, 31.5, "Katavi", "Mpanda"),

        // Mwanza rural fallback
        new Zone(-3.0, -2.0, 32.0, 33.5, "Mwanza", "Mwanza"),

        // Dar es Salaam wide fallback
        new Zone(-7.2, -6.4, 38.8, 40.5, "Dar es Salaam", "Dar es Salaam"),

        // Arusha wide fallback
        new Zone(-4.0, -2.5, 35.5, 38.0, "Arusha", "Arusha"),

        // Mbeya wide fallback
        new Zone(-9.5, -8.0, 32.5, 34.5, "Mbeya", "Mbeya"),

        // Dodoma wide fallback
        new Zone(-7.5, -5.5, 35.0, 37.0, "Dodoma", "Dodoma"),

        // Morogoro wide fallback
        new Zone(-8.5, -6.0, 36.0, 39.0, "Morogoro", "Morogoro")
    );

    /**
     * Converts latitude/longitude to a Tanzanian region and district.
     *
     * @param lat latitude
     * @param lng longitude
     * @return RegionDistrict, or a default "Unknown" pair if no zone matches
     */
    public static RegionDistrict toRegionDistrict(double lat, double lng) {
        for (Zone zone : ZONES) {
            if (zone.contains(lat, lng)) {
                return new RegionDistrict(zone.region, zone.district);
            }
        }
        return new RegionDistrict("Unknown", "Unknown");
    }

    private static final class Zone {
        final double latMin;
        final double latMax;
        final double lngMin;
        final double lngMax;
        final String region;
        final String district;

        Zone(double latMin, double latMax, double lngMin, double lngMax,
             String region, String district) {
            this.latMin = latMin;
            this.latMax = latMax;
            this.lngMin = lngMin;
            this.lngMax = lngMax;
            this.region = region;
            this.district = district;
        }

        boolean contains(double lat, double lng) {
            return lat >= latMin && lat <= latMax && lng >= lngMin && lng <= lngMax;
        }
    }
}

