package ru.mirea.kinostudio.service;

import ru.mirea.kinostudio.model.RequestStatus;
import ru.mirea.kinostudio.model.RequestView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Расчёт статистики по заявкам. Все показатели считаются на Java Stream API
 * из уже загруженного списка — без дополнительных SQL-запросов.
 */
public class StatisticsService {

    public record LocationStat(String locationName, int requests, BigDecimal totalBudget) {
    }

    public record Statistics(
            int total,
            BigDecimal totalBudget,
            BigDecimal avgBudget,
            BigDecimal minBudget,
            BigDecimal maxBudget,
            double avgDuration,
            int upcoming,
            double completedSharePercent,
            String topLocation,
            Map<RequestStatus, Integer> byStatus,
            List<LocationStat> byLocation) {
    }

    /** Заявки считаются «ближайшими», если съёмка в ближайшие N дней и заявка ещё не закрыта. */
    public static final int UPCOMING_DAYS = 30;

    public Statistics calculate(List<RequestView> views, LocalDate today) {
        int total = views.size();

        BigDecimal sum = views.stream().map(RequestView::getBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = total == 0 ? BigDecimal.ZERO
                : sum.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        BigDecimal min = views.stream().map(RequestView::getBudget)
                .min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        BigDecimal max = views.stream().map(RequestView::getBudget)
                .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);

        double avgDuration = views.stream().mapToInt(RequestView::getDurationHours).average().orElse(0);

        LocalDate limit = today.plusDays(UPCOMING_DAYS);
        int upcoming = (int) views.stream()
                .filter(v -> isOpen(v.getStatus()))
                .filter(v -> !v.getShootingDate().isBefore(today) && !v.getShootingDate().isAfter(limit))
                .count();

        long completed = views.stream().filter(v -> v.getStatus() == RequestStatus.COMPLETED).count();
        double completedShare = total == 0 ? 0 : 100.0 * completed / total;

        Map<RequestStatus, Integer> byStatus = new EnumMap<>(RequestStatus.class);
        for (RequestStatus s : RequestStatus.values()) byStatus.put(s, 0);
        views.forEach(v -> byStatus.merge(v.getStatus(), 1, Integer::sum));

        List<LocationStat> byLocation = views.stream()
                .collect(Collectors.groupingBy(RequestView::getLocationName))
                .entrySet().stream()
                .map(e -> new LocationStat(
                        e.getKey(),
                        e.getValue().size(),
                        e.getValue().stream().map(RequestView::getBudget).reduce(BigDecimal.ZERO, BigDecimal::add)))
                .sorted(Comparator.comparing(LocationStat::requests).reversed()
                        .thenComparing(Comparator.comparing(LocationStat::totalBudget).reversed()))
                .toList();

        String top = byLocation.isEmpty() ? "—" : byLocation.get(0).locationName();

        return new Statistics(total, sum, avg, min, max, avgDuration, upcoming, completedShare,
                top, byStatus, byLocation);
    }

    private static boolean isOpen(RequestStatus s) {
        return s == RequestStatus.NEW || s == RequestStatus.APPROVED || s == RequestStatus.IN_PROGRESS;
    }
}
