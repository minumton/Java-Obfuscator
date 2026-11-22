package ru.sut.obfuscator.core;

/**
 * Ошибка обфускации, которую нужно показать пользователю:
 * неверный путь, синтаксическая ошибка в исходном файле, сбой записи и т. п.
 */
public class ObfuscationException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Создаёт ошибку с сообщением.
     *
     * @param message понятное пользователю описание проблемы
     */
    public ObfuscationException(String message) {
        super(message);
    }

    /**
     * Создаёт ошибку с сообщением и исходной причиной.
     *
     * @param message понятное пользователю описание проблемы
     * @param cause   исключение, которое привело к ошибке
     */
    public ObfuscationException(String message, Throwable cause) {
        super(message, cause);
    }
}
