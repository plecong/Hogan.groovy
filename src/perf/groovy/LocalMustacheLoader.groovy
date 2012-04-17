import com.samskivert.mustache.*

class LocalMustacheLoader implements Mustache.TemplateLoader {

	Map partials = [:]

	@Override
	Reader getTemplate(String resourceName) {
		if (partials.containsKey(resourceName)) {
			return new StringReader(partials[resourceName])
		}
		return null
	}

}