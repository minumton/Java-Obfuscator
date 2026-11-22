package library.model;

import java.util.ArrayList;
import java.util.List;

public record Reader(String name, int age, List<Book> books) {

    public static final int ADULT_AGE = 18;

    public Reader {
        if (age < 0) {
            throw new IllegalArgumentException("Negative age: " + age);
        }
        books = new ArrayList<>(books);
    }

    public Reader(String name, int age) {
        this(name, age, new ArrayList<>());
    }

    public boolean isAdult() {
        return age >= ADULT_AGE;
    }

    public int count() {
        return books.size();
    }
}
