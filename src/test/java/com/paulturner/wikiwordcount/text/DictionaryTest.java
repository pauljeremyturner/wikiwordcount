package com.paulturner.wikiwordcount.text;

import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class DictionaryTest {

    private Dictionary dictionary;

    @Before
    public void before() {
        dictionary = new Dictionary();
    }

    @Test
    public void shouldIndicateItContainsAllWordsInDictionaryFile() throws Exception {
        // easier to read from classpath, but using a different mechanism than the class under test.
        String fileName = new File(".").getAbsolutePath() + String.format("%ssrc%smain%sresources%sdictionary.txt", File.separator, File.separator, File.separator, File.separator);
        try (BufferedReader br = Files.newBufferedReader(Paths.get(fileName))) {
            br.lines().forEach(w ->
                    assertThat(dictionary.isDictionaryWord(w)).isTrue()
            );
        }
    }

    @Test
    public void shouldNotIndicateItContainsNonDictionaryWords() {
        assertThat(dictionary.isDictionaryWord("spam")).isFalse();
    }


}