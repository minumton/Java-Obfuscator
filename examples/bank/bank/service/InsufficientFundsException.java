package bank.service;

public class InsufficientFundsException extends Exception {

    private final long missingAmount;

    public InsufficientFundsException(String owner, long missingAmount) {
        super("not enough money on the account of " + owner);
        this.missingAmount = missingAmount;
    }

    public long getMissingAmount() {
        return missingAmount;
    }
}
