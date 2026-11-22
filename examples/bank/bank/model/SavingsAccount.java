package bank.model;

public class SavingsAccount extends Account {

    private static final int PERCENT = 100;

    private final int rate;

    public SavingsAccount(String owner, long balance, int rate) {
        super(owner, balance);
        this.rate = rate;
    }

    public void addInterest() {
        long interest = balance * rate / PERCENT;
        deposit(interest);
    }

    public int getRate() {
        return rate;
    }

    @Override
    public String describe() {
        return "Savings " + super.describe() + " at " + rate + "%";
    }
}
