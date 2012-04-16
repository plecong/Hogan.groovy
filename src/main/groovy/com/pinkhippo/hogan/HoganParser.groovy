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

/**
 * Parses the stream of <code>HoganToken</code>s into a tree that will
 * be used by the <code>HoganCompiler</code> to generate the compiled
 * page. This mostly consists of grouping the tokens inside of a section
 * underneath the token representing the opener of the section.
 *
 * @author plecong
 */
class HoganParser {

	static allowedInSuper = [
		'_t', '\n', '$', '/'
	]

	List parse(List tokens, String text = null, Map options = [:]) {
		buildTree(new ArrayDeque(tokens), '', new ArrayDeque(), options.sectionTags ?: [])
	}

	List buildTree(Deque tokens, String kind, Deque stack, Collection customTags) {
		def instructions = []
		def tail = null
		def token = null

		tail = stack.peekLast()
		boolean checkSuper = (tail?.tag == '<')

		while (tokens) {
			token = tokens.pop()

			if (checkSuper && !(token.tag in allowedInSuper)) {
				throw new ParseException('Illegal content in < super tag.', token)
			}

			if (Hogan.tags[token.tag] <= Hogan.tags['$'] || isOpener(token, customTags)) {
				stack.push(token)
				token.nodes = buildTree(tokens, token.tag, stack, customTags)
			} else if (token.tag == '/') {
				if (stack.empty) {
					throw new ParseException('Closing tag without opener: ' + token.n, token)
				}

				def opener = stack.pop()

				if (token.n != opener.n && !isCloser(token.n, opener.n, customTags)) {
					throw new ParseException('Nesting error: ' + opener.n + ' vs. ' + token.n, token)
				}

				opener.end = token.i
				return instructions
			} else if (token.tag == '\n') {
				token.last = tokens.empty || tokens.peek().tag == '\n'
			}

			instructions.add(token)
		}

		if (!stack.empty) {
			def top = stack.pop()
			throw new ParseException('Missing closing tag: ' + top.n, top)
		}

		return instructions
	}

	private boolean isOpener(HoganToken token, List tags) {
		def custom = tags.find { it.o == token.n }

		if (custom) {
			token.tag = '#'
			return true
		}

		return false
	}

	private boolean isCloser(String close, String open, List tags) {
		tags.any { it.c == close && it.o == open }
	}
}
