package ru.sut.obfuscator.core;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты текстового отчёта.
 */
class ReportFormatterTest {

    @Test
    void reportListsFilesRenamesAndKeptNames() {
        ObfuscationResult result = new ObfuscationResult(
                Path.of("out"),
                Map.of(Path.of("Main.java"), Path.of("lIIII.java")),
                Map.of("Main", "lIIII", "value", "lIIIl"),
                Set.of("main"));

        String report = new ReportFormatter().format(result);

        assertTrue(report.contains("Output directory: out"));
        assertTrue(report.contains("Main.java -> lIIII.java"));
        assertTrue(report.contains("value -> lIIIl"));
        assertTrue(report.contains("Renamed names (2)"));
        assertTrue(report.contains("Kept unchanged (1): main"));
    }
}
