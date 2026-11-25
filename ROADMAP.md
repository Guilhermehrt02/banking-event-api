# Roadmap — Banking Event Processing API

Legenda: 🧠 conceito · ✍️ você implementa · 🤖 Claude implementa/explica · 🎤 pergunta de entrevista ou de "primeira semana" · ⭐ prioridade antes de 11/01

Regra: só avance de fase quando o 🎤 for respondido por você com segurança.

Objetivo até **11/01/2027**: chegar no novo emprego (testes e certificação de pagamentos) com
Java afiado (8 e 21), Spring Boot, testes, e vocabulário do domínio: ISO 8583, fluxo de autorização,
EMV e noções de criptografia de pagamento.

---

## Cronograma

| Período | Fase | Ritmo |
|---|---|---|
| 25/11 – 01/12 | ⭐ Fase 1 — Domínio + checkpoint de Java | ~1h30 por noite |
| 02/12 – 08/12 | ⭐ Fase 2 — Persistência e testes | ~1h30 por noite |
| 09/12 – 15/12 | ⭐ Fase 3 — Transferências e concorrência | ~1h30 por noite |
| 16/12 – 22/12 | ⭐ Fase P1 — Parser ISO 8583 | ~1h30 por noite |
| 26/12 – 30/12 | ⭐ Fase P2 — Simulador de autorização e test runner · Fase 4 se sobrar tempo | dias livres |
| 31/12 – 10/01 | Fase L — Leitura offline (viagem) | leve, sem código |
| depois de 11/01 | Fases 5 a 10 (opcionais) | sem pressa |

Se atrasar: corte primeiro a Fase 4 e o detalhe da Fase 3 (teste de concorrência pode ficar simples).
Não corte P1 e P2.

---

## Fase 0 — Fundação ✅ (concluída em 17/11)
- 🧠 Containers, Docker Compose, Kafka em modo KRaft, o que é um broker, tópico e partição
- 🤖 Skeleton Spring Boot, `docker-compose.yml` (Postgres + Kafka), Actuator `/health`
- ✍️ Ler e explicar cada serviço do compose; subir tudo e validar
- 🎤 "Por que usar Docker Compose no desenvolvimento? O que ele não resolve?"

## ⭐ Fase 1 — Domínio, Arquitetura Hexagonal e checkpoint de Java
- 🧠 Ports & Adapters, por que o domínio não conhece framework, value objects, invariantes
- 🧠 **Checkpoint de Java** (pontos que escaparam na entrevista técnica):
  `finally` sempre executa (e quando não executa), auto-unboxing de `null` → NPE,
  `protected` (pacote + subclasse), `final` em variável/método/classe, `finalize` deprecado,
  herança vs composição, `equals`/`hashCode`, imutabilidade
- 🧠 **Java 8 vs 21**: `record`, `sealed`, pattern matching (`instanceof` e `switch`), `var`,
  text blocks, streams e `Optional` (já no 8), `java.time`. Como o mesmo código ficaria em Java 8
- ✍️ `Money` (como `record`), `Account` (abrir conta, depositar, debitar, saldo insuficiente), testes unitários puros (JUnit 5)
- 🤖 Estrutura de pacotes, revisão do seu código, 5 exercícios rápidos de "o que este código imprime?"
- 🎤 "Por que arquitetura hexagonal? Qual o custo?" / "O que acontece em `Integer x = null; int y = x;`?"

## ⭐ Fase 2 — Persistência e testes
- 🧠 JPA/Hibernate, mapeamento domínio vs entidade, migrations, Testcontainers
- 🧠 Mockito: quando mockar (porta de saída) e quando não mockar (domínio, banco real no teste de integração)
- ✍️ Porta de saída `AccountRepository` + adapter JPA, migration Flyway, casos de uso "criar conta" e "consultar saldo"
- ✍️ Um teste do caso de uso com Mockito e um teste do adapter com Testcontainers
- 🤖 Config do Testcontainers, controller REST
- 🎤 "Como você testa a integração com o banco sem mock?" / "Quando um mock atrapalha mais do que ajuda?"

## ⭐ Fase 3 — Transferências e consistência
- 🧠 ACID, níveis de isolamento, lock otimista vs pessimista, ledger imutável (partidas dobradas)
- ✍️ Caso de uso `Transfer` (débito + crédito + movimentações na MESMA transação), tratamento de concorrência
- 🤖 Teste de concorrência (várias transferências simultâneas na mesma conta)
- 🎤 "Como você garante a consistência das movimentações?" / "E se duas transferências debitarem a mesma conta ao mesmo tempo?"

## ⭐ Fase P1 — Mensagens ISO 8583
Por que importa: é o formato das mensagens trocadas entre terminal, adquirente, bandeira e emissor.
Testar e certificar um host é, em boa parte, montar, enviar e conferir essas mensagens.

- 🧠 Estrutura da mensagem: **MTI** (ex.: 0100 pedido de autorização, 0110 resposta, 0200, 0400 estorno),
  **bitmap** primário e secundário, **data elements** (DE 2 PAN, DE 3 processing code, DE 4 valor,
  DE 7, DE 11 STAN, DE 37 RRN, DE 39 código de resposta, DE 41 terminal, DE 55 dados EMV)
- 🧠 Codificação: ASCII vs **BCD** vs binário; campos **fixos** vs variáveis (**LLVAR**, **LLLVAR**);
  bits, bytes e operações bit a bit em Java (`&`, `|`, `<<`, `>>>`, `byte` com sinal)
- 🧠 Onde fica na hexagonal: o modelo da mensagem é domínio? O codec é adapter? (decisão → ADR)
- ✍️ `Iso8583Message` (MTI + campos), **bitmap** (montar e ler), codec `encode(): byte[]` e `decode(byte[])`
  para um subconjunto de campos (2, 3, 4, 7, 11, 37, 39, 41)
- ✍️ Testes com **vetores em hexadecimal** conhecidos (ida e volta: `decode(encode(m)).equals(m)`)
- 🤖 Utilitários de hex/BCD, tabela de definição de campos, revisão de casos de borda (campo vazio, tamanho errado, bitmap secundário)
- 🎤 "Como funciona o bitmap de uma ISO 8583?" / "Por que BCD?" / "Como você testaria um parser binário?"

## ⭐ Fase P2 — Simulador de autorização e test runner de certificação
Por que importa: é o modelo do negócio da empresa — verificar se um host responde como a bandeira exige.

- 🧠 Fluxo de autorização: terminal → adquirente → bandeira → emissor → volta; autorização vs captura vs liquidação
- 🧠 Códigos de resposta (DE 39): 00 aprovado, 05 não autorizado, 51 saldo insuficiente, 54 cartão vencido, 14 cartão inválido
- 🧠 **Luhn**, mascaramento de PAN, **tokenização** (por que o token substitui o PAN e quem guarda o mapeamento)
- ✍️ "Emissor simulado": recebe uma 0100, valida Luhn, validade e saldo (reaproveitando `Account` das fases 1–3), responde 0110 com o DE 39 correto
- ✍️ **Test runner**: lê casos de teste (JSON ou YAML: mensagem de entrada + resposta esperada), executa contra o emissor
  simulado e gera um relatório `PASS/FAIL` por caso, mostrando o campo que divergiu
- 🤖 Endpoint REST para disparar o runner, formato do arquivo de casos, revisão
- 🎤 "Como você estruturaria uma suíte de testes de certificação?" / "O que é tokenização e o que ela protege?"

## Fase 4 — Idempotency Key (se sobrar tempo antes de 11/01)
- 🧠 Idempotência, retries de cliente, replay da resposta, constraint única, janela de expiração
- 🧠 Paralelo com pagamentos: retransmissão de uma 0100 com o mesmo STAN/RRN não pode aprovar duas vezes
- ✍️ Header `Idempotency-Key`, tabela de chaves, comportamento para chave repetida e para requisição concorrente com a mesma chave
- 🤖 Testes de cenário, revisão
- 🎤 "Como você evita uma transferência (ou autorização) duplicada?"

## Fase L — Leitura offline (31/12 a 10/01, viagem)
Sem código. Baixe os materiais antes de viajar. Anote dúvidas no `docs/learning-log.md` para revisar com o Claude na volta.
- EMV: chip vs tarja, **TLV** (tag-length-value), o que vai no DE 55, níveis de certificação (L1, L2, L3), o que é um kernel
- Criptografia de pagamento (conceitos): PIN block (formato ISO 0), MAC, chaves (TMK, TPK, ZPK), **HSM**, DUKPT em alto nível
- PCI DSS: o que é, o que protege, por que não se loga PAN
- Revisar o learning log das fases 1 a P2 e responder de novo os 🎤

---

## Fases opcionais (depois de 11/01)

## Fase 5 — Transactional Outbox
- 🧠 Problema do dual write (banco + Kafka), outbox, at-least-once, ordenação
- ✍️ Tabela `outbox_event` gravada na mesma transação da transferência; relay/poller que publica e marca como enviado
- 🤖 Config do producer, revisão de falhas de borda
- 🎤 "Por que não publicar direto no Kafka dentro do caso de uso?"

## Fase 6 — Kafka e notificações
- 🧠 Tópicos, partições, chave de particionamento (accountId), consumer groups, offsets, commit
- ✍️ Consumer de notificações **idempotente** (deduplicação por event id)
- 🤖 Config do consumer, testes com Testcontainers Kafka
- 🎤 "O que garante a ordem das mensagens? O que acontece se o consumer cair no meio?"

## Fase 7 — Falhas e reprocessamento
- 🧠 Retry com backoff, Dead Letter Topic, poison messages, replay
- ✍️ Simulação de falhas (Kafka fora, consumer lançando exceção), DLT, endpoint/rotina de reprocessamento
- 🤖 Scripts para derrubar/subir o broker no Compose
- 🎤 "O que acontece quando o Kafka fica indisponível?" / "Como o sistema se recupera após uma falha?"

## Fase 8 — Observabilidade
- 🧠 Actuator, Micrometer, métricas vs logs vs traces, consumer lag
- ✍️ Métricas customizadas (eventos pendentes no outbox, falhas por tipo, autorizações por código de resposta), health indicators
- 🤖 Config de exposição de métricas (e Prometheus/Grafana opcional)
- 🎤 "Como você identifica um consumidor atrasado?"

## Fase 9 — Testes de integração ponta a ponta
- 🧠 Pirâmide de testes, testes de contrato/cenário, determinismo
- ✍️ Cenários completos com Testcontainers (Postgres + Kafka): transferência → outbox → Kafka → notificação; falha e recuperação
- 🤖 Revisão de flakiness
- 🎤 "Como testa a integração entre banco e Kafka?"

## Fase 10 — Fechamento
- ✍️ README com diagrama de arquitetura, decisões (ADRs), como rodar e respostas escritas aos 🎤 principais
- 🤖 Revisão final como "tech lead no primeiro mês" (code review e perguntas sobre o domínio)
