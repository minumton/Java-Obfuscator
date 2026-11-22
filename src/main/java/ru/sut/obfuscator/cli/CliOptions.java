package ru.sut.obfuscator.cli;

import java.nio.file.Path;
import java.util.List;

/**
 * Разобранные аргументы командной строки.
 *
 * @param inputs          входные файлы и каталоги
 * @param outputDirectory каталог результата
 * @param helpRequested   {@code true}, если пользователь запросил справку
 */
public record CliOptions(List<Path> inputs, Path outputDirectory, boolean helpRequested) {
}
