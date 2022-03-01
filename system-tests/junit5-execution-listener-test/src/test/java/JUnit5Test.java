import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JUnit5Test {

    @Test
    void testAdd() {
        int result = new SystemUnderTest().add(1, 2);
        Assertions.assertEquals(3, result);
    }

}
