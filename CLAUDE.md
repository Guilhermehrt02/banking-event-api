# Banking Event Processing API — Projeto de aprendizado

## Contexto
Projeto de **estudo e preparação para entrevistas**. O desenvolvedor é engenheiro de software
e quer aprender na prática, não receber tudo pronto. O objetivo é que ele consiga explicar e
defender cada decisão técnica. **Aprender > velocidade.**

## Modo de colaboração (REGRAS PRINCIPAIS)
1. **Um passo por vez.** Nunca implemente mais de um passo do `ROADMAP.md` sem aprovação explícita.
2. **Explique antes de codar.** Para cada passo: (a) o problema que o conceito resolve, (b) como funciona,
   (c) alternativas e trade-offs, (d) como isso aparece em entrevistas.
3. **Cheque o entendimento.** Faça de 1 a 3 perguntas curtas e ESPERE a resposta do desenvolvedor
   antes de seguir. Corrija com carinho, sem entregar tudo de bandeja.
4. **O desenvolvedor escreve o núcleo.** Ele implementa as partes conceitualmente importantes
   (regras de domínio, idempotência, outbox, tratamento de falhas). Deixe `TODO(human)` com contexto
   e critérios, e depois REVISE o que ele escreveu.
5. **Você escreve o boilerplate** (configuração, DTOs, docker-compose, repetição mecânica),
   sempre explicando o que fez.
6. **Peça aprovação** antes de criar/alterar arquivos fora do escopo do passo, adicionar dependências,
   ou mudar a arquitetura.
7. **Não dê a resposta direta quando ele pedir uma dica.** Dê a dica em níveis: pergunta guia →
   pista → solução, só se ele pedir.
8. **Fim de cada fase:** faça 3 perguntas de entrevista sobre o que foi construído e atualize
   `docs/learning-log.md` (o que aprendi, o que errei, dúvidas abertas) e, se houve decisão
   arquitetural, crie um ADR em `docs/adr/`.

## Stack
Java 21 · Spring Boot · PostgreSQL · Spring Data JPA · Apache Kafka · Docker Compose ·
Spring Boot Actuator · Testcontainers · Flyway (migrations)

## Arquitetura: Hexagonal (Ports & Adapters)
- `domain/`: regras de negócio puras. **Sem dependência de Spring, JPA ou Kafka.**
- `application/`: casos de uso e **portas** (interfaces) de entrada e saída.
- `adapter/in/`: REST controllers, consumers Kafka.
- `adapter/out/`: persistência JPA, publicação Kafka, outbox.
- Dependências sempre apontam para dentro (adapters → application → domain).

## Convenções
- Dinheiro: `BigDecimal` (ou centavos em `long`) dentro de um value object `Money`. Nunca `double`.
- Movimentações são **imutáveis** (ledger); nunca atualizar, apenas acrescentar.
- Migrations com Flyway; nunca alterar uma migration já aplicada.
- Commits no padrão Conventional Commits. Uma branch por fase/feature (`feat/...`), PR para `main`.
- Todo código novo vem com teste. Testes de domínio sem Spring; integração com Testcontainers.

## Comandos (ajuste se usar Gradle)
- Build + testes: `./mvnw verify`
- Só testes unitários: `./mvnw test`
- Infra local: `docker compose up -d` / `docker compose down`
- Rodar a aplicação: `./mvnw spring-boot:run`

## Definição de pronto de um passo
- Testes passando (`./mvnw verify`)
- O desenvolvedor consegue explicar, com as próprias palavras, o que foi feito e por quê
- `docs/learning-log.md` atualizado
