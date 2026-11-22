package ru.sut.obfuscator;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.sut.obfuscator.core.ObfuscationResult;
import ru.sut.obfuscator.core.Obfuscator;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Главная проверка задания: обфусцированная программа компилируется
 * и выводит то же самое, что и исходная.
 */
class ExampleProgramsTest {

    @TempDir
    Path directory;

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "examples/simple, Calculator",
            "examples/bank, bank.Main",
            "examples/shapes, Shapes",
            "examples/library, library.LibraryApp",
            "src/test/resources/features, Features"
    })
    void obfuscatedProgramPrintsSameOutputAsOriginal(String sourceDirectory, String mainClass) throws Exception {
        assumeTrue(JavaProgram.isCompilerAvailable(), "JDK is required to compile example programs");
        String expected = compileAndRun(Path.of(sourceDirectory), "original", mainClass);
        assertFalse(expected.isBlank());

        ObfuscationResult first = obfuscate(Path.of(sourceDirectory), "obfuscated");
        String firstMain = renamed(mainClass, first.renames());
        assertEquals(expected, compileAndRun(first.outputDirectory(), "obfuscated-classes", firstMain));

        ObfuscationResult second = obfuscate(first.outputDirectory(), "obfuscated-twice");
        String secondMain = renamed(firstMain, second.renames());
        assertEquals(expected, compileAndRun(second.outputDirectory(), "obfuscated-twice-classes", secondMain));
    }

    private ObfuscationResult obfuscate(Path sourceDirectory, String outputName) throws Exception {
        return new Obfuscator().obfuscate(List.of(sourceDirectory), directory.resolve(outputName));
    }

    private String compileAndRun(Path sourceDirectory, String classesName, String mainClass) throws Exception {
        Path classes = directory.resolve(classesName);
        JavaProgram.compile(sourceDirectory, classes);
        return JavaProgram.run(classes, mainClass);
    }

    /**
     * Возвращает имя класса после обфускации: пакет сохраняется, простое имя заменяется.
     */
    private static String renamed(String className, Map<String, String> renames) {
        int separator = className.lastIndexOf('.');
        String simpleName = className.substring(separator + 1);
        return className.substring(0, separator + 1) + renames.getOrDefault(simpleName, simpleName);
    }
}
