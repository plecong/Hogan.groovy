package com.pinkhippo.hogan

class JsHoganCompiler extends HoganCompiler {

	def stringify(Map codeObj, String text, Map options) {
		new StringBuilder() << "{code: function (c,p,i) { " << Hogan.wrapMain(codeObj.code) << " }," << stringifyPartials(codeObj) << "}"
	}

	// #
	void section(node, context) {
		context.code << 'if(t.s(t.' + chooseMethod(node.n) + '("' + esc(node.n) + '",c,p,1),' +
					   'c,p,0,' + node.i + ',' + node.end + ',"' + node.otag + " " + node.ctag + '")){' +
					   't.rs(c,p,' + 'function(c,p,t){';
		walk(node.nodes, context);
		context.code << '});c.pop();}';
	}

	// ^
	void invertedSection(node, context) {
		context.code << 'if(!t.s(t.' + chooseMethod(node.n) + '("' + esc(node.n) + '",c,p,1),c,p,1,0,0,"")){';
		walk(node.nodes, context);
		context.code << '};';
	}

	// >
	void partial(node, context) {
		createPartial(node, context)
	}

	// <
	void include(node, context) {
		def ctx = [partials: context.partials, code: new StringBuilder(), subs: [:], inPartial: true];
		walk(node.nodes, ctx);

		def template = context.partials[createPartial(node, context)]
		template.subs = ctx.subs
		template.partials = ctx.partials
	}

	// $
	void includeSub(node, context) {
		def ctx = [subs: [:], code: new StringBuilder(), partials: context.partials, prefix: node.n]
		walk(node.nodes, ctx)
		context.subs[node.n] = ctx.code
		if (!context.inPartial) {
			context.code << 't.sub("' + esc(node.n) + '",c,p);'
		}
	}

	// \n
	void newLine(node, context) {
		context.code << print('"\\n"' + (node.last ? '' : ' + i'))
	}

	// _v
	void variable(node, context) {
		context.code << 't.b(t.v(t.' + chooseMethod(node.n) + '("' + esc(node.n) + '",c,p,0)));'
	}

	// _t
	void text(node, context) {
		context.code << print('"' + esc(node.text) + '"')
	}

	// & and {
	void tripleStache(node, context) {
		context.code << 't.b(t.t(t.' + chooseMethod(node.n) + '("' + esc(node.n) + '",c,p,0)));'
	}

	// was 'write()'
	private String print(String s) {
		't.b(' + s + ');'
	}

	private String wrapMain(code) {
		new StringBuilder() << 'var t=this;t.b(i=i||"");' << code << 'return t.fl();'
	}

	private String stringifyPartials(codeObj) {
		return "partials: {" + codeObj.partials.collect({ key, val ->
				'"' + esc(key) + '":{name:"' + esc(val.name) + '", ' + stringifyPartials(val) + "}"
			}).join(',') + '}, subs: ' + stringifyFunctions(codeObj.subs)
	}

	private String stringifyFunctions(obj) {
		"{ " + obj.collect({ key, val ->  '"' + esc(key) + '": function(c,p,t) {' + val + '}' }).join(',') + " } "
	}

	private String createPartial(node, context) {
		String prefix = '<' + (content.prefix ?: '')
		String sym = prefix + node.n + context.serialNo++
		context.partials[sym] = [name: node.n, partials: [:]]
		context.code << 't.b(t.rp("' +  esc(sym) + '",c,p,"' + (node.indent ?: '') + '"));'
		sym
	}

}