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

import groovy.text.Template
import groovy.text.TemplateEngine

import com.github.plecong.hogan.groovy.GroovyHoganCompiler

/**
 * An implementation of Groovy's <code>TemplateEngine</code> using
 * Hogan/Mustache templates
 */
class HoganTemplateEngine extends TemplateEngine {

	final HoganCompiler compiler = new GroovyHoganCompiler()

	/**
	 * Creates an instance of a HoganTemplate that implements the Template
	 * interface.
	 *
	 * Note: instances returned are not thread-safe yet.
	 */
	Template createTemplate(Reader reader) {
		compiler.compile(reader.text, [:])
	}

}