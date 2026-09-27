package ru.mirea.kinostudio.repository;

import ru.mirea.kinostudio.model.Location;
import ru.mirea.kinostudio.model.LocationType;
import ru.mirea.kinostudio.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LocationRepository implements Repository<Location> {

    @Override
    public Location save(Location loc) throws SQLException {
        String sql = "INSERT INTO locations (name, address, capacity, type) VALUES (?, ?, ?, ?) RETURNING id, created_at";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, loc.getName());
            ps.setString(2, loc.getAddress());
            ps.setInt(3, loc.getCapacity());
            ps.setString(4, loc.getType().name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    loc.setId(rs.getInt("id"));
                    loc.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                }
            }
        }
        return loc;
    }

    @Override
    public Optional<Location> findById(int id) throws SQLException {
        String sql = "SELECT * FROM locations WHERE id = ?";
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
    public List<Location> findAll() throws SQLException {
        List<Location> result = new ArrayList<>();
        String sql = "SELECT * FROM locations ORDER BY id";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapRow(rs));
        }
        return result;
    }

    @Override
    public void update(Location loc) throws SQLException {
        String sql = "UPDATE locations SET name = ?, address = ?, capacity = ?, type = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, loc.getName());
            ps.setString(2, loc.getAddress());
            ps.setInt(3, loc.getCapacity());
            ps.setString(4, loc.getType().name());
            ps.setInt(5, loc.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteById(int id) throws SQLException {
        String sql = "DELETE FROM locations WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public boolean existsById(int id) throws SQLException {
        String sql = "SELECT 1 FROM locations WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Проверяет, есть ли у локации активные (не терминальные) заявки — используется бизнес-правилом на удаление. */
    public boolean hasActiveRequests(int locationId) throws SQLException {
        String sql = "SELECT 1 FROM shooting_requests WHERE location_id = ? " +
                "AND status NOT IN ('COMPLETED','REJECTED','CANCELLED') LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, locationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Location mapRow(ResultSet rs) throws SQLException {
        LocalDateTime createdAt = rs.getTimestamp("created_at") != null
                ? rs.getTimestamp("created_at").toLocalDateTime() : null;
        return new Location(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("address"),
                rs.getInt("capacity"),
                LocationType.fromString(rs.getString("type")),
                createdAt
        );
    }
}
