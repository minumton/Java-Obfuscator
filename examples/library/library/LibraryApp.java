package library;

import library.model.Book;
import library.model.Genre;
import library.model.Info;
import library.model.Reader;
import library.service.Catalog;
import library.service.LoanException;
import library.storage.MemoryRepository;
import library.storage.Repository;
import library.util.Session;
import library.util.Texts;
import library.util.Texts.Pair;
import library.util.Worker;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static library.util.Texts.SEPARATOR;
import static library.util.Texts.join;

/**
 * Демонстрационная программа: библиотека. Здесь собраны разные возможности языка,
 * включая методы, названные так же, как методы JDK (add, get, size, format, join).
 */
public class LibraryApp {

    @FunctionalInterface
    interface Formatter<T> {
        String apply(T value);

        default Formatter<T> framed() {
            return value -> "[" + apply(value) + "]";
        }
    }

    private final Catalog catalog = new Catalog();
    private int step;
    private int attempts;

    public static void main(String[] args) throws InterruptedException {
        LibraryApp app = new LibraryApp();
        app.fillCatalog();
        app.showBooks();
        app.showLoans();
        app.showUtilities();
        app.showConcurrency();
        System.out.println("Steps: " + app.step + ", lend attempts: " + app.attempts);
    }

    private void header(String title) {
        step++;
        System.out.println("== " + step + ". " + title + " ==");
    }

    private void fillCatalog() {
        catalog.addBooks(
                Book.builder("War and Peace").author("Tolstoy").year(1869).build(),
                Book.builder("Brief History of Time").author("Hawking").year(1988).genre(Genre.SCIENCE).build(),
                Book.builder("Eugene Onegin").author("Pushkin").year(1833).genre(Genre.POETRY).build(),
                Book.builder("Anna Karenina").author("Tolstoy").year(1877).build());
    }

    private void showBooks() {
        header("Books");
        Formatter<Book> formatter = book -> book.getTitle() + " / " + book.getGenre().getTitle();
        for (Book book : catalog.all()) {
            System.out.println(formatter.framed().apply(book));
        }
        for (Map.Entry<Genre, List<String>> entry : catalog.titlesByGenre().entrySet()) {
            System.out.println(entry.getKey().name() + ": " + join(entry.getValue().toArray(new String[0])));
        }
        catalog.oldest().ifPresent(book -> System.out.println("Oldest: " + book));
        List<Book> tolstoy = catalog.search(book -> book.getAuthor().equals("Tolstoy"));
        System.out.println("Tolstoy: " + tolstoy.size() + " books, first is " + tolstoy.get(0));
        System.out.println(Genre.parse("poetry").loanDays() + " days for " + Genre.valueOf("POETRY"));
    }

    private void showLoans() {
        header("Loans");
        Reader adult = catalog.register("Anna", 30);
        Reader child = catalog.register("Petya", 10);
        List<Book> books = catalog.all();
        catalog.lend(books.get(0), adult);
        tryLend(books.get(0), child);
        tryLend(books.get(1), child);
        catalog.lend(books.get(2), child);
        System.out.print(catalog.loanReport());
        System.out.println("Available: " + catalog.countAvailable() + ", Anna holds " + adult.count()
                + ", adult=" + adult.isAdult() + ", child is " + child.age());
    }

    private void tryLend(Book book, Reader reader) {
        try {
            catalog.lend(book, reader);
        } catch (LoanException | IllegalStateException exception) {
            System.out.println("Refused: " + exception.getMessage());
        } finally {
            attempts++;
        }
    }

    private void showUtilities() {
        header("Utilities");
        System.out.println(Texts.format("fib", Texts.fibonacci(15)));
        System.out.println(Texts.format("word", "obfuscator"));
        System.out.println(Texts.repeat('-', 12) + SEPARATOR.trim() + join("a", "b", "c"));
        Pair<String, Integer> pair = Texts.pair("answer", 42);
        Pair<String, Boolean> other = pair.withSecond(true);
        System.out.println(pair.getFirst() + "=" + pair.getSecond() + " " + other.getSecond());
        for (Object value : Arrays.asList(500, 7, "", "java", null, true)) {
            System.out.println(Texts.classify(value));
        }
        Function<Integer, Integer> twice = number -> number * 2;
        System.out.println(twice.apply(21) + " " + twice.andThen(twice).apply(1));
        Repository<String> words = Repository.of(List.of("delta", "alpha", "charlie"));
        System.out.println(words.size() + " " + words.isEmpty() + " " + words.get(1) + " "
                + words.find(word -> word.startsWith("c")).orElse("?") + " "
                + new MemoryRepository<Integer>().isEmpty());
        Info info = Catalog.class.getAnnotation(Info.class);
        System.out.println(info.developer() + " r" + info.revision());
    }

    private void showConcurrency() throws InterruptedException {
        header("Concurrency");
        Worker worker = new Worker("worker-1", 1, 2, 3, 4);
        worker.start();
        worker.join();
        System.out.println(worker.describe() + ", name " + worker.getName() + ", total " + worker.getTotal());
        try (Session session = new Session("main");
             Session nested = new Session(session.getName() + "-nested")) {
            System.out.println("Opened: " + Session.getOpened() + " " + nested.getName().length());
        }
        Runnable task = new Runnable() {
            private int runs;

            @Override
            public void run() {
                runs++;
                report();
            }

            private void report() {
                System.out.println("Task run " + runs + " at step " + step);
            }
        };
        task.run();
        task.run();
    }
}
