package com.pinkhippo.hogan

import java.util.concurrent.atomic.AtomicInteger

class GroovyHoganCompiler extends HoganCompiler {

	ClassLoader classLoader

	static AtomicInteger counter = new AtomicInteger()

	static importLines = [
		'com.pinkhippo.hogan.HoganCompiler',
		'com.pinkhippo.hogan.HoganPage'
	]

	public GroovyHoganCompiler() {
		classLoader = new GroovyClassLoader(Thread.currentThread().getContextClassLoader());
	}

	HoganPage compile(String source, Map options = [:]) {
		def scanner = new HoganScanner()
		def parser = new HoganParser()

		def tokens = scanner.scan(source, options.delimiters)
		def tree = parser.parse(tokens)
		compile(tree, source, options)
	}

	HoganPage compile(List tree, String source, Map options = [:]) {
		def generated = generate(tree)
		def clazz = classLoader.parseClass(generated)
		def args = new Object[3]
		args[0] = source
		args[1] = this
		args[2] = options
		def page = (HoganPage)clazz.newInstance(args)
		page.generated = generated
		page
	}

	void writeImports() {
		importLines.each {
			println("import ${it}")
		}
		println()
	}

	void writeCode(tree) {
		writeImports()

		def name = "hogan${System.nanoTime()}_${counter.incrementAndGet()}"


		print('class ')
		print(name)
		println(' extends HoganPage {')
		println("public ${name}() { super(null, null); }")
		println("public ${name}(String s, HoganCompiler c, Map o = [:]) { super(s, c, o); }")
		println('String r(Deque c, Map p, String i) {')
		println('def _ = this')
		println('_.b(i ?: "")')
		walk(tree)
		println()
		println('return _.fl()')
		println('}')
		println('}')
	}

	void section(nodes, id, method, start, end, tags) {
		def escId = esc(id)
		println("if(_.s(_.${method}('${escId}',c,p,true),c,p,false,${start},${end},'${tags}')) {")
		println('_.rs(c,p) { ')
		walk(nodes)
		println('}')
		println('c.pop()')
		println('}')
	}

	void invertedSection(nodes, id, method) {
		def escId = esc(id)
		println("if(!_.s(_.${method}('${escId}',c,p,true),c,p,true,0,0,'')) {")
		walk(nodes)
		println('}')
	}

	void partial(tok) {
		def escN = esc(tok.n)
		println("_.b(_.rp('${escN}',c,p,'${tok.indent ?: ''}'));")
	}

	void tripleStache(id, method) {
		def escId = esc(id)
		println("_.b(_.t(_.${method}('${escId}',c,p,false)))")
	}

	void variable(id, method) {
		def escId = esc(id)
		println("_.b(_.v(_.${method}('${escId}',c,p,false)))")
	}

	void text(str) {
		println("_.b(${str})")
	}
}