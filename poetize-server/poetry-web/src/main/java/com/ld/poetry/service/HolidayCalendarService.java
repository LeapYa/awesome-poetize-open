package com.ld.poetry.service;

import java.time.LocalDate;
import java.util.List;

public interface HolidayCalendarService {

    FestivalInfo getFestivalInfo(LocalDate date);

    UpcomingFestival getNextFestival(LocalDate fromDate);

    HolidayDayInfo getHolidayDayInfo(LocalDate date);

    UpcomingHolidayBreak getNextHolidayBreak(LocalDate fromDate);

    record FestivalInfo(
            LocalDate date,
            String lunarDate,
            String zodiac,
            String cyclical,
            String solarTerm,
            List<String> festivals,
            List<String> statutoryFestivals,
            String source) {
    }

    record UpcomingFestival(
            String name,
            LocalDate date,
            long daysLeft,
            boolean statutory) {
    }

    record HolidayDayInfo(
            LocalDate date,
            int dayTypeCode,
            String dayType,
            String displayName,
            String holidayName,
            boolean workday,
            boolean offDay,
            boolean official,
            boolean predicted,
            String source,
            Integer wage,
            String note) {
    }

    record UpcomingHolidayBreak(
            String name,
            LocalDate startDate,
            LocalDate endDate,
            long daysLeft,
            int durationDays,
            boolean official,
            boolean predicted,
            String source,
            String note) {
    }
}
