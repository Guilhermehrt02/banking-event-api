package com.ghorita.banking.domain.model;

import java.math.BigDecimal;
import java.util.Currency;

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
        // TODO(human): garantir as invariantes do Money.
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
}
