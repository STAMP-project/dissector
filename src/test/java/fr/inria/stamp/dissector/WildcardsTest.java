package fr.inria.stamp.dissector;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

public class WildcardsTest {

    @Test
    public void testValidWildcards() {
        String[] validWildcards = {
                "*",
                "?",
                "A",
                "*.*",
                "????.*",
                "java.*.Some???Class$Inner"
        };

        for(String spec : validWildcards) {
            assertTrue(Wildcards.isValid(spec), spec + " is expected to be a valid specification.");
        }

    }

    @Test
    public void testInvalidWildcards() {
        String[] invalidWildcards = {
                ".some",
                "some.",
                "a..b"
        };

        for(String spec: invalidWildcards) {
            assertFalse(Wildcards.isValid(spec), spec + "spec is expected to be a non-valid specification");
        }

    }

}