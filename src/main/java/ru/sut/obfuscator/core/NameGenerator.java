package ru.sut.obfuscator.core;

import java.util.Set;

/**
 * Генератор новых имён для обфускации.
 *
 * <p>Имя строится из порядкового номера, записанного в пятеричной системе счисления
 * похожими друг на друга символами {@code I}, {@code l}, {@code 1}, {@code O}, {@code 0}.
 * Перед числом ставится буква, чтобы имя было допустимым идентификатором Java.</p>
 */
public class NameGenerator {

    /** «Цифры» пятеричной системы: символы, которые трудно различить глазами. */
    private static final char[] DIGITS = {'I', 'l', '1', 'O', '0'};

    /** Первая буква имени: идентификатор Java не может начинаться с цифры. */
    private static final char PREFIX = 'l';

    /** Минимальное количество «цифр» в имени, чтобы все имена выглядели одинаково. */
    private static final int MIN_DIGITS = 4;

    private final Set<String> forbiddenNames;
    private int counter;

    /**
     * Создаёт генератор.
     *
     * @param forbiddenNames имена, которые нельзя выдавать (уже встречаются в исходном коде)
     */
    public NameGenerator(Set<String> forbiddenNames) {
        this.forbiddenNames = Set.copyOf(forbiddenNames);
    }

    /**
     * Возвращает следующее свободное имя. Имена не повторяются
     * и не совпадают с запрещёнными.
     *
     * @return новое уникальное имя
     */
    public String next() {
        String name = encode(counter++);
        while (forbiddenNames.contains(name)) {
            name = encode(counter++);
        }
        return name;
    }

    /**
     * Превращает номер в имя: записывает его «цифрами» {@link #DIGITS}
     * и дополняет слева до минимальной длины.
     *
     * @param number неотрицательный порядковый номер
     * @return имя, соответствующее номеру
     */
    static String encode(int number) {
        StringBuilder digits = new StringBuilder();
        int rest = number;
        do {
            digits.append(DIGITS[rest % DIGITS.length]);
            rest /= DIGITS.length;
        } while (rest > 0);
        while (digits.length() < MIN_DIGITS) {
            digits.append(DIGITS[0]);
        }
        return PREFIX + digits.reverse().toString();
    }
}
