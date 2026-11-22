package library.model;

public enum Genre {
    NOVEL("Novel") {
        @Override
        public int loanDays() {
            return 30;
        }
    },
    SCIENCE("Science") {
        @Override
        public int loanDays() {
            return 14;
        }
    },
    POETRY("Poetry") {
        @Override
        public int loanDays() {
            return 7;
        }
    };

    private final String title;

    Genre(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public abstract int loanDays();

    public static Genre parse(String text) {
        for (Genre genre : values()) {
            if (genre.title.equalsIgnoreCase(text)) {
                return genre;
            }
        }
        return valueOf(text.toUpperCase());
    }
}
