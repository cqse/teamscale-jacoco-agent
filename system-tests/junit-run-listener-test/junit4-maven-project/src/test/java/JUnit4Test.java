import org.junit.Test;
import org.junit.Assert;

import systemundertest.SystemUnderTest;

public class JUnit4Test {

    @Test
    public void testAdd() {
        int result = new SystemUnderTest().add(1, 2);
        Assert.assertEquals(3, result);
    }

}
