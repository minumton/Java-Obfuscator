package ru.sut.obfuscator.core;

import com.github.javaparser.ast.CompilationUnit;

import java.nio.file.Path;

/**
 * Один исходный java-файл, разобранный в синтаксическое дерево.
 *
 * @param path         абсолютный путь до исходного файла
 * @param relativePath путь относительно входного каталога (для отдельного файла — только имя);
 *                     по нему строится путь в каталоге результата
 * @param unit         синтаксическое дерево файла
 */
public record SourceFile(Path path, Path relativePath, CompilationUnit unit) {
}
