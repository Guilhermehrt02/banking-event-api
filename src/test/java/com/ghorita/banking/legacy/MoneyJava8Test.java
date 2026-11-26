package com.ghorita.banking.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Verifica que a versão Java 8 respeita o contrato de equals/hashCode — o mesmo que o record entrega de graça.
 */
class MoneyJava8Test {

    @Test
    void mesmoValorComEscalasDiferentesEhIgualETemMesmoHashCode() {
        MoneyJava8 a = MoneyJava8.of("10.0", "BRL");
        MoneyJava8 b = MoneyJava8.of("10.00", "BRL");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void ehSimetrico() {
        MoneyJava8 a = MoneyJava8.of("10.00", "BRL");
        MoneyJava8 b = MoneyJava8.of("10.00", "BRL");

        assertTrue(a.equals(b) && b.equals(a));
    }

    @Test
    void mesmoValorEmMoedasDiferentesNaoEhIgual() {
        assertNotEquals(MoneyJava8.of("10.00", "BRL"), MoneyJava8.of("10.00", "USD"));
    }

    @Test
    void naoEhIgualANuloNemAOutroTipo() {
        MoneyJava8 dez = MoneyJava8.of("10.00", "BRL");

        assertFalse(dez.equals(null));
        assertFalse(dez.equals("10.00 BRL"));
    }

    @Test
    void funcionaComoElementoDeHashSet() {
        Set<MoneyJava8> valores = new HashSet<>();
        valores.add(MoneyJava8.of("10.0", "BRL"));

        // Se o hashCode não acompanhar o equals, o HashSet procura no "balde" errado e não acha.
        assertTrue(valores.contains(MoneyJava8.of("10.00", "BRL")));
    }
}
