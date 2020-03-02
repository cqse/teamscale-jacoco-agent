import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CalculatorTest {

    @Test
    public void testSum() {
        assertEquals(3, new Calculator().sum(1, 2));
    }

    @Test
    public void testMinus() {
        assertEquals(1, new Calculator().minus(3, 2));
    }
}