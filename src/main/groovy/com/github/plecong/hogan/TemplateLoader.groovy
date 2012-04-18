package com.github.plecong.hogan

interface TemplateLoader {
	HoganTemplate load(String name)
	Object getAt(String key)
}