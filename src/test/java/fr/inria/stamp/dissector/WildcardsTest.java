package fr.inria.stamp.dissector;


import org.junit.Test;
import static org.junit.Assert.*;

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
            assertTrue(spec + " is expected to be a valid specification.", Wildcards.isValid(spec));
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
            assertFalse(spec + "spec is expected to be a non-valid specification", Wildcards.isValid(spec));
        }

    }

}