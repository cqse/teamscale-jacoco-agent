package org.example;

import org.junit.jupiter.api.Test;
import org.example.SUTB2;

public class UnitB2Test {

    @Test
    public void utBlub() {
        new SUTB2().blub();
        new SUTB1().blub();
    }

    @Test
    public void utGoo() {
        new SUTB2().goo();
        new SUTB1().goo();
    }
}
