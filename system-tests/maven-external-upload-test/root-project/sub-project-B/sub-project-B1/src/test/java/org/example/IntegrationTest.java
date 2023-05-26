package bar;

import org.junit.jupiter.api.Test;
import org.example.SUTB1;

public class IntegrationTest {

    @Test
    public void itBlub() throws Exception {
        new SUTB1().blub();
    }

    @Test
    public void itGoo() {
        new SUTB1().goo();
    }
}
