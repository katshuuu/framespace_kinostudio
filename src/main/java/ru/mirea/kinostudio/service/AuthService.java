package ru.mirea.kinostudio.service;

import ru.mirea.kinostudio.exception.BusinessException;
import ru.mirea.kinostudio.model.Role;
import ru.mirea.kinostudio.model.User;
import ru.mirea.kinostudio.repository.UserRepository;
import ru.mirea.kinostudio.util.AppConfig;
import ru.mirea.kinostudio.util.PasswordHasher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Регистрация и вход.
 * Обычные пользователи хранятся в таблице users (пароль — только в виде хеша).
 * Администратор один: его логин и пароль задаются в .env (ADMIN_LOGIN, ADMIN_PASSWORD) и в базу не пишутся.
 */
public class AuthService {

    public static final int MIN_LOGIN = 3;
    public static final int MAX_LOGIN = 30;
    public static final int MIN_PASSWORD = 8;
    public static final int MAX_PASSWORD = 72;

    private static final Pattern LOGIN = Pattern.compile("^[A-Za-z0-9_.-]+$");
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MILLIS = 30_000;
    private static final String WRONG = "Неверный логин или пароль.";

    private final UserRepository repository;
    private final Map<String, int[]> failures = new HashMap<>();      // логин -> [число ошибок]
    private final Map<String, Long> lockedUntil = new HashMap<>();

    public AuthService(UserRepository repository) {
        this.repository = repository;
    }

    public void init() throws SQLException {
        repository.ensureTable();
    }

    /** Логин администратора из .env (null, если вход администратора не настроен). */
    private static String adminLogin() {
        return AppConfig.get("ADMIN_LOGIN");
    }

    private static String adminPassword() {
        return AppConfig.get("ADMIN_PASSWORD");
    }

    public User register(String login, String password, String confirm) throws BusinessException, SQLException {
        String l = login == null ? "" : login.trim();
        String p = password == null ? "" : password;

        if (l.length() < MIN_LOGIN || l.length() > MAX_LOGIN) {
            throw new BusinessException("Логин: от " + MIN_LOGIN + " до " + MAX_LOGIN + " символов.");
        }
        if (!LOGIN.matcher(l).matches()) {
            throw new BusinessException("Логин: допустимы латинские буквы, цифры и символы «_», «.», «-».");
        }
        validatePassword(p);
        if (!p.equals(confirm)) {
            throw new BusinessException("Пароль и подтверждение не совпадают.");
        }
        String admin = adminLogin();
        if (admin != null && admin.equalsIgnoreCase(l)) {
            throw new BusinessException("Этот логин зарезервирован, выберите другой.");
        }
        if (repository.existsByLogin(l)) {
            throw new BusinessException("Пользователь с таким логином уже существует.");
        }
        return repository.save(l, PasswordHasher.hash(p), Role.USER);
    }

    /** Проверка пароля при регистрации (отдельно, чтобы форма могла показать все ошибки сразу). */
    public static void validatePassword(String p) throws BusinessException {
        if (p.length() < MIN_PASSWORD || p.length() > MAX_PASSWORD) {
            throw new BusinessException("Пароль: от " + MIN_PASSWORD + " до " + MAX_PASSWORD + " символов.");
        }
        boolean letter = p.chars().anyMatch(Character::isLetter);
        boolean digit = p.chars().anyMatch(Character::isDigit);
        if (!letter || !digit) {
            throw new BusinessException("Пароль: нужна хотя бы одна буква и одна цифра.");
        }
    }

    public User login(String login, String password) throws BusinessException, SQLException {
        String l = login == null ? "" : login.trim();
        String p = password == null ? "" : password;
        if (l.isEmpty() || p.isEmpty()) {
            throw new BusinessException("Введите логин и пароль.");
        }
        String key = l.toLowerCase(Locale.ROOT);
        checkLock(key);

        User user = authenticate(l, p);
        if (user == null) {
            registerFailure(key);
            throw new BusinessException(WRONG);
        }
        failures.remove(key);
        lockedUntil.remove(key);
        return user;
    }

    private User authenticate(String login, String password) throws SQLException {
        String admin = adminLogin();
        String adminPass = adminPassword();
        if (admin != null && adminPass != null && admin.equalsIgnoreCase(login)) {
            // Единый администратор: сравнение за постоянное время
            boolean ok = MessageDigest.isEqual(adminPass.getBytes(StandardCharsets.UTF_8),
                    password.getBytes(StandardCharsets.UTF_8));
            return ok ? new User(0, admin, Role.ADMIN) : null;
        }
        Optional<UserRepository.StoredUser> stored = repository.findByLogin(login);
        if (stored.isEmpty()) {
            PasswordHasher.hash(password); // выравниваем время ответа, чтобы нельзя было угадать существующие логины
            return null;
        }
        return PasswordHasher.verify(password, stored.get().passwordHash()) ? stored.get().user() : null;
    }

    private void checkLock(String key) throws BusinessException {
        Long until = lockedUntil.get(key);
        if (until != null) {
            long left = until - System.currentTimeMillis();
            if (left > 0) {
                throw new BusinessException("Слишком много неудачных попыток. Подождите " + (left / 1000 + 1) + " с.");
            }
            lockedUntil.remove(key);
            failures.remove(key);
        }
    }

    private void registerFailure(String key) {
        int[] count = failures.computeIfAbsent(key, k -> new int[1]);
        if (++count[0] >= MAX_ATTEMPTS) {
            lockedUntil.put(key, System.currentTimeMillis() + LOCK_MILLIS);
        }
    }
}
