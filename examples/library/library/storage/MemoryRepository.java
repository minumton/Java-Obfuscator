package library.storage;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class MemoryRepository<T> implements Repository<T> {

    private Object[] items = new Object[2];
    private int size;

    @Override
    public void add(T item) {
        if (size == MAX_SIZE) {
            throw new IllegalStateException("Repository is full");
        }
        ensureCapacity();
        items[size++] = item;
    }

    private void ensureCapacity() {
        if (size == items.length) {
            Object[] larger = new Object[items.length * 2];
            System.arraycopy(items, 0, larger, 0, size);
            items = larger;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("No item at " + index);
        }
        return (T) items[index];
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Iterator<T> iterator() {
        return new ItemIterator();
    }

    private class ItemIterator implements Iterator<T> {
        private int position;

        @Override
        public boolean hasNext() {
            return position < size;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return get(position++);
        }
    }
}
