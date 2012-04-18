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

abstract class BaseHoganTemplate implements HoganTemplate {

	abstract void render(Writer writer, Map context, TemplateLoader loader)

	String render(Map context = [:], Map partials = [:]) {
		render(context, new TemplateLoader() {
			HoganTemplate load(String name) { partials[name] }
			Object getAt(String key) { partials[key] }
		})
	}

	String render(Map context = [:], TemplateLoader loader) {
		def writer = new StringWriter();
		render(writer, context, loader)
		writer.toString()
	}

	Writable make(Map binding = [:]) {
		new Writable() {
			Writer writeTo(Writer writer) {
				render(writer, binding, null)
				writer
			}
		}
	}

}