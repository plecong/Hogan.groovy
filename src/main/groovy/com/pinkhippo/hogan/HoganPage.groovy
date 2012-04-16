package com.pinkhippo.hogan

/**
 * The base script used for content generated by a compiled
 * template. Provides methods to access the context and is
 * equivalent to Hogan.Template in Hogan.js (name changed
 * to match Groovy templating convention)
 */
abstract class HoganPage extends Script {

	static AMP_PATTERN = ~/&/
	static LT_PATTERN = ~/</
	static GT_PATTERN = ~/>/
	static APOS_PATTERN = ~/\'/
	static QUOT_PATTERN = ~/\"/
	static HCHARS_PATTERN = ~/[&<>\"\']/

	String source
	String generated
	List tokens
	HoganCompiler compiler
	Map options

	Map partials = [:]
	Map subs = [:]

	private StringBuilder buffer

	HoganPage(String source, HoganCompiler compiler, Map options = [:], Map p = [:], Map u = [:]) {
		this.source = source
		this.compiler = compiler
		this.options = options

		this.partials += codeData.partials + p
		this.subs += codeData.subs + u

		ib();
	}

	Object run() {
		render(getBinding()?.variables)
	}

	// overridden by compiled
	abstract Map getCodeData()
	abstract String r(Deque context, Map partials, String indent)

	// variable escaping (was hoganEscape())
	String v(def str) {
		str = t(str)
		return HCHARS_PATTERN.matcher(str) ?
			str
				.replaceAll(AMP_PATTERN, '&amp;')
				.replaceAll(LT_PATTERN, '&lt;')
				.replaceAll(GT_PATTERN, '&gt;')
				.replaceAll(APOS_PATTERN, '&#39;')
				.replaceAll(QUOT_PATTERN, '&quot;') :
			str
	}

	// triple stache
	String t(def val) {
		coerceToString(val)
	}

	String coerceToString(def val) {
		val == null ? '' : val as String
	}

	def render(Map context = [:], Map partials = [:], String indent = '') {
		def contextStack = new ArrayDeque()
		contextStack.push(context ?: [:])
		ri(contextStack, partials ?: [:], indent)
	}

	// render internal
	def ri(Deque context, Map partials, String indent = '') {
		r(context, partials, indent)
	}

	// ensure partial
	def ep(String symbol, Map partials) {
		def partial = this.partials[symbol]
		def template = partials[partial.name]
		def compiledTemplate = null

		if (partial.instance && partial.base == template) {
			return partial.instance
		}

		if (template instanceof HoganPage) {
			compiledTemplate = template
		} else if (template instanceof String) {
			if (!compiler) {
				throw new RuntimeException('No compiler available.')
			}
			compiledTemplate = compiler.compile(template, options + [name: getClass().name + '_' + partial.name])
		}

		if (!compiledTemplate) {
			return null
		}

		partial.base = compiledTemplate
		if (partial.subs) {
			// this is "specialized" because it has it's own set of subs
			// that will override the default ones compiled into the template
			// based on this execution
			compiledTemplate = createSpecializedPartial(compiledTemplate, partial.subs, partial.partials)
		}
		partial.instance = compiledTemplate
		return partial.instance
	}

	// render partial
	def rp(String symbol, Deque context, Map partials, String indent) {
		def partial = this.ep(symbol, partials)
		partial ? partial.ri(context, partials, indent) : ''
	}

	// render section
	def rs(Deque context, Map partials, Closure section) {
		def tail = context.peek()

		if (!ObjectUtils.isArray(tail)) {
			// do we need to clone here?
			def c = section.clone()
			c.call() // the generated code will inherit the context/partials references
			return
		}

		tail.each {
			context.push(it)
			def c = section.clone()
			c.call()
			context.pop()
		}
	}

	// section
	def s(def val, Deque ctx, Map partials, boolean inverted, int start, int end, String tags) {
		boolean pass

		// TODO: check if also a collection
		if (ObjectUtils.isArray(val) && val.size() == 0) {
			return false
		}

		if (val instanceof Closure) {
			val = this.ms(val, ctx, partials, inverted, start, end, tags)
		}

		// default groovy truthyness will work now that the js code is
		// (val === 0) || !!val
		pass = val == '' || val as Boolean

		if (!inverted && pass && ctx) {
			ctx.push(!ObjectUtils.isPrimitive(val) ? val : ctx.peek())
		}

		return pass
	}

	def d(String key, Deque ctx, Map partials, boolean returnFound) {
		def names = key.tokenize('.')
		def val = f(names[0], ctx, partials, returnFound)
		def cx = null

		if (key == '.' && ObjectUtils.isArray(ctx.toArray()[-2])) {
			return ctx.peek()
		}

		for (int i = 1; i < names.size(); i++) {
			if (val && !ObjectUtils.isPrimitive(val) && ObjectUtils.hasProperty(val, names[i])) {
				cx = val
				val = val[names[i]]
			} else {
				val = ''
			}
		}

		if (returnFound && !val) {
			return false
		}

		if (!returnFound && val instanceof Closure) {
			ctx.push(cx)
			val = this.mv(val, ctx, partials)
			ctx.pop()
		}

		return val
	}

	// find
	def f(String key, Deque ctx, Map partials, boolean returnFound) {
		def level = ctx.find { m -> ObjectUtils.hasProperty(m, key) }

		if (level == null) {
			return (returnFound) ? false : ''
		}

		def val = level[key]

		if (!returnFound && val instanceof Closure) {
			val = this.mv(val, ctx, partials)
		}

		val
	}

	// lambda section: higher order templates
	def ls(Closure val, Map cx, Map partials, String text, String tags) {
		def cl = val.clone()
		cl.delegate = cx
		def result

		if (cl.maximumNumberOfParameters >= 1) {
			result = cl.call(text)
		} else {
			result = cl.call()
		}

		b(ct(coerceToString(result), cx, partials, [delimiters: tags]))
		return false
	}

	// compile template
	def ct(String text, Map cx, Map partials, Map options = [:]) {
		if (this.options.disableLambda) {
			throw new RuntimeException('Lambda features disabled.')
		}
		compiler.compile(text, this.options + options).render(cx, partials)
	}

	// buffer
	void b(s) {
		buffer << (s ?: '')
	}

	// flush buffer
	String fl() {
		def output = buffer.toString()
		ib()
		output
	}

	// init the buffer
	void ib() {
		this.buffer = new StringBuilder()
	}

	// method replace section
	def ms(Closure func, Deque ctx, Map partials, boolean inverted, int start, int end, String tags) {
		def cx = ctx.peek()
		def cl = func.clone()
		cl.delegate = cx
		def result = cl.call()

		if (result instanceof Closure) {
			if (inverted) {
				return true
			} else {
				return ls(result, cx, partials, this.source[start..<end], tags)
			}
		}

		return result
	}

	// method replace variable
	def mv(Closure func, Deque ctx, Map partials) {
		def cx = ctx.peek()
		def cl = func.clone()
		cl.delegate = cx
		def result = cl.call()

		if (result instanceof Closure) {
			def resultCl = result.clone()
			resultCl.delegate = cx
			return ct(coerceToString(resultCl.call()), cx, partials)
		}

		return result
	}

	// sub
	def sub(String name, Deque context, Map partials) {
		def cl = subs[name]

		if (!cl) {
			throw new RuntimeException('Missing subs for name: "' + name + '", subs: ' + subs)
		}

		cl.clone().call(context, partials, this)
	}

	private def createSpecializedPartial(HoganPage template, Map extraSubs, Map extraPartials) {
		def other = template.class.newInstance(source, compiler, options, extraPartials, extraSubs)
		other.ib()
		other
	}

}
