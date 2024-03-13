package plc.project;

import java.util.List;
import java.util.ArrayList;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier. */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        //throw new UnsupportedOperationException(); //TODO
        var tokens = new ArrayList<Token>();

        while(chars.has(0)){
            tokens.add(lexToken());
        }

        return tokens;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        //throw new UnsupportedOperationException(); //TODO
        if(peek("@|[A-Za-z]")){
            return lexIdentifier();
        }
        else if(peek("-|[0-9]")){
            return lexNumber();
        }
        else if(peek("'")){
            return lexCharacter();
        }
        else if(peek("\"")){
            return lexString();
        }
        else if(peek(".")){
            return lexOperator();
        }
        else{
            throw new ParseException("Unsupported token.", 0);
        }
    }

    public Token lexIdentifier() {
        //throw new UnsupportedOperationException(); //TODO
        while(match("[A-Za-z0-9_-]")){
            if(chars.has(0) && !peek("[A-Za-z0-9_-]")){
                if(!peek("[ \b\\n\\r\\t\\;]")){
                    throw new ParseException("Invalid identifier.", chars.index);
                }
            }
        }
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        //throw new UnsupportedOperationException(); //TODO
        match("-");
        if(chars.get(0) == '0' && peek("0")){
            throw new ParseException("Invalid number.", chars.index);
        }

        while(match("[0-9]")){
            if(chars.has(0) && !peek("[0-9]")){
                if(!peek("[ \b\\n\\r\\t;\\.]")){
                    throw new ParseException("Invalid number.", chars.index);
                }
            }
        }

        if(match("\\.")){
            if(!peek("[0-9]")){
                throw new ParseException("Invalid number.", chars.index);
            }

            while(match("[0-9]")){
                if(chars.has(0) && !peek("[0-9]")){
                    if(!peek("[ \b\\n\\r\\t;]")){
                        throw new ParseException("Invalid number.", chars.index);
                    }
                }
            }
            return chars.emit(Token.Type.DECIMAL);
        }

        return chars.emit(Token.Type.INTEGER);
    }

    public Token lexCharacter() {
        //throw new UnsupportedOperationException(); //TODO
        if(!match("'")){
            throw new ParseException("Invalid character", chars.index);
        }

        if(peek("'")){
            throw new ParseException("Invalid character", chars.index);
        }

        if(peek("[^'\\n\\r\\\\]")) {
            match("[^'\\n\\r\\\\]");
        }
        else if(peek("\\\\")){
            lexEscape();
        }
        else{
            throw new ParseException("Invalid character: here", chars.index);
        }

        if(!match("'")){
            throw new ParseException("Invalid character", chars.index);
        }

        return chars.emit(Token.Type.CHARACTER);
    }

    public Token lexString() {
        //throw new UnsupportedOperationException(); //TODO
        chars.advance();
        while(true){
            if(peek("\\\\")){
                lexEscape();
            }
            else if(peek("[^\"\\n\\r\\\\]")){
                match("[^\"\\n\\r\\\\]");
            }
            else if(peek("\\n\\r\\\\")){
                throw new ParseException("Invalid string: here", chars.index);
            }
            else{
                break;
            }
        }

        if(!match("\"")){
            throw new ParseException("Invalid string", chars.index);
        }

        return chars.emit(Token.Type.STRING);
    }

    public void lexEscape() {
        chars.advance();
        if(peek("[bnrt'\"\\\\]")){
            match("[bnrt'\"\\\\]");
        }
        else{
            throw new ParseException("Invalid Escape Sequence", chars.length);
        }
    }

    public Token lexOperator() {
        //throw new UnsupportedOperationException(); //TODO
        if(match("[<>=!]")){
            if(peek("=")){
                match("=");
            }
        }
        else{
            chars.advance();
        }

        return chars.emit(Token.Type.OPERATOR);
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        //throw new UnsupportedOperationException(); //TODO (in Lecture)
        for(int i = 0; i < patterns.length; i++){
            if(!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])){
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        //throw new UnsupportedOperationException(); //TODO (in Lecture)
        boolean peek = peek(patterns);

        if(peek){
            for(int i = 0; i < patterns.length; i++){
                chars.advance();
            }
        }

        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }


}
