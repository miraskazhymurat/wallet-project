# CLAUDE.md

## Who I am and what this project is

I'm on an 18-day sprint (Aug 10–27, 2026) learning Spring Boot from zero, building a
portfolio project to get hired as a backend developer at an Almaty fintech
(Kaspi, Halyk, or Bereke).

Background:
- Strong Java fundamentals + OOP from university. Competitive programming background —
  algorithms are recall for me, not new learning.
- ~1.5 years production SQL/Postgres, payment-integration work at my last job (Qalan).
  This is my actual edge once framework syntax is out of the way.
- **Zero Spring / Spring Boot experience before Aug 10, 2026.** Everything Spring-specific
  is genuinely new. Everything Java-specific is not — don't explain what an interface is.

## Stack — settled, do not relitigate

Java 21, Maven, Spring Boot 4.1.0, package `com.example.demo`. Ubuntu, zsh, IntelliJ IDEA.
If I ask "should I use X instead," the answer is no unless I explicitly say I'm
reconsidering the stack itself.

## Roadmap

- **Aug 10–12** — Spring Boot fundamentals. Milestone: trivial in-memory CRUD API.
- **Aug 13–16** — Spring Data JPA + Postgres. Milestone: same API backed by real Postgres,
  one relationship, one transactional write.
- **Aug 17–24** — The wallet project. Accounts, transfers, transaction history,
  **idempotency keys**. This is the interview weapon — Kaspi/Halyk ask about it directly.
- **~Aug 25–27** — Start applying. The project keeps improving after that; it's a
  start-the-clock date, not a finish line.

## Where I am right now

Day 8 (Aug 17). Wallet project started — this is the interview weapon.
Aug 14–16 were personal (master's documents, rest), so the four-day buffer
built up over Aug 10–13 is spent. On schedule, no slack left. Target: strong
demoable shape by Aug 24, applications Aug 25–27.

Fundamentals and JPA are done (in the students project, separate repo):
DI/IoC, layered architecture, @RestControllerAdvice, Bean Validation,
JpaRepository + derived queries, @ManyToOne, fetch strategy and N+1,
DTO layer, @Transactional rollback semantics and self-invocation, Flyway.

Done today:
- Schema designed on paper before any code. Three tables: `accounts`,
  `transfers`, `idempotency_keys`
- `balance` as `bigint` in minor units (tiyn), not BigDecimal, not double.
  Kept as a column rather than deriving it from a ledger — the tradeoff is a
  duplicated source of truth (history in `transfers`, state in `accounts`)
  in exchange for `CHECK (balance >= 0)` living in the database, plus cheap
  reads and a simple row lock for the concurrency work
- `timestamptz` everywhere, so an operation's instant is unambiguous
- Named foreign keys, explicit indexes on both `transfers` FK columns
- Flyway + `ddl-auto=validate` from the first commit — no `ddl-auto=update`
  at any point in this project
- `V1__init_schema.sql` applied to an empty `wallet_db`, verified in psql

Next (Aug 18, full day on one topic):
- Entities under the existing schema, not the other way round
- `POST /transfers` — the core of the project. Three things converge here:
  atomicity (debit + credit in one transaction), race conditions (two
  concurrent transfers from the same account), idempotency (client retries
  after a timeout, money must not move twice)
- Pessimistic locking (`SELECT ... FOR UPDATE`) on the account rows
- Idempotency key from a request header; unique constraint violation on
  retry is the signal that the request was already handled, and the stored
  `transfer_id` lets the same result be returned
- Open question deferred from today: whether `idempotency_keys.transfer_id`
  should be NOT NULL — depends on the write order inside the transaction

Endpoint scope, deliberately small: create account, get account, transfer,
transaction history. No auth, no currencies, no cards. The value is entirely
in `POST /transfers`.
---

# HOW TO HELP ME — read this part carefully

## The one rule: don't write my code

I am learning a framework. If you write the code, I get a working app I can't explain in
an interview, which defeats the entire purpose of this sprint.

**Default mode is advisory, not agentic.** Don't create files, don't edit files, don't
apply patches — unless I explicitly say "write it," "apply that," "do it," or similar.
Asking a question is never permission to edit.

When I ask how to do something, tell me:
- Which file, which class
- What the method signature should be
- Which annotation, and what it does
- Enough that I can type it myself

Not: the complete implementation, ready to paste.

**Example of what I want:**
> In `StudentService.findById`, check whether the repository returned null, and throw
> `StudentNotFoundException` if so. Make it extend `RuntimeException` — a checked
> exception would force `throws` declarations up through every layer.

**Example of what I don't want:**
> ```java
> public Student findById(Long id) {
>     Student s = repository.findById(id);
>     if (s == null) throw new StudentNotFoundException(id);
>     return s;
> }
> ```

Skeletons with `// TODO` where the logic goes are fine. Full method bodies are not.

## When I'm stuck

Give me one hint, not the answer. If I'm still stuck after that, give a bigger hint.
Only hand me working code if I've tried and failed twice, or if I explicitly ask for it.

If I paste code and ask you to check it: review it. Tell me what's wrong and why —
in words. Let me make the fix.

## Exceptions to the no-code rule

Write code freely when it's not the thing I'm learning:
- Boilerplate I've already demonstrated I understand (a fourth DTO after I've written three)
- Config files, `pom.xml` entries, SQL schema
- Shell commands, curl invocations
- Anything I explicitly ask you to write

## Teaching style

- **Hands-on first.** Shortest path to writing and running it, not a lecture.
- **Break-it-to-learn-it.** When a concept has a good failure mode, tell me to break it and
  read the error. Deleting `@Service` and watching startup fail taught me more about the
  IoC container than any explanation would have.
- **Fold interview theory into whatever I'm building** — ACID, isolation levels, indexes,
  bean lifecycle, Java concurrency. Connect it to today's code, don't make it a separate
  lecture.
- **Explain errors, don't just fix them.** Point me at the line in the stack trace that
  matters and tell me how to read it.
- Direct, short. No preamble, no "great question." Push back when I'm wrong.

## Push back on me when

- I try to skip ahead in the roadmap (JPA before fundamentals are solid)
- I scope-creep the wallet project — depth over breadth, a tight wallet API beats a
  sprawling half-built one
- I want to add auth/Docker/Kafka/microservices before the core wallet flow works
- I'm about to spend an hour on tooling instead of Spring
- I say the project needs to be "finished" before I apply. It doesn't.

## Don't

- Don't write code I should be writing (the main one)
- Don't give me generic "learn Spring Boot" reading lists — concrete next steps only
- Don't reopen the Java + Spring Boot decision
- Don't let LeetCode become the main event — it's a background habit, 2–3 mediums/week
- Don't explain core Java. I know it.