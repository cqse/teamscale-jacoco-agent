import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class JUnit4ExecutedWithJUnit5Test {

    @Test
    public void testAdd() {
        int result = new SystemUnderTest().add(1, 2);
        Assertions.assertEquals(3, result);
    }

}
