

Properties properties = new Properties()
File propertiesFile = new File(args[0])
propertiesFile.withInputStream {
    properties.load(it)
}


def bindings = parse(properties)
//println "Bindings = ${bindings}"

generate(bindings)


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

	def engine = new groovy.text.XmlTemplateEngine()
	def text = '''\
	    <Configuration xmlns:gsp='http://groovy.codehaus.org/2005/gsp'>
	 		<Appenders>
	 		    <gsp:scriptlet>appenders.each { name, values -> </gsp:scriptlet>
	 		    	<gsp:scriptlet>def pattern=values['pattern']</gsp:scriptlet>
	 		    	<gsp:scriptlet>if (values['type']=='Console') { </gsp:scriptlet>
						<Console name="${name}" target="SYSTEM_OUT">
							<PatternLayout pattern="${pattern}"/>
						</Console>
				    <gsp:scriptlet>}</gsp:scriptlet>
	 		    	<gsp:scriptlet>if (values['type']=='RollingFile') { </gsp:scriptlet>
	 		    		<gsp:scriptlet>def fileName=values['File']</gsp:scriptlet>
	 		    		<gsp:scriptlet>def filePattern=values['DatePattern']</gsp:scriptlet>
						<RollingFile name="${name}" fileName="${fileName}" filePattern="${filePattern}">
							<PatternLayout pattern="${pattern}"/>
						</RollingFile>
				    <gsp:scriptlet>}</gsp:scriptlet>
	 		    <gsp:scriptlet>}</gsp:scriptlet>
			</Appenders>
			<Loggers>
				 <gsp:scriptlet>loggers.each { name, value -> </gsp:scriptlet>
					<Logger name="${name.trim()}" level="${value.trim()}"/>
	 		    <gsp:scriptlet>}</gsp:scriptlet>
				<Root level="${rootLevel}">
					<AppenderRef ref="${rootAppender}"/>
				</Root>
			</Loggers>
	</Configuration>
	'''
	def template = engine.createTemplate(text).make(bindings)
	println template.toString()
}