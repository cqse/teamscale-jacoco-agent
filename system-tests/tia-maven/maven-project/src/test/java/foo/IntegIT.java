package foo;

import org.junit.jupiter.api.Test;

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
