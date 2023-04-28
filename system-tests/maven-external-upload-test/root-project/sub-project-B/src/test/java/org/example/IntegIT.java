package bar;

import org.junit.jupiter.api.Test;
import org.example.SUTB;

public class IntegIT {

    @Test
    public void itBlub() throws Exception {
        new SUTB().blub();
    }

    @Test
    public void itGoo() {
        new SUTB().goo();
    }
}
