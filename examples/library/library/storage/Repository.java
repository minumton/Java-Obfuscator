package library.storage;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface Repository<T> extends Iterable<T> {

    int MAX_SIZE = 100;

    void add(T item);

    T get(int index);

    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

    default Optional<T> find(Predicate<T> condition) {
        for (T item : this) {
            if (condition.test(item)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    static <E> Repository<E> of(List<E> items) {
        Repository<E> repository = new MemoryRepository<>();
        items.forEach(repository::add);
        return repository;
    }
}
