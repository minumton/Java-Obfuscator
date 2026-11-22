package ru.sut.obfuscator.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Записывает обфусцированные файлы в каталог результата.
 * Если класс, по которому назван файл, был переименован, файл получает новое имя.
 */
public class SourceWriter {

    private static final Logger LOGGER = LogManager.getLogger(SourceWriter.class);

    /**
     * Записывает все файлы в каталог результата.
     *
     * @param sources         обработанные файлы
     * @param renames         таблица «старое имя → новое имя»
     * @param outputDirectory каталог результата (создаётся при необходимости)
     * @return соответствие «исходный файл → записанный файл» (пути относительно каталогов)
     * @throws ObfuscationException если два файла получают одно имя или запись не удалась
     */
    public Map<Path, Path> write(List<SourceFile> sources, Map<String, String> renames, Path outputDirectory)
            throws ObfuscationException {
        Map<Path, SourceFile> plan = planTargets(sources, renames);
        Map<Path, Path> written = new LinkedHashMap<>();
        for (Map.Entry<Path, SourceFile> target : plan.entrySet()) {
            writeFile(outputDirectory.resolve(target.getKey()), target.getValue().unit().toString());
            written.put(target.getValue().relativePath(), target.getKey());
        }
        return written;
    }

    /**
     * Определяет путь результата для каждого файла и проверяет, что пути не совпадают.
     * Проверка выполняется до записи, чтобы при ошибке не оставить результат наполовину готовым.
     *
     * @param sources обработанные файлы
     * @param renames таблица «старое имя → новое имя»
     * @return соответствие «путь результата относительно каталога → файл»
     * @throws ObfuscationException если два исходных файла дают один и тот же путь результата
     */
    private Map<Path, SourceFile> planTargets(List<SourceFile> sources, Map<String, String> renames)
            throws ObfuscationException {
        Map<Path, SourceFile> plan = new LinkedHashMap<>();
        for (SourceFile source : sources) {
            Path target = source.relativePath().resolveSibling(targetFileName(source, renames));
            SourceFile previous = plan.putIfAbsent(target, source);
            if (previous != null) {
                throw new ObfuscationException("Two input files produce the same output file " + target
                        + ": " + previous.path() + " and " + source.path());
            }
        }
        return plan;
    }

    /**
     * Вычисляет имя файла результата. Файл переименовывается, только если в нём
     * объявлен тип верхнего уровня с тем же именем, что и у файла.
     *
     * @param source  обработанный файл (имена в дереве уже заменены)
     * @param renames таблица «старое имя → новое имя»
     * @return имя файла результата с расширением
     */
    private String targetFileName(SourceFile source, Map<String, String> renames) {
        String fileName = source.path().getFileName().toString();
        String typeName = fileName.substring(0, fileName.length() - SourceLoader.JAVA_EXTENSION.length());
        String newTypeName = renames.getOrDefault(typeName, typeName);
        boolean declaresType = source.unit().getTypes().stream()
                .anyMatch(type -> type.getNameAsString().equals(newTypeName));
        return (declaresType ? newTypeName : typeName) + SourceLoader.JAVA_EXTENSION;
    }

    /**
     * Записывает текст в файл в кодировке UTF-8, создавая недостающие каталоги.
     *
     * @param target путь до файла
     * @param code   исходный код
     * @throws ObfuscationException если каталог или файл не удалось создать
     */
    private void writeFile(Path target, String code) throws ObfuscationException {
        try {
            Files.createDirectories(target.toAbsolutePath().getParent());
            Files.writeString(target, code, StandardCharsets.UTF_8);
            LOGGER.debug("Written {}", target);
        } catch (IOException e) {
            throw new ObfuscationException("Cannot write file: " + target, e);
        }
    }
}
