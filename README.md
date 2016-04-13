# Swagger Play Integration

The goal of Swagger™ is to define a standard, language-agnostic interface to REST APIs which allows both humans and computers to discover and understand the capabilities of the service without access to source code, documentation, or through network traffic inspection. When properly defined via Swagger, a consumer can understand and interact with the remote service with a minimal amount of implementation logic. Similar to what interfaces have done for lower-level programming, Swagger removes the guesswork in calling the service.

Swagger-play is an integration specifically for the Play framework.

### Versions

Play has many versions with breaking incompatibilities.  It does not follow Semver and therefore the Swagger Play module is split up into several different directories.

Please see the project [Root Folder](https://github.com/swagger-api/swagger-play) for the various Play versions.

### Compatibility

Scala Versions | Play Version | Swagger Version | swagger-play version
---------------|--------------|-----------------|---------------------
2.8.1          | 1.2.x        | 1.2             | 0.1
2.9.1, 2.10.4  | 2.1.x        | 1.2             | 1.3.5
2.9.1, 2.10.4  | 2.2.x        | 1.2             | 1.3.7
2.10.4, 2.11.1 | 2.3.x        | 1.2             | 1.3.12
2.11.6, 2.11.7 | 2.4.x        | 2.0             | 1.5.0
2.11.7         | 2.5.x        | 2.0             | 1.6.0

Other Swagger-Play integrations
-------
This Swagger-Play integration allows you to use [Swagger annotations](https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X) on your Play actions to generate a Swagger spec at runtime. The libraries below take different approaches to creating an integration.

* [iheartradio/play-swagger](https://github.com/iheartradio/play-swagger) - Write a Swagger spec in your routes file
* [zalando/play-swagger](https://github.com/zalando/play-swagger) - Generate Play code from a Swagger spec


License
-------

Copyright 2011-2016 SmartBear Software

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
[apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

---
<img src="http://swagger.io/wp-content/uploads/2016/02/logo.jpg"/>
