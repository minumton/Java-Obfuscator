package ru.sut.obfuscator.cli;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.sut.obfuscator.core.ObfuscationException;
import ru.sut.obfuscator.core.ObfuscationResult;
import ru.sut.obfuscator.core.Obfuscator;
import ru.sut.obfuscator.core.ReportFormatter;

/**
 * Запускает обфускацию из командной строки и сообщает о результате.
 */
public class ConsoleRunner {

    /** Код возврата при успешном завершении. */
    public static final int EXIT_SUCCESS = 0;

    /** Код возврата при ошибке. */
    public static final int EXIT_FAILURE = 1;

    private static final Logger LOGGER = LogManager.getLogger(ConsoleRunner.class);

    private static final String USAGE = String.join(System.lineSeparator(),
            "Usage: java -jar java-obfuscator.jar [options] <path>...",
            "",
            "  <path>               .java file or directory with .java files (searched recursively)",
            "  -o, --output <dir>   output directory (default: " + CliParser.DEFAULT_OUTPUT + ")",
            "  -h, --help           show this help",
            "",
            "Run without arguments to open the graphical interface.");

    /**
     * Печатает справку по использованию программы.
     */
    public static void printUsage() {
        System.out.println(USAGE);
    }

    /**
     * Выполняет программу в консольном режиме. Никогда не бросает исключений:
     * любая ошибка превращается в сообщение и код возврата.
     *
     * @param args аргументы командной строки
     * @return код возврата: {@link #EXIT_SUCCESS} или {@link #EXIT_FAILURE}
     */
    public int run(String[] args) {
        try {
            CliOptions options = new CliParser().parse(args);
            if (options.helpRequested()) {
                printUsage();
                return EXIT_SUCCESS;
            }
            return obfuscate(options);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid arguments: {}", e.getMessage());
            printUsage();
            return EXIT_FAILURE;
        }
    }

    /**
     * Запускает обфускацию и выводит отчёт.
     *
     * @param options параметры запуска
     * @return код возврата
     */
    private int obfuscate(CliOptions options) {
        try {
            ObfuscationResult result = new Obfuscator().obfuscate(options.inputs(), options.outputDirectory());
            LOGGER.info("Obfuscation finished{}{}", System.lineSeparator(), new ReportFormatter().format(result));
            return EXIT_SUCCESS;
        } catch (ObfuscationException e) {
            LOGGER.error(e.getMessage());
            LOGGER.debug("Error details", e);
            return EXIT_FAILURE;
        } catch (RuntimeException e) {
            LOGGER.error("Unexpected error: {}", e.toString());
            LOGGER.debug("Error details", e);
            return EXIT_FAILURE;
        }
    }
}
