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

abstract class HoganCompiler {

	static QUOT_PATTERN = ~/\"/
	static SINGLE_QUOT_PATTERN = ~/\'/
	static NEWLINE_PATTERN = ~/\n/
	static CR_PATTERN = ~/\r/

	// generates the final code
	abstract def stringify(Map codeObj, String text, Map options)
	// #
	abstract void section(node, context)
	// ^
	abstract void invertedSection(node, context)
	// >
	abstract void partial(node, context)
	// <
	abstract void include(node, context)
	// $
	abstract void includeSub(node, context)
	// \n
	abstract void newLine(node, context)
	// _v
	abstract void variable(node, context)
	// _t
	abstract void text(node, context)
	// & and {
	abstract void tripleStache(node, context)

	String generate(List tree, String source, Map options = [:]) {
		def context = [code: createBuffer(), subs: [:], partials: [:], serialNo: 0]
		walk(tree, context)
		stringify(context, source, options) // can only output strings
	}

	def createBuffer() {
		new StringBuffer()
	}

	protected def walk(nodeList, context) {
		nodeList.each { node ->
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
					throw new RuntimeException("Compiler couldn't recognize token: ${node.tag}")
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
