# Roadmap — Banking Event Processing API

Legenda: 🧠 conceito · ✍️ você implementa · 🤖 Claude implementa/explica · 🎤 pergunta de entrevista

Regra: só avance de fase quando o 🎤 for respondido por você com segurança.

---

## Fase 0 — Fundação
- 🧠 Containers, Docker Compose, Kafka em modo KRaft, o que é um broker, tópico e partição
- 🤖 Skeleton Spring Boot, `docker-compose.yml` (Postgres + Kafka), Actuator `/health`
- ✍️ Ler e explicar cada serviço do compose; subir tudo e validar
- 🎤 "Por que usar Docker Compose no desenvolvimento? O que ele não resolve?"

## Fase 1 — Domínio e Arquitetura Hexagonal
- 🧠 Ports & Adapters, por que o domínio não conhece framework, value objects, invariantes
- ✍️ `Money`, `Account` (abrir conta, depositar, debitar, saldo insuficiente), testes unitários puros
- 🤖 Estrutura de pacotes, revisão do seu código
- 🎤 "Por que arquitetura hexagonal? Qual o custo?"

## Fase 2 — Persistência
- 🧠 JPA/Hibernate, mapeamento domínio vs entidade, migrations, Testcontainers
- ✍️ Porta de saída `AccountRepository` + adapter JPA, migration Flyway, caso de uso "criar conta" e "consultar saldo"
- 🤖 Config do Testcontainers, controller REST
- 🎤 "Como você testa a integração com o banco sem mock?"

## Fase 3 — Transferências e consistência
- 🧠 ACID, níveis de isolamento, lock otimista vs pessimista, ledger imutável (partidas dobradas)
- ✍️ Caso de uso `Transfer` (débito + crédito + movimentações na MESMA transação), tratamento de concorrência
- 🤖 Teste de concorrência (várias transferências simultâneas na mesma conta)
- 🎤 "Como você garante a consistência das movimentações?" / "E se duas transferências debitarem a mesma conta ao mesmo tempo?"

## Fase 4 — Idempotency Key
- 🧠 Idempotência, retries de cliente, replay da resposta, constraint única, janela de expiração
- ✍️ Header `Idempotency-Key`, tabela de chaves, comportamento para chave repetida e para requisição concorrente com a mesma chave
- 🤖 Testes de cenário, revisão
- 🎤 "Como você evita uma transferência duplicada?"

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
- ✍️ Métricas customizadas (eventos pendentes no outbox, falhas por tipo, transferências/seg), health indicators
- 🤖 Config de exposição de métricas (e Prometheus/Grafana opcional)
- 🎤 "Como você identifica um consumidor atrasado?"

## Fase 9 — Testes de integração ponta a ponta
- 🧠 Pirâmide de testes, testes de contrato/cenário, determinismo
- ✍️ Cenários completos com Testcontainers (Postgres + Kafka): transferência → outbox → Kafka → notificação; falha e recuperação
- 🤖 Revisão de flakiness
- 🎤 "Como testa a integração entre banco e Kafka?"

## Fase 10 — Fechamento
- ✍️ README com diagrama de arquitetura, decisões (ADRs), como rodar, e respostas escritas às 6 perguntas-alvo
- 🤖 Revisão final como "entrevistador sênior" (simulação de entrevista técnica)
