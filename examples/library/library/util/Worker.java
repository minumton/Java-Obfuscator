package library.util;

public class Worker extends Thread {

    private final int[] numbers;
    private long total;

    public Worker(String title, int... numbers) {
        super(title);
        this.numbers = numbers;
    }

    @Override
    public void run() {
        for (int number : numbers) {
            total += square(number);
        }
    }

    private long square(int number) {
        return (long) number * number;
    }

    public long getTotal() {
        return total;
    }

    public String describe() {
        return getName() + " computed " + total;
    }
}
