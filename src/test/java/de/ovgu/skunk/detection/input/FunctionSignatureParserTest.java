package de.ovgu.skunk.detection.input;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Created by wfenske on 13.03.17.
 */
public class FunctionSignatureParserTest {

    @DataProvider(name = "normalizeWhitespaceInput")
    public static Object[][] normalizeWhitespaceTestCases() {
        return new Object[][]{
                {"", ""}
                , {" foo ", "foo"}
                , {" foo\n", "foo"}
                , {"foo\nbar", "foo bar"}
                , {"foo\tbar", "foo bar"}
                , {"foo\t\tbar", "foo bar"}
                , {"foo , bar", "foo, bar"}
                , {"foo,bar", "foo, bar"}
                , {"(foo) ", "(foo)"}
                , {"(foo)bar", "(foo) bar"}
                , {"( (foo* ) )bar", "((foo *)) bar"}
                , {"foo (bar)", "foo(bar)"}
                , {"foo( bar)", "foo(bar)"}
                , {"foo(\n\tbar,\nbaz\n)", "foo(bar, baz)"}
                , {"foo(char * s)", "foo(char * s)"}
                , {"foo(char *s)", "foo(char * s)"}
                , {"foo(char* s)", "foo(char * s)"}
                , /* An actual example from OpenLDAP */
                {"void ldif_sput( char **out, int type, LDAP_CONST char *name, LDAP_CONST char *val, ber_len_t vlen )",
                        "void ldif_sput(char * * out, int type, LDAP_CONST char * name, LDAP_CONST char * val, ber_len_t vlen)"}
                , /* An actual example from Blender (3D modelling tool) */
                {"void info_depsgraphview_main(const struct bContext *C, struct ARegion *ar)\\\n",
                        "void info_depsgraphview_main(const struct bContext * C, struct ARegion * ar)"}
                , {"void info_depsgraphview_main(const struct bContext *C, struct ARegion *ar)\\",
                "void info_depsgraphview_main(const struct bContext * C, struct ARegion * ar)"}
                , /* Another one from Blender */
                {"static void xdnd_send_status (DndClass * dnd, Window window, Window from, int will_accept, \\\n" +
                        "              int want_position, int x, int y, int w, int h, Atom action)\n",
                        "static void xdnd_send_status(DndClass * dnd, Window window, Window from, int will_accept, int want_position, int x, int y, int w, int h, Atom action)"}
        };
    }

    @DataProvider(name = "removeCommentsKeepStringsInput")
    public static Object[][] removeCommentsKeepStringsTestCases() {
        return new Object[][]{
                {"", ""}
                , {"no comment", "no comment"}
                , {"no comment // line comment", "no comment "}
                , {"no comment // line comment\nno comment", "no comment \nno comment"}
                , {"no comment /* block comment */", "no comment "}
                , {"no comment /* block\n * comment */", "no comment "}
                , {"no comment /* block\n * comment */ more text", "no comment  more text"}

                // String literals
                , {"text \"string\"", "text \"string\""}
                , {"text \"string\" more text", "text \"string\" more text"}
                , {"text \"string w/ \\\" escape\"", "text \"string w/ \\\" escape\""}
                , {"text \"string w/ \\n escape\"", "text \"string w/ \\n escape\""}
                , {"text \"string w/ \\\"\"", "text \"string w/ \\\"\""}
                , {"text \"string\" more text", "text \"string\" more text"}

                // Char literals
                , {"no comment 'c'", "no comment 'c'"}
                , {"no comment '\\n'", "no comment '\\n'"}
                , {"no comment '\\''", "no comment '\\''"} // escaped single quote in char literal

                // Comments in string literals
                , {"text \"// a line comment in a string literal*/ \" more text", "text \"// a line comment in a string literal*/ \" more text"}
                , {"text \"/* a block comment in a string literal*/ \" more text", "text \"/* a block comment in a string literal*/ \" more text"}
                , {"text \"/* a malformed block comment in a string literal \" more text", "text \"/* a malformed block comment in a string literal \" more text"}

                // Stuff that's broken
                , {"no comment /* broken block comment", "no comment "}
                , {"no comment \"broken string literal", "no comment \"broken string literal"}
                , {"no comment 'c", "no comment 'c"} // broken character literal
        };
    }

    @DataProvider(name = "removeCommentsRemoveStringsInput")
    public static Object[][] removeCommentsRemoveStringsTestCases() {
        return new Object[][]{
                {"", ""}
                , {"no comment", "no comment"}
                , {"no comment // line comment", "no comment "}
                , {"no comment // line comment\nno comment", "no comment \nno comment"}
                , {"no comment /* block comment */", "no comment "}
                , {"no comment /* block\n * comment */", "no comment "}
                , {"no comment /* block\n * comment */ more text", "no comment  more text"}

                // String literals
                , {"text \"string\"", "text "}
                , {"text \"string\" more text", "text  more text"}
                , {"text \"string w/ \\\" escape\"", "text "}
                , {"text \"string w/ \\n escape\"", "text "}
                , {"text \"string w/ \\\"\"", "text "}
                , {"text \"string\" more text", "text  more text"}

                // Char literals
                , {"no comment 'c'", "no comment "}
                , {"no comment '\\n'", "no comment "}
                , {"no comment '\\''", "no comment "} // escaped single quote in char literal

                // Comments in string literals
                , {"text \"// a line comment in a string literal*/ \" more text",
                "text  more text"}
                , {"text \"/* a block comment in a string literal*/ \" more text",
                "text  more text"}
                , {"text \"/* a malformed block comment in a string literal \" more text",
                "text  more text"}

                // Stuff that's broken
                , {"no comment /* broken block comment", "no comment "}
                , {"no comment \"broken string literal", "no comment "}
                , {"no comment 'c", "no comment "} // broken character literal
        };
    }

    @Test(dataProvider = "removeCommentsKeepStringsInput")
    public void testRemoveCommentsKeepStrings(String input, String expectedOutput) throws Exception {
        String actualOutput = FunctionSignatureParser.removeComments(input, false);
        Assert.assertEquals(actualOutput, expectedOutput);
    }

    @Test(dataProvider = "removeCommentsRemoveStringsInput")
    public void testRemoveCommentsRemoveStrings(String input, String expectedOutput) throws Exception {
        String actualOutput = FunctionSignatureParser.removeComments(input, true);
        Assert.assertEquals(actualOutput, expectedOutput);
    }

    @Test(dataProvider = "normalizeWhitespaceInput")
    public void testNormalizeWhitespace(String input, String expectedOutput) {
        String actualOutput = FunctionSignatureParser.normalizeWhitespace(input);
        Assert.assertEquals(actualOutput, expectedOutput);
    }
}
