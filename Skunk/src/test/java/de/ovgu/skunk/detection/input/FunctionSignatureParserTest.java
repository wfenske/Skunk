package de.ovgu.skunk.detection.input;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Created by wfenske on 13.03.17.
 */
public class FunctionSignatureParserTest {

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
}
