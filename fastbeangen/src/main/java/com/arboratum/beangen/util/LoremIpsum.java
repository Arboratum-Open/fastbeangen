package com.arboratum.beangen.util;

public abstract class LoremIpsum {
    public static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, " +
            "sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. " +
            "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata " +
            "sanctus est Lorem ipsum dolor sit amet.";

    private static final String[] loremIpsumWords = LOREM_IPSUM.split("\\s");
    ;

    /**
     * Returns one sentence (50 words) of the lorem ipsum text.
     *
     * @return 50 words of lorem ipsum text
     */
    public static String getWords() {
        return getWords(50);
    }

    /**
     * Returns words from the lorem ipsum text.
     *
     * @param amount Amount of words
     * @return Lorem ipsum text
     */
    public static String getWords(int amount) {
        return getWords(amount, 0);
    }

    /**
     * Returns words from the lorem ipsum text.
     *
     * @param amount     Amount of words
     * @param startIndex Start index of word to begin with (must be >= 0 and < 50)
     * @return Lorem ipsum text
     * @throws IndexOutOfBoundsException If startIndex is < 0 or > 49
     */
    public static String getWords(int amount, int startIndex) {
        if (startIndex < 0 || startIndex > 49) {
            throw new IndexOutOfBoundsException("startIndex must be >= 0 and < 50");
        }

        int word = startIndex;
        StringBuilder lorem = new StringBuilder();

        for (int i = 0; i < amount; i++) {
            if (word == 50) {
                word = 0;
            }

            lorem.append(loremIpsumWords[word]);

            if (i < amount - 1) {
                lorem.append(' ');
            }

            word++;
        }

        return lorem.toString();
    }

    /**
     * Returns two paragraphs of lorem ipsum.
     *
     * @return Lorem ipsum paragraphs
     */
    public static String getParagraphs() {
        return getParagraphs(2);
    }

    /**
     * Returns paragraphs of lorem ipsum.
     *
     * @param amount Amount of paragraphs
     * @return Lorem ipsum paragraphs
     */
    public static String getParagraphs(int amount) {
        StringBuilder lorem = new StringBuilder();

        for (int i = 0; i < amount; i++) {
            lorem.append(LOREM_IPSUM);

            if (i < amount - 1) {
                lorem.append("\n\n");
            }
        }

        return lorem.toString();
    }
}
