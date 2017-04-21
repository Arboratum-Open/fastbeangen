package com.arboratum.beangen.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class LoremIpsumTest {

    @Test
    public void testWords() {
        String words = LoremIpsum.getWords( 1 );
        assertThat( words, is( "Lorem" ) );

        words = LoremIpsum.getWords( 25 );
        assertThat( words, is( "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At" ) );

        words = LoremIpsum.getWords( 50 );
        assertThat( words, is( "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet." ) );

        words = LoremIpsum.getWords( 10, 2 );
        assertThat( words, is( "dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod" ) );
    }

    @Test( expected = IndexOutOfBoundsException.class )
    public void testWordsExceptionBelow() {
        LoremIpsum.getWords( 50, -1 );
    }

    @Test( expected = IndexOutOfBoundsException.class )
    public void testWordsExceptionAbove() {
        LoremIpsum.getWords( 50, 50 );
    }

    @Test
    public void testParagraphs() {
        String paragraphs = LoremIpsum.getParagraphs( 2 );
        assertThat( paragraphs, is( "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.\n\nLorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet." ) );
    }
}