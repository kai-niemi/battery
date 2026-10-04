/*
    ANLTR4 Grammar for Battery - A SQL database load generator scripting language.

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
parser grammar BatteryParser;

options {
    tokenVocab = BatteryLexer;
    caseInsensitive = true;
}

script
    : block EOF
    ;

block
    : statement*
    ;

statement
    : expression_statement SEMI # exprStmt
    | fork_statement SEMI       # forkStmt
    | returning SEMI            # returnStmt
    | joining SEMI              # joinStmt
    | if_statement              # ifStmt
    | while_statement           # whileStmt
    | foreach_statement         # foreachStmt
    | for_statement             # forStmt
    | SEMI                      # emptyStmt
    ;

expression_statement
    : expression
    ;

fork_statement
    : identifier EQUAL FORK statement_block
    ;

returning
    : RETURN expression
    ;

joining
    : JOIN (identifier | array)
    ;

if_statement
    : IF condition_block (ELSE IF condition_block)*  (ELSE statement_block)?
    ;

condition_block
    : LPAREN expression RPAREN statement_block
    ;

statement_block
    : LBRACE block RBRACE
    | statement
    ;

while_statement
    : WHILE LPAREN expression RPAREN statement_block
    ;

foreach_statement
    : FOREACH LPAREN expression RPAREN statement_block
    ;

for_statement
    : FOR identifier FROM expression TO expression statement_block
    ;

// ----------------------------------------
// Expressions
// ----------------------------------------

expression
    : LPAREN expression RPAREN                                 # parExpr

    | expression DOT identifier LPAREN expression_list? RPAREN # methodCallExpr
    | expression DOT identifier                                # nestedExpr
    | expression SELECT expression                             # arrayElementExpr

    | MINUS expression                                         # unaryMinusExpr
    | NOT expression                                           # notExpr
    | <assoc=right> expression POW expression                  # powExpr

    | expression op=(DIV | MULT | MOD) expression              # multiplicationExpr
    | expression op=(PLUS | MINUS) expression                  # additiveExpr

    | expression op=(LT | LE | GE | GT) expression             # relationalExpr
    | expression op=(EQ | NE) expression                       # equalityExpr
    | expression IN expression                                 # inExpr
    | expression NOT IN expression                             # notInExpr
    | expression AND expression                                # andExpr
    | expression OR expression                                 # orExpr

    | <assoc=right> identifier EQUAL expression                # assignExpr

    | atom                                                     # atomExpr
    ;

expression_list
    : expression (COMMA expression)*
    ;

// ----------------------------------------
// Atoms or data types
// ----------------------------------------

atom
    : boolean_literal           # booleanAtom
    | int_literal               # intAtom
    | double_literal            # doubleAtom
    | bigdecimal_literal        # bigDecimalAtom
    | string_literal            # stringAtom
    | date_literal              # dateAtom
    | time_literal              # timeAtom
    | datetime_literal          # dateTimeAtom
    | array                     # arrayAtom
    | list                      # listAtom
    | set                       # setAtom
    | map                       # mapAtom
    | identifier                # identifierAtom
    | null                      # nullAtom
    ;

array
    : OPEN_BRACKET expression_list? CLOSE_BRACKET
    ;

list
    : LIST_BRACKET expression_list? CLOSE_BRACKET
    ;

set
    : SET_BRACKET expression_list? CLOSE_BRACKET
    ;

map
    : MAP_BRACKET expression_list? CLOSE_BRACKET
    ;

identifier
    : ID
    ;

int_literal
    : INT_LITERAL
    ;

double_literal
    : DOUBLE_LITERAL
    ;

bigdecimal_literal
    : BIGDECIMAL_LITERAL
    ;

boolean_literal
    : TRUE
    | FALSE
    ;

string_literal
    : STRING_LITERAL
    | TEXT_BLOCK
    ;

date_literal
    : DATE_LITERAL
    ;

time_literal
    : TIME_LITERAL
    ;

datetime_literal
    : DATETIME_LITERAL
    ;

null
    : NULL
    ;