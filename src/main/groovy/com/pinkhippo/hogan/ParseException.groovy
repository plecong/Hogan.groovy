package com.pinkhippo.hogan

class ParseException extends RuntimeException {
	def token
	ParseException(String message, token = null) {
		super(message)
		this.token = token
	}
}