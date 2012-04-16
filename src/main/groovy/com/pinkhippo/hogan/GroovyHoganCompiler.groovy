/*
 *  Copyright 2012 Phuong LeCong
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.pinkhippo.hogan

import java.util.concurrent.atomic.AtomicInteger

class GroovyHoganCompiler extends HoganCompiler {

	ClassLoader classLoader

	static AtomicInteger counter = new AtomicInteger()
	int ctxcnt = 0

	static importLines = [
		'com.pinkhippo.hogan.HoganCompiler',
		'com.pinkhippo.hogan.HoganPage'
	]

	public GroovyHoganCompiler() {
		classLoader = new GroovyClassLoader(Thread.currentThread().getContextClassLoader());
	}

	def createBuffer() { new HoganBuffer() }

	HoganPage compile(String source, Map options = [:]) {
		def scanner = new HoganScanner()
		def parser = new HoganParser()

		def tokens = scanner.scan(source, options.delimiters)
		def tree = parser.parse(tokens, source, options)
		compile(tree, source, options)
	}

	HoganPage compile(List tree, String source, Map options = [:]) {
		def generated = generate(tree, source, options)

		if (options.keepGenerated) {
			def path = (options.keepGenerated instanceof String) ? options.keepGenerated : ''
			def dir = new File(path)
			dir.mkdirs()
			def file = new File(dir, "hogan${System.nanoTime()}_${counter.incrementAndGet()}.groovy")
			file.text = generated
		}

		def clazz = classLoader.parseClass(generated)
		def args = new Object[3]
		args[0] = source
		args[1] = this
		args[2] = options
		def page = (HoganPage)clazz.newInstance(args)
		page.generated = generated
		page
	}

	void writeImports(writer) {
		importLines.each {
			writer.println("import ${it}")
		}
		writer.println()
	}

	def stringify(Map codeObj, String text, Map options) {
		def writer = new StringWriter()
		def name = options.name ?: "hogan${System.nanoTime()}_${counter.incrementAndGet()}"

		writeImports(writer)
		writer.println("class ${name} extends HoganPage {")

		// partials variable, which is just a map of symbol name, to name of partial
		writer.print('Map getCodeData() { [')
		writer.print(stringifyPartials(codeObj))
		writer.println('] }')

		// constructors
		writer.println("public ${name}() { super(null, null); }")
		writer.println("public ${name}(String s, HoganCompiler c, Map o = [:], Map p = [:], Map u = [:]) { super(s, c, o, p, u); }")

		// main rendering method
		writer.println('String r(Deque c, Map p, String i) {')
		wrapMain(writer, codeObj.code)
		writer.println('}')

		writer.println('}')
		writer.toString()
	}

	private void wrapMain(Writer writer, def code) {
		writer.println('def t=this;')
		writer.println('t.b(i ?: "");')
		writer.println(code)
		writer.println('return t.fl();')
	}

	// #
	void section(node, context) {
		def method = chooseMethod(node.n)
		def escId = esc(node.n)
		def start = node.i
		def end = node.end
		def tags = "${node.otag} ${node.ctag}"

		context.code << "if(t.s(t.${method}('${escId}',c,p,true),c,p,false,${start},${end},'${tags}')){"
		context.code << 't.rs(c,p){'
		walk(node.nodes, context)
		context.code << '};c.pop();};'
	}

	// ^
	void invertedSection(node, context) {
		def method = chooseMethod(node.n)
		def escId = esc(node.n)

		context.code << "if(!t.s(t.${method}('${escId}',c,p,true),c,p,true,0,0,'')){";
		walk(node.nodes, context);
		context.code << '};';
	}

	// >
	void partial(node, context) {
		createPartial(node, context)
	}

	// <
	void include(node, context) {
		def ctx = [partials: [:], code: new StringBuilder(), subs: [:], inPartial: true, serialNo: context.serialNo];
		walk(node.nodes, ctx);
		def template = context.partials[createPartial(node, context)]
		template.subs = ctx.subs
		template.partials = ctx.partials
	}

	// $
	void includeSub(node, context) {
		def ctx = [subs: [:], code: new StringBuilder(), partials: context.partials, prefix: node.n, serialNo: context.serialNo]
		walk(node.nodes, ctx)
		context.subs[node.n] = ctx.code
		if (!context.inPartial) {
			context.code << 't.sub("' + esc(node.n) + '",c,p);'
		}
	}

	// \n
	void newLine(node, context) {
		context.code << writeStr("'\\n'" + (node.last ? '' : ' + i'))
	}

	// _v
	void variable(node, context) {
		context.code << 't.b(t.v(t.' + chooseMethod(node.n) + '("' + esc(node.n) + '",c,p,false)));'
	}

	// _t
	void text(node, context) {
		context.code << writeStr('\'' + esc(node.text) + '\'')
	}

	// & and {
	void tripleStache(node, context) {
		context.code << 't.b(t.t(t.' + chooseMethod(node.n) + '("' + esc(node.n) + '",c,p,false)));'
	}

	private String createPartial(node, context) {
		String prefix = '<' + (context.prefix ?: '')
		String sym = prefix + node.n + context.serialNo++
		context.partials[sym] = [name: node.n, partials: [:], subs: [:]]
		context.code << 't.b(t.rp("' +  esc(sym) + '",c,p,"' + (node.indent ?: '') + '"));'
		sym
	}

	private String stringifyPartials(Map obj) {
		def partialLines = obj.partials.collect { key, val ->
			"'${esc(key)}': [name: '${esc(val.name)}', ${stringifyPartials(val)}]"
		}
		def partialLinesCode = partialLines ? partialLines.join(',') : ':'
		return "partials: [${partialLinesCode}], subs: " + stringifyFunctions(obj.subs)
	}

	private String stringifyFunctions(Map obj) {
		def functionLines = obj.collect { key, val -> "'${esc(key)}': { Deque c, Map p, HoganPage t -> ${val} }" }
		return functionLines ? '[' + functionLines.join(',') + ']' : '[:]'
	}

	private String writeStr(String str) {
		"t.b(${str});"
	}
}