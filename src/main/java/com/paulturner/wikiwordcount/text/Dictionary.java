package com.paulturner.wikiwordcount.text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Dictionary {

    private static final Set<String> dictionaryWords;
    private static final Logger logger = LoggerFactory.getLogger(Dictionary.class);

    static {
        final Set<String> tmpWords = new HashSet<>();
        try (
                InputStream is = Dictionary.class.getResourceAsStream("/dictionary.txt");
                InputStreamReader inputStreamReader = new InputStreamReader(is);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader)
        ) {
            String word;
            while (Objects.nonNull(word = bufferedReader.readLine())) {
                tmpWords.add(word.trim().toLowerCase());
            }
            dictionaryWords = Collections.unmodifiableSet(tmpWords);
            logger.info("Dictionary Loaded [wordcount={}]", dictionaryWords.size());
        } catch (final IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    public boolean isDictionaryWord(final String word) {
        return dictionaryWords.contains(word);
    }

    public int wordCount() {
        return dictionaryWords.size();
    }

}
