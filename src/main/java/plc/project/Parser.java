package plc.project;

import java.util.*;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Global> globals = new ArrayList<>();
        List<Ast.Function> functions = new ArrayList<>();

        while(tokens.has(0)) {
            if (match("FUN")) {
                functions.add(parseFunction());
            } else if(peek("VAL") || peek("VAR") || peek("LIST")) {
                globals.add(parseGlobal());
            }
            else {
                throw new ParseException("Invalid source.", tokens.get(0).getIndex());
            }
        }

        return new Ast.Source(globals, functions);
    }

    /**
     * Parses the {@code global} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        if(match("VAR")) {
            Ast.Global g = parseMutable();
            if(!match(";")) {
                throw new ParseException("Invalid global declaration, missing ';': ", tokens.get(-1).getIndex());
            }
            return g;
        }
        else if(match("VAL")) {
            Ast.Global g = parseImmutable();
            if(!match(";")) {
                throw new ParseException("Invalid global declaration, missing ';': ", tokens.get(-1).getIndex());
            }
            return g;
        }
        else if(match("LIST")) {
            Ast.Global g = parseList();
            if(!match(";")) {
                throw new ParseException("Invalid global declaration, missing ';': ", tokens.get(-1).getIndex());
            }
            return g;
        }
        else {
            throw new ParseException("Invalid global: ", tokens.get(0).getIndex());
        }
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        if(!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Invalid use of a global declaration: ", tokens.get(-1).getIndex());
        }

        String lit = tokens.get(-1).getLiteral();

        //New type declarations:
        if(!match(":")) {
            throw new ParseException("Invalid declaration of Global 'LIST', bad type declaration: ", tokens.get(-1).getIndex());
        }
        if(!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Invalid declaration of Global 'LIST', bad type declaration: ", tokens.get(-1).getIndex());
        }

        String type = tokens.get(-1).getLiteral();

        if(!match("=")) {
            throw new ParseException("Invalid declaration of Global 'LIST', must be initialized to a value: ", tokens.get(-1).getIndex());
        }

        if(!match("[")) {
            throw new ParseException("Invalid assignment of Global 'LIST': ", tokens.get(-1).getIndex());
        }

        Ast.Expression list = parsePlcList();
        if(match("]")) {
            return new Ast.Global(lit, type, true, Optional.of(list));
        }
        else {
            throw new ParseException("Invalid assignment of Global 'LIST': ", tokens.get(-1).getIndex());
        }
    }

    private Ast.Expression parsePlcList() {
        List<Ast.Expression> list = new ArrayList<>();
            list.add(parseExpression());

        while(match(",")) {
            list.add(parseExpression());
        }

        return new Ast.Expression.PlcList(list);
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        if(!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Invalid use of global declaration: ", tokens.get(-1).getIndex());
        }

        String lit = tokens.get(-1).getLiteral();

        //New type declarations:
        if(!match(":")) {
            throw new ParseException("Invalid declaration of Global 'LIST', bad type declaration: ", tokens.get(-1).getIndex());
        }
        if(!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Invalid declaration of Global 'LIST', bad type declaration: ", tokens.get(-1).getIndex());
        }

        String type = tokens.get(-1).getLiteral();

        if(match("=")) {
            Ast.Expression e = parseExpression();
            return new Ast.Global(lit, type, true, Optional.of(e));
        }
        else {
            return new Ast.Global(lit, type, true, Optional.empty());
        }
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        if(!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Invalid use of global declaration: ", tokens.get(-1).getIndex());
        }

        String lit = tokens.get(-1).getLiteral();

        //New type declarations:
        if(!match(":")) {
            throw new ParseException("Invalid declaration of Global 'LIST', bad type declaration: ", tokens.get(-1).getIndex());
        }
        if(!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Invalid declaration of Global 'LIST', bad type declaration: ", tokens.get(-1).getIndex());
        }

        String type = tokens.get(-1).getLiteral();

        if(!match("=")) {
            throw new ParseException("Invalid IMMUTABLE declaration, missing '=': ", tokens.get(-1).getIndex());
        }

        Ast.Expression e = parseExpression();
        return new Ast.Global(lit, type, false, Optional.of(e));
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        if(!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Invalid function declaration, missing identifier: ", tokens.get(-1).getIndex());
        }

        String lit = tokens.get(-1).getLiteral();
        List<String> params = new ArrayList<>(), pTypes = new ArrayList<>();

        if(!match("(")) {
            throw new ParseException("Invalid function declaration, missing open parentheses: ", tokens.get(-1).getIndex());
        }

        if(match(Token.Type.IDENTIFIER)) {
            params.add(tokens.get(-1).getLiteral());
            //New type declarations:
            if(!match(":")) {
                throw new ParseException("Invalid declaration of Global 'LIST', bad type declaration: ", tokens.get(-1).getIndex());
            }
            if(!match(Token.Type.IDENTIFIER)) {
                throw new ParseException("Invalid declaration of Global 'LIST', bad type declaration: ", tokens.get(-1).getIndex());
            }
            pTypes.add(tokens.get(-1).getLiteral());

            while(match(",")) {
                if(!match(Token.Type.IDENTIFIER)) {
                    throw new ParseException("Invalid identifier in function signature: ", tokens.get(-1).getIndex());
                }
                params.add(tokens.get(-1).getLiteral());
                //New type declarations:
                if(!match(":")) {
                    throw new ParseException("Invalid declaration of Global 'LIST', bad type declaration: ", tokens.get(-1).getIndex());
                }
                if(!match(Token.Type.IDENTIFIER)) {
                    throw new ParseException("Invalid declaration of Global 'LIST', bad type declaration: ", tokens.get(-1).getIndex());
                }
                pTypes.add(tokens.get(-1).getLiteral());
            }
        }

        if(!match(")")) {
            throw new ParseException("Invalid function declaration, missing closing parentheses: ", tokens.get(-1).getIndex());
        }

        String rT = null;
        if(match(":")) {
            if(!match(Token.Type.IDENTIFIER)) {
                throw new ParseException("Return type is missing in type declaration or is not a valid identifier.", tokens.get(-1).getIndex());
            }
            rT = tokens.get(-1).getLiteral();
        }

        if(!match("DO")) {
            throw new ParseException("Invalid function declaration, missing 'DO': ", tokens.get(-1).getIndex());
        }

        List<Ast.Statement> statements = parseBlock();

        if(!match("END")) {
            throw new ParseException("Invalid function declaration, missing 'END': ", tokens.get(-1).getIndex());
        }

        return new Ast.Function(lit, params, pTypes, Optional.ofNullable(rT), statements);
        // throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block of statements.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        List<Ast.Statement> list = new ArrayList<>();
        if(peek("END") || peek("ELSE")) {
            return list;
        }

        list.add(parseStatement());
        while(!peek("END") && !peek("ELSE")) {
            list.add(parseStatement());
        }

        return list;
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        if(match("LET")) {
            return parseDeclarationStatement();
        }
        else if(match("SWITCH")) {
            return parseSwitchStatement();
        }
        else if(match("IF")) {
            return parseIfStatement();
        }
        else if(match("WHILE")) {
            return parseWhileStatement();
        }
        else if(match("RETURN")) {
            return parseReturnStatement();
        }
        else {
            Ast.Expression e = parseExpression();
            if(match("=")) {
                Ast.Expression eq = parseExpression();
                if(!match(";")) {
                    throw new ParseException("Invalid ASSIGNMENT statement, missing ';': ", tokens.get(-1).getIndex());
                }
                return new Ast.Statement.Assignment(e, eq);
            }
            else {
                if(!match(";")) {
                    throw new ParseException("Invalid EXPRESSION statement, missing ';': ", tokens.get(-1).getIndex());
                }
                return new Ast.Statement.Expression(e);
            }
        }

        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        if(!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Statement declaration missing identifier: ", tokens.get(-1).getIndex());
        }

        String lit = tokens.get(-1).getLiteral();
        //New type declarations:
        String type = null;
        if(match(":")) {
            if(!match(Token.Type.IDENTIFIER))
                throw new ParseException("Invalid declaration of Global 'LIST', bad type declaration: ", tokens.get(-1).getIndex());

            type = tokens.get(-1).getLiteral();
        }

        if(match("=")) {
            Ast.Expression e = parseExpression();
            if(!match(";")) {
                throw new ParseException("Invalid DECLARATION statement, missing ';': ", tokens.get(-1).getIndex());
            }

            return new Ast.Statement.Declaration(lit, Optional.ofNullable(type), Optional.of(e));
        }

        return new Ast.Statement.Declaration(lit, Optional.ofNullable(type), Optional.empty());
        // throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        Ast.Expression condition = parseExpression();
        if(!match("DO")) {
            throw new ParseException("Invalid IF statement, missing 'DO': ", tokens.get(-1).getIndex());
        }

        List<Ast.Statement> statements = parseBlock(), elseStatements = new ArrayList<>();

        //System.out.println(tokens.get(0).getLiteral());
        if(match("ELSE")) {
            elseStatements = parseBlock();
        }

        if(!match("END")) {
            throw new ParseException("Invalid IF statement, missing 'END': ", tokens.get(-1).getIndex());
        }

        return new Ast.Statement.If(condition, statements, elseStatements);
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        Ast.Expression condition = parseExpression();
        List<Ast.Statement.Case> cases = new ArrayList<>();

        while(!peek("DEFAULT")) {
            cases.add(parseCaseStatement());
        }

        cases.add(parseCaseStatement());
        if(!match("END")) {
            throw new ParseException("Invalid SWITCH statement, missing 'END': ", tokens.get(-1).getIndex());
        }

        return new Ast.Statement.Switch(condition, cases);
        // throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule. 
     * This method should only be called if the next tokens start the case or 
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        if(match("CASE")) {
            Ast.Expression value = parseExpression();
            if (!match(":")) {
                throw new ParseException("Invalid CASE statement, missing ':': ", tokens.get(-1).getIndex());
            }

            return new Ast.Statement.Case(Optional.of(value), parseBlock());
        }
        else if(match("DEFAULT")) {
            return new Ast.Statement.Case(Optional.empty(), parseBlock());
        }
        else {
            throw new ParseException("Invalid CASE statement: ", tokens.get(-1).getIndex());
        }
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        Ast.Expression condition = parseExpression();

        if(!match("DO")) {
            throw new ParseException("Invalid WHILE statement, missing 'DO': ", tokens.get(-1).getIndex());
        }

        List<Ast.Statement> statements = parseBlock();

        if(!match("END")) {
            throw new ParseException("Invalid WHILE statement, missing 'END': ", tokens.get(-1).getIndex());
        }

        return new Ast.Statement.While(condition, statements);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        Ast.Expression value = parseExpression();

        if(!match(";")) {
            throw new ParseException("Invalid RETURN statement, missing ';': ", tokens.get(-1).getIndex());
        }

        return new Ast.Statement.Return(value);
        // throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        Ast.Expression left = parseComparisonExpression();

        while(match("&&") || match("||")) {
            left = new Ast.Expression.Binary(tokens.get(-1).getLiteral(), left, parseComparisonExpression());
        }

        return left;
    }

    /**
     * Parses the {@code comparison-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        Ast.Expression left = parseAdditiveExpression();

        while(match("<") || match(">") || match("==") || match("!=")) {
            left = new Ast.Expression.Binary(tokens.get(-1).getLiteral(), left, parseAdditiveExpression());
        }

        return left;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression left = parseMultiplicativeExpression();

        while(match("+") || match("-")) {
            left = new Ast.Expression.Binary(tokens.get(-1).getLiteral(), left, parseMultiplicativeExpression());
        }

        return left;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression left = parsePrimaryExpression();

        while(match("*") || match("/") || match("^")) {
            left = new Ast.Expression.Binary(tokens.get(-1).getLiteral(), left, parsePrimaryExpression());
        }

        return left;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        //System.out.println(tokens.get(0).getType());

        if(match("NIL")) {
            return new Ast.Expression.Literal(null);
        }
        else if(match("TRUE")) {
            return new Ast.Expression.Literal(true);
        }
        else if(match("FALSE")) {
            return new Ast.Expression.Literal(false);
        }
        else if(match(Token.Type.INTEGER)) {
            return new Ast.Expression.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        }
        else if(match(Token.Type.DECIMAL)) {
            return new Ast.Expression.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        }
        else if(match(Token.Type.CHARACTER)) {
            String lit = escapeSequences(tokens.get(-1).getLiteral());
            return new Ast.Expression.Literal(lit.charAt(0));
        }
        else if(match(Token.Type.STRING)) {
            String lit = escapeSequences(tokens.get(-1).getLiteral());
            return new Ast.Expression.Literal(lit);
        }
        else if(match("(")) {
            Ast.Expression e = parseExpression();
            if(match(")")) {
                return new Ast.Expression.Group(e);
            }
            else {
                throw new ParseException("Invalid Grouping expression: ", tokens.get(-1).getIndex());
            }
        }
        else if(match(Token.Type.IDENTIFIER)) {
            String lit = tokens.get(-1).getLiteral();

            if(match("(")) {
                if(match(")")) {
                    return new Ast.Expression.Function(lit, new ArrayList<>());
                }

                List<Ast.Expression> list = new ArrayList<>();
                list.add(parseExpression());
                while(match(",")) {
                    list.add(parseExpression());
                }

                if(match(")")) {
                    return new Ast.Expression.Function(lit, list);
                }
                else {
                    throw new ParseException("Invalid function call, missing closing parentheses: ", tokens.get(-1).getIndex());
                }
            }
            else if(match("[")) {
                if(match("]")) {
                    return new Ast.Expression.Access(Optional.empty(), lit);
                }

                Ast.Expression e = parseExpression();
                if(match("]")) {
                    return new Ast.Expression.Access(Optional.of(e), lit);
                }
                else {
                    throw new ParseException("Invalid access, missing closing bracket: ", tokens.get(-1).getIndex());
                }
            }
            else {
                //System.out.println(tokens.get(-1).getLiteral());
                return new Ast.Expression.Access(Optional.empty(), tokens.get(-1).getLiteral());
            }
        }
        else {
            throw new ParseException("Invalid token type: ", tokens.get(-1).getIndex());
        }
    }

    private String escapeSequences(String s) {
        return s.substring(1, s.length() - 1)
                .replace("\\'", "'")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\r", "\r")
                .replace("\\b", "\b")
                .replace("\\\\", "\\")
                .replace("\\\"", "\"");
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for(int i = 0; i < patterns.length; i++) {
            if(!tokens.has(i)) {
                return false;
            }
            else if(patterns[i] instanceof Token.Type) {
                if(!patterns[i].equals(tokens.get(i).getType())) {
                    return false;
                }
            }
            else if(patterns[i] instanceof String) {
                if(!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            }
            else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }

        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);

        if(peek) {
            for(int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }

        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
