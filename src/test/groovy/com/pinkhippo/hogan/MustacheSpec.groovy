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
		'Section': { text  -> text == '{{x}}' ? 'yes' : 'no' },
		'Section - Expansion': { text -> text + '{{planet}}' + text },
		'Section - Alternate Delimiters': { text -> text + '{{planet}} => |planet|' + text },
		'Section - Multiple Calls': { text -> '__' + text + '__'},
		'Inverted Section': { false }
	]

	def loadSpecs() {
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
			s == test.expected.toString()
		where:
			test << loadSpecs()
	}

}
