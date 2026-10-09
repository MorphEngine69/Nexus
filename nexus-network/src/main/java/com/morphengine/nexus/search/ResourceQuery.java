package com.morphengine.nexus.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * What a player typed in the search of a terminal, understood as a condition on a {@link SearchTarget}.
 *
 * <ul>
 *   <li>Words side by side must all match; {@code |} between them lets either side match instead.</li>
 *   <li>A plain word matches the name; {@code @} matches the mod, by id or by name, {@code #} the tags and
 *       {@code $} the tooltip. All of them match any part of the text.</li>
 *   <li>{@code !} or {@code -} before a word or a group turns it round.</li>
 *   <li>Parentheses group; double quotes keep spaces in a word, as in {@code "iron ingot"}.</li>
 * </ul>
 *
 * <p>A query that is not finished, such as an open parenthesis or quote, matches as far as it goes, and a prefix
 * with nothing after it matches everything, so that the list does not blink while the player types. Immutable.
 */
public final class ResourceQuery {

    private static final ResourceQuery EVERYTHING = new ResourceQuery(new Everything());

    private final Node root;

    private ResourceQuery(final Node root) {
        this.root = root;
    }

    /**
     * @return the query that {@code text} says; one that matches everything for blank text
     */
    public static ResourceQuery parse(final String text) {
        Objects.requireNonNull(text, "text must not be null");
        final List<Token> tokens = Tokens.scan(text.toLowerCase(Locale.ROOT));
        if (tokens.isEmpty()) {
            return EVERYTHING;
        }
        return new ResourceQuery(new Parser(tokens).parseAll());
    }

    public boolean matches(final SearchTarget target) {
        Objects.requireNonNull(target, "target must not be null");
        return root.matches(target);
    }

    /**
     * What a word is looked for in.
     */
    enum Field {
        NAME, MOD, TAG, TOOLTIP;

        static Field of(final char prefix) {
            return switch (prefix) {
                case '@' -> MOD;
                case '#' -> TAG;
                case '$' -> TOOLTIP;
                default -> NAME;
            };
        }

        boolean contains(final SearchTarget target, final String text) {
            return switch (this) {
                case NAME -> target.name().contains(text);
                case MOD -> target.modId().contains(text) || target.modName().contains(text);
                case TAG -> target.tags().stream().anyMatch(tag -> tag.contains(text));
                case TOOLTIP -> target.tooltip().contains(text);
            };
        }
    }

    private sealed interface Node permits Everything, Word, Not, And, Or {

        boolean matches(SearchTarget target);
    }

    private record Everything() implements Node {

        @Override
        public boolean matches(final SearchTarget target) {
            return true;
        }
    }

    private record Word(Field field, String text) implements Node {

        @Override
        public boolean matches(final SearchTarget target) {
            return text.isEmpty() || field.contains(target, text);
        }
    }

    private record Not(Node inner) implements Node {

        @Override
        public boolean matches(final SearchTarget target) {
            return !inner.matches(target);
        }
    }

    private record And(List<Node> parts) implements Node {

        @Override
        public boolean matches(final SearchTarget target) {
            for (Node part : parts) {
                if (!part.matches(target)) {
                    return false;
                }
            }
            return true;
        }
    }

    private record Or(List<Node> alternatives) implements Node {

        @Override
        public boolean matches(final SearchTarget target) {
            for (Node alternative : alternatives) {
                if (alternative.matches(target)) {
                    return true;
                }
            }
            return false;
        }
    }

    private sealed interface Token permits Open, Close, Bar, Negation, WordToken {
    }

    private record Open() implements Token {
    }

    private record Close() implements Token {
    }

    private record Bar() implements Token {
    }

    private record Negation() implements Token {
    }

    private record WordToken(Field field, String text) implements Token {
    }

    /**
     * Cuts the text into parentheses, bars, negations and words.
     */
    private static final class Tokens {

        private static final String DELIMITERS = "()|";
        private static final char QUOTE = '"';

        private Tokens() {
        }

        static List<Token> scan(final String text) {
            final List<Token> tokens = new ArrayList<>();
            int at = 0;
            while (at < text.length()) {
                final char current = text.charAt(at);
                if (Character.isWhitespace(current)) {
                    at++;
                } else if (current == '(' || current == ')' || current == '|') {
                    tokens.add(delimiter(current));
                    at++;
                } else if (current == '!' || current == '-' && isFollowedByWord(text, at)) {
                    tokens.add(new Negation());
                    at++;
                } else {
                    at = word(text, at, tokens);
                }
            }
            return tokens;
        }

        private static boolean isFollowedByWord(final String text, final int at) {
            return at + 1 < text.length() && !Character.isWhitespace(text.charAt(at + 1));
        }

        private static Token delimiter(final char current) {
            return switch (current) {
                case '(' -> new Open();
                case ')' -> new Close();
                default -> new Bar();
            };
        }

        /**
         * @return where the scan goes on after the word that starts at {@code start}
         */
        private static int word(final String text, final int start, final List<Token> tokens) {
            int at = start;
            Field field = Field.NAME;
            if ("@#$".indexOf(text.charAt(at)) >= 0) {
                field = Field.of(text.charAt(at));
                at++;
            }
            final StringBuilder word = new StringBuilder();
            if (at < text.length() && text.charAt(at) == QUOTE) {
                at++;
                while (at < text.length() && text.charAt(at) != QUOTE) {
                    word.append(text.charAt(at++));
                }
                at = Math.min(at + 1, text.length());
            } else {
                while (at < text.length() && !Character.isWhitespace(text.charAt(at))
                        && DELIMITERS.indexOf(text.charAt(at)) < 0) {
                    word.append(text.charAt(at++));
                }
            }
            tokens.add(new WordToken(field, word.toString()));
            return at;
        }
    }

    /**
     * Builds the condition from the tokens: bars bind loosest, then words side by side, then negation.
     */
    private static final class Parser {

        private final List<Token> tokens;
        private int at;

        Parser(final List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * A closing parenthesis with nothing open is skipped, and what follows it is read as more words.
         */
        Node parseAll() {
            final List<Node> parts = new ArrayList<>();
            while (at < tokens.size()) {
                parts.add(alternatives());
                if (at < tokens.size()) {
                    at++;
                }
            }
            return parts.size() == 1 ? parts.getFirst() : new And(parts);
        }

        private Node alternatives() {
            final List<Node> alternatives = new ArrayList<>();
            alternatives.add(sequence());
            while (at < tokens.size() && tokens.get(at) instanceof Bar) {
                at++;
                alternatives.add(sequence());
            }
            return alternatives.size() == 1 ? alternatives.getFirst() : new Or(alternatives);
        }

        private Node sequence() {
            final List<Node> parts = new ArrayList<>();
            while (at < tokens.size() && !(tokens.get(at) instanceof Bar) && !(tokens.get(at) instanceof Close)) {
                parts.add(unary());
            }
            return switch (parts.size()) {
                case 0 -> new Everything();
                case 1 -> parts.getFirst();
                default -> new And(parts);
            };
        }

        private Node unary() {
            final Token token = tokens.get(at++);
            return switch (token) {
                case Negation ignoredNegation -> at < tokens.size() ? new Not(unary()) : new Everything();
                case Open ignoredOpen -> group();
                case WordToken word -> new Word(word.field(), word.text());
                case Bar ignoredBar -> new Everything();
                case Close ignoredClose -> new Everything();
            };
        }

        private Node group() {
            final Node inner = alternatives();
            if (at < tokens.size() && tokens.get(at) instanceof Close) {
                at++;
            }
            return inner;
        }
    }
}
