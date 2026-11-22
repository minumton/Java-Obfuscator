package ru.sut.obfuscator.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты поиска и разбора исходных файлов.
 */
class SourceLoaderTest {

    private static final String VALID_CLASS = "public class A { }";

    @TempDir
    Path directory;

    @Test
    void singleFileIsLoadedWithItsNameAsRelativePath() throws Exception {
        Path file = write("A.java", VALID_CLASS);

        List<SourceFile> sources = load(file);

        assertEquals(1, sources.size());
        assertEquals(Path.of("A.java"), sources.get(0).relativePath());
        assertEquals("A", sources.get(0).unit().getType(0).getNameAsString());
    }

    @Test
    void directoryIsSearchedRecursivelyAndOnlyJavaFilesAreTaken() throws Exception {
        write("A.java", VALID_CLASS);
        write("pkg/B.java", "package pkg; class B { }");
        write("pkg/notes.txt", "not java");

        List<SourceFile> sources = load(directory);

        assertEquals(List.of(Path.of("A.java"), Path.of("pkg", "B.java")),
                sources.stream().map(SourceFile::relativePath).toList());
    }

    @Test
    void filesInsideOutputDirectoryAreSkipped() throws Exception {
        write("A.java", VALID_CLASS);
        write("out/Old.java", "class Old { }");

        List<SourceFile> sources = new SourceLoader().load(List.of(directory), directory.resolve("out"));

        assertEquals(1, sources.size());
    }

    @Test
    void fileGivenTwiceIsLoadedOnce() throws Exception {
        Path file = write("A.java", VALID_CLASS);

        assertEquals(1, load(directory, file).size());
    }

    @Test
    void byteOrderMarkIsIgnored() throws Exception {
        Path file = write("A.java", "﻿" + VALID_CLASS);

        assertEquals(1, load(file).size());
    }

    @Test
    void missingPathIsReported() {
        ObfuscationException error = assertThrows(ObfuscationException.class, () -> load(directory.resolve("no")));
        assertTrue(error.getMessage().contains("does not exist"));
    }

    @Test
    void fileWithOtherExtensionIsRejected() throws Exception {
        Path file = write("notes.txt", "text");

        ObfuscationException error = assertThrows(ObfuscationException.class, () -> load(file));
        assertTrue(error.getMessage().contains("Not a .java file"));
    }

    @Test
    void directoryWithoutJavaFilesIsRejected() {
        ObfuscationException error = assertThrows(ObfuscationException.class, () -> load(directory));
        assertTrue(error.getMessage().contains("No .java files"));
    }

    @Test
    void syntaxErrorIsReportedWithFileAndLine() throws Exception {
        Path file = write("Broken.java", "public class Broken {\n    void run( {\n}");

        ObfuscationException error = assertThrows(ObfuscationException.class, () -> load(file));
        assertTrue(error.getMessage().contains("Syntax error"));
        assertTrue(error.getMessage().contains("Broken.java"));
        assertTrue(error.getMessage().contains("line 2"));
    }

    @Test
    void fileInWrongEncodingIsRejected() throws Exception {
        Path file = directory.resolve("A.java");
        Files.write(file, "class A { String s = \"Привет\"; }".getBytes(Charset.forName("windows-1251")));

        ObfuscationException error = assertThrows(ObfuscationException.class, () -> load(file));
        assertTrue(error.getMessage().contains("UTF-8"));
    }

    private List<SourceFile> load(Path... inputs) throws ObfuscationException {
        return new SourceLoader().load(List.of(inputs), directory.resolve("unused-output"));
    }

    private Path write(String relativePath, String text) throws IOException {
        Path file = directory.resolve(relativePath);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, text);
    }
}
