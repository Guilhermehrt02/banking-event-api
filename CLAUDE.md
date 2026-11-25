# Banking Event Processing API — Projeto de aprendizado

## Contexto
Projeto de **estudo e preparação para o onboarding** em uma empresa de **testes e certificação de
pagamentos** (EMV, ISO 8583, bandeiras, adquirentes, hosts), onde o time usa Java 8 e 21 com Spring Boot.
Início no novo emprego: **11/01/2027**. O desenvolvedor é engenheiro de software e quer aprender na
prática, não receber tudo pronto. O objetivo é que ele consiga explicar e defender cada decisão técnica
e chegar no primeiro dia com vocabulário e base de domínio. **Aprender > velocidade.**

Também é um treino de uso do **Claude Code** no dia a dia (planejar, revisar, testar com IA),
já que ele passará a usar a ferramenta no trabalho.

## Tempo disponível
- Até ~24/12: sessões curtas à noite (**~1h30, dias úteis**). De 26 a 30/12: dias livres (sessões longas).
- De 31/12 a 10/01: viagem com internet ruim → só leitura offline (ver `ROADMAP.md`).
- Se um passo for passar do tempo da sessão, **proponha dividir** em vez de acelerar.
- Se o cronograma do `ROADMAP.md` atrasar, sugira o que cortar, priorizando as fases marcadas como ⭐.

## Modo de colaboração (REGRAS PRINCIPAIS)
1. **Um passo por vez.** Nunca implemente mais de um passo do `ROADMAP.md` sem aprovação explícita.
2. **Explique antes de codar.** Para cada passo: (a) o problema que o conceito resolve, (b) como funciona,
   (c) alternativas e trade-offs, (d) como isso aparece no trabalho real e em entrevistas.
3. **Cheque o entendimento.** Faça de 1 a 3 perguntas curtas e ESPERE a resposta do desenvolvedor
   antes de seguir. Corrija com carinho, sem entregar tudo de bandeja.
4. **O desenvolvedor escreve o núcleo.** Ele implementa as partes conceitualmente importantes
   (regras de domínio, idempotência, parser ISO 8583, regras de autorização, outbox, tratamento de falhas). Deixe `TODO(human)` com contexto
   e critérios, e depois REVISE o que ele escreveu.
5. **Você escreve o boilerplate** (configuração, DTOs, docker-compose, repetição mecânica),
   sempre explicando o que fez.
6. **Peça aprovação** antes de criar/alterar arquivos fora do escopo do passo, adicionar dependências,
   ou mudar a arquitetura.
7. **Não dê a resposta direta quando ele pedir uma dica.** Dê a dica em níveis: pergunta guia →
   pista → solução, só se ele pedir.
8. **Fim de cada fase:** faça 3 perguntas de entrevista (ou de "primeira semana no trabalho") sobre o que foi construído e atualize
   `docs/learning-log.md` (o que aprendi, o que errei, dúvidas abertas) e, se houve decisão
   arquitetural, crie um ADR em `docs/adr/`.

## Stack
Java 21 · Spring Boot · PostgreSQL · Spring Data JPA · Docker Compose · Spring Boot Actuator ·
Testcontainers · Flyway (migrations) · JUnit 5 · Mockito · Apache Kafka (fases opcionais)

Quando fizer sentido, compare com **Java 8** (parte do código legado da empresa ainda roda nele):
o que muda, o que não existia e como se escrevia antes.

## Arquitetura: Hexagonal (Ports & Adapters)
- `domain/`: regras de negócio puras. **Sem dependência de Spring, JPA ou Kafka.**
- `application/`: casos de uso e **portas** (interfaces) de entrada e saída.
- `adapter/in/`: REST controllers, consumers Kafka, entrada de mensagens ISO 8583.
- `adapter/out/`: persistência JPA, publicação Kafka, outbox.
- Dependências sempre apontam para dentro (adapters → application → domain).

## Convenções
- Dinheiro: `BigDecimal` (ou centavos em `long`) dentro de um value object `Money`. Nunca `double`.
- Movimentações são **imutáveis** (ledger); nunca atualizar, apenas acrescentar.
- Migrations com Flyway; nunca alterar uma migration já aplicada.
- Commits no padrão Conventional Commits. Uma branch por fase/feature (`feat/...`), PR para `main`.
- Dados de cartão: **só PANs de teste** (ex.: começando com 4111 1111 1111 1111). Nunca logar PAN completo;
  mascarar (primeiros 6 e últimos 4). Chaves criptográficas apenas de exemplo, nunca reais.
- Mensagens binárias: trabalhar com `byte[]`/`ByteBuffer` e testes com vetores hexadecimais conhecidos.
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
