package com.pinkhippo.hogan

class HoganParser {
	List parse(List tokens, String text = null, Map options = [:]) {
		buildTree(new ArrayDeque(tokens), '', new ArrayDeque(), options.sectionTags ?: [])
	}

	List buildTree(Queue tokens, String kind, Queue stack, Collection customTags) {
		def instructions = []
		def token = null

		while (!tokens.empty) {
			token = tokens.pop()

			if (!(token instanceof String) && (token.tag == '#' || token.tag == '^' || isOpener(token, customTags))) {
				stack.push(token)
				token.nodes = buildTree(tokens, token.tag, stack, customTags)
				instructions.add(token)

			} else if (!(token instanceof String) && token.tag == '/') {
				if (stack.empty) {
					throw new ParseException('Closing tag without opener: ' + token.n, token)
				}

				def opener = stack.pop()

				if (token.n != opener.n && !isCloser(token.n, opener.n, customTags)) {
					throw new ParseException('Nesting error: ' + opener.n + ' vs. ' + token.n, token)
				}

				opener.end = token.i
				return instructions
			} else {
				instructions.add(token)
			}
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
