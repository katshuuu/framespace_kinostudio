package ru.mirea.kinostudio.api.auth;

import ru.mirea.kinostudio.exception.BusinessException;
import ru.mirea.kinostudio.exception.ConflictException;
import ru.mirea.kinostudio.model.User;
import ru.mirea.kinostudio.repository.UserStore;

import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * Регистрация и вход сотрудника API.
 * Заказчики из таблицы clients здесь не проверяются: они в систему не входят.
 */
public final class AuthService {

    static final String INVALID_CREDENTIALS = "Неверный логин или пароль.";

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,50}$");

    private final UserStore users;
    private final JwtService jwt;
    private final PasswordHasher passwords;

    public AuthService(UserStore users, JwtService jwt, PasswordHasher passwords) {
        this.users = users;
        this.jwt = jwt;
        this.passwords = passwords;
    }

    public AuthResponse register(String username, String password, String fullName)
            throws BusinessException, SQLException {
        if (username == null || !USERNAME.matcher(username).matches()) {
            throw new BusinessException(
                    "Логин должен содержать от 3 до 50 символов: латиница, цифры и знак подчёркивания.");
        }
        if (password == null || password.length() < 6) {
            throw new BusinessException("Пароль должен содержать не меньше 6 символов.");
        }
        if (fullName == null || fullName.isBlank() || fullName.length() > 150) {
            throw new BusinessException("ФИО сотрудника не может быть пустым и длиннее 150 символов.");
        }
        if (users.findByUsername(username).isPresent()) {
            throw new ConflictException("Логин уже занят.");
        }
        User saved = users.save(new User(username, passwords.hash(password), fullName));
        return response(saved);
    }

    public AuthResponse login(String username, String password) throws BusinessException, SQLException {
        User user = username == null ? null : users.findByUsername(username).orElse(null);
        if (user == null || password == null || !passwords.matches(password, user.getPasswordHash())) {
            throw new BusinessException(INVALID_CREDENTIALS);
        }
        return response(user);
    }

    private AuthResponse response(User user) {
        String token = jwt.issue(user.getId(), user.getUsername());
        return new AuthResponse(
                token,
                "Bearer",
                jwt.ttlSeconds(),
                user.getId(),
                user.getUsername(),
                user.getFullName());
    }
}
