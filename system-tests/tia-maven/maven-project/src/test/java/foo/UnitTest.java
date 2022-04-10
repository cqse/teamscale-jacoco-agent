package foo;

import org.junit.jupiter.api.Test;

public class UnitTest {

    @Test
    public void utBla() {
        new SUT().bla();
    }

    @Test
    public void utFoo() {
        new SUT().foo();
    }
}
