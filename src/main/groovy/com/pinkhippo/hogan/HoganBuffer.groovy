package com.pinkhippo.hogan

class HoganBuffer implements CharSequence {
	@Delegate StringBuffer buffer = new StringBuffer()

	StringBuffer leftShift(Object value) {
		buffer << value
		buffer << '\n'
	}

	String toString() {
		buffer.toString()
	}
}