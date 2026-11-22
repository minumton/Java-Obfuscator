package library.util;

public class Session implements AutoCloseable {

    private static int opened;

    private final String name;

    public Session(String name) {
        this.name = name;
        opened++;
        System.out.println("open " + name);
    }

    public String getName() {
        return name;
    }

    public static int getOpened() {
        return opened;
    }

    @Override
    public void close() {
        System.out.println("close " + name);
    }
}
