package com.ghorita.banking.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Value object que representa uma quantia em uma moeda.
 *
 * <p>Características:
 * <ul>
 *   <li><b>Imutável</b>: um {@code record} já tem campos {@code private final} e é uma classe {@code final}.</li>
 *   <li><b>Igualdade por valor</b>: {@code equals}/{@code hashCode} gerados pelo record a partir de
 *       {@code amount} e {@code currency}.</li>
 *   <li><b>Sempre válido</b>: as invariantes são garantidas no construtor compacto; não existe
 *       {@code Money} em estado inválido.</li>
 * </ul>
 *
 * <p>Na ISO 8583, o valor vai no DE 4 (unidades menores) e a moeda no DE 49 (código numérico ISO 4217).
 */
public record Money(BigDecimal amount, Currency currency) {

    /**
     * Construtor compacto: roda ANTES de os campos serem atribuídos.
     * O que você fizer com os parâmetros {@code amount} e {@code currency} aqui é o que fica guardado.
     */
    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative: " + amount.toPlainString());
        }

        // Atenção: para pseudo-moedas (XAU, XXX...) getDefaultFractionDigits() retorna -1,
        // e setScale(-1) arredondaria para dezenas. Não usamos essas moedas no projeto.
        int fractionDigits = currency.getDefaultFractionDigits();
        try {
            // UNNECESSARY: rejeita em vez de arredondar em silêncio (10.005 BRL não é válido).
            amount = amount.setScale(fractionDigits, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "Amount " + amount.toPlainString() + " has more decimal places than "
                            + currency.getCurrencyCode() + " allows (" + fractionDigits + ")",
                    e);
        }
    }

    /**
     * Fábrica de conveniência: {@code Money.of("10.50", "BRL")}.
     *
     * <p>Usa {@code new BigDecimal(String)} de propósito: {@code new BigDecimal(0.1)} (double)
     * vira 0.1000000000000000055511151231257827...
     */
    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    /** Soma este valor com {@code other}. Ex.: 10.50 BRL + 0.75 BRL = 11.25 BRL. */
    public Money plus(Money other) {
        requireSameCurrency(other, "add");
        return new Money(amount.add(other.amount), currency);
    }

    /** Subtrai {@code other} deste valor. Resultado negativo é inválido (invariante do construtor). */
    public Money minus(Money other) {
        requireSameCurrency(other, "subtract");
        return new Money(amount.subtract(other.amount), currency);
    }

    /**
     * {@code true} se este valor for menor que {@code other}. Usa {@code compareTo}, não {@code equals}:
     * compara só o valor numérico (aqui a escala já é igual, mas é o hábito certo com BigDecimal).
     */
    public boolean isLessThan(Money other) {
        requireSameCurrency(other, "compare");
        return amount.compareTo(other.amount) < 0;
    }

    /**
     * Regra única para toda operação entre dois {@code Money}: o outro não pode ser nulo e precisa
     * estar na mesma moeda. Não existe conversão implícita (câmbio é uma regra de negócio à parte).
     */
    private void requireSameCurrency(Money other, String operation) {
        Objects.requireNonNull(other, "Money to " + operation + " cannot be null");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Cannot " + operation + " " + other.currency.getCurrencyCode()
                            + " to/from " + currency.getCurrencyCode());
        }
    }
}
