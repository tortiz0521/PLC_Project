package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. A framework of the test structure 
 * is provided, you will fill in the remaining pieces.
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false),
                Arguments.of("One Character", "a@gmail.com", false),
                Arguments.of("No '@'", "no_atgmail.com", false),
                Arguments.of("Two Domain Dots in-a-row", "twodots@gmail..com", false),
                Arguments.of("Dot At End", "dotatend@gmail.com.", false),
                Arguments.of("Multiple Dots For Domain", "multiple@gmail.ufl.edu.com", true),
                Arguments.of("All Upper-Case User Alias", "UPPER_CASE@gmail.com", true),
                Arguments.of("All Upper-Case Domain", "upper.domain@GMAIL.COM", false),
                Arguments.of("All Valid User Alias Symbols", "_timothy.ortiz_@ufl.edu", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testOddStringsRegex(String test, String input, boolean success) {
        test(input, Regex.ODD_STRINGS, success);
    }

    public static Stream<Arguments> testOddStringsRegex() {
        return Stream.of(
                // what have eleven letters and starts with gas?
                Arguments.of("11 Characters", "automobiles", true),
                Arguments.of("13 Characters", "i<3pancakes13", true),
                Arguments.of("5 Characters", "5five", false),
                Arguments.of("14 Characters", "i<3pancakes14!", false),
                Arguments.of("10 Characters", "0123456789", false),
                Arguments.of("20 Characters", "[cucumbers, bananas]", false),
                Arguments.of("Greater than 20", "This line is easily more then 20 characters.", false),
                Arguments.of("12 Characters", "Twelve or 12", false),
                Arguments.of("17 Characters", "I love Regex <3!!", true),
                Arguments.of("15 Characters", "15 Characters!!", true),
                Arguments.of("11 Symbols", "!@#$%^&*()_", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testCharacterListRegex(String test, String input, boolean success) {
        test(input, Regex.CHARACTER_LIST, success);
    }

    public static Stream<Arguments> testCharacterListRegex() {
        return Stream.of(
                Arguments.of("Single Element", "['a']", true),
                Arguments.of("Multiple Elements", "['a','b','c']", true),
                Arguments.of("Missing Brackets", "'a','b','c'", false),
                Arguments.of("Missing Commas", "['a' 'b' 'c']", false),
                Arguments.of("Missing One Coma", "['a', 'b' 'c', 'd']", false),
                Arguments.of("Missing Front Bracket", "'a', 'b', 'c']", false),
                Arguments.of("Missing Back Bracket", "['a', 'b', 'c'", false),
                Arguments.of("Many Characters", "['a', '2', '~', 'D', ' ']", true),
                Arguments.of("Spaces and No Spaces", "['a', 'b','c']", true),
                Arguments.of("Trailing Coma", "['a',]", false),
                Arguments.of("Trailing Space", "['a' ]", false),
                Arguments.of("Trailing ', '", "['a', ]", false),
                Arguments.of("No Spaces", "['a','b','c','d']", true),
                Arguments.of("Nothing", "[]", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testDecimalRegex(String test, String input, boolean success) {
        //throw new UnsupportedOperationException(); //TODO
        test(input, Regex.DECIMAL, success);
    }

    public static Stream<Arguments> testDecimalRegex() {
        //throw new UnsupportedOperationException(); //TODO
        return Stream.of(
                Arguments.of("Standard Decimal", "1.01", true),
                Arguments.of("Leading 0", "012.01", false),
                Arguments.of("Only 0 Decimals", "0.00000", true),
                Arguments.of("Many Digits", "54328.102398", true),
                Arguments.of("Multiple Zeroes Before Decimal", "00.0", false),
                Arguments.of("Whole Number", "10", false),
                Arguments.of("Missing Numbers after Decimal", "1.", false),
                Arguments.of("Non-numbers", "a/b", false),
                Arguments.of("Letter in Number", "12.0b13", false),
                Arguments.of("No Leading Figure", ".12345", false),
                Arguments.of("Negative Decimal", "-0.1", true),
                Arguments.of("Negative Whole #", "-21", false),
                Arguments.of("Negative Leading 0", "-098.76", false),
                Arguments.of("Negative No Leading #", "-.01", false),
                Arguments.of("Negative Many Digits", "-9876.54321", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        test(input, Regex.STRING, success);
    }

    public static Stream<Arguments> testStringRegex() {
        return Stream.of(
                Arguments.of("Hello, World!", "\"Hello, World!\"", true),
                Arguments.of("Escape Characters", "\"1\\t2\"", true),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),
                Arguments.of("Literal Backslash", "\"\\\\\"", true),
                Arguments.of("Single Backslash", "\"\\\"", false),
                Arguments.of("Multiple Escapes", "\"\\b\\n\\t\\r\"", true),
                Arguments.of("Missing Leading Quote", "No Leading\"", false),
                Arguments.of("Missing Closing Quote", "\"No Closing", false),
                Arguments.of("Missing Both Quotes", "No Quotes", false),
                Arguments.of("More Invalid Escapes", "\"\\an \\invalid\\ escape\"", false),
                Arguments.of("Valid and Invalid Escapes", "\"\\this is\\ i\\nva\\lid\"", false),
                Arguments.of("\"Nothing\"", "\"\"", true),
                Arguments.of("Nothing", "", false)
        );
    }


    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
