package bank.model;

public enum TransactionType {
    DEPOSIT("money in"),
    WITHDRAWAL("money out");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
