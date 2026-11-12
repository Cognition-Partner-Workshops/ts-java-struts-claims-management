# NorthStar Claims Management

NorthStar Claims Management is a deliberately dated Apache Struts 1.3.10
property and casualty claims system. It uses Servlet 2.4 deployment
descriptors, JSP runtime compilation, raw JDBC, and an HSQLDB file database.
The source reflects the conventions and maintenance history of a long-lived
enterprise application.

## Module map

The default Struts module contains policy search and viewing, FNOL intake,
claim workbench, settlement, payment, and reporting screens. The `/admin`
module contains adjuster and reference-data screens. Data access is split
between raw JDBC DAOs and several direct-JDBC workflow actions.

## Build and run

```text
make build
make test
make seed
make run
```

The application is served at `http://localhost:8080/claims/`. The default
operator login is `supervisor` / `supervisor`. Maven is the supported build;
`build.xml` is retained as the official historical Ant build.

## Database reset

The database is stored under `target/db/northstar`. `make seed` recreates the
schema and loads the fixed literal data. `make clean` removes generated
database files and build output.

## Transcript harness

`make capture` resets the database, starts Jetty, logs in, executes the fixed
workflow scenario list, writes normalized JSON transcripts under
`transcripts/`, and shuts Jetty down. The transcripts are fixtures for a
replay harness and expose semantic values through the `<ns:field>` extraction
spans.

## Known issues / technical debt

The application declares a container-managed JNDI datasource but retains a
DriverManager fallback because deployment environments differ. Several
actions contain direct SQL, SQL construction is inconsistent, and the
connection pool is hand-rolled. JSPs use shared includes rather than a
proper layout framework. The Maven Jetty run emits duplicate JSTL scanning
warnings. Authentication uses plaintext-style adjuster credentials.

This repository is the legacy source of truth. The Spring Boot replacement is
being built in `claims-boot/` beside the untouched legacy baseline.

With Java 21 and Maven, run `cd claims-boot && mvn spring-boot:run` and open
http://localhost:8081/claims/login.do (currently a placeholder). The application
uses port 8081 and context path `/claims`; legacy Jetty remains on port 8080.
Replacement controllers must map explicit `"/xxx.do"` paths, preserving legacy
request parameter names and redirects; Spring Boot 3 does not use suffix pattern
matching. Real screens and the data layer will be added in later steps.
