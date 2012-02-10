package com.pinkhippo.hogan

import java.util.regex.Pattern

import org.codehaus.groovy.ast.ClassHelper

class HoganScanner {

	enum State {
		IN_TEXT,
		IN_TAG_TYPE,
		IN_TAG
	}

	static tagTypes = [
		'#': 1,
		'^': 2,
		'/': 3,
		'!': 4,
		'>': 5,
		'<': 6,
		'=': 7,
		'_v': 8,
		'{': 9,
		'&': 10
	]

	List tokens = []
	StringBuilder buf = new StringBuilder()
	int lineStart = 0
	String otag = '{{'
	String ctag = '}}'
	def seenTag = false

	def scan(String text, String delimiters = null) {
		def tagType

		State state = State.IN_TEXT
		int len = text.length()
		int i = 0

		tokens = []
		buf = new StringBuilder()
		lineStart = 0

		if (delimiters) {
			def d = delimiters.split()
			otag = d[0]
			ctag = d[1]
		}

		for (i = 0; i < len; i++) {
			if (state == State.IN_TEXT) {
				if (tagChange(otag, text, i)) {
					--i;
					addBuf();
					state = State.IN_TAG_TYPE
				} else {
					if (text[i] == '\n') {
						filterLine(seenTag)
					} else {
						buf << text[i]
					}
				}
			} else if (state == State.IN_TAG_TYPE) {
				i += otag.length() - 1
				boolean tag = tagTypes.containsKey(text[i+1])
				tagType = tag ? text[i+1] : '_v'
				if (tagType == '=') {
					i  = changeDelimiters(text, i)
					state = State.IN_TEXT
				} else {
					if (tag) {
						i++
					}
					state = State.IN_TAG
				}
				seenTag = i
			} else {
				if (tagChange(ctag, text, i)) {
					tokens.add(new HoganToken(
						tag: tagType,
						n: buf.toString().trim(),
						otag: otag,
						ctag: ctag,
						i: (tagType == '/') ? seenTag - otag.length() : i + ctag.length()
					))
					buf = new StringBuilder()
					i += ctag.length() - 1
					state = State.IN_TEXT

					if (tagType == '{') {
						if (ctag == '}}') {
							i++
						} else {
							cleanTripleStache(tokens[-1])
						}
					}
				} else {
					buf << text[i]
				}
			}
		}

		filterLine(seenTag, true)
		tokens
	}

	private filterLine(def haveSeenTag, boolean noNewLine = false) {
		addBuf()

		if (haveSeenTag && lineIsWhitespace()) {
			for (int j = lineStart; j < tokens.size(); j++) {
				if (tokens[j] instanceof String) {
					if (j < (tokens.size() - 1)) {
						def next = tokens[j+1]
						if (next.tag == '>') {
							next.indent = tokens[j]
						}
					}
					tokens.remove(j)
				}
			}
		} else if (!noNewLine) {
			tokens.push(new HoganToken(tag: '\n'))
		}

		seenTag = false
		lineStart = tokens.size()
	}

	private int changeDelimiters(String text, int index) {
		// TODO: optimize this since this will search the entire
		// string in memory and eventually we want to be able to
		// do the scanning from an input stream with look ahead
		// buffer
		def close = '=' + ctag
		def closeIndex = text.indexOf(close, index)
		def delimiters = text.substring(text.indexOf('=', index) + 1, closeIndex)
			.trim()
			.split(' ')

		otag = delimiters[0]
		ctag = delimiters[1]

		return closeIndex + close.length() - 1
	}

	private boolean tagChange(String tag, String text, int index) {
		if (text[index] != tag[0])
			return false

		for (int j = 0; j < tag.length(); j++) {
			if (text[index + j] != tag[j])
				return false
		}

		return true
	}

	private boolean lineIsWhitespace() {
		tokens[lineStart..<tokens.size()].every { t ->
			(t instanceof String) ?
				t.trim().length() == 0 :
				tagTypes[t.tag] < tagTypes['_v']
		}
	}

	private addBuf() {
		if (buf.length()) {
			tokens.add(buf.toString())
			buf = new StringBuilder()
		}
	}

}