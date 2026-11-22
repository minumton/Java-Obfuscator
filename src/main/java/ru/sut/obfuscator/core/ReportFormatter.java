package ru.sut.obfuscator.core;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Превращает итог обфускации в текстовый отчёт.
 * Используется и консольным, и графическим интерфейсом.
 */
public class ReportFormatter {

    private static final String INDENT = "  ";
    private static final String ARROW = " -> ";

    /**
     * Формирует отчёт: список файлов, таблица переименований и сохранённые имена.
     *
     * @param result итог обфускации
     * @return многострочный текст отчёта
     */
    public String format(ObfuscationResult result) {
        String lineBreak = System.lineSeparator();
        return "Output directory: " + result.outputDirectory() + lineBreak
                + "Files (" + result.files().size() + "):" + lineBreak
                + formatFiles(result.files()) + lineBreak
                + "Renamed names (" + result.renames().size() + "):" + lineBreak
                + formatRenames(result.renames()) + lineBreak
                + "Kept unchanged (" + result.keptNames().size() + "): " + String.join(", ", result.keptNames());
    }

    /**
     * Формирует строки вида «исходный файл → записанный файл».
     *
     * @param files соответствие исходных и записанных файлов
     * @return текст со списком файлов
     */
    private String formatFiles(Map<Path, Path> files) {
        return files.entrySet().stream()
                .map(file -> INDENT + file.getKey() + ARROW + file.getValue())
                .collect(Collectors.joining(System.lineSeparator()));
    }

    /**
     * Формирует строки вида «старое имя → новое имя».
     *
     * @param renames таблица переименований
     * @return текст с таблицей переименований
     */
    private String formatRenames(Map<String, String> renames) {
        return renames.entrySet().stream()
                .map(rename -> INDENT + rename.getKey() + ARROW + rename.getValue())
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
