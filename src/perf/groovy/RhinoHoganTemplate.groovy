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

import groovy.json.JsonBuilder
import org.mozilla.javascript.*

class RhinoHoganTemplate {
	Scriptable scope
	def template

	def render(Map data = [:], Map partials = [:]) {
		def result = null

		try {
			Context cx = Context.enter()
			def renderScope = cx.newObject(scope)
			renderScope.parentScope = scope

			renderScope.put('dataArg', renderScope, new JsonBuilder(data).toString())
			renderScope.put('partialArg', renderScope, new JsonBuilder(partials).toString())

			def dataArg = cx.evaluateString(renderScope, 'JSON.parse(dataArg)', 'argParse1', 0, null)
			def partialArg = cx.evaluateString(renderScope, 'JSON.parse(partialArg)', 'argParse2', 0, null)
			result = ScriptableObject.callMethod(cx, template, 'render', [dataArg, partialArg].toArray())

		} finally {
			Context.exit()
		}

		result
	}
}