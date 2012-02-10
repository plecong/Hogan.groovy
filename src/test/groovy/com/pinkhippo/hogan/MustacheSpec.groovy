package com.pinkhippo.hogan

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import groovy.json.JsonSlurper

class MustacheSpec extends Specification {

	// for Interpolation - Multiple Calls test
	static int counter = 0

	static LAMBDAS = [
		'Interpolation': { 'world' },
		'Interpolation - Expansion': { '{{planet}}' },
		'Interpolation - Alternate Delimiters': { return '|planet| => {{planet}}' },
		'Interpolation - Multiple Calls': { ++counter },
		'Escaping': { '>' },
		// must have arity 2 to trigger higher-order template
		'Section': { text, render -> text == '{{x}}' ? 'yes' : 'no' },
		'Section - Expansion': { text, render -> text + '{{planet}}' + text },
		'Section - Alternate Delimiters': { text, render -> text + '{{planet}} => |planet|' + text },
		'Section - Multiple Calls': { text, render -> '__' + text + '__'},
		'Inverted Section': { false }
	]

	def loadSpec(String name) {
		def tests = []

		new File('mustache-spec/specs').eachFile { file ->
			if (file.name.endsWith('.json')) {
				def json = new JsonSlurper().parse(new FileReader(file))
				tests.addAll(json.tests)
			}
		}

		tests
	}

	@Unroll({"${test.name}: ${test.desc}"})
	def 'mustache spec'() {
		when:
			def t = Hogan.compile(test.template)
			def d = test.data + (LAMBDAS[test.name] ? [lambda: { return LAMBDAS[test.name] } ] : [:])
			def s = t.render(d, test.partials)
		then:
			s == test.expected
		where:
			test << loadSpec('comments')
	}

}
