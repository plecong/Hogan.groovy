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

package com.github.plecong.hogan.groovy

import java.util.concurrent.atomic.AtomicInteger
import com.github.plecong.hogan.BaseHoganCompiler
import com.github.plecong.hogan.HoganTemplate
import com.github.plecong.hogan.parser.HoganToken
import com.github.plecong.hogan.utils.HoganBuffer

class GroovyHoganCompiler extends BaseHoganCompiler {

	ClassLoader classLoader

	static AtomicInteger counter = new AtomicInteger()

	static importLines = [
		'com.github.plecong.hogan.HoganCompiler',
		'com.github.plecong.hogan.TemplateLoader',
		'com.github.plecong.hogan.groovy.GroovyHoganTemplate'
	]

	public GroovyHoganCompiler() {
		classLoader = new GroovyClassLoader(Thread.currentThread().getContextClassLoader());
	}

	@Override
	protected def createBuffer() { new HoganBuffer() }

	HoganTemplate compile(String source, Map options = [:]) {
		def clazz = compileClass(source, options)
		(HoganTemplate)clazz.newInstance([source, this, options].toArray())
	}

	Class<HoganTemplate> compileClass(String source, Map options = [:]) {
		// generate the name if one doesn't exist because the generated class must have a name
		if (!options.name) {
			options.name = "hogan${System.nanoTime()}_${counter.incrementAndGet()}"
		}

		def generated = generate(source, options)

		if (options.keepGenerated) {
			def path = (options.keepGenerated instanceof String) ? options.keepGenerated : ''

			def dir = new File(path)
			dir.mkdirs()

			def file = new File(dir, "${options.name}.groovy")
			file.text = generated
		}

		classLoader.parseClass(generated)
	}

	void writeImports(writer) {
		importLines.each {
			writer.println("import ${it}")
		}
		writer.println()
	}

	String stringify(Map codeObj, String text, String name) {
		def writer = new StringWriter()

		writeImports(writer)
		writer.println("class ${name} extends GroovyHoganTemplate {")

		// partials variable, which is just a map of symbol name, to name of partial
		writer.print('Map getCodeData() { [')
		writer.print(stringifyPartials(codeObj))
		writer.println('] }')

		// constructors
		writer.println("public ${name}() { super(null, null); }")
		writer.println("public ${name}(String s, HoganCompiler c, Map o = [:], Map p = [:], Map u = [:]) { super(s, c, o, p, u); }")

		// main rendering method
		writer.println('String r(Deque c, TemplateLoader p, String i) {')
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
	void section(HoganToken node, Map context) {
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
	void invertedSection(HoganToken node, Map context) {
		def method = chooseMethod(node.n)
		def escId = esc(node.n)

		context.code << "if(!t.s(t.${method}('${escId}',c,p,true),c,p,true,0,0,'')){";
		walk(node.nodes, context);
		context.code << '};';
	}

	// >
	void partial(HoganToken node, Map context) {
		createPartial(node, context)
	}

	// <
	void include(HoganToken node, Map context) {
		def ctx = [partials: [:], code: new StringBuilder(), subs: [:], inPartial: true, serialNo: context.serialNo];
		walk(node.nodes, ctx);
		def template = context.partials[createPartial(node, context)]
		template.subs = ctx.subs
		template.partials = ctx.partials
	}

	// $
	void includeSub(HoganToken node, Map context) {
		def ctx = [subs: [:], code: new StringBuilder(), partials: context.partials, prefix: node.n, serialNo: context.serialNo]
		walk(node.nodes, ctx)
		context.subs[node.n] = ctx.code
		if (!context.inPartial) {
			context.code << 't.sub("' + esc(node.n) + '",c,p);'
		}
	}

	// \n
	void newLine(HoganToken node, Map context) {
		context.code << writeStr("'\\n'" + (node.last ? '' : ' + i'))
	}

	// _v
	void variable(HoganToken node, Map context) {
		context.code << 't.b(t.v(t.' + chooseMethod(node.n) + '("' + esc(node.n) + '",c,p,false)));'
	}

	// _t
	void text(HoganToken node, Map context) {
		context.code << writeStr('\'' + esc(node.text) + '\'')
	}

	// & and {
	void tripleStache(HoganToken node, Map context) {
		context.code << 't.b(t.t(t.' + chooseMethod(node.n) + '("' + esc(node.n) + '",c,p,false)));'
	}

	private String createPartial(HoganToken node, Map context) {
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
		def functionLines = obj.collect { key, val -> "'${esc(key)}': { Deque c, TemplateLoader p, GroovyHoganTemplate t -> ${val} }" }
		return functionLines ? '[' + functionLines.join(',') + ']' : '[:]'
	}

	private String writeStr(String str) {
		"t.b(${str});"
	}
}