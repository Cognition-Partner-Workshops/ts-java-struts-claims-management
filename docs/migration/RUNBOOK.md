# Frozen parity baseline

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
