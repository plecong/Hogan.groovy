package com.twitter.hogan

import spock.lang.Specification

class HoganScannerSpec extends Specification {

	HoganScanner scanner

	def setup() {
		scanner = new HoganScanner()
	}

	def 'scan text no tags'() {
		setup:
			def text = '<h2>hi</h2>'
		when:
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 1
			tokens[0] == text
	}

	def 'scan one tag'() {
		setup:
			def text = '{{hmm}}'
		when:
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 1
			tokens[0].n == 'hmm'
	}

	def 'scan multiple tags'() {
		setup:
			def text = 'asdf{{hmm}}asdf2{{hmm2}}asdf3'
		when:
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 5
			tokens[0] == 'asdf'
			tokens[1].n == 'hmm'
			tokens[1].tag == '_v'
			tokens[2] == 'asdf2'
			tokens[3].n == 'hmm2'
			tokens[3].tag == '_v'
			tokens[4] == 'asdf3'
	}

	def 'scan section open'() {
		setup:
			def text = '{{#hmm}}'
		when:
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 1
			tokens[0].n == 'hmm'
			tokens[0].tag == '#'
	}

	def 'scan section close'() {
		setup:
			def text = '{{/hmm}}'
		when:
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 1
			tokens[0].n == 'hmm'
			tokens[0].tag == '/'
	}

	def 'scan section'() {
		setup:
			def text = '{{#hmm}}{{/hmm}}'
		when:
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 2
			tokens[0].n == 'hmm'
			tokens[0].tag == '#'
			tokens[1].n == 'hmm'
			tokens[1].tag == '/'
	}

	def 'scan section in content'() {
		setup:
			def text = 'abc{{#hmm}}def{{/hmm}}ghi'
		when:
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 5
			tokens[0] == 'abc'
			tokens[1].n == 'hmm'
			tokens[1].tag == '#'
			tokens[2] == 'def'
			tokens[3].n == 'hmm'
			tokens[3].tag == '/'
			tokens[4] == 'ghi'
	}

	def 'scan negative section'() {
		setup:
			def text = '{{^hmm}}{{/hmm}}'
		when:
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 2
			tokens[0].n == 'hmm'
			tokens[0].tag == '^'
			tokens[1].n == 'hmm'
			tokens[1].tag == '/'
	}

	def 'scan partial'() {
		setup:
			def text = '{{>hmm}}'
		when:
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 1
			tokens[0].n == 'hmm'
			tokens[0].tag == '>'
	}

	def 'scan backward partial'() {
		setup:
			def text = '{{<hmm}}'
		when:
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 1
			tokens[0].n == 'hmm'
			tokens[0].tag == '<'
	}

	def 'scan ampersand no escape tag'() {
		setup:
			def text = '{{&hmm}}'
		when:
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 1
			tokens[0].n == 'hmm'
			tokens[0].tag == '&'
	}

	def 'scan triple stache'() {
		setup:
			def text = '{{{hmm}}'
		when:
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 1
			tokens[0].n == 'hmm'
			tokens[0].tag == '{'
	}

	def 'scan section with triple stache inside'() {
		setup:
			def text = 'a{{#yo}}b{{{hmm}}}c{{/yo}}d'
		when:
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 7
			tokens[0] == 'a'
			tokens[1].n == 'yo'
			tokens[1].tag == '#'
			tokens[2] == 'b'
			tokens[3].n == 'hmm'
			tokens[3].tag == '{'
			tokens[4] == 'c'
			tokens[5].n == 'yo'
			tokens[5].tag == '/'
			tokens[6] == 'd'
	}

	def 'scan set delimiter'() {
		setup:
			def text = 'a{{=<% %>=}}b'
		when:
			def tokens = scanner.scan(text)
		then: "change delimiter doesn't appear as token"
			tokens.size() == 2
			tokens[0] == 'a'
			tokens[1] == 'b'
	}

	def 'scan reset delimiter'() {
		setup:
			def text = 'a{{=<% %>=}}b<%hmm%>c<%={{ }}=%>d{{hmm}}'
		when:
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 6
			tokens[0] == 'a'
			tokens[1] == 'b'
			tokens[2].n == 'hmm'
			tokens[2].tag == '_v'
			tokens[3] == 'c'
			tokens[4] == 'd'
			tokens[5].n == 'hmm'
			tokens[5].tag == '_v'
	}

}