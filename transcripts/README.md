# NorthStar transcript format

Each scenario is one JSON object in `transcripts/<scenario>.json`. The
required top-level keys are `scenario`, `description`, `actor`, `request`, and
`expected`. `request` contains `method`, a context-qualified `path` beginning
with `/claims`, and a form object. `expected`
contains `status`, `result`, `business_fields`, `validation_errors`, and
`db_state`.

`business_fields` is a flat string-to-string map extracted from
`<span id="f_NAME">VALUE</span>` elements. It contains semantic values, not
raw HTML. Money uses two decimal places and dates use `yyyy-MM-dd`.

`result` is exactly one of:

* `forward:<path>` for the JSP marker emitted by the layout
* `redirect:<path>` for a normalized HTTP redirect location
* `error:<key>` for the application error page

An optional top-level `setup` list holds requests (same shape as `request`)
sent in the same session before the recorded request. `actor` names the
operator whose session made the requests.

`validation_errors` is an ordered list of ApplicationResources keys emitted
by Struts. `db_state` is a flat map of dotted probe keys to string values.

Supported probe vocabulary:

* `claim.<id>.<field>` - workbench view endpoint
* `payment.count.claim.<id>` - payment history endpoint, counting payment
  field spans
* `payment.<id>.<field>` - payment history endpoint
* `settlement.claim.<id>.<field>` - settlement calculation endpoint
* `settlement.saved.claim.<id>.<field>` - settlement detail endpoint
* `policy.<id>.<field>` - policy view endpoint

The capture tool resolves these through the running application's read pages;
it never opens the HSQLDB file while Jetty is running.

The harness strips `jsessionid` URL suffixes, excludes tokens and timestamps,
collapses extracted HTML whitespace, sorts JSON map keys, writes two-space
indentation, and ends every file with a newline. Run `make capture` to reset
the fixed seed, execute the ordered scenarios, and regenerate the fixtures,
including every file under `transcripts/db/` (stale snapshots are removed).
Run it twice and `git diff -- transcripts/` must be empty.

## Database snapshots

Every transcript also has a top-level `db_snapshot` key holding the path,
relative to `transcripts/`, of a sidecar file with the complete database state
taken right after the recorded request returned (after any `setup` requests,
before the `db_state` probes run). The existing keys above are unchanged.
`db/_seed.json` is the same snapshot of the freshly seeded database, taken
before the first request.

```json
{
  "scenario": "workbench_assign",
  "tables": {
    "CLAIM": {
      "columns": [["CLAIM_ID", "INTEGER"], ["CLAIM_NUMBER", "VARCHAR"], ...],
      "primary_key": ["CLAIM_ID"],
      "rows": [
        [121, "CLM-121", 9001, "Capture Claimant", "2019-04-01", ...]
      ]
    }
  }
}
```

* `tables` holds every base table in the `PUBLIC` schema, keyed by its
  upper-case name and sorted by name.
* `columns` lists `[name, SQL type]` pairs in table column order. Each row is
  an array in that same order.
* `primary_key` lists the key columns in key order. Rows are sorted by the
  primary key, or by every column when a table has none.
* Values are typed: integer columns are JSON numbers, `BOOLEAN` columns are
  `true`/`false`, SQL `NULL` is `null`. `DOUBLE` values are strings holding
  the shortest decimal that round-trips the stored double, in plain notation
  (`"4500.0"`, `"1.005"`), so binary floating-point drift stays visible.
  `DECIMAL` values are plain strings at their stored scale. Dates are
  `yyyy-MM-dd` strings. Other types use their JDBC string form.
* Volatile values are replaced with placeholders, the same way the response
  normalization drops them: `TIME`/`TIMESTAMP` columns become `"<timestamp>"`,
  a `DATE` equal to the capture machine's current date becomes `"<today>"`,
  and `jsessionid` URL suffixes are stripped from strings. Columns holding
  generated tokens are listed in `VOLATILE_COLUMNS` in
  `tools/capture/capture.py` and replaced with the placeholder given there.
  The current application writes only fixed dates, so no seeded or captured
  value is replaced today; the rules guard future scenarios.
* Each row is written on one line, so a changed row shows as a one-line diff.

### How the snapshot is read

Capture runs HSQLDB in server mode inside the Jetty JVM, so the harness reads
the live database over JDBC without opening the locked file. `capture.py`
starts Jetty with `-Dclaims.jetty.contextXml=tools/capture/jetty-capture.xml`.
That context file binds the same resources as `WEB-INF/jetty-env.xml`, starts
an `org.hsqldb.server.Server` for `target/db/northstar` on
`127.0.0.1:9001` (`CAPTURE_DB_PORT` overrides the port), and makes `org.hsqldb`
a webapp system class. With that, the application's in-process `jdbc:hsqldb:file:`
connections and the server share one open database. `make run` and the
deployed WAR are unchanged and do not start the server.

`tools/capture/DbSnapshot.java` is the JDBC reader. Capture runs it as a
single-file Java program (JDK 11+) on the project's `hsqldb` jar, resolved
with `maven-dependency-plugin:build-classpath`, and asks it for one snapshot
per request over stdin/stdout, inside one read-only transaction. Capture stops
with an error if port 8080 or the database port is already in use.
