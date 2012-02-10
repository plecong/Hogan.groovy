package com.pinkhippo.hogan

/** Helper object */
class Hogan {
	static compile(String source, Map options = [:]) {
		def scanner = new HoganScanner()
		def parser = new HoganParser()
		def compiler = new GroovyHoganCompiler()

		def tokens = scanner.scan(source, options.delimiters)
		def tree = parser.parse(tokens)

		if (options.asString) {
			compiler.generate(tree)
		} else {
			compiler.compile(tree, source, options)
		}
	}
}