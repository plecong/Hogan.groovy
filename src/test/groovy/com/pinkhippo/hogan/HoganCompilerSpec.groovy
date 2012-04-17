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

	def 'Single Char Delimiter'() {
		when:
			def text = '({{foo}} {{=[ ]=}}[text])'
			def t = compiler.compile(text)
			def s = t.render([foo: 'bar', text: 'It worked!'])
		then:
			'(bar It worked!)' == s
	}

	def 'Set Delimiter With Whitespace'() {
		when:
			def text = '{{= | | =}}|foo|'
			def t = compiler.compile(text)
			def s = t.render([foo: 'bar'])
		then:
			'bar' == s
	}

	def 'Render With Whitespace Around Triple Stache'() {
		when:
			def text = '  {{{ string }}}\n'
			def t = compiler.compile(text)
			def s = t.render([string: '---'])

		then:
			'  ---\n' == s
	}

	def 'Render With Whitespace Around Ampersand'() {
		when:
			def text = '  {{& string }}\n'
			def t = compiler.compile(text)
			def s = t.render([string: '---'])
		then:
			'  ---\n' == s
	}

	def 'Multiple Variables'() {
		when:
			def text = 'test {{foo}} test {{bar}} test {{baz}} test {{foo}} test'
			def t = compiler.compile(text)
			def s = t.render([foo:'42', bar: '43', baz: '44'])
		then:
			'test 42 test 43 test 44 test 42 test' == s
	}

	def 'Number Values'() {
		when:
			def text = 'integer: {{foo}} float: {{bar}} negative: {{baz}}'
			def t = compiler.compile(text)
			def s = t.render([foo: 42, bar: 42.42, baz: -42])
		then:
			'integer: 42 float: 42.42 negative: -42' == s
	}

	def 'Object Render'() {
		when:
			def text = 'object: {{foo}}'
			def t = compiler.compile(text)
			def o = new Object()
			def s = t.render([foo: o])
		then:
			'object: ' + o.toString() == s
	}

	def 'Object To String Render'() {
		when:
			def text = 'object: {{foo}}'
			def t = compiler.compile(text)
			def s = t.render([foo: new Object() {
				public String toString() { 'yo!' }
			}])
		then:
			'object: yo!' == s
	}

	def 'Array Render'() {
		setup:
			def text = 'array: {{foo}}'
		when:
			def t = compiler.compile(text)
			def s = t.render([foo: ['a', 'b', 'c']])
		then:
			'array: [a, b, c]' == s
	}

	def 'Escaping'() {
		when:
			def text = '{{foo}}'
			def t = compiler.compile(text)
			def s = t.render([foo: "< > <div> \' \" &"])
		then:
			'&lt; &gt; &lt;div&gt; &#39; &quot; &amp;' == s
	}

	def 'Escaping Chars'() {
		when:
			def text = '{{foo}}'
			def t = compiler.compile(text)
			def s = t.render([foo: theChar.key + ' just me'])
		then:
			theChar.value + ' just me' == s
		where:
			theChar << [
				"'": "&#39;",
				'"': "&quot;",
				"<": "&lt;",
				">": "&gt;",
				"&": "&amp;"
			]
	}

	def 'Mustach Injection'() {
		when:
			def text = '{{foo}}'
			def t = compiler.compile(text)
			def s = t.render([foo: "{{{<42}}}"])
		then:
			'{{{&lt;42}}}' == s
	}

	def 'Triple Stache'() {
		when:
			def text = '{{{foo}}}'
			def t = compiler.compile(text)
			def s = t.render([foo: "< > <div> \' \" &"])
		then:
			'< > <div> \' \" &' == s
	}

	def 'Amp No Escaping'() {
		when:
			def text = '{{&foo}}'
			def t = compiler.compile(text)
			def s = t.render([foo: "< > <div> \' \" &"])
		then:
			'< > <div> \' \" &' == s
	}

	def 'Partial Basic'() {
		setup:
			def partialText = 'this is text from the partial--the magic number {{foo}} is from a variable'
			def p = compiler.compile(partialText)

			def text = 'This template contains a partial ({{>testPartial}}).'
			def t = compiler.compile(text)
		when:
			def s = t.render([foo: 42], [testPartial: p])
		then:
			'This template contains a partial (this is text from the partial--the magic number 42 is from a variable).' == s
	}

	def 'Nested Partials'() {
		setup:
			def partialText = 'this is text from the partial--the magic number {{foo}} is from a variable'
			def p = compiler.compile(partialText)

			def partialText2 = 'This template contains a partial ({{>testPartial}}).'
			def p2 = compiler.compile(partialText2)

			def text = 'This template contains a partial that contains a partial [{{>testPartial2}}].'
			def t = compiler.compile(text)
		when:
			def s = t.render([foo: 42], [testPartial: p, testPartial2: p2])
		then:
			'This template contains a partial that contains a partial [This template contains a partial (this is text from the partial--the magic number 42 is from a variable).].' == s
	}

	def 'Negative Section'() {
		when:
			def text = 'This template {{^foo}}BOO {{/foo}}contains an inverted section.'
			def t = compiler.compile(text)
		then:
			'This template BOO contains an inverted section.' == t.render()
			'This template BOO contains an inverted section.' == t.render([foo: []])
			'This template BOO contains an inverted section.' == t.render([foo: false])
			'This template contains an inverted section.' == t.render([foo: ''])
			'This template contains an inverted section.' == t.render([foo: true])
			'This template BOO contains an inverted section.' == t.render([foo: { false }])
			'This template BOO contains an inverted section.' == t.render([foo: 0])
	}

	def 'Section Elision'() {
		when:
			def text = 'This template {{#foo}}BOO {{/foo}}contains a section.'
			def t = compiler.compile(text)
		then:
			'This template contains a section.' == t.render()
			'This template contains a section.' == t.render([foo: []])
			'This template contains a section.' == t.render([foo: false])
			'This template BOO contains a section.' == t.render([foo: ''])
			'This template BOO contains a section.' == t.render([foo: true])
			'This template contains a section.' == t.render([foo: { false }])
			'This template contains a section.' == t.render([foo: 0])
	}

	def 'Section Map Context'() {
		when:
			def text = 'This template {{#foo}}{{bar}} {{/foo}}contains a section.'
			def t = compiler.compile(text)
			def s = t.render([foo:[bar: 42]])
		then:
			s == "This template 42 contains a section."
	}

	def 'Section Array Context'() {
		when:
			def text = 'This template {{#foo}}{{bar}} {{/foo}}contains a section.'
			def t = compiler.compile(text)
			def s = t.render([foo: [[bar: 42], [bar: 43], [bar: 44]]])
		then:
			s == "This template 42 43 44 contains a section."
	}

	def 'Falsy Variable No Render'() {
		when:
			def text = 'I ({{cannot}}) be seen!'
			def t = compiler.compile(text)
			def s = t.render()
		then:
			s == "I () be seen!"
	}

	def 'Null Return Value From Lambda'() {
		when:
			def text = 'abc{{foo}}def'
			def t = compiler.compile(text)
			def s = t.render([foo: { null }])
		then:
			s == "abcdef"
	}

	def 'Section Extensions'() {
		when:
		def text = "Test {{_//|__foo}}bar{{/foo}}"
		def options = [sectionTags: [ [o: '_//|__foo', c: 'foo'] ]]
		def tree = parser.parse(scanner.scan(text), text, options)
		def t = compiler.compile(text, options)
		def s = t.render(['_//|__foo': true])

		then:
		tree[1].tag == "#"
		tree[1].n == "_//|__foo"
		s == "Test bar"
	}

	def 'Misnested Section Extensions'() {
		when:
		def text = "Test {{__foo}}bar{{/bar}}"
		def options = [sectionTags:[[o:'__foo', c:'foo'], [o:'__bar', c:'bar']]]
		parser.parse(scanner.scan(text), text, options)

		then:
		def e = thrown(ParseException)
		e.message == 'Nesting error: __foo vs. bar'

	}

	def 'Section Extensions In Higher Order Sections'() {
		setup:
			def text = "Test{{_foo}}bar{{/foo}}"
			def options = [sectionTags:[[o:'_foo', c:'foo'], [o:'_baz', c:'baz']]]
			def t = compiler.compile(text, options)
			def context = [
				'_baz': true,
				'_foo': { return { s -> "{{_baz}}" + s + "{{/baz}}qux" } }
			]
		when:
			def s = t.render(context);
		then:
			s == "Testbarqux"
	}

	def 'Section Extensions In Lambda Replace Variable'() {
		setup:
			def text = "Test{{foo}}"
			def options = [sectionTags:[[o:'_baz', c:'baz']]]
			def t = compiler.compile(text, options)
			def context = [
				'_baz': true,
				'foo': { return { "{{_baz}}abcdef{{/baz}}" } }
			]
		when:
			def s = t.render(context)
		then:
			s == "Testabcdef"
	}

	def 'disableLambda option works on interpolation'() {
		setup:
			def text = "Test{{foo}}"
			def options = [disableLambda: true];
			def t = compiler.compile(text, options)
			def context = [
				'baz': true,
				'foo': { return { "{{#baz}}abcdef{{/baz}}" } }
			]
		when:
			def s = t.render(context)
		then:
			def e = thrown(RuntimeException)
			e.message == "Lambda features disabled."
	}

	def 'disableLambda option works on sections'() {
		setup:
			def text = "Test{{#foo}}{{/foo}}"
			def options = [disableLambda: true];
			def t = compiler.compile(text, options)
			def context = [
				'baz': true,
				'foo': { return { "{{#baz}}abcdef{{/baz}}" } }
			]
		when:
			def s = t.render(context)
		then:
			def e = thrown(RuntimeException)
			e.message == "Lambda features disabled."
	}

	def 'Mustache not reprocessed for method calls in interpolations'() {
		setup:
			def text = "text with {{foo}} inside"
			def t = compiler.compile(text)
			def text2 = "text with {{{foo}}} inside"
			def t2 = compiler.compile(text2)

			def context = [
				'foo': { "no processing of {{tags}}" }
			]

		when:
			def s = t.render(context)
			def s2 = t2.render(context)

		then:
			s == 'text with no processing of {{tags}} inside'
			s2 == 'text with no processing of {{tags}} inside'
	}

	def 'Mustache is reprocessed for lambdas in interpolations'() {
		setup:
			def text = "text with {{foo}} inside"
			def t = compiler.compile(text)
			def context = [
				'bar': 42,
				'foo': { return { "processing of {{bar}}" } }
			]
		when:
			def s = t.render(context)
		then:
			s == 'text with processing of 42 inside'
	}

	def 'Nested Section'() {
		when:
			def text = '{{#foo}}{{#bar}}{{baz}}{{/bar}}{{/foo}}'
			def t = compiler.compile(text)
			def s = t.render([foo: 42, bar: 42, baz: 42])
		then:
			s == "42"
	}

	def 'Dotted Names'() {
		when:
			def text = '"{{person.name}}" == "{{#person}}{{name}}{{/person}}"'
			def t = compiler.compile(text)
			def s = t.render([person: [name: 'Joe']])
		then:
			'"Joe" == "Joe"' == s
	}

	def 'Implicit Iterator'() {
		when:
			def text = '{{#stuff}} {{.}} {{/stuff}}'
			def t = compiler.compile(text)
			def s = t.render([stuff:[42,43,44]])
		then:
			' 42  43  44 ' == s
	}

	def 'Partials And Delimiters'() {
		when:
			def text = '{{>include}}*\n{{= | | =}}\n*|>include|'
			def partialText = ' .{{value}}. '
			def partial = compiler.compile(partialText)
			def t = compiler.compile(text)
			def s = t.render([value: 'yes'], [include: partial])
		then:
			' .yes. *\n* .yes. ' == s
	}

	def 'String Partials'() {
		when:
			def text = 'foo{{>mypartial}}baz'
			def partialText = ' bar '
			def t = compiler.compile(text)
			def s = t.render([:], [mypartial: partialText])
		then:
			'foo bar baz' == s
	}

	def 'Missing Partials'() {
		when:
			def text = 'foo{{>mypartial}} bar'
			def t = compiler.compile(text)
			def s = t.render([:])
		then:
			'foo bar' == s
	}

	def 'Indented Standalone Comment'() {
		when:
			def text = 'Begin.\n {{! Indented Comment Block! }}\nEnd.'
			def t = compiler.compile(text)
			def s = t.render()
		then:
			'Begin.\nEnd.' == s
	}

	def "Doesn't parse templates that have non-\$ tags inside super template tags"() {
		when:
			compiler.compile('{{<foo}}{{busted}}{{/foo}}')
		then:
			def m = thrown(ParseException)
			m.message == 'Illegal content in < super tag.'
	}

	@Shared
	def tests = basicTests + templateInheritanceTests + shootOutTests

	static basicTests = [
		[
			name: 'Basic Output',
			desc: 'template renders one text node',
			text: 'test',
			data: [:],
			expected: 'test'
		],[
			name: 'One Variable',
			desc: 'basic variable substitution works.',
			text: 'test {{foo}} test',
			data: [foo: 'bar'],
			expected: 'test bar test'
		],[
			name: 'Render With Whitespace',
			desc: 'tags with whitespace render correctly.',
			text: '{{ string }}',
			data: [string: '---'],
			expected: '---'
		],[
			name: 'New Line Between Delimiter Changes',
			desc: 'render correct',
			text: '\n{{#section}}\n {{data}}\n |data|\n{{/section}}x\n\n{{= | | =}}\n|#section|\n {{data}}\n |data|\n|/section|',
			data: [section: true, data: 'I got interpolated.'],
			expected: '\n I got interpolated.\n |data|\nx\n\n {{data}}\n I got interpolated.\n',
			excludePerf: true
		],[
			name: 'New Line Between Delimiter Changes JMustache Compatible',
			desc: 'render correct',
			text: '\n{{#section}}\n {{data}}\n |data|\n{{/section}}x\n\n{{=| |=}}\n|#section|\n {{data}}\n |data|\n|/section|',
			data: [section: true, data: 'I got interpolated.'],
			expected: '\n I got interpolated.\n |data|\nx\n\n {{data}}\n I got interpolated.\n'
		],[
			name: 'Mustache JS Apostrophe',
			desc: 'Apostrophe is escaped.',
			text: '{{apos}}{{control}}',
			data: ['apos':"'", 'control':"X"],
			expected: '&#39;X'
		],[
			name: 'Mustache JS Array Of Implicit Partials',
			desc: 'Partials with implicit iterators work.',
			text: 'Here is some stuff!\n{{#numbers}}\n{{>partial}}\n{{/numbers}}\n',
			data: [numbers:[1,2,3,4]],
			partials: [partial: '{{.}}\n'],
			expected: 'Here is some stuff!\n1\n2\n3\n4\n'
		],[
			name: 'Mustache JS Array Of Partials',
			desc: 'Partials with arrays work.',
			text: 'Here is some stuff!\n{{#numbers}}\n{{>partial}}\n{{/numbers}}\n',
			data: [numbers:[[i:1],[i:2],[i:3],[i:4]]],
			partials: [partial: '{{i}}\n'],
			expected: 'Here is some stuff!\n1\n2\n3\n4\n'
		],[
			name: 'Mustache JS Array Of Strings',
			desc: 'array of strings works with implicit iterators.',
			text: '{{#strings}}{{.}} {{/strings}}',
			data: [strings: ['foo', 'bar', 'baz']],
			expected: 'foo bar baz '
		],[
			name: 'Mustache JS Undefined String',
			desc: 'undefined value does not render.',
			text: 'foo{{bar}}baz',
			data: [:],
			expected: 'foobaz'
		],[
			name: 'Mustache JS Undefined Triple Stache',
			desc: 'undefined value does not render in triple stache.',
			text: 'foo{{{bar}}}baz',
			data: [:],
			expected: 'foobaz'
		],[
			name: 'Mustache JS Null String',
			desc: 'undefined value does not render.',
			text: 'foo{{{bar}}}baz',
			data: [bar: null],
			expected: 'foobaz'
		],[
			name: 'Mustache JS Undefined Triple Stache',
			desc: 'undefined value does not render in triple stache.',
			text: 'foo{{{bar}}}baz',
			data: [bar: null],
			expected: 'foobaz'
		],[
			name: 'Mustache JS Triple Stache Alt Delimiter',
			desc: 'triple stache inside alternate delimiter works.',
			text: '{{=<% %>=}}<% foo %> {{foo}} <%{bar}%> {{{bar}}}',
			data: [foo: 'yeah', bar: 'hmm'],
			expected: 'yeah {{foo}} hmm {{{bar}}}'
		]
	]

	static templateInheritanceTests = [
		[
			name: 'Default content',
			text: '{{$title}}Default title{{/title}}',
			data: null,
			expected: 'Default title'
		],[
			name: 'Default content renders variables',
			desc: 'default content renders variables',
			text: '{{$foo}}default {{bar}} content{{/foo}}',
			data: [bar: 'baz'],
			expected: 'default baz content'
		],[
			name: 'Default content renders triple stache variables',
			desc: 'default content renders triple stache variables',
			text: '{{$foo}}default {{{bar}}} content{{/foo}}',
			data: [bar: '<baz>'],
			expected: 'default <baz> content'
		],[
			name: 'Default content renders sections',
			desc: 'sections work',
			text: '{{$foo}}default {{#bar}}{{baz}}{{/bar}} content{{/foo}}',
			data: [bar: [baz: 'qux']],
			expected: 'default qux content'
		],[
			name: 'Default content renders negative sections',
			desc: 'negative sections work',
			text: '{{$foo}}default{{^bar}}{{baz}}{{/bar}} content{{/foo}}',
			data: [foo: [baz: 'qux']],
			expected: 'default content'
		],[
			name: 'Mustache injection in default content',
			desc: 'mustache tags are not injected.',
			text: '{{$foo}}default {{#bar}}{{baz}}{{/bar}} content{{/foo}}',
			data: [bar: [baz: '{{qux}}']],
			expected: 'default {{qux}} content'
		],[
			name: 'Default content rendered inside included templates',
			desc: 'default content from included template',
			text: '{{<include}}{{/include}}',
			data: [:],
			partials: ['include': Hogan.compile('{{$foo}}default content{{/foo}}')],
			expected: 'default content'
		],[
			name: 'Overridden content',
			desc: 'renders overridden content',
			text: '{{<super}}{{$title}}sub template title{{/title}}{{/super}}',
			data: [:],
			partials: ['super': '...{{$title}}Default title{{/title}}...'],
			expected: '...sub template title...'
		],[
			name: 'Overridden partial',
			desc: '',
			text: 'test {{<partial}}{{$stuff}}override{{/stuff}}{{/partial}}',
			data: [:],
			partials: ['partial': '{{$stuff}}...{{/stuff}}'],
			expected: 'test override'
		],[
			name: 'Two overridden partials with different content',
			desc: '',
			text: 'test {{<partial}}{{$stuff}}override1{{/stuff}}{{/partial}} {{<partial}}{{$stuff}}override2{{/stuff}}{{/partial}}',
			data: [:],
			partials: ['partial': '|{{$stuff}}...{{/stuff}}{{$default}} default{{/default}}|'],
			expected: 'test |override1 default| |override2 default|'
		],[
			name: 'Override one substitution but not the other',
			desc: 'overrides only one substitution',
			text: '{{<partial}}{{$stuff2}}override two{{/stuff2}}{{/partial}}',
			data: [:],
			partials: ['partial': Hogan.compile('{{$stuff}}default one{{/stuff}}, {{$stuff2}}default two{{/stuff2}}')],
			expected: 'default one, override two'
		],[
			name: 'Override one substitution but not the other',
			desc: 'picks up changes to the partial dictionary',
			text: '{{<partial}}{{$stuff2}}override two{{/stuff2}}{{/partial}}',
			data: [:],
			partials: ['partial': Hogan.compile('{{$stuff}}new default one{{/stuff}}, {{$stuff2}}new default two{{/stuff2}}')],
			expected: 'new default one, override two'
		],[
			name: 'Super templates behave identically to partials when called with no parameters',
			desc: 'should be the partial rendered twice',
			text: '{{>include}}|{{<include}}{{/include}}',
			data: [:],
			partials: ['include': Hogan.compile('{{$foo}}default content{{/foo}}')],
			expected: 'default content|default content'
		],[
			name: 'Recursion in inherited templates',
			desc: 'matches expected recursive output',
			text: '{{<include}}{{$foo}}override{{/foo}}{{/include}}',
			data: [:],
			partials: [
				'include': Hogan.compile('{{$foo}}default content{{/foo}} {{$bar}}{{<include2}}{{/include2}}{{/bar}}'),
				'include2': Hogan.compile('{{$foo}}include2 default content{{/foo}} {{<include}}{{$bar}}don\'t recurse{{/bar}}{{/include}}')
			],
			expected: 'override include2 default content default content don\'t recurse'
		],[
			name: 'Allows text inside a super tag, but ignores it',
			desc: 'should render without the text',
			text: '{{<include}} asdfasd asdfasdfasdf {{/include}}',
			data: [:],
			partials: ['include': Hogan.compile('{{$foo}}default content{{/foo}}') ],
			expected: 'default content'
		],[
			name: 'Ignores text inside super templates, but does parse $ tags',
			desc: 'should render without the text',
			text: '{{<include}} asdfasd {{$foo}}hmm{{/foo}} asdfasdfasdf {{/include}}',
			data: [:],
			partials: ['include': Hogan.compile('{{$foo}}default content{{/foo}}') ],
			expected: 'hmm'
		],[
			name: 'Updates object state',
			desc: '',
			text: '{{foo}} {{bar}} {{foo}}',
			data: [foo: 1, bar: { delegate.foo++; return 42} ],
			expected: '1 42 2'
		]
	]

	static shootOutTests = [
		[
			name: 'Shoot Out String',
			desc: 'Shootout String compiled correctly',
			text: 'Hello World!',
			data: [:],
			expected: 'Hello World!'
		],[
			name: 'Shoot Out Replace',
			desc: 'Shootout Replace compiled correctly',
			text: 'Hello {{name}}! You have {{count}} new messages.',
			data: [name: "Mick", count: 30 ],
			expected: 'Hello Mick! You have 30 new messages.'
		],[
			name: 'Shoot Out Array',
			desc: 'Shootout Array compiled correctly',
			text: '{{#names}}{{name}}{{/names}}',
			data: [names: [[name: "Moe"], [name: "Larry"], [name: "Curly"], [name: "Shemp"]] ],
			expected: 'MoeLarryCurlyShemp'
		],[
			name: 'Shoot Out Object',
			desc: 'Shootout Object compiled correctly',
			text: '{{#person}}{{name}}{{age}}{{/person}}',
			data: [person: [ name: "Larry", age: 45 ] ],
			expected: 'Larry45'
		],[
			name: 'Shoot Out Partial',
			desc: 'Shootout Partial compiled correctly',
			text: '{{#peeps}}{{>replace}}{{/peeps}}',
			data: [ peeps: [
				[name: "Moe", count: 15],
				[name: "Larry", count: 5],
				[name: "Curly", count: 2]
			]],
			partials: [replace: Hogan.compile(" Hello {{name}}! You have {{count}} new messages.")],
			expected: ' Hello Moe! You have 15 new messages. Hello Larry! You have 5 new messages. Hello Curly! You have 2 new messages.',
			excludePerf: true
		],[
			name: 'Shoot Out Partial string',
			desc: 'Shootout Partial compiled correctly',
			text: '{{#peeps}}{{>replace}}{{/peeps}}',
			data: [ peeps: [
				[name: "Moe", count: 15],
				[name: "Larry", count: 5],
				[name: "Curly", count: 2]
			]],
			partials: [replace: " Hello {{name}}! You have {{count}} new messages."],
			expected: ' Hello Moe! You have 15 new messages. Hello Larry! You have 5 new messages. Hello Curly! You have 2 new messages.'
		],[
			name: 'Shoot Out Recurse',
			desc: 'Shootout Recurse compiled correctly',
			text: '{{name}}{{#kids}}{{>recursion}}{{/kids}}',
			data: [ name: '1',
				kids: [
				  [
					name: '1.1',
					kids: [
					  [ name: '1.1.1', kids: [] ]
					]
				  ]
			]],
			partials: [recursion: Hogan.compile("{{name}}{{#kids}}{{>recursion}}{{/kids}}")],
			expected: '11.11.1.1',
			excludePerf: true
		],[
			name: 'Shoot Out Recurse string partial',
			desc: 'Shootout Recurse string compiled correctly',
			text: '{{name}}{{#kids}}{{>recursion}}{{/kids}}',
			data: [ name: '1',
				kids: [
				  [
					name: '1.1',
					kids: [
					  [ name: '1.1.1', kids: [] ]
					]
				  ]
			]],
			partials: [recursion: "{{name}}{{#kids}}{{>recursion}}{{/kids}}"],
			expected: '11.11.1.1',
			excludePerf: true
		],[
			name: 'Shoot Out Filter',
			desc: 'Shootout Filter compiled correctly',
			text: '{{#filter}}foo {{bar}}{{/filter}}',
			data: [ filter: { return { it.toUpperCase() + '{{bar}}'} }, bar: 'bar' ],
			partials: [recursion: "{{name}}{{#kids}}{{>recursion}}{{/kids}}"],
			expected: 'FOO bar',
			excludePerf: true
		],[
			name: 'Shoot Out Complex',
			desc: 'Shootout Complex compiled correctly',
			text: "<h1>{{header}}</h1>" +
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
				"{{/hasItems}}",
			data: [
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
			],
			expected: '<h1>Colors</h1><ul><li><strong>red</strong></li><li><a href=\"#Green\">green</a></li><li><a href=\"#Blue\">blue</a></li></ul>',
			excludePerf: true
		]
	]

	@Unroll("#name: #desc")
	def 'shared tests'() {
		when:
			def t = compiler.compile(test.text, test.options ?: [:])
			def s = t.render(test.data, test.partials)
		then:
			s == test.expected
		where:
			test << tests
			name = test.name
			desc = test.desc
	}

	def 'Stringified templates survive a round trip'() {
		// looks JS specific to text evaluation
	}

	def 'Stringified template bug report'() {
		// this test looks like it's JS specific
	}

	def 'Default Render Impl'() {
		// can't do because can't instantiate abstract class
	}

	def 'Lambda From Mustache Spec'() {
		when:
		def data = [
			name: 'Willy',
			wrapped: {
				return { text -> "<b>${text}</b>" }
			}
		]

		def template = compiler.compile('{{#wrapped}}{{name}} is awesome.{{/wrapped}}')
		def s = template.render(data)

		then:
		s == '<b>Willy is awesome.</b>'
	}
}