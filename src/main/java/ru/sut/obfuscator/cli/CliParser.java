package ru.sut.obfuscator.cli;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Разбирает аргументы командной строки.
 */
public class CliParser {

    /** Каталог результата по умолчанию. */
    public static final String DEFAULT_OUTPUT = "obfuscated";

    private static final Set<String> HELP_OPTIONS = Set.of("-h", "--help");
    private static final Set<String> OUTPUT_OPTIONS = Set.of("-o", "--output");
    private static final String OPTION_PREFIX = "-";

    /**
     * Разбирает аргументы.
     *
     * @param args аргументы командной строки
     * @return разобранные параметры запуска
     * @throws IllegalArgumentException если аргументы заданы неверно
     */
    public CliOptions parse(String[] args) {
        List<Path> inputs = new ArrayList<>();
        Path output = Path.of(DEFAULT_OUTPUT);
        boolean help = false;
        Iterator<String> arguments = Arrays.asList(args).iterator();
        while (arguments.hasNext()) {
            String argument = arguments.next();
            if (HELP_OPTIONS.contains(argument)) {
                help = true;
            } else if (OUTPUT_OPTIONS.contains(argument)) {
                output = toPath(optionValue(argument, arguments));
            } else if (argument.startsWith(OPTION_PREFIX)) {
                throw new IllegalArgumentException("Unknown option: " + argument);
            } else {
                inputs.add(toPath(argument));
            }
        }
        if (!help && inputs.isEmpty()) {
            throw new IllegalArgumentException("No input files or directories given");
        }
        return new CliOptions(inputs, output, help);
    }

    /**
     * Возвращает значение параметра — следующий аргумент после него.
     *
     * @param option    имя параметра
     * @param arguments оставшиеся аргументы
     * @return значение параметра
     * @throws IllegalArgumentException если значение не указано
     */
    private String optionValue(String option, Iterator<String> arguments) {
        if (!arguments.hasNext()) {
            throw new IllegalArgumentException("Option " + option + " requires a directory");
        }
        return arguments.next();
    }

    /**
     * Преобразует строку в путь.
     *
     * @param text строка с путём
     * @return путь
     * @throws IllegalArgumentException если строка содержит недопустимые для пути символы
     */
    private Path toPath(String text) {
        try {
            return Path.of(text);
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("Invalid path: " + text, e);
        }
    }
}
