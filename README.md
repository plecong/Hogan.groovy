## Hogan.groovy - A Mustache compiler for Groovy.

[Hogan.groovy](http://github.com/plecong/Hogan.groovy) is a
[Groovy](https://github.com/twitter/hogan.js) port of the
[Hogan.js](http://github.com/twitter/hogan.js) compiler for the
[Mustache](http://mustache.github.com/) templating language.

For more information on Mustache, see the [manpage](http://mustache.github.com/mustache.5.html) and
the [spec](https://github.com/mustache/spec).

## Basics

Hogan compiles templates to `HoganPage` objects, which have a render method.

```groovy
def data = [ screenName: 'plecong' ]

def template = Hogan.compile('Follow @{{screenName}}.')
def output = template.render(data)

// prints "Follow @plecong."
println output
```

## Features

From [Twitter's](http://github.com/twitter) [Hogan](http://github.com/twitter/hogan.js) page:

> Hogan has separate scanning, parsing and code generation phases. This way it's
> possible to add new features without touching the scanner at all, and many
> different code generation techniques can be tried without changing the parser.

This project ports the parsing and scanning functionality to Groovy and implements
a Groovy code generation phase. This means that the compiled templates are Groovy
classes that can be generated ahead of time and compiled. Furthermore, because it
is Groovy, you can use [Groovy closures](http://groovy.codehaus.org/Closures) as the lambdas.

```groovy
def data = [
	name: 'Willy',
	wrapped: {
		return { text -> "<b>${text}</b>" }
	}
]

def template = Hogan.compile('{{#wrapped}}{{name}} is awesome.{{/wrapped}}')
def expected = '<b>Willy is awesome.</b>'

assert expected == template.render(data)
```

There is also an experimental (i.e. untested) JavaScript code generation that should
output identical code to the Hogan.js method. This will allow for server-side
precompilation without having to load Rhino.

The long term vision is that the Groovy compiled templates can be used on the server
and the exact same Mustache templates can be compiled into JavaScript and included
into source page for client-side template generation.


## Motivation

Mostly, because it could be done. There are other alternatives for compiling Mustache templates on the JVM:

* [samskivert's](http://github.com/samskivert) [jmustache](http://github.com/samskivert/jmustache)
* [spullara's](http://github.com/spullara) [mustache.java](http://github.com/spullara/mustache.java)

These libraries being implemented in Java only are going to be much faster. However, Hogan.groovy
is worth using if:

* You're using Groovy and would like to use Groovy closures as Mustache lambdas
* You're already using Hogan.js on the client-side and want the exact same features
during server-side rendering including:
  * Recursive templates
  * Template inheritance


## Compilation options

All of the compilation options for Hogan.js are valid here, including `asString`,
`sectionTags`, `delimiters`, and `disableLambda`. Along with these there are:

keepGenerated: when compiling the HoganPage class, keep the generated Groovy source
file. This option will be used in pre-generating and compiling for inclusion in
WAR files.

## Issues & Versioning

We will try to match the Hogan.js versioning scheme, i.e. semantic versioning,
as much as possible.

## License

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0