package ru.mirea.kinostudio.repository;

import ru.mirea.kinostudio.model.RequestStatus;
import ru.mirea.kinostudio.model.ShootingRequest;
import ru.mirea.kinostudio.util.DatabaseManager;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ShootingRequestRepository implements Repository<ShootingRequest> {

    @Override
    public ShootingRequest save(ShootingRequest r) throws SQLException {
        String sql = "INSERT INTO shooting_requests " +
                "(client_id, location_id, title, description, shooting_date, duration_hours, budget, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id, created_at";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, r.getClientId());
            ps.setInt(2, r.getLocationId());
            ps.setString(3, r.getTitle());
            ps.setString(4, r.getDescription());
            ps.setDate(5, Date.valueOf(r.getShootingDate()));
            ps.setInt(6, r.getDurationHours());
            ps.setBigDecimal(7, r.getBudget());
            ps.setString(8, r.getStatus().name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    r.setId(rs.getInt("id"));
                    r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                }
            }
        }
        return r;
    }

    @Override
    public Optional<ShootingRequest> findById(int id) throws SQLException {
        String sql = "SELECT * FROM shooting_requests WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    @Override
    public List<ShootingRequest> findAll() throws SQLException {
        List<ShootingRequest> result = new ArrayList<>();
        String sql = "SELECT * FROM shooting_requests ORDER BY id";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapRow(rs));
        }
        return result;
    }

    @Override
    public void update(ShootingRequest r) throws SQLException {
        String sql = "UPDATE shooting_requests SET client_id = ?, location_id = ?, title = ?, description = ?, " +
                "shooting_date = ?, duration_hours = ?, budget = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, r.getClientId());
            ps.setInt(2, r.getLocationId());
            ps.setString(3, r.getTitle());
            ps.setString(4, r.getDescription());
            ps.setDate(5, Date.valueOf(r.getShootingDate()));
            ps.setInt(6, r.getDurationHours());
            ps.setBigDecimal(7, r.getBudget());
            ps.setString(8, r.getStatus().name());
            ps.setInt(9, r.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteById(int id) throws SQLException {
        String sql = "DELETE FROM shooting_requests WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ---- Поиск ----

    public List<ShootingRequest> searchByTitle(String keyword) throws SQLException {
        // Сравнение в Java, а не через ILIKE: у базы локаль C, и ILIKE
        // не приводит к одному регистру русские буквы («Клип» и «клип»).
        return findAll().stream()
                .filter(r -> containsIgnoreCase(r.getTitle(), keyword))
                .toList();
    }

    /** Поиск заявок по названию локации (JOIN с таблицей locations). */
    public List<ShootingRequest> searchByLocationName(String keyword) throws SQLException {
        List<ShootingRequest> result = new ArrayList<>();
        String sql = "SELECT sr.*, l.name AS location_name FROM shooting_requests sr " +
                "JOIN locations l ON sr.location_id = l.id ORDER BY sr.id";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                if (containsIgnoreCase(rs.getString("location_name"), keyword)) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    private static boolean containsIgnoreCase(String text, String keyword) {
        if (text == null || keyword == null) {
            return false;
        }
        return text.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    // ---- Фильтрация ----

    public List<ShootingRequest> filterByStatus(RequestStatus status) throws SQLException {
        List<ShootingRequest> result = new ArrayList<>();
        String sql = "SELECT * FROM shooting_requests WHERE status = ? ORDER BY id";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        }
        return result;
    }

    public List<ShootingRequest> filterByDateRange(LocalDate from, LocalDate to) throws SQLException {
        List<ShootingRequest> result = new ArrayList<>();
        String sql = "SELECT * FROM shooting_requests WHERE shooting_date BETWEEN ? AND ? ORDER BY shooting_date";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        }
        return result;
    }

    // ---- Сортировка ----

    public List<ShootingRequest> findAllSortedByDate() throws SQLException {
        return findAllSortedBy("shooting_date");
    }

    public List<ShootingRequest> findAllSortedByBudget() throws SQLException {
        return findAllSortedBy("budget");
    }

    private List<ShootingRequest> findAllSortedBy(String column) throws SQLException {
        List<ShootingRequest> result = new ArrayList<>();
        String sql = "SELECT * FROM shooting_requests ORDER BY " + column;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapRow(rs));
        }
        return result;
    }

    // ---- Статистика ----

    public int countAll() throws SQLException {
        return countWhere(null);
    }

    public int countByStatus(RequestStatus status) throws SQLException {
        return countWhere(status);
    }

    private int countWhere(RequestStatus status) throws SQLException {
        String sql = status == null
                ? "SELECT COUNT(*) FROM shooting_requests"
                : "SELECT COUNT(*) FROM shooting_requests WHERE status = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (status != null) ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public BigDecimal sumBudget() throws SQLException {
        String sql = "SELECT COALESCE(SUM(budget), 0) FROM shooting_requests";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getBigDecimal(1);
        }
    }

    public double avgDuration() throws SQLException {
        String sql = "SELECT COALESCE(AVG(duration_hours), 0) FROM shooting_requests";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getDouble(1);
        }
    }

    private ShootingRequest mapRow(ResultSet rs) throws SQLException {
        return new ShootingRequest(
                rs.getInt("id"),
                rs.getInt("client_id"),
                rs.getInt("location_id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getDate("shooting_date").toLocalDate(),
                rs.getInt("duration_hours"),
                rs.getBigDecimal("budget"),
                RequestStatus.fromString(rs.getString("status")),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
