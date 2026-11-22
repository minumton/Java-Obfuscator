package bank.model;

import bank.util.MoneyFormatter;

public record Transaction(TransactionType type, long amount) {

    public String describe() {
        return type.getLabel() + " " + MoneyFormatter.format(amount);
    }
}
