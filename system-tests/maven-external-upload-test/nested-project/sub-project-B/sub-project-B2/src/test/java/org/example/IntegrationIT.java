package bar;

import org.junit.jupiter.api.Test;
import org.example.SUTB2;

public class IntegrationIT {

    @Test
    public void itBlub() throws Exception {
        new SUTB2().blub();
    }

    @Test
    public void itGoo() {
        new SUTB2().goo();
    }
}
