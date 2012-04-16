package com.pinkhippo.hogan

import groovy.transform.Canonical

@Canonical
class HoganToken {
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

