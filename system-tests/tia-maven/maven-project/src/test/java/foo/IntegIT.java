package bar;

import org.junit.jupiter.api.Test;
import foo.SUT;

public class IntegIT {

    @Test
    public void itBla() throws Exception {
        new SUT().bla();
    }

    @Test
    public void itFoo() {
        new SUT().foo();
    }
}
