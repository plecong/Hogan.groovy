import gbench.*
import com.pinkhippo.hogan.*
import com.samskivert.mustache.*

// essentially find all scripts that don't have closures in the context
def testsToRun =
	(HoganCompilerSpec.basicTests + HoganCompilerSpec.shootOutTests)
	.findAll { !it.excludePerf }

// hogan.groovy precompilation
def hoganTemplates = testsToRun.collectEntries {
	[it.name, Hogan.compile(it.text)]
}

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

// hogan.js rhino precompilation
def rhinoHogan = new RhinoHogan()
def hoganRhinoTemplates = testsToRun.collectEntries {
	[it.name, rhinoHogan.compile(it.text) ]
}

new BenchmarkBuilder().run {

	'Hogan.groovy' {
		testsToRun.each {
			def tmpl = hoganTemplates[it.name]
			tmpl.render(it.data, it.partials)
		}
	}

	'Hogan.js (Rhino)' {
		testsToRun.each {
			def tmpl = hoganRhinoTemplates[it.name]
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

