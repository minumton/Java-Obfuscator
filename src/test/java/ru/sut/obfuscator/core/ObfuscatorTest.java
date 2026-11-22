package ru.sut.obfuscator.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты обфускатора на небольших исходных файлах.
 */
class ObfuscatorTest {

    @TempDir
    Path directory;

    @Test
    void classAndItsFileAreRenamed() throws Exception {
        write("Counter.java", "public class Counter { }");

        ObfuscationResult result = obfuscate();

        String newName = result.renames().get("Counter");
        assertEquals(Path.of(newName + ".java"), result.files().get(Path.of("Counter.java")));
        assertTrue(output(result, "Counter.java").contains("public class " + newName));
        assertFalse(Files.exists(outputDirectory().resolve("Counter.java")));
    }

    @Test
    void fieldsMethodsParametersAndLocalVariablesAreRenamed() throws Exception {
        write("Counter.java", """
                public class Counter {
                    private int value;

                    public int increase(int amount) {
                        int total = value + amount;
                        value = total;
                        return this.value;
                    }
                }
                """);

        ObfuscationResult result = obfuscate();

        String code = output(result, "Counter.java");
        for (String name : List.of("Counter", "value", "increase", "amount", "total")) {
            assertFalse(containsWord(code, name), "Name is not renamed: " + name);
        }
        assertEquals(5, result.renames().size());
    }

    @Test
    void entryPointAndJdkNamesAreKept() throws Exception {
        write("App.java", """
                public class App {
                    public static void main(String[] args) {
                        String text = "hello";
                        System.out.println(text.length());
                    }
                }
                """);

        ObfuscationResult result = obfuscate();

        String code = output(result, "App.java");
        assertTrue(code.contains("public static void main(String[] "));
        assertTrue(code.contains("System.out.println("));
        assertTrue(code.contains(".length()"));
        assertTrue(result.keptNames().contains("main"));
    }

    @Test
    void userMethodIsRenamedButJdkMethodWithSameNameIsKept() throws Exception {
        write("Stack.java", """
                import java.util.ArrayList;
                import java.util.List;

                public class Stack {
                    private final List<String> items = new ArrayList<>();

                    public int size() {
                        return items.size();
                    }

                    public void add(String item) {
                        items.add(item);
                    }

                    public int doubleSize() {
                        return size() * 2;
                    }
                }
                """);

        ObfuscationResult result = obfuscate();

        String code = output(result, "Stack.java");
        String items = result.renames().get("items");
        String size = result.renames().get("size");
        assertTrue(code.contains(items + ".size()"), "JDK method List.size() must be kept");
        assertTrue(code.contains(items + ".add("), "JDK method List.add() must be kept");
        assertTrue(code.contains("public int " + size + "()"));
        assertTrue(code.contains("return " + size + "() * 2"));
    }

    @Test
    void methodsOverridingJdkMethodsAreKept() throws Exception {
        write("Job.java", """
                public class Job implements Runnable, Comparable<Job> {
                    private int priority;

                    @Override
                    public void run() {
                        prepare();
                    }

                    private void prepare() {
                        priority++;
                    }

                    @Override
                    public int compareTo(Job other) {
                        return Integer.compare(priority, other.priority);
                    }

                    @Override
                    public String toString() {
                        return "job";
                    }
                }
                """);

        ObfuscationResult result = obfuscate();

        assertTrue(result.keptNames().containsAll(List.of("run", "compareTo", "toString")));
        assertTrue(result.renames().containsKey("prepare"));
        assertTrue(output(result, "Job.java").contains("public void run()"));
    }

    @Test
    void enumConstantsAreKeptButEnumIsRenamed() throws Exception {
        write("Color.java", "public enum Color { RED, GREEN; Color other() { return this == RED ? GREEN : RED; } }");

        ObfuscationResult result = obfuscate();

        String code = output(result, "Color.java");
        assertTrue(code.contains("RED") && code.contains("GREEN"));
        assertTrue(result.renames().containsKey("Color"));
        assertTrue(result.renames().containsKey("other"));
    }

    @Test
    void commentsAreRemovedAndStringLiteralsAreKept() throws Exception {
        write("Note.java", """
                /** Secret documentation. */
                public class Note {
                    // secret comment
                    String value = "value stays"; /* another secret */
                }
                """);

        ObfuscationResult result = obfuscate();

        String code = output(result, "Note.java");
        assertFalse(code.toLowerCase().contains("secret"));
        assertTrue(code.contains("\"value stays\""));
    }

    @Test
    void usagesInOtherFilesAndImportsAreRenamed() throws Exception {
        write("model/Item.java", """
                package model;

                public class Item {
                    public static final int LIMIT = 3;

                    public static int twice(int number) {
                        return number * 2;
                    }
                }
                """);
        write("app/Shop.java", """
                package app;

                import model.Item;

                import static model.Item.LIMIT;
                import static model.Item.twice;

                public class Shop {
                    Item item = new Item();
                    int count = twice(LIMIT);
                }
                """);

        ObfuscationResult result = obfuscate();

        String item = result.renames().get("Item");
        String code = output(result, "app/Shop.java");
        assertTrue(code.contains("import model." + item + ";"));
        assertTrue(code.contains("import static model." + item + "." + result.renames().get("LIMIT") + ";"));
        assertTrue(code.contains("import static model." + item + "." + result.renames().get("twice") + ";"));
        assertTrue(code.contains("new " + item + "()"));
        assertEquals(Path.of("model", item + ".java"), result.files().get(Path.of("model", "Item.java")));
    }

    @Test
    void nameIsKeptWhenItsUsageCannotBeResolved() throws Exception {
        write("Client.java", """
                import unknown.Library;

                public class Client {
                    private final Library library = new Library();

                    void process() {
                        library.process();
                    }
                }
                """);

        ObfuscationResult result = obfuscate();

        assertTrue(result.keptNames().contains("process"));
        assertTrue(result.renames().containsKey("library"));
        assertTrue(output(result, "Client.java").contains(".process()"));
    }

    @Test
    void resultIsDeterministic() throws Exception {
        write("Counter.java", "public class Counter { int value; int next() { return ++value; } }");

        ObfuscationResult first = new Obfuscator().obfuscate(List.of(inputDirectory()), directory.resolve("first"));
        ObfuscationResult second = new Obfuscator().obfuscate(List.of(inputDirectory()), directory.resolve("second"));

        assertEquals(first.renames(), second.renames());
        Path file = first.files().get(Path.of("Counter.java"));
        assertEquals(Files.readString(directory.resolve("first").resolve(file)),
                Files.readString(directory.resolve("second").resolve(file)));
    }

    @Test
    void sourceFilesAreNotModified() throws Exception {
        String code = "public class Counter { int value; }";
        Path file = write("Counter.java", code);

        obfuscate();

        assertEquals(code, Files.readString(file));
    }

    @Test
    void nothingIsWrittenWhenInputHasSyntaxError() throws Exception {
        write("Good.java", "public class Good { }");
        write("Broken.java", "public class Broken { int }");

        assertThrows(ObfuscationException.class, this::obfuscate);
        assertFalse(Files.exists(outputDirectory()));
    }

    @Test
    void filesProducingSameOutputAreRejected() throws Exception {
        Path first = write("a/Main.java", "public class Main { }");
        Path second = write("b/Main.java", "public class Main { }");

        ObfuscationException error = assertThrows(ObfuscationException.class,
                () -> new Obfuscator().obfuscate(List.of(first, second), outputDirectory()));
        assertTrue(error.getMessage().contains("same output file"));
    }

    private ObfuscationResult obfuscate() throws ObfuscationException {
        return new Obfuscator().obfuscate(List.of(inputDirectory()), outputDirectory());
    }

    private Path inputDirectory() {
        return directory.resolve("input");
    }

    private Path outputDirectory() {
        return directory.resolve("output");
    }

    private Path write(String relativePath, String code) throws IOException {
        Path file = inputDirectory().resolve(relativePath);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, code);
    }

    private String output(ObfuscationResult result, String sourceFile) throws IOException {
        return Files.readString(outputDirectory().resolve(result.files().get(Path.of(sourceFile))));
    }

    private static boolean containsWord(String text, String word) {
        return Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(text).find();
    }
}
