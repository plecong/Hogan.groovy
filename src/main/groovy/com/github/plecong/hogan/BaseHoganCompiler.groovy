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

package com.github.plecong.hogan

import com.github.plecong.hogan.parser.HoganParser
import com.github.plecong.hogan.parser.HoganScanner
import com.github.plecong.hogan.parser.HoganToken

abstract class BaseHoganCompiler implements HoganCompiler {

	static QUOT_PATTERN = ~/\"/
	static SINGLE_QUOT_PATTERN = ~/\'/
	static NEWLINE_PATTERN = ~/\n/
	static CR_PATTERN = ~/\r/

	// the code generation states
	abstract String stringify(Map context, String source, String name = null)
	abstract void section(HoganToken node, Map context)
	abstract void invertedSection(HoganToken node, Map context)
	abstract void partial(HoganToken node, Map context)
	abstract void includeSub(HoganToken node, Map context)
	abstract void newLine(HoganToken node, Map context)
	abstract void variable(HoganToken node, Map context)
	abstract void text(HoganToken node, Map context)
	abstract void tripleStache(HoganToken node, Map context)

	// the compilation interface
	abstract HoganTemplate compile(String source, Map options = [:])
	abstract Class<HoganTemplate> compileClass(String source, Map options = [:])

	String generate(String source, Map options = [:]) {
		def tokens = new HoganScanner().scan(source, options.delimiters)
		def tree = new HoganParser().parse(tokens, source, options)

		def context = [code: createBuffer(), subs: [:], partials: [:], serialNo: 0]
		walk(tree, context)
		return stringify(context, source, options.name)
	}

	protected def createBuffer() {
		new StringBuffer()
	}

	protected void walk(List<HoganToken> tree, Map context) {
		tree.each { node ->
			switch(node.tag) {
			case '#':
				section(node, context)
				break
			case '^':
				invertedSection(node, context)
				break
			case '>':
				partial(node, context)
				break
			case '<':
				include(node, context)
				break
			case '$':
				includeSub(node, context)
				break
			case '\n':
				newLine(node, context)
				break
			case '_v':
				variable(node, context)
				break
			case '_t':
				text(node, context)
				break
			case '&':
			case '{':
				tripleStache(node, context)
				break
			case '!':
				// comment so just ignore
				break;
			default:
				throw new RuntimeException("Compiler didn't recognize token: ${node.tag}")
			}
		}
	}

	protected esc(String s) {
		s.replaceAll('\\\\', "\\\\\\\\")
			.replaceAll(QUOT_PATTERN, '\\\\"')
			.replaceAll(SINGLE_QUOT_PATTERN, "\\\\'")
			.replaceAll(NEWLINE_PATTERN, "\\\\n")
			.replaceAll(CR_PATTERN, "\\\\r")
	}

	protected String chooseMethod(String name) {
		~name.indexOf('.') ? 'd' : 'f'
	}
}
