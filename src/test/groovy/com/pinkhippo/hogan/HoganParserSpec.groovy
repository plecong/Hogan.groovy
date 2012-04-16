package com.pinkhippo.hogan

import spock.lang.Specification

class HoganParserSpec extends Specification {

	HoganScanner scanner
	HoganParser parser

	def setup() {
		scanner = new HoganScanner()
		parser = new HoganParser()
	}

	def 'Parse Basic'() {
		when:
		def text = 'test'
		def tree = parser.parse(scanner.scan(text))

		then:
		tree.size() == 1
		tree[0].text == 'test'
	}

	def 'Parse Variables'() {
		when:
		def text = 'test{{foo}}test!{{bar}}test!!{{baz}}test!!!'
		def tree = parser.parse(scanner.scan(text))

		then:
		tree.size() == 7
		tree[0].text == 'test'
		tree[2].text == 'test!'
		tree[4].text == 'test!!'
		tree[6].text == 'test!!!'
		tree[1].n == 'foo'
		tree[3].n == 'bar'
		tree[5].n =='baz'
	}

	def 'Parse Section'() {
		when:
		def text = 'a{{#foo}}b{{/foo}}c'
		def tree = parser.parse(scanner.scan(text))

		then:
		tree.size() == 3
		tree[0].text == 'a'
		tree[1].nodes != null
		tree[1].tag == '#'
		tree[1].n == 'foo'
		tree[1].nodes[0].text == 'b'
		tree[2].text == 'c'
	}

	def 'Parse Indexes'() {
		when:
		def text = 'abc{{#foo}}asdf{{bar}}asdf{{/foo}}def'
		def tree = parser.parse(scanner.scan(text))

		then:
		text.substring(tree[1].i, tree[1].end) == 'asdf{{bar}}asdf'
	}

	def 'Parse Negative Section'() {
		when:
		def text = 'a{{^foo}}b{{/foo}}c'
		def tree = parser.parse(scanner.scan(text))

		then:
		tree.size() == 3
		tree[0].text == 'a'
		tree[1].nodes != null
		tree[1].tag == '^'
		tree[1].n == 'foo'
		tree[1].nodes[0].text == 'b'
		tree[2].text == 'c'
	}

	def 'Parse Nested Sections'() {
		when:
		def text = '{{#bar}}{{#foo}}c{{/foo}}{{/bar}}'
		def tree = parser.parse(scanner.scan(text))

		then:
		tree.size() == 1
		tree[0].tag == '#'
		tree[0].n == 'bar'
		tree[0].nodes.size() == 1
		tree[0].nodes[0].n == 'foo'
		tree[0].nodes[0].nodes[0].text == 'c'
	}

	def 'Missing Closing Tag'() {
		when:
		def text = 'a{{#foo}}bc'
		def tree = parser.parse(scanner.scan(text))

		then:
		def e = thrown(ParseException)
		e.message == 'Missing closing tag: foo'
	}

	def 'Bad Nesting'() {
		when:
		def text = 'a{{#foo}}{{#bar}}b{{/foo}}{{/bar}}c'
		def tree = parser.parse(scanner.scan(text))

		then:
		def e = thrown(ParseException)
		e.message == 'Nesting error: bar vs. foo'
	}

	/* Template Inheritance */

	def 'Parse a $ tag'() {
		when:
		def text = '{{$title}}Default title{{/title}}'
		def tree = parser.parse(scanner.scan(text))

		then:
		tree[0].tag == '$'
		tree.size() == 1
		tree[0].nodes.size() == 1
	}

}