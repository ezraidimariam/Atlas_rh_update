package com.atlasrh.service;

import java.time.DayOfWeek;
import java.time.LocalDate;

public final class BusinessDayCalculator {

    private BusinessDayCalculator() {
    }

    public static int count(LocalDate startDate, LocalDate endDate) {
        int days = 0;
        LocalDate date = startDate;
        while (!date.isAfter(endDate)) {
            DayOfWeek day = date.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                days++;
            }
            date = date.plusDays(1);
        }
        return days;
    }
}
