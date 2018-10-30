# dissector

## What is dissector?

**dissector** provides both a Java agent and a Maven plugin to gather
dynamic and static information from a Maven project and its test suite.
The current version is able to target a selection of methods and
collects the stack traces and compute the stack distance from the methods
to any test method in the project.

## How to use it?

To collect the stack traces of a selection of methods, execute:

```bash
mvn eu.stamp-project:dissector-monitor:stack-trace
```

The result will be stored in `target/stack-traces.json`

To compute the stack distance from a selection of methods to any test
method, execute:

```bash
mvn eu.stamp-project:dissector-monitor:distance-to-test
```

The result will be stored in `target/distances.json`

Both goals expect by default a `target/methods.json` file containg the
the selection of methods. More details about this files are given in the
next section.

## How does it work?

**dissector** is a Maven plugin but has been designed to target other
Maven projects without any `pom.xml` modification. In this it is easier
to integrate the tool in custom workflows.

When any of the Maven goals is executed, **dissector** executes `mvn test`
for the project and injects a Java agent that instruments the code and
communicates with the Maven plugin.

The current version provides the following goals:
  * *distance-to-test*: Which computes the stack distance from a selection
    of methods to any test method in the project.
  * *stack-trace*: Which collects the stack trace for every invocation
    done to a selection of methods.
  * *test-count*: A goal to count the number of test methods in the
    project. The count is done statically and no test execution is
    triggered. This goal is mostly used to verify the execution of the
    other goals.


Both, *distance-to-test* and *stack-trace* goals provide the following
properties:

  * **methodsOfInterest**: Path to a `JSON` file containing the methods
    the selection of methods that should be targeted by the goal.
    The file is espected to have the following structure:
    ```json
    [
        ...
        {
            "package": "path/to/",
            "class": "Class",
            "name": "method",
            "description": "(I)V"
        }
        ...

    ]
    ```
    Since **dissector** targets compiled code, all data must be specified
    in the internal JVM format. Default value is *${project.build.directory}/methods.json*.
  * **classificationsOfInterest**: List of method classification to filter
    the method selection. If specified, every method entry in the `JSON`
    file should contain a *classification* attribute with a given string.
    Only the methods having a *classification* included in the list will
    be targeted.
  * **output**: Path to the output file. Each goal should produce a `JSON`
    file with the collected information.
  * **injectTesArgs**: A boolean value saying whether a profile and the
    the agent arguments sould be injected when executing `mvn test`.
    Default value is *true*. If it is set to *false*, then a plain
    `mvn test` will be executed and it will rely on the custom `pom.xml`
    configuration.
  * **testingProfile**: Profile to be used when executing `mvn test`.
    The result is equivalent to execute `mvn test <testingProfile>`.
    Default value is *stamp-dissector*.
  * **agentJar**: Path to the `jar` file containing the agent implementation.
    By default **dissector** will use the `jar` bundled within its resources.
  * **methodList**: List to a `txt` file containing a method specification
    by line. This file will be the input for the agent and it is produced
    by each goal. However, the user can opt to create and use a custom file.
    Default value is *${project.build.directory}/methods.input.txt*.
  * **logFolder**: Folder where the logs produced by the agent are to be
    stored. Default value is *${project.build.directory}/dissector-logs/*.

## How to build the project

First clone the repository. Then build the project and install it locally:

```bash
git clone https://github.com/STAMP-project/dissector
cd dissector
mvn clean install
```


## More

**dissector** is currently used to support analysis workflows powered by
[**Descartes**](https://githiub.com/STAMP-project/pitest-descartes).

### License
Dissector is published under LGPL-3.0 (see [LICENSE.md](LICENSE.md) for further details).


### Funding

Disector is partially funded by research project STAMP (European Commission - H2020)
![STAMP - European Commission - H2020](docs/logo_readme_md.png)