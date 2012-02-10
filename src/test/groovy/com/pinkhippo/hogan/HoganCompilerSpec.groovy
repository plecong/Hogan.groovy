package com.pinkhippo.hogan

import spock.lang.*

class HoganCompilerSpec extends Specification {

	HoganScanner scanner
	HoganParser parser
	GroovyHoganCompiler compiler

	def setup() {
		scanner = new HoganScanner()
		parser = new HoganParser()
		compiler = new GroovyHoganCompiler()
	}

	def 'basic output'() {
		setup:
			def text = 'test'
		when:
			def t = Hogan.compile(text)
			def s = t.render()

		then:
			'test' == s
	}

	def 'one variable'() {
		setup:
			def text = 'test {{foo}} test'
		when:
			def t = Hogan.compile(text)
			def s = t.render([foo: 'bar'])

		then:
			'test bar test' == s
	}

	def 'render with whitespace'() {
		setup:
			def text = '{{ string }}'
		when:
			def t = Hogan.compile(text)
			def s = t.render([string: '---'])

		then:
			'---' == s
	}

	def 'render with whitespace around triple stache'() {
		setup:
			def text = '  {{{ string }}}\n'
		when:
			def t = Hogan.compile(text)
			def s = t.render([string: '---'])

		then:
			'  ---\n' == s
	}

	def 'render with whitespace around ampersand'() {
		setup:
			def text = '  {{& string }}\n'
		when:
			def t = Hogan.compile(text)
			def s = t.render([string: '---'])
		then:
			'  ---\n' == s
	}

	def 'multiple variables'() {
		setup:
			def text = 'test {{foo}} test {{bar}} test {{baz}} test {{foo}} test'
		when:
			def t = Hogan.compile(text)
			def s = t.render([foo:'42', bar: '43', baz: '44'])
		then:
			'test 42 test 43 test 44 test 42 test' == s
	}

	def 'number values'() {
		setup:
			def text = 'integer: {{foo}} float: {{bar}} negative: {{baz}}'
		when:
			def t = Hogan.compile(text)
			def s = t.render([foo: 42, bar: 42.42, baz: -42])
		then:
			'integer: 42 float: 42.42 negative: -42' == s
	}

	def 'object render'() {
		setup:
			def text = 'object: {{foo}}'
		when:
			def t = Hogan.compile(text)
			def o = new Object()
			def s = t.render([foo: o])
		then:
			'object: ' + o.toString() == s
	}

	def 'object toString render'() {
		setup:
			def text = 'object: {{foo}}'
		when:
			def t = Hogan.compile(text)
			def s = t.render([foo: new Object() {
				public String toString() { 'yo!' }
			}])
		then:
			'object: yo!' == s
	}

	def 'object list render'() {
		setup:
			def text = 'list: {{foo}}'
		when:
			def t = Hogan.compile(text)
			def s = t.render([foo: ['a', 'b', 'c']])
		then:
			'list: [a, b, c]' == s
	}

	def 'escaping'() {
		setup:
			def text = '{{foo}}'
		when:
			def t = Hogan.compile(text)
			def s = t.render([foo: "< > <div> \' \" &"])
		then:
			'&lt; &gt; &lt;div&gt; &#39; &quot; &amp;' == s

		// TODO: invidividual chars
	}

	def 'mustach injection'() {
		setup:
			def text = '{{foo}}'
		when:
			def t = Hogan.compile(text)
			def s = t.render([foo: "{{{<42}}}"])
		then:
			'{{{&lt;42}}}' == s
	}

	def 'triple stache'() {
		setup:
			def text = '{{{foo}}}'
		when:
			def t = Hogan.compile(text)
			def s = t.render([foo: "< > <div> \' \" &"])
		then:
			'< > <div> \' \" &' == s
	}

	def 'amp no escaping'() {
		setup:
			def text = '{{&foo}}'
		when:
			def t = Hogan.compile(text)
			def s = t.render([foo: "< > <div> \' \" &"])
		then:
			'< > <div> \' \" &' == s
	}

	def 'partial'() {
		setup:
			def partialText = 'this is text from the partial--the magic number {{foo}} is from a variable'
			def p = Hogan.compile(partialText)
			def text = 'This template contains a partial ({{>testPartial}}).'
			def t = Hogan.compile(text)
		when:
			def s = t.render([foo: 42], [testPartial: p])
		then:
			'This template contains a partial (this is text from the partial--the magic number 42 is from a variable).' == s
	}

	def 'nested partials'() {
		setup:
			def partialText = 'this is text from the partial--the magic number {{foo}} is from a variable'
			def p = Hogan.compile(partialText)

			def partialText2 = 'This template contains a partial ({{>testPartial}}).'
			def p2 = Hogan.compile(partialText2)

			def text = 'This template contains a partial that contains a partial [{{>testPartial2}}].'
			def t = Hogan.compile(text)
		when:
			def s = t.render([foo: 42], [testPartial: p, testPartial2: p2])
		then:
			'This template contains a partial that contains a partial [This template contains a partial (this is text from the partial--the magic number 42 is from a variable).].' == s
	}

	def 'negative section'() {
		setup:
			def text = 'This template {{^foo}}BOO {{/foo}}contains an inverted section.'
		when:
			def t = Hogan.compile(text)
		then:
			'This template BOO contains an inverted section.' == t.render()
			'This template BOO contains an inverted section.' == t.render([foo: []])
			'This template BOO contains an inverted section.' == t.render([foo: false])
			'This template contains an inverted section.' == t.render([foo: ''])
			'This template contains an inverted section.' == t.render([foo: true])
			'This template BOO contains an inverted section.' == t.render([foo: { -> false }])
	}

	def 'section elision'() {
		setup:
			def text = 'This template {{#foo}}BOO {{/foo}}contains a section.'
		when:
			def t = Hogan.compile(text)
		then:
			'This template contains a section.' == t.render()
			'This template contains a section.' == t.render([foo: []])
			'This template contains a section.' == t.render([foo: false])
	}

	def 'section map context'() {
		setup:
			def text = 'This template {{#foo}}{{bar}} {{/foo}}contains a section.'
			def t = Hogan.compile(text)
		when:
			def s = t.render([foo:[bar: 42]])
		then:
			s == "This template 42 contains a section."
	}

	def 'section array context'() {
		setup:
			def text = 'This template {{#foo}}{{bar}} {{/foo}}contains a section.'
			def t = Hogan.compile(text)
		when:
			def s = t.render([foo: [[bar: 42], [bar: 43], [bar: 44]]])
		then:
			s == "This template 42 43 44 contains a section."
	}

	def 'falsy variable no render'() {
		setup:
			def text = 'I ({{cannot}}) be seen!'
			def t = Hogan.compile(text)
		when:
			def s = t.render()
		then:
			s == "I () be seen!"
	}

	def 'null return value from lambda'() {
		setup:
			def text = 'abc{{foo}}def'
			def t = Hogan.compile(text)
		when:
			def s = t.render([foo: { null }])
		then:
			s == "abcdef"
	}

	def 'section extensions'() {

	}

	def 'misnested section extensions'() {

	}

	def 'section extens in higher order sections'() {

	}

	def 'section extensions in lambda replace variable'() {

	}

	def 'nested section'() {
		setup:
			def text = '{{#foo}}{{#bar}}{{baz}}{{/bar}}{{/foo}}'
			def t = Hogan.compile(text)
		when:
			def s = t.render([foo: 42, bar: 43, baz: 44])
		then:
			s == "44"
	}

	def 'dotted names'() {
		setup:
			def text = '"{{person.name}}" == "{{#person}}{{name}}{{/person}}"'
			def t = Hogan.compile(text)
		when:
			def s = t.render([person: [name: 'Joe']])
		then:
			'"Joe" == "Joe"' == s
	}

	def 'implicit iterator'() {
		setup:
			def text = '{{#stuff}} {{.}} {{/stuff}}'
			def t = Hogan.compile(text)
		when:
			def s = t.render([stuff:[42,43,44]])
		then:
			' 42  43  44 ' == s
	}

	def 'partials and delimiters'() {
		setup:
			def text = '{{>include}}*\n{{= | | =}}\n*|>include|'
			def partialText = ' .{{value}}. '
			def partial = Hogan.compile(partialText)
			def t = Hogan.compile(text)
		when:
			def s = t.render([value: 'yes'], [include: partial])
		then:
			' .yes. *\n* .yes. ' == s
	}

	def 'string partials'() {
		setup:
			def text = 'foo{{>mypartial}}baz'
			def partialText = ' bar '
			def t = Hogan.compile(text)
		when:
			def s = t.render([:], [mypartial: partialText])
		then:
			'foo bar baz' == s
	}

	def 'missing partials'() {
		setup:
			def text = 'foo{{>mypartial}} bar'
			def t = Hogan.compile(text)
		when:
			def s = t.render([:])
		then:
			'foo bar' == s
	}

	def 'indented standalone comment'() {
		setup:
			def text = 'Begin.\n {{! Indented Comment Block! }}\nEnd.'
			def t = Hogan.compile(text)
		when:
			def s = t.render()
		then:
			'Begin.\nEnd.' == s
	}

	@Shared
	def tests = [
		[
			name: 'testNewLineBetweenDelimiterChanges',
			desc: 'render correct',
			text: '\n{{#section}}\n {{data}}\n |data|\n{{/section}}x\n\n{{= | | =}}\n|#section|\n {{data}}\n |data|\n|/section|',
			data: [ section: true, data : 'I got interpolated.' ],
			expected: '\n I got interpolated.\n |data|\nx\n\n {{data}}\n I got interpolated.\n'
		],[
			name: 'testMustacheJSApostrophe',
			desc: 'Apostrophe is escaped.',
			text: '{{apos}}{{control}}',
			data: [apos: "'", control: 'X'],
			expected: '&#39;X'
		],[
			name: 'testMustacheJSArrayOfImplicitPartials',
			desc: 'Partials with implicit iterators work.',
			text: 'Here is some stuff!\n{{#numbers}}\n{{>partial}}\n{{/numbers}}\n',
			data: [numbers: [1,2,3,4]],
			partials: [partial: '{{.}}\n'],
			expected: 'Here is some stuff!\n1\n2\n3\n4\n'
		],[
			name: 'testMustacheJSArrayOfPartials',
			desc: 'Partials with arrays work.',
			text: 'Here is some stuff!\n{{#numbers}}\n{{>partial}}\n{{/numbers}}\n',
			data: [numbers: [[i:1], [i:2], [i:3], [i:4]]],
			partials: [partial: '{{i}}\n'],
			expected: 'Here is some stuff!\n1\n2\n3\n4\n'
		],[
			name: 'testMustacheJSArrayOfStrings',
			desc: 'array of strings works with implicit iterators.',
			text: '{{#strings}}{{.}} {{/strings}}',
			data: [strings: ['foo', 'bar', 'baz']],
			expected: 'foo bar baz '
		],[
			name: 'testMustacheJSNullString',
			desc: 'undefined value does not render.',
			text: 'foo{{bar}}baz',
			data: [bar: null],
			expected: 'foobaz'
		],[
			name: 'testMustacheJSNullTripleStache',
			desc: 'undefined value does not render in triple stache.',
			text: 'foo{{{bar}}}baz',
			data: [bar: null],
			expected: 'foobaz'
		],[
			name: 'testMustacheJSTripleStacheAltDelimiter',
			desc: 'triple stache inside alternate delimiter works.',
			text: '{{=<% %>=}}<% foo %> {{foo}} <%{bar}%> {{{bar}}}',
			data: [foo: 'yeah', bar: 'hmm'],
			expected: 'yeah {{foo}} hmm {{{bar}}}'
		]
	]

	@Unroll({"${test.name}: ${test.desc}"})
	def 'shared tests'() {
		when:
			def t = Hogan.compile(test.text)
			def s = t.render(test.data, test.partials)
		then:
			s == test.expected
		where:
			test << tests
	}

	def 'shoot out complex'() {
		setup:
			def text = "<h1>{{header}}</h1>" +
			    "{{#hasItems}}" +
			    "<ul>" +
			      "{{#items}}" +
			        "{{#current}}" +
			          "<li><strong>{{name}}</strong></li>" +
			        "{{/current}}" +
			        "{{^current}}" +
			          "<li><a href=\"{{url}}\">{{name}}</a></li>" +
			        "{{/current}}" +
			      "{{/items}}" +
			    "</ul>" +
			    "{{/hasItems}}" +
			    "{{^hasItems}}" +
			      "<p>The list is empty.</p>" +
			    "{{/hasItems}}"
			def expected = "<h1>Colors</h1><ul><li><strong>red</strong></li><li><a href=\"#Green\">green</a></li><li><a href=\"#Blue\">blue</a></li></ul>";
			def t = Hogan.compile(text)
		when:
			def s = t.render([
				header: {
					return 'Colors'
				},
				items: [
					[name: 'red', current: true, url: '#Red'],
					[name: 'green', current: false, url: '#Green'],
					[name: 'blue', current: false, url: '#Blue'],
				],
				hasItems: {
					items.length != 0
				},
				empty: {
					items.lenght == 0
				}
			])
		then:
			expected == s
	}
}