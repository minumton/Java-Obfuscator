package ru.sut.obfuscator.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Тесты консольного запуска: проверяются коды возврата и отсутствие аварийных завершений.
 */
class ConsoleRunnerTest {

    @TempDir
    Path directory;

    private final ConsoleRunner runner = new ConsoleRunner();

    @Test
    void helpReturnsSuccess() {
        assertEquals(ConsoleRunner.EXIT_SUCCESS, runner.run(new String[]{"--help"}));
    }

    @Test
    void validFileIsObfuscated() throws Exception {
        Path source = Files.writeString(directory.resolve("Hello.java"), "public class Hello { int value; }");
        Path output = directory.resolve("out");

        int code = runner.run(new String[]{"-o", output.toString(), source.toString()});

        assertEquals(ConsoleRunner.EXIT_SUCCESS, code);
        try (Stream<Path> files = Files.list(output)) {
            assertEquals(1, files.count());
        }
        assertFalse(Files.exists(output.resolve("Hello.java")));
    }

    @Test
    void unknownOptionReturnsFailure() {
        assertEquals(ConsoleRunner.EXIT_FAILURE, runner.run(new String[]{"--unknown"}));
    }

    @Test
    void missingFileReturnsFailure() {
        String missing = directory.resolve("Missing.java").toString();

        assertEquals(ConsoleRunner.EXIT_FAILURE, runner.run(new String[]{missing}));
    }

    @Test
    void syntaxErrorReturnsFailure() throws Exception {
        Path source = Files.writeString(directory.resolve("Broken.java"), "class Broken { void }");

        assertEquals(ConsoleRunner.EXIT_FAILURE, runner.run(new String[]{source.toString()}));
    }

    @Test
    void unwritableOutputReturnsFailure() throws Exception {
        Path source = Files.writeString(directory.resolve("Hello.java"), "public class Hello { }");
        Path occupied = Files.writeString(directory.resolve("occupied"), "this is a file, not a directory");

        int code = runner.run(new String[]{"-o", occupied.resolve("out").toString(), source.toString()});

        assertEquals(ConsoleRunner.EXIT_FAILURE, code);
    }
}
