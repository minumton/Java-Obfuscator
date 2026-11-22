package library.service;

import library.model.Book;

public class LoanException extends RuntimeException {

    private final Book book;

    public LoanException(Book book, String reason) {
        super("Cannot lend '" + book.getTitle() + "': " + reason);
        this.book = book;
    }

    public Book getBook() {
        return book;
    }
}
