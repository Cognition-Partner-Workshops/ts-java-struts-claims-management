# Frozen parity baseline

## Claims Boot data/run

From the repository root, build with JDK 21:

```sh
JAVA_HOME=/path/to/jdk21 mvn -B -f claims-boot/pom.xml verify
/path/to/jdk21/bin/java -jar claims-boot/target/claims-boot-0.1.0-SNAPSHOT.jar --claims.db.server.enabled=true
```

Boot uses HTTP 8081, HSQL loopback 9002, alias `northstar`, PUBLIC schema,
and its own file `claims-boot/target/db/claims-boot`; legacy remains on
HTTP 8080 with `target/db/northstar` (build/run with JDK 11).
Each Boot launch resets and seeds by default. Use `--claims.db.reset=false`
to keep data, `--claims.db.url=jdbc:hsqldb:file:/absolute/path` to choose a
different file, or `--claims.db.server.port=9003` to move replay access.
The schema, seed and operating properties are packaged from legacy resources,
not maintained separately. Stop Boot with Ctrl-C so its pool/server close.
Only the shared foundation (login, home, logout, error, `/admin`) is ported.
`--area "shared foundation"` matches all 10 legacy responses; its database
snapshots still differ by the rows that unported intake, settlement and payment
scenarios write earlier in the ordered stream. See [Data layer](../../claims-boot/README.md#data-layer)
for DAO/transaction guidance.

## Baseline replay

`parity-v1` is the frozen baseline (217 ordered scenarios). Any change to the
frozen set requires a new tag, never moving `parity-v1`.

Use JDK 11 and compile first (`mvn -B verify`). `make capture` regenerates the
baseline from a clean seed; `make replay` seeds and starts legacy itself and
compares responses and every database table without rewriting expectations.
Stop other servers on ports 8080 and 9001 first.

For an already running replacement app with an HSQLDB server, use:

```
python3 tools/capture/capture.py replay --base-url http://localhost:8081/claims --db-url jdbc:hsqldb:hsql://localhost:9002/northstar --db-user SA
python3 tools/capture/capture.py --verify-manifest parity/MANIFEST.json
```

Seed the replacement database before replay. Set `CAPTURE_DB_PASSWORD` if needed.
The JDBC reader is independent of Jetty; the target must expose the same PUBLIC
schema. `--area settlement` (or another documented area) executes the entire
ordered stream but compares only the selected area's scenarios (plus the seed).
`--expectations PATH` selects another expectation directory for diagnostics.
Replay prints unified response/database diffs, a scenario/diff summary, and
exits nonzero on mismatches or unreadable expectations. Diff counts count changed
response/DB files, including the seed, not individual JSON fields.
