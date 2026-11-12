# Login, home and admin comparison

Start legacy from the repository root with Java 11 using `make build`,
`make seed`, then `make run` (8080). Start Boot with Java 21 using
`mvn spring-boot:run` from `claims-boot/` (8081).
Boot recreates its own in-memory HSQLDB schema and seed on startup and
never opens legacy's file database.

From the repository root run:

```sh
python3 claims-boot/parity/compare.py
```

The script uses the capture harness's field/view/error patterns and refuses
redirect following. It compares status, Location path, every field span
(including empty values), view markers and error markers. Origins and
Jetty's URL session identifiers are removed from redirect Locations, not
from screen data. It also checks the committed login transcript.
A nonzero exit means a difference, including differences awaiting signoff.
This is a small slice comparator, not the future full replay runner.

Current evidence and the unresolved admin decision are in
`docs/parity/SIGNOFF.md`. The Java characterization tests run as part of
`mvn -B verify` and cover the currently implemented admin contents as well
as legacy login's intentionally invisible failure message.
