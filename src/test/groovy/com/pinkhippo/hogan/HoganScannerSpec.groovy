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

import spock.lang.Specification

class HoganScannerSpec extends Specification {

	HoganScanner scanner

	def setup() {
		scanner = new HoganScanner()
	}

	def 'Scan Text No Tags'() {
		when:
			def text = '<h2>hi</h2>'
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 1
			tokens[0].text == text
	}

	def 'Scan One Tag'() {
		when:
			def text = '{{hmm}}'
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 1
			tokens[0].n == 'hmm'
	}

	def 'Scan Multiple Tags'() {
		when:
			def text = 'asdf{{hmm}}asdf2{{hmm2}}asdf3'
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 5
			tokens[0].text == 'asdf'
			tokens[1].n == 'hmm'
			tokens[1].tag == '_v'
			tokens[2].text == 'asdf2'
			tokens[3].n == 'hmm2'
			tokens[3].tag == '_v'
			tokens[4].text == 'asdf3'
	}

	def 'Scan Section Open'() {
		when:
			def text = '{{#hmm}}'
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 1
			tokens[0].n == 'hmm'
			tokens[0].tag == '#'
	}

	def 'Scan Section Close'() {
		when:
			def text = '{{/hmm}}'
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 1
			tokens[0].n == 'hmm'
			tokens[0].tag == '/'
	}

	def 'Scan Section'() {
		when:
			def text = '{{#hmm}}{{/hmm}}'
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 2
			tokens[0].n == 'hmm'
			tokens[0].tag == '#'
			tokens[1].n == 'hmm'
			tokens[1].tag == '/'
	}

	def 'Scan Section In Content'() {
		when:
			def text = 'abc{{#hmm}}def{{/hmm}}ghi'
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 5
			tokens[0].text == 'abc'
			tokens[1].n == 'hmm'
			tokens[1].tag == '#'
			tokens[2].text == 'def'
			tokens[3].n == 'hmm'
			tokens[3].tag == '/'
			tokens[4].text == 'ghi'
	}

	def 'Scan Negative Section'() {
		when:
			def text = '{{^hmm}}{{/hmm}}'
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 2
			tokens[0].n == 'hmm'
			tokens[0].tag == '^'
			tokens[1].n == 'hmm'
			tokens[1].tag == '/'
	}

	def 'Scan Partial'() {
		when:
			def text = '{{>hmm}}'
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 1
			tokens[0].n == 'hmm'
			tokens[0].tag == '>'
	}

	def 'Scan Backward Partial'() {
		when:
			def text = '{{<hmm}}'
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 1
			tokens[0].n == 'hmm'
			tokens[0].tag == '<'
	}

	def 'Scan Ampersand No Escape Tag'() {
		when:
			def text = '{{&hmm}}'
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 1
			tokens[0].n == 'hmm'
			tokens[0].tag == '&'
	}

	def 'Scan Triple Stache'() {
		when:
			def text = '{{{hmm}}'
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 1
			tokens[0].n == 'hmm'
			tokens[0].tag == '{'
	}

	def 'Scan Section With Triple Stache Inside'() {
		when:
			def text = 'a{{#yo}}b{{{hmm}}}c{{/yo}}d'
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 7
			tokens[0].text == 'a'
			tokens[1].n == 'yo'
			tokens[1].tag == '#'
			tokens[2].text == 'b'
			tokens[3].n == 'hmm'
			tokens[3].tag == '{'
			tokens[4].text == 'c'
			tokens[5].n == 'yo'
			tokens[5].tag == '/'
			tokens[6].text == 'd'
	}

	def 'Scan Set Delimiter'() {
		when:
			def text = 'a{{=<% %>=}}b'
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 2
			tokens[0].text == 'a'
			tokens[1].text == 'b'
	}

	def 'Scan Reset Delimiter'() {
		when:
			def text = 'a{{=<% %>=}}b<%hmm%>c<%={{ }}=%>d{{hmm}}'
			def tokens = scanner.scan(text)
		then:
			tokens.size() == 6
			tokens[0].text == 'a'
			tokens[1].text == 'b'
			tokens[2].tag == '_v'
			tokens[2].n == 'hmm'
			tokens[3].text == 'c'
			tokens[4].text == 'd'
			tokens[5].tag == '_v'
			tokens[5].n == 'hmm'
	}

}