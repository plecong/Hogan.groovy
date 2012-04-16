import gbench.*
import com.pinkhippo.hogan.*
import com.samskivert.mustache.*

// pre-compile the scripts
def testsToRun = HoganCompilerSpec.shootOutTests.findAll { !it.excludePerf }

// hogan precompilation
def hoganTemplates = testsToRun.collectEntries { [it.name, Hogan.compile(it.text)] }

// mustache precompilation
def mustacheTemplates = testsToRun.collectEntries {
	// have to create a new compiler each time because someh
	// have partials and need to be able to get the partial
	// from the instance of the test
	final def partials = it.partials

	def mustacheCompiler = Mustache.compiler()
		.nullValue('')
		.withLoader(new Mustache.TemplateLoader() {
			public Reader getTemplate(String name) {
				return new StringReader(partials[name])
			}
		})

	[it.name, mustacheCompiler.compile(it.text)]
}

new BenchmarkBuilder().run {

	'Hogan.groovy' {
		testsToRun.each {
			def tmpl = hoganTemplates[it.name]
			tmpl.render(it.data, it.partials)
		}
	}

	'JMustache' {
		testsToRun.each {
			def tmpl = mustacheTemplates[it.name]
			tmpl.execute(it.data)
		}
	}

}.prettyPrint()
