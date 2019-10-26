# antlr4-doc [![Build Status](https://travis-ci.org/wadoon/antlr4-doc.svg?branch=master)](https://travis-ci.org/wadoon/antlr4-doc)

A documentation generator for ANTLRv4. 

License: GPLv3


## Compile

Compile a fat jar with: 

```
./gradlew shadowJar 
```

Use to the help about the arguments:

```
java -jar antlr4-doc-0.1.0-all.jar --help 
```

```
Usage: antlr4-doc [OPTIONS] [FILES]...

  Documentation Generator for ANTLRv4

Options:
  --skip-simple-tokens / --dont-skip-simple-tokens
                                   Simple tokens, tokens which are only
                                   consisting out a list of alternatives
                                   literal, will be skipped. Default: skip
  --html-title TEXT                title of the documentation
  -o, --output PATH                HTML output file. Default: output.html
  --complete-html                  Generate a complete HTML file incl. head
                                   and body tags.
  --css TEXT                       CSS files to refer to. Use with
                                   `--complete-html`
  --sort-lexical                   Sort the rules in lexcial order
  -h, --help                       Show this message and exit

Arguments:
  FILES  list of ANTLR files to render

```




