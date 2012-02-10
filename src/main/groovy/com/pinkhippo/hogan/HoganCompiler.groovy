package com.pinkhippo.hogan

abstract class HoganCompiler {

	static QUOT_PATTERN = ~/\"/
	static NEWLINE_PATTERN = ~/\n/
	static CR_PATTERN = ~/\r/

	abstract void writeCode(tree)
	abstract void section(nodes, id, method, start, end, tags)
	abstract void invertedSection(nodes, id, method)
	abstract void partial(tok)
	abstract void tripleStache(id, method)
	abstract void variable(id, method)
	abstract void text(id)

	private StringBuilder buffer = new StringBuilder()

	String generate(List tree) {
		buffer = new StringBuilder()
		writeCode(tree)
		return buffer.toString()
	}

	protected walk(tree) {
		tree.eachWithIndex { token, i ->
			def tag = (token instanceof String) ? null : token.tag
			if (tag == '#') {
				section(token.nodes, token.n, chooseMethod(token.n), token.i, token.end, token.otag + ' ' + token.ctag)
			} else if (tag == '^') {
				invertedSection(token.nodes, token.n, chooseMethod(token.n))
			} else if (tag == '>' || tag == '<') {
				partial(token)
			} else if (tag == '{' || tag == '&') {
				tripleStache(token.n, chooseMethod(token.n))
			} else if (tag == '\n') {
				text('"\\n"')
				text(tree.size() - 1 == i ? '' : 'i')
			} else if (tag == '_v') {
				variable(token.n, chooseMethod(token.n))
			} else if (token instanceof String) {
				text('"' + esc(token) + '"')
			}
		}
	}

	protected void print(String str = '') {
		buffer << str
	}

	protected void println(String str = '') {
		buffer << str + '\n'
	}

	protected esc(String s) {
		s.replaceAll('\\\\', "\\\\\\\\")
			.replaceAll(QUOT_PATTERN, '\\\\"')
			.replaceAll(NEWLINE_PATTERN, "\\\\n")
			.replaceAll(CR_PATTERN, "\\\\r")
	}

	protected String chooseMethod(String name) {
		name.indexOf('.') < 0 ? 'f' : 'd'
	}
}