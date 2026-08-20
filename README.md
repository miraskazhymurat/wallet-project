# Wallet

A small payment API: accounts, transfers between them, and transaction history.

The endpoint surface is deliberately narrow. Almost all of the work went into one
endpoint — `POST /transfers` — because that is where atomicity, concurrency and
idempotency meet. The rest of the API exists to give that endpoint something to
move money between.

## Stack

Java 21, Spring Boot 4.1.0, PostgreSQL, Flyway, Maven.

## Running it

Create the database:

```sql
CREATE DATABASE wallet_db OWNER <your_user>;
```

Point the application at it in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/wallet_db
spring.datasource.username=${DB_USERNAME:<your_user>}
spring.datasource.password=${DB_PASSWORD}
```

The password is read from the environment and is not stored in the repository:

```bash
export DB_PASSWORD=<your_password>
./mvnw spring-boot:run
```

Flyway creates the schema on first start. There is no `ddl-auto=update` anywhere
in this project — see *Schema ownership* below.

## API

### Create an account

```bash
curl -X POST localhost:8080/accounts \
  -H 'Content-Type: application/json' \
  -d '{"ownerName":"Aisha","balance":10000}'
```

`ownerName` must not be blank, `balance` must not be negative. Amounts are in
minor units, so `10000` is 100.00.

Returns `201`-style payload with the assigned id, or `400` if the body fails
validation.

### Get an account

```bash
curl localhost:8080/accounts/1
```

`200` with the account, or `404` if it does not exist.

### Transaction history

```bash
curl 'localhost:8080/accounts/1/transactions?page=0&size=20'
```

Returns every transfer where the account is either the sender or the receiver,
newest first. Paginated — `page`, `size` and `sort` are accepted.

`404` if the account does not exist. An account with no transfers returns an
empty page, not a 404: the account exists, the filter just matched nothing.

### Transfer

```bash
curl -X POST localhost:8080/transfers \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000' \
  -d '{"fromAccountId":1,"toAccountId":2,"amount":500}'
```

The `Idempotency-Key` header is required. The client generates it — see
*Idempotency* below for why it cannot be generated server-side.

| Status | Meaning |
|---|---|
| `200` | Transfer completed, or an identical request was already processed |
| `400` | Invalid body, or sender and receiver are the same account |
| `404` | One of the accounts does not exist |
| `409` | This key was already used for a different transfer |
| `422` | Insufficient funds |

## Design decisions

### Money is a `bigint` in minor units

Balances and amounts are stored as whole tiyn, not `BigDecimal` and never
`double`.

`double` is a binary floating point type: `0.1 + 0.2` is `0.30000000000000004`.
That is disqualifying for money. `BigDecimal` is exact, but it is slower, it
carries scale rules that have to be managed on every division, and comparisons
are easy to get subtly wrong. Integers in the smallest unit sidestep all of it —
addition, subtraction and comparison are exact and cheap, and there is no
rounding step anywhere in the transfer path.

### Balance is a column, not a derived ledger

Two models were considered:

- **Ledger** — no stored balance at all. Every movement is an entry, and a
  balance is `SUM(amount)` over that account's entries.
- **Column** — `accounts.balance` holds the current value, and `transfers`
  records what happened.

This project uses the column.

What that buys: `CHECK (balance >= 0)` can live in the database. Under a pure
ledger there is no column to constrain, so the non-negative invariant would have
to be enforced entirely in application code. Having the database refuse to store
a negative balance means a bug in the service layer cannot silently corrupt an
account. Reads are also a single-row lookup rather than an aggregation, and
row-level locking has an obvious target.

What that costs: **two sources of truth for the same fact**. `transfers` is the
history of what moved; `accounts.balance` is the current state. Nothing
structurally guarantees they agree. If a bug writes one without the other, the
two diverge and there is no way to tell from the data alone which one is right.
A production system on this model needs periodic reconciliation — replay the
transfer history and compare against stored balances.

Under a ledger this class of bug is impossible by construction, because the
balance is not stored at all. The tradeoff is read cost, which grows with
history and is usually addressed with periodic balance snapshots.

The concurrency section below shows this divergence happening for real.

### Transfers reference accounts by id, not `@ManyToOne`

`Transfer` holds `Long fromAccountId` and `Long toAccountId` rather than
`@ManyToOne Account`.

A transfer is a fact about two account ids. Nothing in the transfer path or the
history endpoint needs the account objects themselves, and modelling the
relationship as a JPA association would mean every read of a transfer list can
trigger a lazy load per row. The history endpoint returns a page of transfers in
a single query plus one `count`, with no N+1 — verified in the SQL log.

This is a deliberate departure from textbook JPA mapping, made because the read
pattern is known.

### `timestamptz`, not `timestamp`

All timestamps are `timestamptz` in the database and `OffsetDateTime` in Java.

A plain `timestamp` stores "14:30" without recording whose 14:30 it is. Move the
server to a different timezone, or serve a client in another one, and the
instant an operation happened becomes unrecoverable. For financial records the
absolute instant has to be unambiguous.

### Schema ownership: Flyway from the first commit

Flyway owns the schema. `spring.jpa.hibernate.ddl-auto` is set to `validate`,
which changes nothing and only checks that the entities and the tables agree,
failing at startup if they do not.

`ddl-auto=update` is not used at any point. It only adds — it will not drop a
removed column, will not change a column's type, and on a rename it adds a second
column beside the first and leaves the original with its data. It also produces
no history: there is no way to ask what state the schema is in, roll back, or
reproduce it elsewhere. The schema becomes a side effect of whatever happened to
be on the classpath at the last startup.

Failing at deploy time because the database does not match the code is far
cheaper than discovering it mid-transaction.

## Concurrency

The interesting part of this project. Each problem below was reproduced first
and fixed second.

### Lost update

Starting state: account 1 holds 1000, account 2 holds 0. Two transfers of 800
from account 1, issued concurrently.

```
T1: SELECT balance -> 1000
T2: SELECT balance -> 1000        both read before either wrote
T1: 1000 >= 800, proceed
T2: 1000 >= 800, proceed          T2 cannot see that T1 has already decided
T1: UPDATE balance = 1000 - 800 = 200
T2: UPDATE balance = 1000 - 800 = 200   overwrites, rather than 200 - 800
```

Observed result: account 1 at 200, account 2 at 800, **two committed rows in
`transfers` totalling 1600**, and both clients received `200 OK` with a valid
transfer id. 800 was actually moved. One of those transfers is a phantom —
recorded, acknowledged, and backed by no money movement.

Two things about this are worth stating explicitly.

**`CHECK (balance >= 0)` did not catch it.** Both transactions wrote 200, and 200
is not negative. The constraint guards against overdraft, not against a stale
read being written back.

**`sum(balance)` did not catch it either.** It still totalled 1000 — the two
lost updates cancelled out symmetrically because both transfers ran between the
same pair of accounts. Two transfers from one account to *different* accounts
would have made money appear outright. This is the practical argument for
reconciling balances against transfer history rather than against a total.

`@Transactional` alone does not prevent any of this. Atomicity guarantees that a
debit and credit succeed or fail together; it says nothing about two transactions
reading the same row and both acting on it. Under `READ COMMITTED`, T2's read of
1000 was a perfectly legal read of committed data. Nothing promised it would
still be true by the time T2 wrote.

**Fix:** `@Lock(LockModeType.PESSIMISTIC_WRITE)` on the account lookup, which
issues `SELECT ... FOR UPDATE`. The second transaction blocks on its read until
the first commits, then reads 200, evaluates `200 >= 800`, and is correctly
rejected with `422`. The key point is not that it is rejected — it is that the
second transaction never reads a stale value, because its read is deferred.

Pessimistic rather than optimistic locking, because contention on a balance is an
expected condition rather than an exception. A busy account would produce a
steady stream of `OptimisticLockException` and force retry logic onto the client,
and the conflict cannot be merged away — both debits are legitimate and both must
happen, just in sequence.

### Deadlock

Locking two rows introduced a second problem. Two transfers issued at the same
time in opposite directions, 1 → 2 and 2 → 1:

```
T1 (1->2):  locks 1, waits for 2
T2 (2->1):  locks 2, waits for 1
```

Each holds what the other needs. Neither can release early — a lock is held until
commit or rollback, because releasing a row mid-transaction would expose
uncommitted values to anyone who read it, which is exactly the dirty read that
`READ COMMITTED` forbids.

Postgres detected the cycle and terminated one transaction:

```
ERROR: deadlock detected
  Detail: Process 27449 waits for ShareLock on transaction 573;
          blocked by process 27453.
          Process 27453 waits for ShareLock on transaction 572;
          blocked by process 27449.
```

The killed transaction rolled back cleanly — no money was stranded — but the
client received a 500.

**Fix:** lock the two accounts in a fixed order, ascending by id, regardless of
which direction the money is moving. What matters is not which order is chosen
but that *every* transaction uses the same one. Under a consistent order, the
transaction that arrives second blocks on the very first row while holding
nothing at all. A deadlock requires every participant to hold something and wait
for something; a waiter with empty hands cannot close the cycle.

The same principle applies to mutex acquisition in concurrent code — it is lock
ordering, not a database-specific trick.

`CannotAcquireLockException` is also mapped to `409` with a retry hint, so a lock
failure from any other source does not surface as a stack trace.

## Idempotency

A client sends a transfer, the connection times out, and it has no way to know
whether the request was processed. It retries. Without protection, the money
moves twice.

This is a different problem from the race conditions above. There, two *distinct*
legitimate requests arrive together. Here, one request arrives twice.

The client generates a unique key and sends it in an `Idempotency-Key` header.
The key must come from the client: the whole point is to survive a lost response,
and a server-generated key would only be visible in the response that was lost.

Three cases, handled differently.

### Ordinary retry

The key is found in `idempotency_keys`, the stored transfer is loaded, its
sender, receiver and amount are compared against the incoming request. They
match, so the original transfer is returned with `200`. No new transfer is
created and no balance changes.

### Concurrent requests carrying the same key

Checking for the key and inserting it are not atomic. Two simultaneous requests
both find no key, and both proceed to transfer.

The `key` column is the primary key of `idempotency_keys`, so the second insert
is rejected and its entire transaction — including the transfer — rolls back.
Money moves exactly once. **The guarantee comes from the database constraint, not
from the lookup in application code.** The lookup is an optimisation for the
common case.

Returning the right thing to the losing client took some care. Once the
constraint fires, Spring marks the transaction rollback-only: any read performed
inside it would be discarded at commit time, and the commit itself would throw
`UnexpectedRollbackException`. The recovery therefore has to happen *outside* the
transaction boundary.

It is handled in `TransferFacade`, a separate bean with no `@Transactional`,
which wraps the call in a try/catch and on `DataIntegrityViolationException`
calls a second, read-only transactional method to fetch the stored transfer.

Putting that try/catch in `TransferService` itself would not have worked. A call
to `this.executeTransfer(...)` bypasses the Spring AOP proxy, so `@Transactional`
would not apply at all and the transfer would silently lose its transaction —
worse than the original bug. Both calls go bean-to-bean so that they pass through
the proxy.

Verified: two concurrent requests with one key return an identical transfer id,
one row in `transfers`, one balance change.

### Same key, different body

The client reused a key for a genuinely different operation. Returning the
earlier transfer would silently discard the new request, so this answers `409`.

The stored key points at a transfer, and the transfer already carries the sender,
receiver and amount, so the comparison needs no extra column and no migration.
This works only because every field of the request happens to be persisted on the
transfer — see *Known limitations*.

## Known limitations

**Idempotency keys are never expired.** The table grows without bound, and a key
is useless once the operation it guards has settled. A production system would
keep them for a day or two and delete on a schedule; `created_at` is already
there to support that.

**The request fingerprint is derived from `transfers`, not from the request
body.** If a field is ever added to the transfer request that is not persisted on
the transfer — a memo, a client reference, a pre-conversion currency — there
would be nothing to compare it against. The general solution is to store a hash
of the request body on the key row.

**`DataIntegrityViolationException` is assumed to mean a duplicate key.** It can
also come from `CHECK (balance >= 0)` or from a foreign key referencing a missing
account. The current handler compensates by checking whether the key actually
exists and raising an `IllegalStateException` if it does not, rather than
returning a wrong answer — but the diagnosis should really be made from the
constraint name.

**`createdAt` is not formatted consistently across the two idempotent paths.** A
value returned straight from memory keeps its local offset and nanosecond
precision; the same value read back from `timestamptz` comes out in UTC truncated
to microseconds. Same instant, different representation — and an idempotent retry
should be byte-identical. Normalising to UTC on creation would fix it.

**Pagination exposes Spring Data's `Page` directly.** The serialised form
includes the internals of `PageImpl`, which ties the API contract to a framework
class that has changed shape across versions. A dedicated response type would be
more stable.

**No authentication.** Any caller can move money between any two accounts. Out of
scope here; the project is about the transfer path.
