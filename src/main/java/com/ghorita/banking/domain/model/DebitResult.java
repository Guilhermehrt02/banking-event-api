package com.ghorita.banking.domain.model;

import java.util.Objects;

/**
 * Resultado de um débito. Saldo insuficiente é um resultado de NEGÓCIO esperado (DE 39 = 51),
 * não um erro de programação; por isso vem como valor de retorno, e não como exceção.
 *
 * <p>{@code sealed}: só os tipos listados em {@code permits} podem implementar esta interface.
 * O compilador conhece todos os casos, então um {@code switch} sobre o resultado não precisa de
 * {@code default}, e um caso novo quebra a compilação onde ainda não foi tratado.
 *
 * <p>Os subtipos são {@code record}s: já são {@code final}, como o {@code sealed} exige
 * (cada subtipo precisa ser {@code final}, {@code sealed} ou {@code non-sealed}).
 */
public sealed interface DebitResult permits DebitResult.Approved, DebitResult.InsufficientFunds {

    /** Débito aprovado; carrega o saldo resultante. */
    record Approved(Money newBalance) implements DebitResult {
        public Approved {
            Objects.requireNonNull(newBalance, "New balance cannot be null");
        }
    }

    /** Débito recusado por saldo insuficiente; o saldo da conta NÃO muda. */
    record InsufficientFunds(Money balance, Money requested) implements DebitResult {
        public InsufficientFunds {
            Objects.requireNonNull(balance, "Balance cannot be null");
            Objects.requireNonNull(requested, "Requested cannot be null");
        }
    }
}
