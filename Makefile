.PHONY: build run seed test capture clean

build:
	mvn -B clean package

run:
	mvn jetty:run

seed:
	mvn -q org.codehaus.mojo:exec-maven-plugin:3.3.0:java

test:
	mvn -B clean verify

capture:
	python3 tools/capture/capture.py

replay:
	python3 tools/capture/capture.py replay --start-legacy

clean:
	mvn -q clean
	rm -rf data target/db
