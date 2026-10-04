/**
    ANLTR4 Lexer Rules for Battery - A SQL database load generator scripting language.

    The MIT License

    Copyright (c) 2026 Kai Niemi

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
*/
lexer grammar BatteryLexer;

options {
    caseInsensitive = true;
}

// Keywords

FORK : 'fork';
JOIN : 'join';
RETURN : 'return';
WHILE : 'while';
FOREACH : 'foreach';
FOR : 'for';
IF : 'if';
ELSE : 'else';
FROM : 'from';
TO : 'to';

// Lexer rules

COMMA : ',' ;
SEMI: ';' ;
LPAREN : '(' ;
RPAREN : ')' ;
LBRACE : '{' ;
RBRACE : '}' ;
OPEN_BRACKET : '[' ;
CLOSE_BRACKET : ']' ;
LIST_BRACKET : 'L[' ;
SET_BRACKET : 'S[' ;
MAP_BRACKET : 'M[' ;

// Arithmetic operators

POW : ('pow'|'^') ;
MULT : '*' ;
DIV : '/' ;
PLUS : '+' ;
MINUS : '-' ;
MOD : ('mod'|'%') ;

// Logical operators

AND : ('and'|'&&') ;
OR  : ('or'|'||') ;
NOT : ('not'|'!') ;

// Comparative operators

GT : '>' ;
GE : '>=' ;
LT : '<' ;
LE : '<=' ;
EQ : '==' ;
NE : '!=' ;

// Other operators

SELECT : 'select' ;
IN : 'in' ;
DOT : '.' ;
EQUAL: '=';

// Date and Time Literals

// Each literal is a single token including its prefix and braces, so that the quoted part
// doesn't compete with string literals (for example a plain '10:00:00' string)

DATETIME_LITERAL
    : '{' [ \t]* [dD][tT] [ \t]* '\'' DATE ' ' TIME '\'' [ \t]* '}' ;

DATE_LITERAL
    : '{' [ \t]* [dD] [ \t]* '\'' DATE '\'' [ \t]* '}' ;

TIME_LITERAL
    : '{' [ \t]* [tT] [ \t]* '\'' TIME '\'' [ \t]* '}' ;

fragment DATE
    : FOUR_DIGITS '-' TWO_DIGITS '-' TWO_DIGITS ;

fragment TIME
    : TWO_DIGITS ':' TWO_DIGITS ':' TWO_DIGITS ;

fragment FOUR_DIGITS
    : DEC_DIGIT DEC_DIGIT DEC_DIGIT DEC_DIGIT ;

fragment TWO_DIGITS
    : DEC_DIGIT DEC_DIGIT ;

// Boolean Literals

TRUE: 'TRUE';
FALSE: 'FALSE';
NULL: 'NULL';

// Numeric Literals

fragment DEC_DIGIT : [0-9];
fragment NUMBER_BASE : (DEC_DIGIT+ '.' DEC_DIGIT* | '.' DEC_DIGIT+);
fragment INT_BASE : DEC_DIGIT+;

BIGDECIMAL_LITERAL
    : NUMBER_BASE 'bd'
    | INT_BASE 'bd'
    ;

DOUBLE_LITERAL
    : NUMBER_BASE 'd'?
    | INT_BASE 'd'
    ;

INT_LITERAL
    : INT_BASE
    ;

// Identifier Literals

ID
    : [A-Z_] [A-Z_0-9$]*
    ;

// String Literals

// Each quote style excludes its own quote character, so that a string ends at the first
// matching unescaped quote rather than the last quote on the line
STRING_LITERAL
    : '"' (~["\\\r\n] | ESCAPE_SEQUENCE)* '"'
    | '\'' (~['\\\r\n] | ESCAPE_SEQUENCE)* '\''
    ;

TEXT_BLOCK : '"""' [ \t]* [\r\n] (. | ESCAPE_SEQUENCE)*? '"""';

fragment ESCAPE_SEQUENCE:
    '\\' 'u005c'? [btnfr"'\\]
    | '\\' 'u005c'? ([0-3]? [0-7])? [0-7]
    | '\\' 'u'+ HEXDIGIT HEXDIGIT HEXDIGIT HEXDIGIT
    ;

fragment HEXDIGIT  : [0-9a-fA-F];

// Comments

LINE_COMMENT_1
    : '--' ~ [\r\n]* -> skip
    ;

LINE_COMMENT_2
    : '//' ~ [\r\n]* -> skip
    ;

LINE_COMMENT_3
    : '#' ~ [\r\n]* -> skip
    ;

BLOCK_COMMENT
    : '/*' .*? '*/' -> skip
    ;

WS
    : [ \t\r\n] -> skip
    ;