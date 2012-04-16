package com.pinkhippo.hogan

/** Helper object */
class Hogan {

	public static Map tags = [
		'#': 1,
		'^': 2,
		'<': 3,
		'$': 4,
		'/': 5,
		'!': 6,
		'>': 7,
		'=': 8,
		'_v': 9,
		'{': 10,
		'&': 11,
		'_t': 12,
		'\n': 13
	]

	static compile(String source, Map options = [:]) {
		def scanner = new HoganScanner()
		def parser = new HoganParser()
		def compiler = new GroovyHoganCompiler()

		def tokens = scanner.scan(source, options.delimiters)
		def tree = parser.parse(tokens, source, options)

		if (options.asString) {
			compiler.generate(tree)
		} else {
			compiler.compile(tree, source, options)
		}
	}
}