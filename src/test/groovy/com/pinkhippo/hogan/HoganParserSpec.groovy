package com.pinkhippo.hogan

import spock.lang.Specification

class HoganParserSpec extends Specification {

	HoganScanner scanner
	HoganParser parser

	def setup() {
		scanner = new HoganScanner()
		parser = new HoganParser()
	}

	def 'parse basic'() {
		setup:
			def text = 'test'

		when:
			def tree = parser.parse(scanner.scan(text))

		then:
			tree.size() == 1
			tree[0] == 'test'
	}

	def 'parse variables'() {
		setup:
			def text = 'test{{foo}}test!{{bar}}test!!{{baz}}test!!!'

		when:
			def tree = parser.parse(scanner.scan(text))

		then:
			tree.size() == 7
			tree[0] == 'test'
			tree[2] == 'test!'
			tree[4] == 'test!!'
			tree[6] == 'test!!!'
			tree[1].n == 'foo'
			tree[3].n == 'bar'
			tree[5].n =='baz'
	}

	def 'parse section'() {
		setup:
			def text = 'a{{#foo}}b{{/foo}}c'

		when:
			def tree = parser.parse(scanner.scan(text))

		then:
			tree.size() == 3
			tree[0] == 'a'
			tree[1].nodes != null
			tree[1].nodes.size() == 1
			tree[1].tag == '#'
			tree[1].n == 'foo'
			tree[1].nodes[0] == 'b'
			tree[2] == 'c'
	}

	def 'parse indexes'() {
		setup:
			def text = 'abc{{#foo}}asdf{{bar}}asdf{{/foo}}def'

		when:
			def tree = parser.parse(scanner.scan(text))

		then:
			text.substring(tree[1].i, tree[1].end) == 'asdf{{bar}}asdf'
	}

	def 'parse negative section'() {
		setup:
			def text = 'a{{^foo}}b{{/foo}}c'

		when:
			def tree = parser.parse(scanner.scan(text))

		then:
			tree.size() == 3
			tree[0] == 'a'
			tree[1].nodes != null
			tree[1].nodes.size() == 1
			tree[1].tag == '^'
			tree[1].n == 'foo'
			tree[1].nodes[0] == 'b'
			tree[2] == 'c'
	}

	def 'parse nested sections'() {
		setup:
			def text = '{{#bar}}{{#foo}}c{{/foo}}{{/bar}}'

		when:
			def tree = parser.parse(scanner.scan(text))

		then:
			tree.size() == 1
			tree[0].tag == '#'
			tree[0].n == 'bar'
			tree[0].nodes.size() == 1
			tree[0].nodes[0].n == 'foo'
			tree[0].nodes[0].nodes[0] == 'c'
	}

	def 'parse missing closing tag'() {
		setup:
			def text = 'a{{#foo}}bc'

		when:
			def tree = parser.parse(scanner.scan(text))

		then:
			def e = thrown(ParseException)
			e.message == 'Missing closing tag: foo'
	}

	def 'parse bad nesting'() {
		setup:
			def text = 'a{{#foo}}{{#bar}}b{{/foo}}{{/bar}}c'

		when:
			def tree = parser.parse(scanner.scan(text))

		then:
			def e = thrown(ParseException)
			e.message == 'Nesting error: bar vs. foo'
	}

}