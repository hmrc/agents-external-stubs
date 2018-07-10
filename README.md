# Agents External Stubs

[ ![Download](https://api.bintray.com/packages/hmrc/releases/agents-external-stubs/images/download.svg) ](https://bintray.com/hmrc/releases/agents-external-stubs/_latestVersion)

## Running the tests

    sbt test it:test

## Running the tests with coverage

    sbt clean coverageOn test it:test coverageReport

## Running the app locally

    sm --start AGENT_MTD -f
    sm --stop AGENTS_EXTERNAL_STUBS
    sbt run

It should then be listening on port 9009

    browse http://localhost:9009/agents-external-stubs

### License


This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
