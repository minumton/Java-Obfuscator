package ru.sut.obfuscator.cli;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты разбора аргументов командной строки.
 */
class CliParserTest {

    private final CliParser parser = new CliParser();

    @Test
    void inputsAreCollectedAndDefaultOutputIsUsed() {
        CliOptions options = parser.parse(new String[]{"A.java", "src"});

        assertEquals(List.of(Path.of("A.java"), Path.of("src")), options.inputs());
        assertEquals(Path.of(CliParser.DEFAULT_OUTPUT), options.outputDirectory());
        assertFalse(options.helpRequested());
    }

    @Test
    void outputDirectoryIsReadFromShortAndLongOption() {
        assertEquals(Path.of("out"), parser.parse(new String[]{"-o", "out", "A.java"}).outputDirectory());
        assertEquals(Path.of("result"), parser.parse(new String[]{"A.java", "--output", "result"}).outputDirectory());
    }

    @Test
    void helpDoesNotRequireInputs() {
        assertTrue(parser.parse(new String[]{"--help"}).helpRequested());
        assertTrue(parser.parse(new String[]{"-h"}).helpRequested());
    }

    @Test
    void unknownOptionIsRejected() {
        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> parser.parse(new String[]{"--fast", "A.java"}));
        assertTrue(error.getMessage().contains("--fast"));
    }

    @Test
    void outputOptionWithoutValueIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(new String[]{"A.java", "-o"}));
    }

    @Test
    void missingInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(new String[]{"-o", "out"}));
    }

    @Test
    void invalidPathIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(new String[]{"bad\u0000path"}));
    }
}
