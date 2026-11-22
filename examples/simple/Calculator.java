/**
 * Простой пример для первого знакомства: калькулятор, который считает операции.
 */
public class Calculator {

    private int result;
    private int operations;

    public void add(int value) {
        result += value;
        operations++;
    }

    public void multiply(int factor) {
        result *= factor;
        operations++;
    }

    public String describe() {
        return "Result: " + result + " after " + operations + " operations";
    }

    public static void main(String[] args) {
        Calculator calculator = new Calculator();
        calculator.add(5);
        calculator.multiply(4);
        calculator.add(2);
        System.out.println(calculator.describe());
    }
}
