import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Math.max;

/**
 * Тестовая программа: проверяет обфускатор на разных конструкциях языка.
 * Многие методы намеренно названы так же, как методы JDK.
 */
public class Features {

    private int counter;

    static class Bag implements Iterable<String> {
        private final List<String> items = new ArrayList<>();
        private int size;
        int length = 3;

        Bag add(String item) {
            items.add(item);
            size = items.size();
            return this;
        }

        String get(int index) {
            return items.get(index);
        }

        int size() {
            return size;
        }

        boolean isEmpty() {
            return items.isEmpty();
        }

        boolean contains(String item) {
            return items.contains(item);
        }

        Bag sort() {
            items.sort(String::compareTo);
            return this;
        }

        String max() {
            return items.stream().max(String::compareTo).orElse("");
        }

        long count() {
            return items.stream().filter(item -> item.length() > length).count();
        }

        String format(String pattern) {
            return String.format(pattern, size);
        }

        void print() {
            System.out.print(join());
            System.out.println();
        }

        String join() {
            return String.join("+", items);
        }

        void forEachItem(Consumer<String> accept) {
            items.forEach(accept);
        }

        @Override
        public Iterator<String> iterator() {
            return items.iterator();
        }
    }

    abstract static class Animal {
        private static int count;
        protected final String name;

        Animal(String name) {
            this.name = name;
            count++;
        }

        abstract String sound();

        String speak() {
            return name + " says " + sound();
        }

        String speak(int times) {
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < times; index++) {
                builder.append(sound());
            }
            return name + ": " + builder;
        }

        static int count() {
            return count;
        }
    }

    static class Dog extends Animal {
        Dog(String name) {
            super(name);
        }

        @Override
        String sound() {
            return "Woof";
        }

        @Override
        String speak() {
            return super.speak() + "!";
        }
    }

    static class Puppy extends Dog {
        private final String name;

        Puppy(String name) {
            super("Parent of " + name);
            this.name = name;
        }

        @Override
        String sound() {
            String name = "yip";
            return name + "(" + this.name + "/" + super.name + ")";
        }
    }

    interface Priced {
        int price();
    }

    enum Coffee implements Priced {
        ESPRESSO(120), LATTE(180), RAF(220);

        private final int price;

        Coffee(int price) {
            this.price = price;
        }

        @Override
        public int price() {
            return price;
        }

        String size() {
            switch (this) {
                case ESPRESSO:
                    return "small";
                case LATTE:
                    return "medium";
                default:
                    return "large";
            }
        }

        Coffee next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    static class Box<T extends Comparable<T>> {
        private T value;

        Box(T value) {
            this.value = value;
        }

        T get() {
            return value;
        }

        void set(T value) {
            this.value = value;
        }

        boolean isBigger(Box<T> other) {
            return value.compareTo(other.value) > 0;
        }

        <R extends Comparable<R>> Box<R> map(Function<T, R> mapper) {
            return new Box<>(mapper.apply(value));
        }

        static <E extends Comparable<E>> Box<E> of(E value) {
            return new Box<>(value);
        }
    }

    record Point(int x, int y) {
        static final Point ORIGIN = new Point(0, 0);

        Point {
            if (x < -1000) {
                throw new IllegalArgumentException("x is too small: " + x);
            }
        }

        Point plus(Point other) {
            return new Point(x + other.x, y + other.y());
        }

        double distance() {
            return Math.sqrt(x * x + y * y);
        }
    }

    interface Greeter {
        String greet(String name);

        default String greetAll(String... names) {
            return Arrays.stream(names).map(this::decorate).collect(Collectors.joining(" "));
        }

        private String decorate(String name) {
            return greet(name).toUpperCase();
        }

        static Greeter polite() {
            return name -> "Dear " + name;
        }
    }

    class Ticker {
        private final int step;

        Ticker(int step) {
            this.step = step;
        }

        void tick() {
            counter += step;
            Features.this.counter++;
        }
    }

    abstract static class Task {
        private final String label;

        Task(String label) {
            this.label = label;
        }

        abstract int execute(int input);

        String report(int input) {
            return label + "(" + input + ")=" + execute(input);
        }
    }

    public static void main(String[] args) {
        collisions();
        inheritance();
        enums();
        generics();
        records();
        classes();
        lambdas();
        controlFlow();
        textBlocks();
    }

    static void collisions() {
        Bag bag = new Bag().add("pear").add("fig").add("banana");
        List<String> copy = new ArrayList<>();
        bag.forEachItem(copy::add);
        for (String item : bag) {
            copy.add(item.toUpperCase());
        }
        System.out.println(bag.size() + " " + copy.size() + " " + bag.get(0) + " " + copy.get(4)
                + " " + bag.isEmpty() + " " + copy.isEmpty());
        System.out.println(bag.sort().join() + " " + bag.max() + " " + max(3, 4) + " " + bag.count()
                + " " + bag.contains("fig") + " " + copy.contains("fig"));
        System.out.println(bag.format("size=%d") + " " + bag.length + " " + "text".length() + " " + new int[5].length);
        bag.print();
        int size = bag.size();
        int length = bag.length + size;
        String[] values = {"b", "a"};
        Arrays.sort(values);
        System.out.println(length + values[0] + values.length);
    }

    static void inheritance() {
        List<Animal> animals = List.of(new Dog("Rex"), new Puppy("Bim"));
        for (Animal animal : animals) {
            System.out.println(animal.speak() + " | " + animal.speak(2));
        }
        System.out.println("Animals: " + Animal.count());
    }

    static void enums() {
        Map<Coffee, Integer> orders = new EnumMap<>(Coffee.class);
        for (Coffee coffee : Coffee.values()) {
            orders.merge(coffee.next(), coffee.price(), Integer::sum);
        }
        orders.forEach((coffee, total) -> System.out.println(coffee + " " + coffee.size() + " " + total));
        Coffee chosen = Coffee.valueOf("LATTE");
        String note = switch (chosen) {
            case ESPRESSO -> "strong";
            case LATTE, RAF -> "milky";
        };
        Priced priced = chosen;
        System.out.println(note + " " + priced.price() + " " + chosen.name() + " " + chosen.ordinal());
    }

    static void generics() {
        Box<Integer> first = Box.of(5);
        Box<Integer> second = new Box<>(7);
        second.set(second.get() + 1);
        Box<String> text = first.map(number -> "n" + number);
        System.out.println(first.isBigger(second) + " " + second.get() + " " + text.get() + " "
                + Optional.of(3).map(number -> number * 2).get() + " " + List.of(1, 2).get(1));
    }

    static void records() {
        Point point = new Point(3, 4).plus(Point.ORIGIN);
        System.out.println(point.x() + "," + point.y() + " " + point.distance() + " " + point.equals(new Point(3, 4)));
        Greeter greeter = Greeter.polite();
        System.out.println(greeter.greet("Ann") + " / " + greeter.greetAll("bob", "eve"));
    }

    static void classes() {
        Features features = new Features();
        Features.Ticker ticker = features.new Ticker(4);
        ticker.tick();
        ticker.tick();
        class Doubler extends Task {
            Doubler() {
                super("double");
            }

            @Override
            int execute(int input) {
                return input * 2;
            }
        }
        Task negate = new Task("negate") {
            @Override
            int execute(int input) {
                return -input;
            }
        };
        System.out.println(features.counter + " " + new Doubler().report(21) + " " + negate.report(5));
    }

    static void lambdas() {
        Supplier<List<String>> factory = ArrayList::new;
        List<String> names = factory.get();
        names.add("x");
        BiFunction<Integer, Integer, Integer> adder = Features::add;
        UnaryOperator<String> twice = value -> value + value;
        Supplier<Supplier<String>> nested = () -> () -> twice.apply("ab");
        int sum = IntStream.rangeClosed(1, 4).map(number -> add(number, number)).sum();
        System.out.println(names + " " + adder.apply(2, 3) + " " + nested.get().get() + " " + sum);
        Map<Boolean, List<Integer>> parts = IntStream.range(0, 6).boxed()
                .collect(Collectors.partitioningBy(Features::isEven));
        System.out.println(parts.get(true) + " " + parts.get(false));
    }

    static int add(int left, int right) {
        return left + right;
    }

    static boolean isEven(int number) {
        return number % 2 == 0;
    }

    static void controlFlow() {
        int[][] grid = new int[3][4];
        int filled = 0;
        search:
        for (int row = 0; row < grid.length; row++) {
            for (int column = 0; column < grid[row].length; column++) {
                if (row * column > 3) {
                    break search;
                }
                grid[row][column] = row + column;
                filled++;
            }
        }
        char letter = 'a';
        letter += 2;
        long big = 1L << 40;
        int index = 0;
        do {
            index += 3;
        } while (index < 10);
        String state = index > 10 ? "over" : index == 10 ? "exact" : "under";
        try {
            Object[] objects = new String[1];
            objects[0] = 1;
        } catch (ArrayStoreException | NullPointerException exception) {
            state += "!";
        } finally {
            filled += 100;
        }
        System.out.println(filled + " " + letter + " " + big + " " + state + " " + Arrays.deepToString(grid));
    }

    static void textBlocks() {
        String text = """
                Hello,
                  "block"
                end\
                """;
        System.out.println(text.lines().count() + ":" + text);
    }
}
