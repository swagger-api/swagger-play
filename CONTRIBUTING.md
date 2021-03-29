# Contributing

## Code of Conduct

Current project supports the [Scala code of conduct][code-of-conduct] and
wants all of its channels (GitHub, etc.) to be inclusive environments.

## Requirements

You will need the following tools:

*   [Git][git]
*   [Java 8][java-8]
*   [SBT][sbt]

## Workflow

1.  Create your own [fork][fork-and-pull] of the repository
    and work in a local branch based on `main`
2.  Write code
3.  Run `sbt format` (or `sbt fix` and `sbt fmt` sequentially) before creating the pull request
4.  [Submit a pull request](#submit-a-pull-request)

## Formatting

### Scalafix

We use [scalafix][scalafix] to apply some rules that are configured in `.scalafix.conf`.
Make sure to run `sbt fix` to apply those rules.

### Scalafmt

We use [scalafmt][scalafmt] to format the source code according to the rules
described in `.scalafmt.conf`, and recommend you to setup your editor to “format on save”,
as documented [here][scalafmt-install].
Make sure to run `sbt fmt` to ensure code formatting.


## Submit a pull request

*   Pull requests should be submitted from a separate branch (e.g. using
    `git checkout -b "username/fix-123"`).
*   In general we discourage force pushing to an active pull-request branch that other people are
    commenting on or contributing to, and suggest using `git merge master` during development. Once
    development is complete, use `git rebase master` and force push to [clean up the history][squash].
*   The first line of a commit message should be no more than 72 characters long (to accommodate
    formatting in various environments).
*   Commit messages should general use the present tense, normal sentence capitalization, and no final
    punctuation.

## Publish a Release (note for maintainers)

Push a Git tag into `main` branch:

```bash
$ git tag v0.0.1
$ git push origin v0.0.1
```

If release build fails, delete the tag from `main` branch

```bash
$ git tag -d v0.1.0
$ git push origin :refs/tags/v0.1.0
```

then make the corrections and try again.

[code-of-conduct]: https://www.scala-lang.org/conduct/

[fork-and-pull]: https://help.github.com/articles/using-pull-requests/

[git]: https://git-scm.com/

[java-8]: https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html

[scalafix]: https://scalacenter.github.io/scalafix/

[scalafmt]: https://scalameta.org/scalafmt/

[scalatest]: https://www.scalatest.org/

[scalafmt-install]: https://scalameta.org/scalafmt/docs/installation.html

[sbt]: http://www.scala-sbt.org/

[squash]: http://gitready.com/advanced/2009/02/10/squashing-commits-with-rebase.html
