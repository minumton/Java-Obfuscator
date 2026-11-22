package library.model;

import java.util.Objects;

public class Book implements Comparable<Book> {

    private final String title;
    private final String author;
    private final int year;
    private final Genre genre;
    private boolean available = true;

    private Book(Builder builder) {
        this.title = builder.title;
        this.author = builder.author;
        this.year = builder.year;
        this.genre = builder.genre;
    }

    public static Builder builder(String title) {
        return new Builder(title);
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public int getYear() {
        return year;
    }

    public Genre getGenre() {
        return genre;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public int compareTo(Book other) {
        int byYear = Integer.compare(year, other.year);
        return byYear != 0 ? byYear : title.compareTo(other.title);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Book other)) {
            return false;
        }
        return year == other.year && title.equals(other.title) && author.equals(other.author);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, author, year);
    }

    @Override
    public String toString() {
        return title + " (" + author + ", " + year + ")";
    }

    public static class Builder {
        private final String title;
        private String author = "Unknown";
        private int year;
        private Genre genre = Genre.NOVEL;

        private Builder(String title) {
            this.title = title;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder year(int year) {
            this.year = year;
            return this;
        }

        public Builder genre(Genre genre) {
            this.genre = genre;
            return this;
        }

        public Book build() {
            return new Book(this);
        }
    }
}
