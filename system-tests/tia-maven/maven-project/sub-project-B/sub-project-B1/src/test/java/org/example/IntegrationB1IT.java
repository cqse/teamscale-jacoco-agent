package org.example;

import org.junit.jupiter.api.Test;
import org.example.SUTB1;

public class IntegrationB1IT {

    @Test
    public void itBlub() throws Exception {
        new SUTB1().blub();
    }

    @Test
    public void itGoo() {
        new SUTB1().goo();
    }
}
