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

---

## Fase 1 — Passo 1: `Money` e checkpoint de Java (2026-11-25 a 26)

**Entregue:** pacotes da hexagonal (`domain`, `application`, `adapter/in`, `adapter/out`),
`Money` como `record` (BigDecimal + Currency) com invariantes, versão Java 8 (`MoneyJava8`, em
`src/test/.../legacy`) e 11 testes de domínio puro (sem Spring).

**Decisão de formato de estudo:** conceitos do checkpoint de Java aprendidos *dentro do código do
projeto*, não em quiz isolado.

### O que aprendi
- **Value object**: igualdade por valor, imutável, sempre válido. "Estados inválidos irrepresentáveis":
  se o construtor valida, nenhuma outra classe precisa checar de novo.
- **`record`** gera: classe `final`, campos `private final`, construtor canônico, accessors
  `amount()`/`currency()`, `equals`, `hashCode`, `toString`. Em Java 8 isso são ~60 linhas à mão.
- **Construtor compacto** roda antes da atribuição dos campos; reatribuir o parâmetro
  (`amount = amount.setScale(...)`) muda o que fica guardado.
- **`BigDecimal.equals` compara valor E escala** (`10.0 ≠ 10.00`); `compareTo` compara só valor.
  Por isso: normalizar a escala no construtor e usar `compareTo` para checar negativo.
- **Escala por moeda**: `currency.getDefaultFractionDigits()` (BRL 2, JPY 0, KWD 3; pseudo-moedas -1).
- **`RoundingMode.UNNECESSARY`** rejeita em vez de arredondar; `HALF_EVEN` é o arredondamento bancário.
- **Convenção de exceções**: argumento `null` proibido → `NullPointerException` (`Objects.requireNonNull`);
  valor não nulo mas inválido → `IllegalArgumentException`. Encadear a causa (`new IAE(msg, e)`).
- **Contrato `equals`/`hashCode`**: objetos iguais ⇒ mesmo `hashCode`; senão o `HashSet`/`HashMap`
  procura no "balde" errado e o objeto "some".
- **`instanceof` vs `getClass()`** no `equals`: `instanceof` aceita subclasses (pode quebrar a
  simetria); `getClass()` exige a classe exata (quebra Liskov). Em classe `final` não há diferença.
  Não dá para estender uma classe instanciável, acrescentar campo de valor e manter o contrato →
  **composição em vez de herança**, e value objects são `final`.
- **Java 8 vs 21**: sem `record` e sem pattern matching de `instanceof` (Java 16+) no 8;
  `Objects.equals`/`Objects.hash`/`requireNonNull` já existem desde o 7. Legado também usa Lombok `@Value`.
- **Testes**: ciclo red → green; "o teste quebraria se eu desfizesse a regra?" (ideia do mutation testing).

### O que errei (e a correção)
- Lancei `IllegalArgumentException` para `null`. Funciona, mas a convenção do JDK é NPE.
  A mensagem única ("Amount and currency are mandatory") também não dizia *qual* campo veio nulo.
- Escala fixa em `setScale(2, ...)` — quebraria JPY (0 casas) e KWD (3 casas).
- No `equals` do `MoneyJava8`, os nomes das variáveis estavam trocados (`hasSameCurrency` comparava
  `amount`). Os testes passavam porque `&&` é comutativo — **testes verificam comportamento, não intenção**;
  só review pega. Adotei a forma idiomática com retornos antecipados.
- Expliquei o `UNNECESSARY` pelo "o quê" (rejeitar escala maior) sem o "porquê": arredondar em silêncio
  esconde bugs e perde frações; quem tem contexto (juros, câmbio) decide o arredondamento antes.
- Não sabia responder `instanceof` vs `getClass()`; cheguei lá com o contraexemplo `MoneyComCashback`.

### Dúvidas abertas
- `equals` de entidades JPA com proxies do Hibernate (por que costuma usar `instanceof`) → Fase 2.
- Como o `Money` vira DE 4 (12 dígitos, unidades menores) e DE 49 (código numérico ISO 4217) → Fase P1.
- Operações `plus`/`minus` e o que fazer com moedas diferentes → passo 2 (`Account`).
