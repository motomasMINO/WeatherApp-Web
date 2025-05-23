package com.example.demo;

import java.time.LocalDateTime;

public record HourlyForecast(
    LocalDateTime dateTime,
    String description,
    String icon,
    double temp
) {}
