package com.ghorita.banking.legacy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * O mesmo {@link com.ghorita.banking.domain.model.Money}, escrito como seria em Java 8 (sem {@code record}).
 *
 * <p>Material de estudo: vive em {@code src/test} para não ir para o jar. Usa só recursos do Java 8.
 * Compare linha a linha com o record para ver o que o Java 16+ gera automaticamente.
 */
public final class MoneyJava8 { // final: impede subclasses que quebrariam a simetria do equals

    private final BigDecimal amount;   // private final: só atribuídos uma vez, no construtor
    private final Currency currency;

    public MoneyJava8(BigDecimal amount, Currency currency) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative: " + amount.toPlainString());
        }

        int fractionDigits = currency.getDefaultFractionDigits();
        BigDecimal normalized;
        try {
            normalized = amount.setScale(fractionDigits, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "Amount " + amount.toPlainString() + " has more decimal places than "
                            + currency.getCurrencyCode() + " allows (" + fractionDigits + ")",
                    e);
        }

        // Diferença do record: aqui os campos são atribuídos explicitamente (no compacto, é implícito).
        this.amount = normalized;
        this.currency = currency;
    }

    public static MoneyJava8 of(String amount, String currencyCode) {
        return new MoneyJava8(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    // Convenção JavaBeans (getX) do código legado; o record usa amount() e currency().
    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;                    // atalho: mesmo objeto
        if (!(o instanceof MoneyJava8)) return false;   // tipo errado ou null
        MoneyJava8 other = (MoneyJava8) o;              // Java 16+: o instanceof MoneyJava8 other
        // equals (e não compareTo) é seguro: o construtor já normalizou a escala.
        // Campos nunca são null (invariante do construtor), então não precisa de Objects.equals.
        return amount.equals(other.amount) && currency.equals(other.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);          // mesmos campos do equals
    }

    @Override
    public String toString() {
        return "MoneyJava8[amount=" + amount + ", currency=" + currency + "]";
    }
}
