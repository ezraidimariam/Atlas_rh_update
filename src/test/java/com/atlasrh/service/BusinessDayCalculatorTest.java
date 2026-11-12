package com.atlasrh.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BusinessDayCalculatorTest {

    @Test
    void countsWeekdaysInclusivelyAndExcludesWeekends() {
        assertEquals(5, BusinessDayCalculator.count(
                LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 13)));
    }

    @Test
    void returnsZeroForWeekendOnlyPeriod() {
        assertEquals(0, BusinessDayCalculator.count(
                LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 15)));
    }
}
