package com.ghorita.banking.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    private static Account contaCom(String saldo) {
        Account account = Account.open(UUID.randomUUID(), BRL);
        account.deposit(Money.of(saldo, "BRL"));
        return account;
    }

    /**
     * Pattern matching em switch sobre um tipo sealed: sem default, e o compilador reclama se
     * faltar um caso. Prévia da Fase P2, onde o resultado vira o DE 39 da resposta.
     */
    private static String responseCode(DebitResult result) {
        return switch (result) {
            case DebitResult.Approved approved -> "00";
            case DebitResult.InsufficientFunds insufficient -> "51";
        };
    }

    // --- abrir e depositar (prontos) ---

    @Test
    void contaNovaComecaComSaldoZero() {
        Account account = Account.open(UUID.randomUUID(), BRL);
        assertEquals(Money.of("0", "BRL"), account.balance());
    }

    @Test
    void depositoAumentaOSaldo() {
        Account account = contaCom("100.00");
        account.deposit(Money.of("0.50", "BRL"));
        assertEquals(Money.of("100.50", "BRL"), account.balance());
    }

    @Test
    void rejeitaDepositoDeZero() {
        Account account = Account.open(UUID.randomUUID(), BRL);
        assertThrows(IllegalArgumentException.class, () -> account.deposit(Money.of("0", "BRL")));
    }

    @Test
    void contasSaoIguaisPeloIdENaoPeloSaldo() {
        UUID id = UUID.randomUUID();
        Account a = Account.open(id, BRL);
        Account b = Account.open(id, BRL);
        b.deposit(Money.of("10", "BRL"));
        assertEquals(a, b);
        assertNotEquals(Account.open(UUID.randomUUID(), BRL), a);
    }

    // --- debitar (TODO(human)) ---

    @Test
    void debitoComSaldoSuficienteEhAprovado() {
        Account account = contaCom("100.00");

        DebitResult result = account.debit(Money.of("30.00", "BRL"));

        DebitResult.Approved approved = assertInstanceOf(DebitResult.Approved.class, result);
        assertEquals(Money.of("70.00", "BRL"), approved.newBalance());
        assertEquals(Money.of("70.00", "BRL"), account.balance());
        assertEquals("00", responseCode(result));
    }

    @Test
    void debitarTodoOSaldoEhAprovado() {
        Account account = contaCom("100.00");

        DebitResult result = account.debit(Money.of("100.00", "BRL"));

        assertInstanceOf(DebitResult.Approved.class, result);
        assertEquals(Money.of("0", "BRL"), account.balance());
    }

    @Test
    void debitoMaiorQueOSaldoEhRecusadoSemAlterarOSaldo() {
        Account account = contaCom("100.00");

        DebitResult result = account.debit(Money.of("100.01", "BRL"));

        DebitResult.InsufficientFunds insufficient =
                assertInstanceOf(DebitResult.InsufficientFunds.class, result);
        assertEquals(Money.of("100.00", "BRL"), insufficient.balance());
        assertEquals(Money.of("100.01", "BRL"), insufficient.requested());
        assertEquals(Money.of("100.00", "BRL"), account.balance());
        assertEquals("51", responseCode(result));
    }

    @Test
    void rejeitaDebitoDeZero() {
        Account account = contaCom("100.00");
        assertThrows(IllegalArgumentException.class, () -> account.debit(Money.of("0", "BRL")));
    }

    @Test
    void rejeitaDebitoNulo() {
        Account account = contaCom("100.00");
        assertThrows(NullPointerException.class, () -> account.debit(null));
    }

    @Test
    void rejeitaDebitoEmOutraMoeda() {
        Account account = contaCom("100.00");
        assertThrows(IllegalArgumentException.class, () -> account.debit(Money.of("10.00", "USD")));
        assertEquals(Money.of("100.00", "BRL"), account.balance());
    }
}
