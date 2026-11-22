package ru.sut.obfuscator;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Вспомогательный класс для тестов: компилирует каталог с исходным кодом
 * и запускает программу, возвращая её вывод.
 */
public final class JavaProgram {

    private JavaProgram() {
    }

    /**
     * Проверяет, доступен ли компилятор (тесты запущены на JDK, а не на JRE).
     *
     * @return {@code true}, если программы можно компилировать
     */
    public static boolean isCompilerAvailable() {
        return ToolProvider.getSystemJavaCompiler() != null;
    }

    /**
     * Компилирует все java-файлы каталога. Ошибка компиляции проваливает тест.
     *
     * @param sourceDirectory  каталог с исходным кодом
     * @param classesDirectory каталог для class-файлов
     * @throws IOException если файлы не удалось прочитать
     */
    public static void compile(Path sourceDirectory, Path classesDirectory) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        Files.createDirectories(classesDirectory);
        List<String> options = List.of("-d", classesDirectory.toString(), "-encoding", "UTF-8", "-nowarn");
        try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);
             Stream<Path> paths = Files.walk(sourceDirectory)) {
            List<Path> sources = paths.filter(path -> path.toString().endsWith(".java")).toList();
            boolean compiled = compiler.getTask(null, files, diagnostics, options, null,
                    files.getJavaFileObjectsFromPaths(sources)).call();
            assertTrue(compiled, () -> "Compilation failed: " + diagnostics.getDiagnostics());
        }
    }

    /**
     * Запускает метод {@code main} скомпилированной программы и перехватывает её вывод.
     *
     * @param classesDirectory каталог с class-файлами
     * @param mainClass        полное имя класса с методом {@code main}
     * @return всё, что программа напечатала в стандартный вывод
     * @throws Exception если программу не удалось запустить или она завершилась с ошибкой
     */
    public static String run(Path classesDirectory, String mainClass) throws Exception {
        PrintStream originalOutput = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        URL[] classPath = {classesDirectory.toUri().toURL()};
        try (URLClassLoader loader = new URLClassLoader(classPath, ClassLoader.getPlatformClassLoader())) {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            Method main = loader.loadClass(mainClass).getMethod("main", String[].class);
            main.invoke(null, (Object) new String[0]);
        } finally {
            System.setOut(originalOutput);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
