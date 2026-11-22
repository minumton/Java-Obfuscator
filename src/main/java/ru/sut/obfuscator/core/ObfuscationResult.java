package ru.sut.obfuscator.core;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/**
 * Итог обфускации.
 *
 * @param outputDirectory каталог, в который записан результат
 * @param files           соответствие «исходный файл → записанный файл»; пути заданы
 *                        относительно входного каталога и каталога результата
 * @param renames         таблица «старое имя → новое имя»
 * @param keptNames       объявленные имена, которые были сохранены без изменений
 */
public record ObfuscationResult(Path outputDirectory, Map<Path, Path> files, Map<String, String> renames,
                                Set<String> keptNames) {
}
