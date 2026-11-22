package library.service;

import library.model.Book;
import library.model.Genre;
import library.model.Info;
import library.model.Reader;
import library.storage.MemoryRepository;
import library.storage.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Info(developer = "student", revision = 2)
public class Catalog {

    private final Repository<Book> books = new MemoryRepository<>();
    private final Map<String, Reader> readers = new TreeMap<>();
    private final Map<Book, Reader> loans = new LinkedHashMap<>();

    public void addBooks(Book... newBooks) {
        for (Book book : newBooks) {
            books.add(book);
        }
    }

    public Reader register(String name, int age) {
        Reader reader = new Reader(name, age);
        readers.put(name, reader);
        return reader;
    }

    public void lend(Book book, Reader reader) {
        if (!book.isAvailable()) {
            throw new LoanException(book, "already taken");
        }
        if (book.getGenre() == Genre.SCIENCE && !reader.isAdult()) {
            throw new LoanException(book, "for adults only");
        }
        book.setAvailable(false);
        loans.put(book, reader);
        reader.books().add(book);
    }

    public List<Book> all() {
        List<Book> result = new ArrayList<>();
        books.forEach(result::add);
        return result;
    }

    public Map<Genre, List<String>> titlesByGenre() {
        return all().stream()
                .collect(Collectors.groupingBy(Book::getGenre, TreeMap::new,
                        Collectors.mapping(Book::getTitle, Collectors.toList())));
    }

    public Optional<Book> oldest() {
        return all().stream().min(Comparator.naturalOrder());
    }

    public List<Book> search(Predicate<Book> filter) {
        return all().stream()
                .filter(filter)
                .sorted()
                .collect(Collectors.toList());
    }

    public long countAvailable() {
        return all().stream().filter(Book::isAvailable).count();
    }

    public String loanReport() {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<Book, Reader> loan : loans.entrySet()) {
            out.append(loan.getValue().name())
                    .append(" has ")
                    .append(loan.getKey().getTitle())
                    .append(" for ")
                    .append(loan.getKey().getGenre().loanDays())
                    .append(" days\n");
        }
        return out.toString();
    }
}
