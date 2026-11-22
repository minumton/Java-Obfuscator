package ru.sut.obfuscator.core;

import org.junit.jupiter.api.Test;

import javax.lang.model.SourceVersion;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты генератора имён.
 */
class NameGeneratorTest {

    private static final int NAME_COUNT = 2000;

    @Test
    void generatedNamesAreValidJavaIdentifiers() {
        NameGenerator generator = new NameGenerator(Set.of());
        for (int index = 0; index < NAME_COUNT; index++) {
            String name = generator.next();
            assertTrue(SourceVersion.isName(name), "Not a valid identifier: " + name);
        }
    }

    @Test
    void generatedNamesAreUniqueEvenIgnoringCase() {
        NameGenerator generator = new NameGenerator(Set.of());
        Set<String> names = new HashSet<>();
        for (int index = 0; index < NAME_COUNT; index++) {
            names.add(generator.next().toLowerCase(Locale.ROOT));
        }
        assertEquals(NAME_COUNT, names.size());
    }

    @Test
    void forbiddenNamesAreSkipped() {
        String first = NameGenerator.encode(0);
        String second = NameGenerator.encode(1);
        NameGenerator generator = new NameGenerator(Set.of(first, second));
        String name = generator.next();
        assertNotEquals(first, name);
        assertNotEquals(second, name);
        assertEquals(NameGenerator.encode(2), name);
    }

    @Test
    void generationIsDeterministic() {
        NameGenerator first = new NameGenerator(Set.of());
        NameGenerator second = new NameGenerator(Set.of());
        for (int index = 0; index < NAME_COUNT; index++) {
            assertEquals(first.next(), second.next());
        }
    }

    @Test
    void namesConsistOnlyOfConfusingCharacters() {
        NameGenerator generator = new NameGenerator(Set.of());
        for (int index = 0; index < NAME_COUNT; index++) {
            assertTrue(generator.next().matches("[Il1O0]+"));
        }
    }
}
