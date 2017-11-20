package fr.inria.stamp.dissector;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


//TODO: Remove if it is not going to be used
public class Wildcards {

    String spec;
    Pattern pattern;

    public Wildcards(String spec) {
        if(!isValid(spec))
            throw new IllegalArgumentException("String must represent a valid wildcards specification.");
        this.spec = spec;
        pattern = toPattern();
    }

    @Override
    public String toString() {
        return spec;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public boolean matches(String input) {
        return pattern.matcher(input).matches();
    }

    public Predicate<String> asPredicate() {
        return pattern.asPredicate();
    }

    private Pattern toPattern() {
        StringBuilder buffer = new StringBuilder(spec.length());
        buffer.append('^');
        for(char c : spec.toCharArray()) {
            switch(c) {
                case '*':
                    buffer.append(".*");
                    break;
                case '$':
                case '.':
                    buffer.append('\\').append(c);
                    break;
                default:
                    buffer.append(c);
            }
        }
        buffer.append('$');
        return Pattern.compile(buffer.toString());
    }

    public static Pattern toPattern(String spec) {
        return new Wildcards(spec).toPattern();
    }

    public static boolean isValid(String wildcards) {
        return VALID_WILDCARDS_SPEC.matcher(wildcards).matches();
    }

    // Wildcards pattern can be a combination of *, ?, letter, numbers, and .
    // that can not start or end by a . and can't have two consecutive .'s
    // Right it might accept some incorrect values.
    private final static Pattern VALID_WILDCARDS_SPEC = Pattern.compile("^[\\*\\w\\?\\$]+(\\.[\\*\\w\\?\\$]+)*$");

}
