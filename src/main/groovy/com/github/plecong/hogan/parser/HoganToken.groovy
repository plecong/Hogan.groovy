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

package com.github.plecong.hogan.parser

import groovy.transform.Canonical

@Canonical
class HoganToken {

	public static Map tags = [
		'#': 1,
		'^': 2,
		'<': 3,
		'$': 4,
		'/': 5,
		'!': 6,
		'>': 7,
		'=': 8,
		'_v': 9,
		'{': 10,
		'&': 11,
		'_t': 12,
		'\n': 13
	]

	CharSequence n
	def tag
	def i
	def indent
	String text
	String otag
	String ctag
	def nodes
	def end
	boolean last
}

