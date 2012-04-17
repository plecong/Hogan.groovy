import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.MustacheException

class LocalDefaultMustacheFactory extends DefaultMustacheFactory {

	Map partials = [:]

	@Override
	Reader getReader(String resourceName) {
		if (partials.containsKey(resourceName)) {
			return new StringReader(partials[resourceName])
		}

		throw new MustacheException("Template ${resourceName} not found")
	}

}