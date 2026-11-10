package org.example.recruitservice.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.apiPayload.error.code.BaseErrorCode;
import org.example.common.apiPayload.error.exception.CustomException;
import org.example.recruitservice.dto.map.NearbyPlaceDto;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleMapService {

    @Value("${google.maps.api-key}")
    private String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();

    public String getAddressFromCoordinates(double latitude, double longitude) {
        String url = "https://maps.googleapis.com/maps/api/geocode/json"
                + "?latlng=" + latitude + "," + longitude
                + "&key=" + apiKey
                + "&language=ko";

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JSONObject json = new JSONObject(response.getBody());
            JSONArray results = json.getJSONArray("results");

            if (results.length() > 0) {
                JSONObject formatted = results.getJSONObject(0);
                return formatted.getString("formatted_address");
            } else {
                return new CustomException(BaseErrorCode.ADDRESS_NOT_FOUND).getMessage();
            }
        } catch (Exception e) {
            throw new CustomException(BaseErrorCode.GOOGLE_MAP_API_ERROR);
        }
    }

    public Map<String, Object> getGeocode(String address) {
        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
                    .queryParam("address", address)
                    .queryParam("key", apiKey)
                    .queryParam("language", "ko")
                    .encode(StandardCharsets.UTF_8)
                    .build()
                    .toUri();

            log.info("Requesting geocode URL: [{}]", uri);

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

            JSONObject json = new JSONObject(response.getBody());
            String status = json.optString("status", "");
            if (!"OK".equals(status)) {
                switch (status) {
                    case "ZERO_RESULTS":
                        throw new CustomException(BaseErrorCode.ADDRESS_NOT_FOUND);
                    case "OVER_QUERY_LIMIT":
                        throw new CustomException(BaseErrorCode.GOOGLE_OVER_QUERY_LIMIT);
                    case "REQUEST_DENIED":
                        throw new CustomException(BaseErrorCode.GOOGLE_REQUEST_DENIED);
                    case "INVALID_REQUEST":
                        throw new CustomException(BaseErrorCode.GOOGLE_INVALID_REQUEST);
                    default:
                        throw new CustomException(BaseErrorCode.GOOGLE_MAP_API_ERROR);
                }
            }

            JSONObject result = json.getJSONArray("results").getJSONObject(0);
            JSONObject location = result.getJSONObject("geometry").getJSONObject("location");

            Map<String, Object> geocodeResult = new HashMap<>();
            geocodeResult.put("latitude", location.getDouble("lat"));
            geocodeResult.put("longitude", location.getDouble("lng"));
            geocodeResult.put("formattedAddress", result.getString("formatted_address"));
            geocodeResult.put("placeId", result.optString("place_id", null));
            return geocodeResult;

        } catch (CustomException ce) {
            throw ce;
        } catch (RestClientException rce) {
            log.error("HTTP error from Geocode API", rce);
            throw new CustomException(BaseErrorCode.GOOGLE_MAP_API_ERROR);
        } catch (JSONException je) {
            log.error("JSON parse error from Geocode API response", je);
            throw new CustomException(BaseErrorCode.GOOGLE_RESPONSE_PARSE_ERROR);
        } catch (Exception e) {
            log.error("Unexpected error during geocoding", e);
            throw new CustomException(BaseErrorCode.GOOGLE_MAP_API_ERROR);
        }
    }


    public List<NearbyPlaceDto> getNearbyPlaces(double latitude, double longitude, Integer radiusInMeters) {
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                + "?location=" + latitude + "," + longitude
                + "&radius=" + radiusInMeters
                + "&key=" + apiKey
                + "&language=ko";

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JSONObject json = new JSONObject(response.getBody());

            JSONArray results = json.getJSONArray("results");

            List<NearbyPlaceDto> places = new ArrayList<>();

            for (int i = 0; i < results.length(); i++) {
                JSONObject obj = results.getJSONObject(i);
                JSONObject location = obj.getJSONObject("geometry").getJSONObject("location");

                places.add(NearbyPlaceDto.builder()
                        .name(obj.optString("name"))
                        .address(obj.optString("vicinity"))
                        .latitude(location.getDouble("lat"))
                        .longitude(location.getDouble("lng"))
                        .build());
            }

            return places;

        } catch (Exception e) {
            throw new CustomException(BaseErrorCode.GOOGLE_NEARBY_API_ERROR);
        }
    }
}
