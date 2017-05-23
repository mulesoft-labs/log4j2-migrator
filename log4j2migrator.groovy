
def cli = new CliBuilder(usage: 'log4j2-migrator [options] input-file')
cli.with {
    h(longOpt: 'help'  , 'usage information'   , required: false )
    o(longOpt: 'output', 'file to write the output', required: false  , args: 1 )
    d(longOpt: 'debug', 'print debug information', required: false  )
}

OptionAccessor opt = cli.parse(args)
if(!opt) {
    return
}

// print usage if -h or --help or number of arguments incorrect
if(opt.h || opt.arguments().size() != 1) {
    cli.usage()
    return
}

def outputFile
if( opt.o ) {
    outputFile = opt.o
} 

@groovy.transform.Field def debug // global print debug information flag
if( opt.d ) {
    debug = opt.d
} 

def extraArguments = opt.arguments()
if (extraArguments) {
    inputFile = extraArguments[0]
}

Properties properties = new Properties()
File propertiesFile = new File(inputFile)
propertiesFile.withInputStream {
    properties.load(it)
}


def bindings = parse(properties)

if (debug) { System.err.println "Bindings = ${bindings}" }

def output=generate(bindings)

if (outputFile) {
    new File(outputFile).write(output)
} else {
    println output
}



def parse(properties) {

	def appenders=[:]
	def loggers=[:]
	def additivity=[:]
	def rootLevel
	def rootAppenders

	properties.each { key, value ->
        value=value.trim()
		if (debug) { System.err.println "property ${key}=[${value}]" }

		if (key.startsWith("log4j.appender")) {
			def (log4j, appender, name, property,extra)=key.tokenize('.')
			if (debug) { System.err.println "log4j=${log4j}, appender=${appender}, name=${name}, property=${property}, extra=${extra}, value=${value}" }
			if (!appenders.containsKey(name)) {
				appenders[name] = [:]
			}
			if (property == null) {
				if (value == "org.apache.log4j.ConsoleAppender") {
					appenders[name]['type']='Console'
				} else if (value == "org.apache.log4j.DailyRollingFileAppender") {
					appenders[name]['type']='DailyRollingFile'
				} else if (value == "org.apache.log4j.RollingFileAppender") {
					appenders[name]['type']='RollingFile'
				} else {
                    System.err.println "WARNING: unknown appender type ${value} ignored!"
                }
			} else {
				appenders[name][property]=value
			}
			if (extra != null) {
				appenders[name]['pattern']=value
			}
		} else if (key.startsWith("log4j.logger")) {
			def loggerName=key.substring("log4j.logger.".size())
			loggers[loggerName]=value.tokenize( ',' )

		} else if (key.startsWith("log4j.rootCategory") || key.startsWith("log4j.rootLogger")) {
			def rootCategories = value.tokenize( ',' )
            rootLevel = rootCategories[0]
            rootAppenders = rootCategories[1..-1]
		} else if (key.startsWith("log4j.additivity")) {
			def loggerName=key.substring("log4j.additivity.".size())
			additivity[loggerName]=value
		} else {
            System.err.println "WARNING: unknown property ${key} ignored!"
        }
	}
	
	def values = ['rootLevel': rootLevel, 'rootAppenders':rootAppenders, 'appenders': appenders, 'loggers': loggers, 'additivity': additivity]

	return values
}

def generate(bindings) {
    def xmlWriter = new StringWriter() 
    def xmlMarkup = new groovy.xml.MarkupBuilder(xmlWriter)
    xmlMarkup.setOmitNullAttributes(true)
    xmlMarkup.setDoubleQuotes(true)
    xmlMarkup.mkp.xmlDeclaration(version: "1.0", encoding: "utf-8")
    xmlMarkup
        .'Configuration' {
            'Appenders' {
                bindings['appenders'].each { name, values -> 

                    if (values['type'] == 'Console') {
                        def target = 'SYSTEM_OUT' 
                        if (values['target'] == 'System.err') { target = 'SYSTEM_ERR' }
                        'Console'(name: name, target:target) {
                            'PatternLayout'(pattern:values['pattern'])
                        }
                    } else if (values['type'] == 'DailyRollingFile') {
                        'RollingFile'(name:name, fileName:values['File'], filePattern:"${fileName:values['File']}-%d{'+values['DatePattern']+'}") {
                            mkp.comment('TODO filePattern is autogenerated. Please review. ')
                            'PatternLayout'(pattern:values['pattern'])
                            'TimeBasedTriggeringPolicy' ()
                        }                        
                    } else if (values['type'] == 'RollingFile') {
                        'RollingFile'(name:name, fileName:values['File'], filePattern:"${fileName:values['File']}.%i") {
                            'PatternLayout'(pattern:values['pattern'])
                            'Policies' {
                                'SizeBasedTriggeringPolicy' (size: values['MaxFileSize'])
                            }
                            'DefaultRolloverStrategy' (max: values['MaxBackupIndex'])
                        }
                    }
                }
            }
            'Loggers' {
                bindings['loggers'].each { name, value ->
                    'AsyncLogger' (name:name, level:value[0].trim(), additivity: bindings['additivity'][name]) {
                        if (value.size() > 1) {
                            def loggerAppenders = value[1..-1]
                            loggerAppenders.each {
                                'AppenderRef' (ref:it.trim())
                            }
                        }
                    }
                }
                'AsyncRoot' (level:bindings['rootLevel']) {
                    bindings['rootAppenders'].each { name ->
                        AppenderRef (ref:name.trim())
                    }
                }
            }
        }
    return xmlWriter.toString()

}
