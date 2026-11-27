package com.ghorita.banking.domain.model;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

/**
 * Conta: uma ENTIDADE (identidade pelo {@code id}), não um value object. Duas contas com o mesmo
 * saldo são contas diferentes; a mesma conta continua sendo ela depois de um débito.
 *
 * <p><b>Composição, não herança</b>: a conta TEM um {@link Money} (o saldo). A classe é
 * {@code final} e o saldo é {@code private}: só muda por {@link #deposit} e {@link #debit}, que
 * aplicam as regras. Com {@code protected}, qualquer classe do pacote ou subclasse poderia
 * alterar o saldo sem passar por elas.
 */
public final class Account {

    private final UUID id;
    private Money balance;

    private Account(UUID id, Money balance) {
        this.id = Objects.requireNonNull(id, "Id cannot be null");
        this.balance = Objects.requireNonNull(balance, "Balance cannot be null");
    }

    /** Abre uma conta nova, com saldo zero na moeda informada. */
    public static Account open(UUID id, Currency currency) {
        Objects.requireNonNull(currency, "Currency cannot be null");
        return new Account(id, new Money(BigDecimal.ZERO, currency));
    }

    public UUID id() {
        return id;
    }

    public Money balance() {
        return balance;
    }

    /** Credita {@code amount} no saldo. Moeda diferente é rejeitada pelo próprio {@code Money}. */
    public void deposit(Money amount) {
        requirePositive(amount, "deposit");
        balance = balance.plus(amount);
    }

    // TODO(human): implementar o débito.
    // - Valor nulo ou zero: erro de programação → exceção (use requirePositive).
    // - Saldo insuficiente: resultado de negócio → InsufficientFunds, SEM alterar o saldo.
    // - Caso contrário: atualiza o saldo e devolve Approved com o novo saldo.
    /** Debita {@code amount} do saldo, se houver saldo suficiente. */
    public DebitResult debit(Money amount) {
        requirePositive(amount, "debit");

        if(amount.isLessThan(balance) || amount.equals(balance)) {
            balance = balance.minus(amount);
            return new DebitResult.Approved(balance);
        }else{
            return new DebitResult.InsufficientFunds(balance,  amount);
        }

    }

    /** Valor de operação precisa existir e ser maior que zero (zero passa no Money, mas não aqui). */
    private static void requirePositive(Money amount, String operation) {
        Objects.requireNonNull(amount, "Amount to " + operation + " cannot be null");
        if (amount.amount().signum() == 0) {
            throw new IllegalArgumentException("Cannot " + operation + " a zero amount");
        }
    }

    /** Entidade: igualdade pelo id, não pelo saldo. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Account[id=" + id + ", balance=" + balance + "]";
    }
}
