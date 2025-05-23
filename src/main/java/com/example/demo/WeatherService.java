package com.example.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.http.*;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class WeatherService {
    private static final String API_KEY = "7e30e1e9d070182750eb5bcca3fccfde";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonNode getWeatherData(String location) {
        // 位置情報を取得し、現在の天気を取得
        try {
            String url = "https://api.openweathermap.org/data/2.5/weather?q=" + location +
                    "&units=metric&lang=ja&appid=" + API_KEY;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readTree(response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<HourlyForecast> getFiveDayForecast(String location) {
        // 位置情報を取得し、5日分の3時間ごとの天気を取得
        Coordinates coords = fetchCoordinates(location);
        if (coords == null) return List.of();

        String url = String.format(
            "https://api.openweathermap.org/data/2.5/forecast?lat=%f&lon=%f&units=metric&lang=ja&appid=%s",
            coords.lat(), coords.lon(), API_KEY
        );

        try {
             HttpClient client = HttpClient.newHttpClient();
             HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                // エラーが起きた場合は詳細を表示
                System.err.println("Failed to get 5-day forecast. Status code: " + response.statusCode());
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode list = root.get("list");
            if (list == null || !list.isArray()) return List.of();

            List<HourlyForecast> result = new ArrayList<>();
            for (JsonNode item : list) {
                HourlyForecast forecast = new HourlyForecast(
                    Instant.ofEpochSecond(item.get("dt").asLong()).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                    item.get("weather").get(0).get("description").asText(), // 天気の説明
                    item.get("weather").get(0).get("icon").asText(), // アイコン
                    item.get("main").get("temp").asDouble() // 温度
                );
                result.add(forecast);
            }

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    private Coordinates fetchCoordinates(String location) {
        try {
            String url = "http://api.openweathermap.org/geo/1.0/direct?q=" + location +
                    "&limit=1&appid=" + API_KEY;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            //System.out.println("Geocoding response: " + response.body());

            if (response.statusCode() == 200) {
                JsonNode jsonNode = objectMapper.readTree(response.body());
                if (jsonNode.isArray() && jsonNode.size() > 0) {
                    JsonNode loc = jsonNode.get(0);
                    double lat = loc.get("lat").asDouble(); // 緯度
                    double lon = loc.get("lon").asDouble(); // 経度
                    return new Coordinates(lat, lon);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
