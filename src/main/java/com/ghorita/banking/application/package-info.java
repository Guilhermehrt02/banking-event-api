/**
 * Casos de uso e portas.
 *
 * <ul>
 *   <li><b>Portas de entrada</b>: interfaces que os adapters de entrada chamam (ex.: "abrir conta").</li>
 *   <li><b>Portas de saída</b>: interfaces que o caso de uso precisa do mundo externo
 *       (ex.: {@code AccountRepository}), implementadas em {@code adapter.out}.</li>
 * </ul>
 *
 * <p>Depende apenas de {@code domain}.
 */
package com.ghorita.banking.application;
