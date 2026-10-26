package ru.mirea.kinostudio.api.auth;

import com.auth0.jwt.exceptions.JWTVerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mirea.kinostudio.exception.BusinessException;
import ru.mirea.kinostudio.exception.ConflictException;
import ru.mirea.kinostudio.repository.InMemoryUserStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTest {

    private static final String SECRET = "framespace-test-secret-key-32chars";

    private JwtService jwt;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        jwt = new JwtService(SECRET, 3600);
        auth = new AuthService(new InMemoryUserStore(), jwt, new PasswordHasher(4));
    }

    @Test
    @DisplayName("регистрация")
    void registrationReturnsBearerToken() throws Exception {
        AuthResponse response = auth.register("admin", "admin123", "Администратор");

        assertEquals("Bearer", response.tokenType());
        assertEquals(3600, response.expiresIn());
        assertEquals("admin", response.username());
        assertEquals("Администратор", response.fullName());
        assertEquals(Integer.toString(response.id()), jwt.verify(response.token()).getSubject());
        assertEquals("admin", jwt.verify(response.token()).getClaim("username").asString());
    }

    @Test
    @DisplayName("повторный вход")
    void repeatedLoginVerifiesBothTokens() throws Exception {
        auth.register("editor", "secret1", "Редактор");

        AuthResponse first = auth.login("editor", "secret1");
        AuthResponse second = auth.login("editor", "secret1");

        assertEquals("editor", jwt.verify(first.token()).getClaim("username").asString());
        assertEquals("editor", jwt.verify(second.token()).getClaim("username").asString());
    }

    @Test
    @DisplayName("дубликат логина")
    void duplicateUsernameIsRejected() throws Exception {
        auth.register("admin", "admin123", "Администратор");

        BusinessException error = assertThrows(BusinessException.class,
                () -> auth.register("admin", "other12", "Другой сотрудник"));

        assertEquals(ConflictException.class, error.getClass());
        assertEquals("Логин уже занят.", error.getMessage());
    }

    @Test
    @DisplayName("короткий пароль")
    void shortPasswordIsRejected() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> auth.register("admin", "12345", "Администратор"));

        assertEquals("Пароль должен содержать не меньше 6 символов.", error.getMessage());
    }

    @Test
    @DisplayName("неверный пароль")
    void wrongPasswordMatchesUnknownUserMessage() throws Exception {
        auth.register("admin", "admin123", "Администратор");

        BusinessException wrongPassword = assertThrows(BusinessException.class,
                () -> auth.login("admin", "nope12"));
        BusinessException unknownUser = assertThrows(BusinessException.class,
                () -> auth.login("ghost", "nope12"));

        assertEquals(AuthService.INVALID_CREDENTIALS, wrongPassword.getMessage());
        assertEquals(wrongPassword.getMessage(), unknownUser.getMessage());
    }

    @Test
    @DisplayName("токен с испорченной подписью")
    void corruptedSignatureIsRejected() throws Exception {
        String token = auth.register("admin", "admin123", "Администратор").token();
        int dot = token.lastIndexOf('.');
        String broken = token.substring(0, dot) + ".invalid-signature";

        assertThrows(JWTVerificationException.class, () -> jwt.verify(broken));
    }

    @Test
    void secretShorterThan32CharactersIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService("too-short", 3600));
    }
}
