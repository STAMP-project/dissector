# dissector
Dynamic analysis tool to compute the stack distance from *pseudo-tested* and *partially-tested* methods to any public method in the test folder of a given Maven project.

## What is dissector?

**dissector** is a tool to compute the stack distance from a given set fo methods to any public method delcared in the test folder of a Maven project.

It is composed of two modules:
 - **disector-monitor**: A Maven plugin that exposes several goals to compute the aforementioned distance and other miscelaneous tasks.
 - **dissector-agent**: A Java agent to instrument a set of target methods to report each method call occurred.


## How does it work?

 **dissector** analyses a given Maven project. One of the goals provided by **dissector-monitor** should be executed to obtain the result.
 **dissector-monitor** will then execute the test suite and inject **dissector-agent** into the execution.
 **dissector-agent** instruments the given set of methods with instructions that allows to track each method call.
 The traces are sent to the error output of the test process. The monitor captures the error output and simulates the call stack in orther to gather the required information.
 The result is stored in a `JSON` file.


## Available goals

- `count-tests`: Counts the number of public methods declared in classes from the test folder.
- `execute`: Desgined to work with the `METHODS` ouput of [Descartes](https://github.com/STAMP-Project/pitest-descartes).
This goal computes the stack distance from any *pseudo-tested* or *patially-tested* detected by *Descartes* to any public method declared in the test folder.

## How to use it?

First clone the repository, build the project and install it locally:

```bash
git clone https://github.com/STAMP-project/dissector
cd dissector
mvn clean install
```

Then go to the project you want to *dissect* and run:

```bash
mvn eu.stamp-project:dissector:execute
```

The project should have been analyzed first by Descartes. See more on this matter [here](https://github.com/STAMP-Project/pitest-descartes).

## More

### License
Dissector is published under LGPL-3.0 (see [LICENSE.md](LICENSE.md) for further details).


### Funding

Disector is partially funded by research project STAMP (European Commission - H2020)
![STAMP - European Commission - H2020](docs/logo_readme_md.png)