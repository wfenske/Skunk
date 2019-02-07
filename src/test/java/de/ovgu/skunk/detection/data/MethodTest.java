package de.ovgu.skunk.detection.data;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class MethodTest {
    @DataProvider(name = "functionNameExtractionInput")
    public static Object[][] functionNameTestCases() {
        return new Object[][]{
                {"void WM_widget_operator(struct wmWidget * widget, int( * initialize_op)(struct bContext *, const struct wmEvent *, struct wmWidget *, struct PointerRNA *), const char * opname, const char * propname)"
                        , "WM_widget_operator"}
        };
    }

    @Test(dataProvider = "functionNameExtractionInput")
    public void testFunctionNameExtraction(String input, String expectedOutput) throws Exception {
        Method f = new Method(null, input, "/foo.c", 1, 42, 1, "full function definition");
        String actualOutput = f.functionName;
        Assert.assertEquals(actualOutput, expectedOutput);
    }
}
