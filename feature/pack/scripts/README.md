# Groovy Module Checker Script

## What is this for?

This relates to [WildFly Camel issue #184](https://github.com/wildfly-extras/wildfly-camel/issues/184). Some means
of detecting when dependencies have been duplicated across WildFly-Camel modules and WildFly app server modules
is required to ensure that a known, consistent and stable set of dependencies is shipped with the Camel subsystem.

## How does it work?

The script does the following....

1. Builds a list of modules and associated resources by parsing the 'base' and 'fuse' module layers

2. Checks to see if any duplicate combination of module name and slot id exists between WildFly-Camel and WildFly app server modules

3. Checks for duplicated dependencies between WildFly-Camel and WildFly app server modules

4. Outputs any identified problems and fails the build
