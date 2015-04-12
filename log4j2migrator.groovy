

Properties properties = new Properties()
File propertiesFile = new File(args[0])
propertiesFile.withInputStream {
    properties.load(it)
}


def bindings = parse(properties)
//println "Bindings = ${bindings}"

println generate(bindings)


def parse(properties) {

	def appenders=[:]
	def loggers=[:]
	def rootLevel
	def rootAppender

	properties.each { key, value ->
		//println "property ${key}=${value}"
		if (key.startsWith("log4j.appender")) {
			def (log4j, appender, name, property,extra)=key.tokenize('.')
			//println "log4j=${log4j}, appender=${appender}, name=${name}, property=${property}, extra=${extra}, value=${value}"
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
				}
			} else {
				appenders[name][property]=value
			}
			if (extra != null) {
				appenders[name]['pattern']=value
			}
		} else if (key.startsWith("log4j.logger")) {
			def loggerName=key.substring("log4j.logger.".size())
			loggers[loggerName]=value
			
		} else if (key.startsWith("log4j.rootCategory")) {
			(rootLevel, rootAppender) = value.tokenize( ',' )
		}
	}
	
    def values = ['rootLevel': rootLevel, 'rootAppender':rootAppender, 'appenders': appenders, 'loggers': loggers]
	//println "values=${values}"
	return values
}

def generate(bindings) {
    def xmlWriter = new StringWriter() 
    def xmlMarkup = new groovy.xml.MarkupBuilder(xmlWriter)
    xmlMarkup
        .'Configuration' {
            'Appenders' {
                bindings['appenders'].each { name, values -> 

                    if (values['type'] == 'Console') { 
                        'Console'(name: name, target:'SYSTEM_OUT') {
                            'PatternLayout'(pattern:values['pattern'])
                        }
                    } else if (values['type'] == 'DailyRollingFile') {
                        'RollingFile'(name:name, fileName:values['File'], filePattern:values['DatePattern']) {
                            'PatternLayout'(pattern:values['pattern'])
                            'TimeBasedTriggeringPolicy' ()
                        }
                    } else if (values['type'] == 'RollingFile') {
                        'RollingFile'(name:name, fileName:values['File'], filePattern:values['DatePattern']) {
                            'PatternLayout'(pattern:values['pattern'])
                            'Policies' {
                                'SizeBasedTriggeringPolicy' (size: values['maxFileSize'])
                                'DefaultRolloverStrategy' (max: values['maxBackupIndex'])
                            }
                        }
                    }
                }
            }
            'Loggers' {
                bindings['loggers'].each { name, value ->
                    'AsyncLogger' (name:name, level:value.trim())
                }
            }
            'AsyncRoot' (level:bindings['rootLevel']) {
                AppenderRef (ref:bindings['rootAppender'].trim())
            }
        }
    return xmlWriter.toString()

}
