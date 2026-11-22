package library.util;

public final class Texts {

    public static final String SEPARATOR = ", ";

    private Texts() {
    }

    public static String join(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < parts.length; index++) {
            if (index > 0) {
                builder.append(SEPARATOR);
            }
            builder.append(parts[index]);
        }
        return builder.toString();
    }

    public static String format(String label, int value) {
        return String.format("%-6s|%5d|", label, value);
    }

    public static String format(String label, String value) {
        return format(label, value.length()) + " " + value;
    }

    public static String repeat(char symbol, int length) {
        char[] values = new char[length];
        java.util.Arrays.fill(values, symbol);
        return new String(values);
    }

    public static int fibonacci(int number) {
        return number < 2 ? number : fibonacci(number - 1) + fibonacci(number - 2);
    }

    public static String classify(Object value) {
        if (value instanceof Integer number && number > 100) {
            return "big number";
        }
        if (value instanceof String text) {
            return text.isEmpty() ? "empty text" : "text of " + text.length();
        }
        return switch (String.valueOf(value)) {
            case "null" -> "nothing";
            case "true", "false" -> "flag";
            default -> "other";
        };
    }

    public static <A, B> Pair<A, B> pair(A first, B second) {
        return new Pair<>(first, second);
    }

    public static class Pair<A, B> {
        private final A first;
        private final B second;

        Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }

        public A getFirst() {
            return first;
        }

        public B getSecond() {
            return second;
        }

        public <C> Pair<A, C> withSecond(C value) {
            return new Pair<>(first, value);
        }
    }
}
