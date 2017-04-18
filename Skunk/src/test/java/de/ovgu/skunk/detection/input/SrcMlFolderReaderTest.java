package de.ovgu.skunk.detection.input;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Created by wfenske on 18.04.17.
 */
public class SrcMlFolderReaderTest {

    @DataProvider(name = "countLinesInput")
    public static Object[][] countLinesTestCases() {
        return new Object[][]{
                {"", 0}
                , {"foo", 1}
                , {"fo\ro", 1} // don't want carriage return to count as a newline
                , {"foo\n", 1}
                , {"\n", 1}
                , {"\nbar\r", 2}
                , {"\r\nbar\n", 2}
                , {"foo\nbar\n", 2}
        };
    }

    @Test(dataProvider = "countLinesInput")
    public void testcountLines(String input, Integer expectedOutput) throws Exception {
        int actualOutput = SrcMlFolderReader.countLines(input);
        Assert.assertEquals(actualOutput, (int) expectedOutput);
    }
}
