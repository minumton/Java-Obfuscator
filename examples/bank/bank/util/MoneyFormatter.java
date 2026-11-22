package bank.util;

public final class MoneyFormatter {

    private static final int KOPECKS_IN_RUBLE = 100;
    private static final String CURRENCY = "RUB";

    private MoneyFormatter() {
    }

    public static String format(long amount) {
        long rubles = amount / KOPECKS_IN_RUBLE;
        long kopecks = amount % KOPECKS_IN_RUBLE;
        return rubles + "." + (kopecks < 10 ? "0" : "") + kopecks + " " + CURRENCY;
    }
}
