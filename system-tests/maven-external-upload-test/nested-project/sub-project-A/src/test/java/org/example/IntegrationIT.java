package bar;

import org.junit.jupiter.api.Test;
import org.example.SUTA;

public class IntegrationIT {

    @Test
    public void itBla() throws Exception {
        new SUTA().bla();
    }

    @Test
    public void itFoo() {
        new SUTA().foo();
    }
}
