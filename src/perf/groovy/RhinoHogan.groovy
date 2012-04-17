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

import org.mozilla.javascript.*

class RhinoHogan {

	Scriptable hoganObj
	def globalScope

	RhinoHogan() {
		try {
			Context cx = Context.enter()
			cx.optimizationLevel = 9

			this.globalScope = cx.initStandardObjects()

			def reader = new FileReader(new File('hogan.js/lib/template.js'))
			cx.evaluateReader(globalScope, reader, 'template.js', 0, null)

			reader = new FileReader(new File('hogan.js/lib/compiler.js'))
			cx.evaluateReader(globalScope, reader, 'compiler.js', 0, null)

			this.hoganObj = globalScope.get('Hogan')
		} finally {
			Context.exit()
		}

	}

	def compile(String text) {
		def tmpl

		try {
			Context cx = Context.enter()
			def compileScope = cx.newObject(globalScope)
			compileScope.parentScope = globalScope

			def args = [text].toArray()
			def template = ScriptableObject.callMethod(cx, hoganObj, 'compile', args)

			tmpl = new RhinoHoganTemplate(scope: compileScope, template: template)
		} finally {
			Context.exit()
		}

		return tmpl
	}

}
