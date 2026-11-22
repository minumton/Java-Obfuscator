package ru.sut.obfuscator;

import javafx.application.Application;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.sut.obfuscator.cli.ConsoleRunner;
import ru.sut.obfuscator.gui.ObfuscatorApp;

/**
 * Точка входа в приложение.
 *
 * <p>С аргументами программа работает в консольном режиме,
 * без аргументов открывает графический интерфейс.</p>
 */
public final class Main {

    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    /**
     * Закрытый конструктор: объекты этого класса не создаются.
     */
    private Main() {
    }

    /**
     * Запускает приложение.
     *
     * @param args аргументы командной строки: пути до файлов и каталогов, параметры
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            launchGui();
        } else {
            System.exit(new ConsoleRunner().run(args));
        }
    }

    /**
     * Открывает графический интерфейс. Если графическая среда недоступна
     * (например, на сервере без экрана), сообщает об этом и подсказывает консольный режим.
     */
    private static void launchGui() {
        try {
            Application.launch(ObfuscatorApp.class);
        } catch (RuntimeException | LinkageError e) {
            LOGGER.error("Graphical interface is not available: {}", e.toString());
            ConsoleRunner.printUsage();
            System.exit(ConsoleRunner.EXIT_FAILURE);
        }
    }
}
