package ru.mirea.kinostudio.fx;

/**
 * Контроллер формы в диалоге. Метод save() проверяет введённые данные и сохраняет их;
 * возвращает false, если есть ошибки — тогда диалог остаётся открытым и показывает сообщение.
 */
public interface FormController {
    boolean save();
}
