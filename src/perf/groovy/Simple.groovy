import gbench.*
import com.github.plecong.hogan.*
import com.github.plecong.hogan.groovy.*
import com.samskivert.mustache.*

// essentially find all scripts that don't have closures in the context
def testsToRun = (HoganCompilerSpec.basicTests + HoganCompilerSpec.shootOutTests)
	.findAll { !it.excludePerf }

def hoganCompiler = new GroovyHoganCompiler();

// hogan.groovy precompilation
def hoganTemplates = testsToRun.collectEntries {
	it.compiledPartials = (it.partials ?: [:]).collectEntries { k, v ->
		[k, Hogan.compile(v)]
	}
	[it.name, Hogan.compile(it.text)]
}

// mustache precompilation
def mustacheTemplates = testsToRun.collectEntries {
	def mustacheCompiler = Mustache.compiler()
		.nullValue('')
		.withLoader(new LocalMustacheLoader(partials: it.partials))

	[it.name, mustacheCompiler.compile(it.text)]
}

// mustache.java precompilation
def mustacheJavaTemplates = testsToRun.collectEntries {
	def mf = new LocalDefaultMustacheFactory(partials: it.partials);
	[it.name, mf.compile(new StringReader(it.text), it.name)]
}

// hogan.js rhino precompilation
def rhinoHogan = new RhinoHogan()
def hoganRhinoTemplates = testsToRun.collectEntries {
	it.rhinoPartials = it.partials ? it.partials.collectEntries { k, v -> [k, rhinoHogan.compile(v).template] } : [:]
	[it.name, rhinoHogan.compile(it.text) ]
}

new BenchmarkBuilder().run {

	'Hogan.groovy' {
		testsToRun.each {
			def tmpl = hoganTemplates[it.name]
			def s = tmpl.render(it.data, it.compiledPartials)
			assert s == it.expected
		}
	}

	'Mustache.java' {
		testsToRun.each {
			def tmpl = mustacheJavaTemplates[it.name]
			def writer = new StringWriter()
			tmpl.execute(writer, it.data)
		}
	}

	'Hogan.js (Rhino)' {
		testsToRun.each {
			def tmpl = hoganRhinoTemplates[it.name]
			def s = tmpl.render(it.data, it.rhinoPartials)
			assert s == it.expected
		}
	}

	'JMustache' {
		testsToRun.each {
			def tmpl = mustacheTemplates[it.name]
			tmpl.execute(it.data)
		}
	}

}.prettyPrint()

