# Legacy parity cold start

Run from the repository root on `demo-insurance` (or a branch based on it).
Verified on Ubuntu with OpenJDK **11.0.33**, Maven **3.6.3**, and Python 3.
The POM targets Java 7; use JDK 11, as CI does, not the system's JDK 17.

```sh
sudo apt-get update
sudo apt-get install -y maven openjdk-11-jdk
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
java -version
mvn -version
mkdir -p ~/.m2
```

Put this mirror in **`~/.m2/settings.xml` only**, never in the repository
(merge it into existing settings if necessary):

```xml
<settings>
  <mirrors>
    <mirror>
      <id>gcs-central</id>
      <mirrorOf>central</mirrorOf>
      <url>https://maven-central.storage-download.googleapis.com/maven2/</url>
    </mirror>
  </mirrors>
</settings>
```

Run these commands in order:

```sh
make build
make test
make seed
make run
```

Jetty serves `http://localhost:8080/claims/`; wait for startup, then log in
with `supervisor` / `supervisor`. From another terminal, a quick HTTP check is:

```sh
curl -sS -L -o /dev/null -w '%{http_code}\n' http://localhost:8080/claims/
curl -sS -d 'username=supervisor&password=supervisor' \
  http://localhost:8080/claims/login.do
```

The first command returns `200`; the login response contains
`<span id="f_signedInUser">supervisor</span>` on success.
Stop `make run` with Ctrl-C and ensure nothing is listening on port 8080
(`ss -ltnp 'sport = :8080'`) before capture.

```sh
make capture
git diff --stat -- transcripts/
git status --porcelain transcripts/
make capture
git diff --stat -- transcripts/
git status --porcelain transcripts/
```

Both captures were verified to exit 0 with **empty output from both Git
checks after each run**, matching the committed transcripts. Capture resets
`target/db/northstar*`, reseeds the database, starts its own Jetty on 8080,
and stops it afterward. Do not run it against an already-running server or
use `--no-reset` for the determinism check. Append new scenarios at the
**end** of `SCENARIOS` in `tools/capture/capture.py`; earlier scenarios depend
on ordering and seeded IDs. Do not hand-edit transcripts. Keep `target/`
and `data/` build/runtime artifacts out of commits.

Gotchas encountered: fresh apt indexes were needed before packages could be
found; an existing apt update temporarily held the lock (wait for it rather
than deleting locks). Unit tests log `DataSource lookup failed:
javax.naming.NoInitialContextException` outside Jetty and fall back to
DriverManager; all 18 tests passed. Jetty also logged a missing
`ApplicationResources_en.properties` locale bundle; the base bundle exists
and login/capture succeeded. Initial curl connection refusals simply meant
Jetty was still starting; wait for readiness before checking HTTP results.
