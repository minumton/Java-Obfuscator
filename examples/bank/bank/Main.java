package bank;

import bank.model.Account;
import bank.model.SavingsAccount;
import bank.model.Transaction;
import bank.service.Bank;
import bank.service.InsufficientFundsException;

import java.util.List;

import static bank.util.MoneyFormatter.format;

/**
 * Демонстрационная программа: банк со счетами и переводами.
 */
public class Main {

    private static final long INITIAL_DEPOSIT = 100_000;

    public static void main(String[] args) {
        Bank bank = new Bank("Bonch Bank");
        Account first = bank.openAccount("Ivanov", INITIAL_DEPOSIT);
        Account second = bank.openAccount("Petrov", 25_000);
        SavingsAccount savings = bank.openSavingsAccount("Sidorov", 500_000, 7);

        transfer(bank, first, second, 30_000);
        transfer(bank, second, first, 9_000_000);
        savings.addInterest();

        printAccounts(bank);
        printHistory(first.getHistory());
        System.out.println("Total: " + format(bank.getTotalBalance()));
        bank.findRichest().ifPresent(account -> System.out.println("Richest: " + account.getOwner()));
    }

    private static void transfer(Bank bank, Account source, Account target, long amount) {
        try {
            bank.transfer(source, target, amount);
            System.out.println("Transferred " + format(amount));
        } catch (InsufficientFundsException exception) {
            System.out.println("Transfer failed: " + exception.getMessage()
                    + ", missing " + format(exception.getMissingAmount()));
        }
    }

    private static void printAccounts(Bank bank) {
        System.out.println("Accounts of " + bank.getName() + ":");
        for (Account account : bank.getAccountsSortedByBalance()) {
            System.out.println("  " + account.describe());
        }
    }

    private static void printHistory(List<Transaction> history) {
        int index = 1;
        for (Transaction transaction : history) {
            System.out.println(index + ". " + transaction.type() + " -> " + transaction.describe());
            index++;
        }
    }
}
