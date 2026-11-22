package ru.sut.obfuscator.core;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.MemoryTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Находит исходные java-файлы по переданным путям и разбирает их в синтаксические деревья.
 *
 * <p>Все типы из загруженных файлов регистрируются в {@link TypeSolver}, чтобы при анализе
 * можно было отличить пользовательские классы от классов JDK.</p>
 */
public class SourceLoader {

    /** Расширение обрабатываемых файлов. */
    public static final String JAVA_EXTENSION = ".java";

    private static final Logger LOGGER = LogManager.getLogger(SourceLoader.class);
    private static final String BYTE_ORDER_MARK = "﻿";

    /** С этих слов в сообщении парсера начинается перечень ожидаемых лексем. */
    private static final String EXPECTED_TOKENS_MARKER = ", expected";

    /** Искать внешние классы не только в JDK, но и в библиотеках самого приложения (JavaFX, Log4j). */
    private static final boolean JDK_CLASSES_ONLY = false;

    private final MemoryTypeSolver userTypeSolver = new MemoryTypeSolver();
    private final TypeSolver typeSolver =
            new CombinedTypeSolver(userTypeSolver, new ReflectionTypeSolver(JDK_CLASSES_ONLY));
    private final JavaParser parser = new JavaParser(createConfiguration());

    /**
     * Возвращает объект, который умеет находить объявления типов:
     * пользовательских (из загруженных файлов) и стандартных (из JDK).
     *
     * @return общий для всех загруженных файлов {@link TypeSolver}
     */
    public TypeSolver getTypeSolver() {
        return typeSolver;
    }

    /**
     * Загружает и разбирает все java-файлы по указанным путям.
     *
     * @param inputs          файлы {@code *.java} и/или каталоги (просматриваются рекурсивно)
     * @param outputDirectory каталог результата; файлы внутри него пропускаются,
     *                        чтобы не обфусцировать собственный вывод повторно
     * @return список разобранных файлов
     * @throws ObfuscationException если путь не найден, файлов нет или в файле синтаксическая ошибка
     */
    public List<SourceFile> load(List<Path> inputs, Path outputDirectory) throws ObfuscationException {
        Map<Path, Path> files = collectFiles(inputs, outputDirectory.toAbsolutePath().normalize());
        if (files.isEmpty()) {
            throw new ObfuscationException("No .java files found in the given paths");
        }
        List<SourceFile> sources = new ArrayList<>();
        for (Map.Entry<Path, Path> file : files.entrySet()) {
            sources.add(parse(file.getKey(), file.getValue()));
        }
        sources.forEach(this::registerTypes);
        LOGGER.info("Loaded {} source file(s)", sources.size());
        return sources;
    }

    /**
     * Создаёт настройки парсера: самая новая версия языка, кодировка UTF-8
     * и подключённый механизм разрешения имён.
     *
     * @return настройки для {@link JavaParser}
     */
    private ParserConfiguration createConfiguration() {
        return new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE)
                .setCharacterEncoding(StandardCharsets.UTF_8)
                .setSymbolResolver(new JavaSymbolSolver(typeSolver));
    }

    /**
     * Собирает java-файлы по всем входным путям без повторов.
     *
     * @param inputs входные пути
     * @param output нормализованный абсолютный путь каталога результата
     * @return соответствие «абсолютный путь файла → относительный путь для результата»
     * @throws ObfuscationException если путь не существует или указан файл не {@code *.java}
     */
    private Map<Path, Path> collectFiles(List<Path> inputs, Path output) throws ObfuscationException {
        Map<Path, Path> files = new LinkedHashMap<>();
        for (Path input : inputs) {
            Path absolute = input.toAbsolutePath().normalize();
            if (Files.isDirectory(absolute)) {
                findJavaFiles(absolute, output).forEach(file -> files.putIfAbsent(file, absolute.relativize(file)));
            } else if (Files.isRegularFile(absolute)) {
                requireJavaFile(absolute);
                files.putIfAbsent(absolute, absolute.getFileName());
            } else {
                throw new ObfuscationException("Path does not exist: " + input);
            }
        }
        return files;
    }

    /**
     * Рекурсивно ищет java-файлы в каталоге.
     *
     * @param directory каталог для поиска
     * @param output    каталог результата, содержимое которого пропускается
     * @return отсортированный список найденных файлов
     * @throws ObfuscationException если каталог не удалось прочитать
     */
    private List<Path> findJavaFiles(Path directory, Path output) throws ObfuscationException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                    .filter(SourceLoader::isJavaFile)
                    .filter(file -> !file.startsWith(output))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException | UncheckedIOException e) {
            throw new ObfuscationException("Cannot read directory: " + directory, e);
        }
    }

    /**
     * Проверяет, что файл имеет расширение {@code .java}.
     *
     * @param file проверяемый файл
     * @return {@code true}, если это исходный файл Java
     */
    private static boolean isJavaFile(Path file) {
        return file.getFileName().toString().endsWith(JAVA_EXTENSION);
    }

    /**
     * Убеждается, что явно указанный файл является исходным файлом Java.
     *
     * @param file проверяемый файл
     * @throws ObfuscationException если расширение файла не {@code .java}
     */
    private void requireJavaFile(Path file) throws ObfuscationException {
        if (!isJavaFile(file)) {
            throw new ObfuscationException("Not a .java file: " + file);
        }
    }

    /**
     * Читает файл и разбирает его в синтаксическое дерево.
     *
     * @param file         абсолютный путь до файла
     * @param relativePath относительный путь для каталога результата
     * @return разобранный файл
     * @throws ObfuscationException если файл не читается или содержит синтаксические ошибки
     */
    private SourceFile parse(Path file, Path relativePath) throws ObfuscationException {
        ParseResult<CompilationUnit> result = parser.parse(readText(file));
        if (!result.isSuccessful() || result.getResult().isEmpty()) {
            throw new ObfuscationException(describeProblems(file, result.getProblems()));
        }
        LOGGER.debug("Parsed {}", file);
        return new SourceFile(file, relativePath, result.getResult().get());
    }

    /**
     * Читает текст файла в кодировке UTF-8.
     *
     * @param file путь до файла
     * @return содержимое файла без метки порядка байтов
     * @throws ObfuscationException если файл недоступен или сохранён в другой кодировке
     */
    private String readText(Path file) throws ObfuscationException {
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            return text.startsWith(BYTE_ORDER_MARK) ? text.substring(1) : text;
        } catch (CharacterCodingException e) {
            throw new ObfuscationException("File is not in UTF-8 encoding: " + file, e);
        } catch (IOException e) {
            throw new ObfuscationException("Cannot read file: " + file, e);
        }
    }

    /**
     * Формирует сообщение о синтаксических ошибках в файле.
     *
     * @param file     файл с ошибками
     * @param problems проблемы, найденные парсером
     * @return текст сообщения со списком ошибок
     */
    private String describeProblems(Path file, List<Problem> problems) {
        String details = problems.stream()
                .map(SourceLoader::describeProblem)
                .collect(Collectors.joining("; "));
        return "Syntax error in " + file + ": " + details;
    }

    /**
     * Кратко описывает одну проблему парсера: номер строки и начало сообщения
     * (без длинного списка ожидаемых лексем).
     *
     * @param problem проблема, найденная парсером
     * @return краткое описание
     */
    private static String describeProblem(Problem problem) {
        String line = problem.getLocation()
                .flatMap(TokenRange::toRange)
                .map(range -> "line " + range.begin.line + ": ")
                .orElse("");
        String summary = problem.getMessage().lines().findFirst().orElse("invalid syntax");
        int details = summary.indexOf(EXPECTED_TOKENS_MARKER);
        return line + (details < 0 ? summary : summary.substring(0, details));
    }

    /**
     * Регистрирует все типы файла (включая вложенные) как пользовательские.
     *
     * @param source разобранный файл
     */
    private void registerTypes(SourceFile source) {
        for (TypeDeclaration<?> type : source.unit().findAll(TypeDeclaration.class)) {
            type.getFullyQualifiedName().ifPresent(name -> userTypeSolver.addDeclaration(name, type.resolve()));
        }
    }
}
