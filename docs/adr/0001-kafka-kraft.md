# ADR 0001 — Kafka em modo KRaft, nó único combinado, para desenvolvimento local

- **Status:** Aceito
- **Data:** 2026-11-17
- **Fase:** 0 — Fundação

## Contexto
A aplicação vai publicar eventos de domínio (ex.: `TransferCompleted`) via Transactional Outbox
(Fase 5) e consumi-los no serviço de notificações (Fase 6). Precisamos de um Kafka local,
reproduzível, leve e próximo do que se usa hoje em produção.

Historicamente o Kafka dependia do **ZooKeeper** para guardar metadados do cluster (tópicos,
líderes de partição, brokers vivos). Isso implicava:
- operar **dois sistemas distribuídos** (deploy, versão, segurança e monitoramento separados);
- **failover lento** do controller: o novo controller precisava recarregar todos os metadados
  do ZooKeeper antes de atuar, levando minutos em clusters grandes;
- limite prático de escala no número de partições.

Desde o Kafka 4.0 o ZooKeeper foi removido; **KRaft** é o único modo suportado.

## Decisão
Usar `apache/kafka:4.3.2` em **KRaft**, com **um único nó** exercendo `broker` e `controller`
(`KAFKA_PROCESS_ROLES: broker,controller`), no `docker-compose.yml`:
- listener `CONTROLLER` (9093) separado, não exposto ao host — tráfego Raft e broker↔controller;
- listeners de dados `INTERNAL://kafka:29092` (rede do compose) e `EXTERNAL://localhost:9092` (host);
- `auto.create.topics.enable=false` — tópicos serão criados explicitamente;
- fatores de replicação dos tópicos internos = 1 (só há um broker);
- **sem volume** para os dados do Kafka (estado descartável em dev).

## Alternativas consideradas
- **Kafka + ZooKeeper**: legado; removido no Kafka 4.x. Mais containers e nada a aprender de útil para o futuro.
- **Redpanda**: compatível com a API do Kafka e mais leve, mas não é o Kafka real; diferenças de
  comportamento podem confundir o aprendizado.
- **Cluster KRaft com 3 controllers dedicados + N brokers**: espelha produção, mas é pesado para
  dev e não agrega nas fases atuais. Pode ser revisitado na Fase 7 (falhas).
- **RabbitMQ**: fila tradicional; sem retenção/replay nem ordenação por chave/partição no mesmo modelo.

## Consequências
- ✅ Um container, sobe em segundos, mesma arquitetura de metadados de produção (Raft).
- ✅ Separação explícita entre plano de dados e plano de controle já visível na config.
- ⚠️ Quórum de **1 voto**: nenhuma tolerância a falhas. Não serve para simular eleição de líder
  ou perda de broker — limitação aceita para dev.
- ⚠️ Sem volume: tópicos, mensagens e **offsets de consumer groups** somem no `docker compose down`,
  enquanto o Postgres persiste. Essa assimetria não existe em produção e pode mascarar bugs
  (ex.: outbox marcado como enviado sem a mensagem existir no Kafka). Revisitar nas Fases 5–7.
- ⚠️ `advertised.listeners` precisa refletir o ponto de vista de cada cliente; erro aqui causa
  "conecta no bootstrap e depois dá timeout".

## Atualização — 2026-11-25
O ROADMAP foi refocado no onboarding em pagamentos: as fases com Kafka (5 a 10) passaram a ser
opcionais, depois de 11/01/2027. A decisão acima continua válida, mas o serviço `kafka` agora está
atrás do **profile** `kafka` no `docker-compose.yml`:
- `docker compose up -d` → só Postgres (economiza RAM e tempo de startup nas fases 1 a P2);
- `docker compose --profile kafka up -d` → Postgres + Kafka.
- Atenção: `docker compose down` sem `--profile kafka` **não** derruba o Kafka (comportamento
  confirmado no Compose v5.7.0).
