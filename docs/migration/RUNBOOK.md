# Legacy NorthStar Claims: cold-start runbook

How to build, seed, run and capture the legacy Struts 1.3 app from the
`demo-insurance` branch in one pass. Other branches belong to an abandoned
attempt; do not use them.

## Toolchain

| Tool | Version | Notes |
|------|---------|-------|
| JDK | 11 (CI: Temurin 11) | The pom compiles with `source`/`target` 7. JDK 17 also works; JDK 21 fails with "Source option 7 is no longer supported". |
| Maven | 3.6+ | `sudo apt-get update && sudo apt-get install -y maven openjdk-11-jdk` on Ubuntu. |
| Python | 3 (stdlib only) | Used by `make capture`. |

Check `java -version` and `mvn -v` both report 11 before building. If
several JDKs are installed, export
`JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64` for the shell. claims-boot
is a separate Spring Boot project and builds with JDK 21. Keep the two
JDKs in separate shells.

### Maven Central rate limits (HTTP 429)

Put a mirror in `~/.m2/settings.xml` only. Never commit it to the repo:

```xml
<settings><mirrors><mirror>
  <id>gcs-central</id><mirrorOf>*</mirrorOf>
  <url>https://maven-central.storage-download.googleapis.com/maven2/</url>
</mirror></mirrors></settings>
```

## One-shot start

```bash
git checkout demo-insurance
make build      # mvn -B clean package  -> target/northstar-claims-management.war
make test       # mvn -B clean verify   -> 18 tests, BUILD SUCCESS
make seed       # DatabaseBootstrap: recreate schema + literal data in target/db/northstar
make run        # mvn jetty:run (foreground) on :8080
```

Open http://localhost:8080/claims/login.do and log in as `supervisor` /
`supervisor` (capture also uses `adjuster4` / `legacy4`). The bare context
root `/claims/` has no welcome page. Jetty shows a directory listing there
(`northstar.css`, `WEB-INF`), not a login form. A successful login renders
`home.jsp` (`<!-- ns:view /WEB-INF/jsp/home.jsp -->`). Stop Jetty with
Ctrl-C.

Note: `make test` and `make build` both run `clean`, which deletes `target/`,
including `target/db`. **Run `make seed` after any build or test, and before
`make run`.**

## Ports and the database

- Jetty: `8080`, context path `/claims` (set in `pom.xml`, `jetty-maven-plugin`).
- The HSQLDB file database lives at `target/db/northstar*`. You can override
  it with `-Dclaims.db.path=...`. Unit tests use a separate file,
  `target/db/test-northstar*`.
- `jetty-env.xml` binds `jdbc/ClaimsDB` to `${java.io.tmpdir}/northstar-claims-db`.
  Under `mvn jetty:run` the JNDI lookup fails. The log shows
  `DataSource lookup failed` once, which is expected, and the DAOs fall back
  to `target/db/northstar`. So the seeded file is the one the app reads.
- **DB reset:** stop Jetty first. HSQLDB holds a lock on the file, so do not
  seed while it is running and never delete an active `.lck`. Then run
  `make seed`. That is enough, because the bootstrap drops and recreates
  everything. To wipe the database fully, run `make clean` (removes `target/`
  and `data/`) and then `make seed`.

## Transcript capture and the Jetty port clash

`make capture` (`tools/capture/capture.py`) does all of the following
itself:
1. deletes `target/db/northstar*` and reseeds the database,
2. starts **its own** `mvn jetty:run` on 8080,
3. polls `/claims/login.do` until it is ready,
4. runs the fixed `SCENARIOS`, then writes `transcripts/*.json` and
   `transcripts/index.json`,
5. stops Jetty.

**Do not have `make run` (or any other process on 8080) running during
capture.** If one is running, the capture Jetty cannot bind the port, and the
readiness poll succeeds against the *old* server. The old server is also
running on a database file that capture just deleted and reseeded under it.
The transcripts come out wrong, or the run fails with lock errors. Before
capturing, check that nothing is listening:

```bash
ss -ltnp | grep 8080 || echo "8080 free"
pkill -f jetty:run    # if something is left over
```

Stability gate (the same one CI runs):

```bash
make capture && make capture
git diff --exit-code -- transcripts/   # must be empty
```

Each capture takes about 6 seconds on a warm `~/.m2`. Add new scenarios
at the end of `SCENARIOS`. Inserting one in the middle shifts the generated
claim and payment ids, and every later transcript changes.

## Troubleshooting

- `Source option 7 is no longer supported`: you are on JDK 21. Switch to 11.
- 429 / `Could not transfer artifact`: add the mirror above.
- CLAIM not found / empty screens: a build or test removed `target/db`.
  Stop Jetty and run `make seed`.
- `Jetty did not become ready` from capture: check the first Jetty start
  (cold `~/.m2`), or look for a stale process on 8080.
