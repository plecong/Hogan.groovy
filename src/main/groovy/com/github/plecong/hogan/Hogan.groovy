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

package com.github.plecong.hogan

import com.github.plecong.hogan.groovy.GroovyHoganCompiler

/**
 * A convenience class that is a facade on top of the parsing and compilation
 * classes to enable easy access to compiled templates.
 */
class Hogan {

	static GroovyHoganCompiler COMPILER = new GroovyHoganCompiler()

	/**
	 * Helper method to output the Groovy source of a "compiled"
	 * Hogan template source. This is useful in utilities that will
	 * pre-generate the source to be compiled by the groovyc compiler.
	 */
	static String generate(String source, Map options = [:]) {
		COMPILER.generate(source, options)
	}

	/**
	 * Compiles the Hogan template source into an actual
	 * Groovy object that can be used to render output.
	 */
	static HoganTemplate compile(String source, Map options = [:]) {
		COMPILER.compile(source, options)
	}

	/**
	 * Compiles the template source into a class that implements
	 * HoganTemplate that can be instantiated to create the
	 * actual template object.
	 */
	static Class<HoganTemplate> compileClass(String source, Map options = [:]) {
		COMPILER.compileClass(source, options)
	}

	/**
	 * Helper method that takes the class from compileClass and
	 * instantiates the template object with the appropriate constructor
	 * arguments and a reference to a compiler.
	 */
	static HoganTemplate create(Class<HoganTemplate> clazz, String source, Map options = [:]) {
		(HoganTemplate)clazz.newInstance([source, COMPILER, options].toArray())
	}
}