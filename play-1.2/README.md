# Swagger Play 1.2.x Framework Module

**NOTE!  This repository is available for historical reasons and the project is no longer supported**

## Overview
This is a project to add swagger to play-framework; an implementation of the Swagger spec.  
You can find out more about both the spec and the framework at http://swagger.wordnik.com.  
For more information about Wordnik's APIs, please visit http://swagger.io

### Prerequisites
You need the following installed and available in your $PATH:

<li>- Play Framework 1.2.x

<li>- Scala 2.8.1  (http://www.scala-lang.org)

### To build

The swagger-play module depends on swagger-core_2.8.1-1.1.  You can find this artifact in a public maven repo:

https://oss.sonatype.org/content/repositories/releases/com/wordnik/swagger-core_2.8.1/

Get the dependencies for the swagger-play module:

<pre>
export SCALA_HOME=path/to/scala-2.8.1
play deps --sync
play build-module
</pre>

