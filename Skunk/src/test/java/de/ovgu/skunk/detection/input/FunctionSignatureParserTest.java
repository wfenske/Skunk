package de.ovgu.skunk.detection.input;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Created by wfenske on 13.03.17.
 */
public class FunctionSignatureParserTest {

    @DataProvider(name = "removeCommentsInput")
    public static Object[][] removeCommentsTestCases() {
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
                , {"no comment \'c", "no comment \'c"} // broke character literal
        };
    }

    @Test(dataProvider = "removeCommentsInput")
    public void testRemoveComments(String input, String expectedOutput) throws Exception {
        String actualOutput = FunctionSignatureParser.removeComments(input);
        Assert.assertEquals(expectedOutput, actualOutput);
        //System.out.println("\"" + input + "\" -> \"" + actualOutput + "\"");
    }
}
