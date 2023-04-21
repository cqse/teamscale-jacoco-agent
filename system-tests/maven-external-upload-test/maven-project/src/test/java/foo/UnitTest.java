package bar;

import org.junit.jupiter.api.Test;
import foo.SUT;

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
