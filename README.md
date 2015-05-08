# log4j2-migrator
Script to migrate log4j configurations to log4j2.

The objective is to migrate a log4j.properties to log4j2.xml configuration for simple use cases. For more complex uses you'll need to translate part or all the configuration manually.

The script doesn't have any error checking whatsoever. The input should be valid and correct.

No guarantee on output correction. Please review and test the generated configuration.

Please report any issues at https://github.com/mulesoft-labs/log4j2-migrator/issues

## Usage
groovy log4jmigrator.grovvy pathname-to-log4j.properties

### Options

 -d,--debug          print debug information
 -h,--help           usage information
 -o,--output <arg>   file to write the output

## Example

```
groovy log4jmigrator.groovy test/log4j.properties > log4j2.xml

```

## Limitations
* Can't translate multiple appenders for a logger
* Only knows about ConsoleAppenders, RollingFileAppender and DailyRollingFileAppender (simple configuration) appenders 
* Doesn't understand log4j.xml as input
