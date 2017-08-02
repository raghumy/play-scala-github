# GitHub Stats Collector

This repo contains a Play application that provides a REST api interface into the Github API. It proxies
calls to get repository information and provides statistics.

# Features

* Caches results of calls to github to retrieve list of members and repos for the org. The cache is in a database.
This can be configured - using sqlite for demo purposes.

* Updates happen periodically. A state is maintained in the database and only orgs that haven't been updated recently
are processed.

* Stats are collected after every updated and stored in the database

* Multiple instances of the app can run. They will use the same database. For this use a different database (e.g. Postgres)
would be a better fit.

## Pre-requisites

* Install Java 8
* Install sbt

## Installation

* Clone repo from git@github.com:raghumy/play-scala-github.git
* `cd play-scala-github`
* `sbt compile` to compile the code

## Configuration

* Setup an environment variable called GITHUB_TOKEN
* Configure property `slick.dbs.default.db.url` in `conf/application.conf`. This will be the name of the sqlite database file
* Update frequency is configured in `conf/application.conf` with property github.update.interval. This defaults to 5 minutes.
* Another property defines how often to check for updates github.check.interval. This defaults to 2 minutes.
## Running

* Run `sbt run`. This should start the play framework

* Run `sbt "run 8080"` to run this on a different port

## Testing

* A script call test.sh is checked into the repo. This should setup the org and run thru the tests.

* A new org is added with `curl -X PUT "$BASE_URL/orgs/$ORG"`

* During initial setup, Play will run migrations to create database tables.

* When an org is initially added it may take a few moments to retrieve the data depending on the speed