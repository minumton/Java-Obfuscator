package bank.service;

import bank.model.Account;
import bank.model.SavingsAccount;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Bank {

    private final String name;
    private final Map<Integer, Account> accounts = new LinkedHashMap<>();

    public Bank(String name) {
        this.name = name;
    }

    public Account openAccount(String owner, long balance) {
        return register(new Account(owner, balance));
    }

    public SavingsAccount openSavingsAccount(String owner, long balance, int rate) {
        SavingsAccount account = new SavingsAccount(owner, balance, rate);
        register(account);
        return account;
    }

    private Account register(Account account) {
        accounts.put(account.getNumber(), account);
        return account;
    }

    public void transfer(Account source, Account target, long amount) throws InsufficientFundsException {
        if (!source.canWithdraw(amount)) {
            throw new InsufficientFundsException(source.getOwner(), amount - source.getBalance());
        }
        source.withdraw(amount);
        target.deposit(amount);
    }

    public long getTotalBalance() {
        return accounts.values().stream()
                .mapToLong(Account::getBalance)
                .sum();
    }

    public Optional<Account> findRichest() {
        return accounts.values().stream().max(Comparator.naturalOrder());
    }

    public List<Account> getAccountsSortedByBalance() {
        return accounts.values().stream()
                .sorted(Comparator.comparingLong(Account::getBalance).reversed())
                .collect(Collectors.toList());
    }

    public String getName() {
        return name;
    }
}
