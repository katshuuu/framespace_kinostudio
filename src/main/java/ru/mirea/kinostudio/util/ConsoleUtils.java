package ru.mirea.kinostudio.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * Утилиты безопасного чтения пользовательского ввода из консоли.
 */
public final class ConsoleUtils {

    private ConsoleUtils() {
    }

    public static String readLine(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public static int readInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: значение должно быть целым числом. Повторите ввод.");
            }
        }
    }

    public static BigDecimal readBigDecimal(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().replace(",", ".");
            try {
                return new BigDecimal(input);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: значение должно быть числом. Повторите ввод.");
            }
        }
    }

    public static LocalDate readDate(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt + " (гггг-мм-дд): ");
            String input = scanner.nextLine().trim();
            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException e) {
                System.out.println("Ошибка: неверный формат даты. Пример: 2026-10-15");
            }
        }
    }
}
