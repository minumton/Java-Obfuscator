package bank.model;

import bank.util.MoneyFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Account implements Comparable<Account> {

    private static int nextNumber = 1;

    private final int number;
    private final String owner;
    protected long balance;
    private final List<Transaction> history = new ArrayList<>();

    public Account(String owner, long balance) {
        this.number = nextNumber++;
        this.owner = owner;
        this.balance = balance;
    }

    public void deposit(long amount) {
        requirePositive(amount);
        balance += amount;
        history.add(new Transaction(TransactionType.DEPOSIT, amount));
    }

    public void withdraw(long amount) {
        requirePositive(amount);
        balance -= amount;
        history.add(new Transaction(TransactionType.WITHDRAWAL, amount));
    }

    public boolean canWithdraw(long amount) {
        return amount <= balance;
    }

    private void requirePositive(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + amount);
        }
    }

    public int getNumber() {
        return number;
    }

    public String getOwner() {
        return owner;
    }

    public long getBalance() {
        return balance;
    }

    public List<Transaction> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public String describe() {
        return "Account " + this;
    }

    @Override
    public int compareTo(Account other) {
        return Long.compare(balance, other.balance);
    }

    @Override
    public String toString() {
        return "#" + number + " " + owner + ": " + MoneyFormatter.format(balance);
    }
}
