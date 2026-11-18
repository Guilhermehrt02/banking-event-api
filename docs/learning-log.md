# Learning Log

Registro honesto do que aprendi, do que errei e do que ficou em aberto em cada fase.

---

## Fase 0 — Fundação (2026-11-17)

**Entregue:** skeleton Spring Boot 4.1.2 / Java 21, `docker-compose.yml` com Postgres 16.15 e
Kafka 4.3.2 em KRaft (nó único), Actuator `/health` UP com `db` UP, `./mvnw verify` verde.

### O que aprendi
- **Docker Compose** transforma a infra de dev em código versionado: mesmas versões para todos,
  um comando para subir, rede interna entre containers e portas expostas para o host.
- **Volume nomeado**: os dados do Postgres sobrevivem ao `docker compose down`; só somem com `down -v`.
  O Flyway recria *schema*, nunca *dados*.
- **Healthchecks** fazem o `depends_on` / `up --wait` esperarem o serviço ficar *pronto*, não só *iniciado*.
- **Kafka**: broker (recebe, grava, entrega mensagens), tópico (nome lógico), partição (log
  append-only ordenado, unidade de paralelismo), offset (posição na partição).
- **Ordem** só é garantida dentro de uma partição → mesma chave (`accountId`) = mesma partição.
  Cuidados: retries do producer (idempotência) e aumento de partições mudam o mapeamento.
- **Broker vs controller**: broker = plano de dados; controller = plano de controle (metadados,
  eleição de líderes). No KRaft os metadados são um log replicado via Raft entre controllers.
- **Listener `CONTROLLER` separado**: falam por ele os controllers entre si *e* os brokers com os
  controllers (registro, heartbeats, metadados). Separar isola o tráfego de controle do de dados,
  evitando que heartbeats atrasem e gerem falsos positivos de falha. A app nunca usa a 9093.
- **Quórum de 1 voto** (nosso compose): zero tolerância a falhas. Produção: 3 controllers (maioria).
- **Advertised listeners**: o bootstrap só descobre o cluster; o cliente depois conecta nos
  endereços *anunciados*. Por isso há `INTERNAL://kafka:29092` e `EXTERNAL://localhost:9092`.
- **Liveness vs readiness** no Actuator: "devo reiniciar?" vs "posso receber tráfego?".

### O que errei (e a correção)
- Achei que os dados do Kafka "só importam enquanto a app roda". **Errado**: no Kafka os dados são
  o produto (retenção, replay, offsets dos consumer groups). Ele está sem volume só por conveniência de dev.
- Expliquei o timeout após a conexão inicial com heartbeats/controller (conceito certo, sintoma
  errado). **Causa real**: `advertised.listeners` anuncia um host que o cliente não alcança.
  Lição: raciocinar a partir do sintoma ("conecta e depois falha" → algo muda entre a 1ª conexão e as seguintes).
- Na primeira tentativa disse que "faz sentido separar o controller para não sobrecarregar o broker".
  Mais preciso: **isolamento** do plano de controle, não só carga.
- Descrevi o ZooKeeper como "componente único". Ele também era um cluster; o problema era ser
  **um segundo sistema distribuído** para operar + failover lento do controller em clusters grandes.

### Dúvidas abertas
- Se o relay do outbox marcar um evento como `sent` no Postgres e depois o Kafka for recriado
  sem volume, em que estado ficam os dois sistemas? Que bug isso esconde nos testes? (retomar na Fase 5)
- Como o cliente Kafka trata a troca de líder de partição e o refresh de metadados? (Fase 6)
- Quando vale colocar volume no Kafka do compose (ex.: testes de replay na Fase 7)?

### Ambiente (para lembrar)
- macOS arm64. `brew install --cask` com `.pkg` exige `sudo` → rodar no Terminal.app, não pelo `!` do Claude Code.
- Testes de contexto ainda dependem do Postgres do compose de pé; Testcontainers entra na Fase 2.
