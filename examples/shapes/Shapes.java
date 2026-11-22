import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Демонстрационная программа в одном файле: геометрические фигуры.
 */
public class Shapes {

    private static final double PI = 3.14159;
    private static final Map<String, Integer> PRIORITY = new HashMap<>();
    private static int created;

    static {
        PRIORITY.put("Circle", 1);
        PRIORITY.put("Square", 2);
    }

    private int drawn;

    interface Shape {
        double area();

        String name();

        default String describe() {
            return name() + " with area " + round(area());
        }
    }

    abstract static class Polygon implements Shape {
        protected final int sides;

        Polygon(int sides) {
            this.sides = sides;
            created++;
        }

        int getSides() {
            return sides;
        }
    }

    static class Circle implements Shape {
        private final double radius;

        Circle(double radius) {
            this.radius = radius;
            created++;
        }

        @Override
        public double area() {
            return PI * radius * radius;
        }

        @Override
        public String name() {
            return "Circle";
        }
    }

    static class Rectangle extends Polygon {
        private final double width;
        private final double height;

        Rectangle(double width, double height) {
            super(4);
            this.width = width;
            this.height = height;
        }

        @Override
        public double area() {
            return width * height;
        }

        @Override
        public String name() {
            return isSquare() ? "Square" : "Rectangle";
        }

        boolean isSquare() {
            return width == height;
        }
    }

    static class Triangle extends Polygon {
        private final double base;
        private final double height;

        Triangle(double base, double height) {
            super(3);
            this.base = base;
            this.height = height;
        }

        @Override
        public double area() {
            return base * height / 2;
        }

        @Override
        public String name() {
            return "Triangle";
        }
    }

    class Canvas {
        private final List<Shape> shapes = new ArrayList<>();
        private final String title;

        Canvas(String title) {
            this.title = title;
        }

        void draw(Shape shape) {
            shapes.add(shape);
            Shapes.this.drawn++;
        }

        String summary() {
            double total = totalArea(shapes.toArray(new Shape[0]));
            return title + ": " + shapes.size() + " shapes, total area " + round(total);
        }
    }

    static String round(double value) {
        long scaled = Math.round(value * 100);
        long fraction = scaled % 100;
        return scaled / 100 + "." + (fraction < 10 ? "0" : "") + fraction;
    }

    static double totalArea(Shape... shapes) {
        double total = 0;
        for (Shape shape : shapes) {
            total += shape.area();
        }
        return total;
    }

    static <T extends Shape> T largest(List<T> shapes) {
        T largest = shapes.get(0);
        for (T shape : shapes) {
            if (shape.area() > largest.area()) {
                largest = shape;
            }
        }
        return largest;
    }

    static int corners(String name) {
        switch (name) {
            case "Circle":
                return 0;
            case "Triangle":
                return 3;
            default:
                return 4;
        }
    }

    static int[] countCells(int size) {
        int[] counts = new int[size];
        outer:
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                if (column > row) {
                    continue outer;
                }
                counts[row]++;
            }
        }
        return counts;
    }

    public static void main(String[] args) {
        List<Shape> shapes = new ArrayList<>();
        shapes.add(new Circle(1.5));
        shapes.add(new Rectangle(2, 3));
        shapes.add(new Rectangle(2, 2));
        shapes.add(new Triangle(4, 5));
        Shape unit = new Shape() {
            @Override
            public double area() {
                return 1;
            }

            @Override
            public String name() {
                return "Unit";
            }
        };
        shapes.add(unit);

        shapes.sort(Comparator.comparingDouble(Shape::area));
        for (Shape shape : shapes) {
            int priority = PRIORITY.getOrDefault(shape.name(), 9);
            System.out.println(shape.describe() + ", corners " + corners(shape.name()) + ", priority " + priority);
        }
        System.out.println("Largest: " + largest(shapes).name());

        record Size(String label, double limit) {
        }
        List<Size> sizes = List.of(new Size("small", 2), new Size("medium", 5), new Size("large", 1000));
        for (Shape shape : shapes) {
            for (Size size : sizes) {
                if (shape.area() <= size.limit()) {
                    System.out.println(shape.name() + " is " + size.label());
                    break;
                }
            }
        }

        Shapes app = new Shapes();
        Canvas canvas = app.new Canvas("Main canvas");
        for (Shape shape : shapes) {
            if (shape instanceof Polygon polygon && polygon.getSides() == 4) {
                canvas.draw(shape);
            }
        }
        System.out.println(canvas.summary() + ", drawn " + app.drawn);
        System.out.println(Arrays.toString(countCells(4)) + ", created " + created);
    }
}
