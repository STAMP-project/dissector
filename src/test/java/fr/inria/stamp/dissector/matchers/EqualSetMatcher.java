package fr.inria.stamp.dissector.matchers;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class EqualSetMatcher <T> extends BaseMatcher<Set<T>> {

    private Set<T> expected;

    public EqualSetMatcher(Set<T> expected) {
        this.expected = expected;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("has the same elements as the expected set");
    }

    @Override
    public void describeMismatch(Object item, Description description) {
        if(!(item instanceof Set)) {
            description.appendText("Actual item is not a set");
            return;
        }
        Set actual = (Set) item;
        if(actual.size() != expected.size())
            description.appendText("Actual set has size ")
                    .appendValue(actual.size())
                    .appendText(" while expected set should have size ")
                    .appendValue(expected.size());
        else
            description.appendText("Actual set contains elements not included in the expected set");
    }

    @Override
    public boolean matches(Object item) {
        if(!(item instanceof Set))
            return false;
        Set<T> actual = new HashSet<>((Set)item);
        if(expected.size() != actual.size())
            return false;
        actual.removeAll(expected);
        return actual.isEmpty();
    }

    public static <T> EqualSetMatcher<T> equalSet(Iterable<T> items) {
        HashSet<T> expected = new HashSet<>();
        for(T t:items)
            expected.add(t);
        return equalSet(expected);
    }

    public static <T> EqualSetMatcher<T> equalSet(T... items) {
        return equalSet(Arrays.asList(items));
    }

    public static <T> EqualSetMatcher<T> equalSet(Collection<T> items) {
        return equalSet(new HashSet<>(items));
    }

    public static <T> EqualSetMatcher<T> equalSet(Set<T> expected) {
        return new EqualSetMatcher<>(expected);
    }
}
