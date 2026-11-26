package com.ghorita.banking.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;

/**
 * Teste de domínio puro: sem Spring, sem banco. Deve rodar em milissegundos.
 */
class MoneyTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    @Test
    void rejeitaValorNulo() {
        assertThrows(NullPointerException.class, () -> new Money(null, BRL));
    }

    @Test
    void rejeitaMoedaNula() {
        assertThrows(NullPointerException.class, () -> new Money(BigDecimal.TEN, null));
    }

    @Test
    void mesmoValorComEscalasDiferentesEhIgual() {
        Money dezComUmaCasa = Money.of("10.0", "BRL");
        Money dezComDuasCasas = Money.of("10.00", "BRL");

        assertEquals(dezComDuasCasas, dezComUmaCasa);
        assertEquals(dezComDuasCasas.hashCode(), dezComUmaCasa.hashCode());
    }

    @Test
    void mesmoValorEmMoedasDiferentesNaoEhIgual() {
        assertNotEquals(Money.of("10.00", "BRL"), Money.of("10.00", "USD"));
    }
}
