package com.paulturner.wikiwordcount.text;

import com.mongodb.annotations.NotThreadSafe;
import com.paulturner.wikiwordcount.domain.ChunkDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

@NotThreadSafe
public class WordExtractor implements Callable<ChunkDigest> {

    private static final Dictionary dictionary = new Dictionary();
    private static final Logger logger = LoggerFactory.getLogger(WordExtractor.class);

    private static final char[] XML_TAG_REVISION = "<revision" .toCharArray();
    private static final char[] XML_TAG_PAGE = "<page" .toCharArray();
    private static final char[] XML_TAG_TEXT = "<text" .toCharArray();
    private static final char[] XML_TAG_TITLE_OPEN = "<title>" .toCharArray();
    private static final char[] XML_TAG_FORMAT = "<format" .toCharArray();
    private static final char[] XML_TAG_MODEL = "<model>" .toCharArray();
    private static final char[] XML_TAG_NS = "<ns" .toCharArray();
    private static final char[] XML_TAG_CONTRIBUTOR_OPEN = "<contributor" .toCharArray();
    private static final char[] XML_TAG_CONTRIBUTOR_CLOSE = "</contributor" .toCharArray();
    private static final char[] XML_TAG_ID = "<id>" .toCharArray();
    private static final char[] XML_TAG_SHA1 = "<sha1>" .toCharArray();
    private static final char[] XML_TAG_REDIRECT = "<redirect" .toCharArray();
    private static final char[] XML_TAG_PARENTID = "<parentid" .toCharArray();
    private static final char[] XML_TAG_MINOR = "<minor" .toCharArray();
    private static final char[] XML_TAG_TIMESTAMP = "<timestamp" .toCharArray();
    private static final char[] XML_TAG_USERNAME = "<username" .toCharArray();
    private static final char[] XML_TAG_COMMENT_OPEN = "<comment>" .toCharArray();

    private final ByteBuffer byteBuffer;
    private final Map<String, Integer> wordCountMap = new HashMap<>();


    public WordExtractor(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    @Override
    public ChunkDigest call() {

        CharBuffer charBuffer = StandardCharsets.UTF_8.decode(byteBuffer);

        return extract(charBuffer);
    }


    public ChunkDigest extract(CharBuffer charBuffer) {


        boolean startOfLine = true;


        char[] line = new char[4096];
        int pos = 0;
        while (charBuffer.hasRemaining()) {
            char c = charBuffer.get();


            if (startOfLine) {
                boolean indentation = isIndentation(c);
                if (indentation) {
                    continue;
                } else {
                    startOfLine = false;
                    pos = 0;
                }
            }

            line[pos++] = c;

            if (isIgnoreLine(line)) {
                ignoreToEndOfLine(charBuffer);
                startOfLine = true;
                Arrays.fill(line, (char) 0);
                pos = 0;
                continue;
            } else if (isComment(line)) {
                pos = 0;
                processComment(charBuffer, line);
                startOfLine = true;
                Arrays.fill(line, (char) 0);
            } else if (isTitle(line)) {
                processTitle(charBuffer, line);
                startOfLine = true;
                Arrays.fill(line, (char) 0);
                pos = 0;
            } else if (isText(line)) {
                processText(charBuffer, line);
            }
        }

        ChunkDigest chunkDigest = new ChunkDigest(wordCountMap);

        return chunkDigest;

    }

    private void processText(CharBuffer charBuffer, char[] line) {
        ignoreToXmlCloseTag(charBuffer);

        char c;
        Arrays.fill(line, (char) 0);
        int pos = 0;
        do {
            c = charBuffer.get();
            if (Character.isLetter(c)) {
                line[pos] = Character.toLowerCase(c);
                pos++;
            } else if (pos > 0) {
                incrementCountForWord(String.valueOf(Arrays.copyOf(line, pos)));
                Arrays.fill(line, (char) 0);
                pos = 0;
            }

            if ('\n' == c) {
                pos = 0;
                Arrays.fill(line, (char) 0);
            }
        } while (charBuffer.hasRemaining() && (c != '<'));
        ignoreToEndOfLine(charBuffer);
    }

    private boolean isText(char[] line) {
        return lineStartsWith(line, XML_TAG_TEXT);
    }

    private void processTitle(CharBuffer charBuffer, char[] line) {

        logger.debug("Processing a page");

        char c;
        Arrays.fill(line, (char) 0);
        int pos = 0;
        do {
            c = charBuffer.get();
            if (Character.isLetter(c)) {
                line[pos] = Character.toLowerCase(c);
                pos++;
            } else if (pos > 0) {
                incrementCountForWord(String.valueOf(Arrays.copyOf(line, pos)));
                Arrays.fill(line, (char) 0);
                pos = 0;
            }
        } while (charBuffer.hasRemaining() && (c != '<'));
        ignoreToEndOfLine(charBuffer);
    }

    private boolean isTitle(char[] line) {
        return lineStartsWith(line, XML_TAG_TITLE_OPEN);
    }

    private void processComment(CharBuffer charBuffer, char[] line) {
        char c;
        int pos = 0;
        while (charBuffer.hasRemaining() && ((c = charBuffer.get()) != '<')) {
            if (Character.isLetter(c)) {
                line[pos] = Character.toLowerCase(c);
                pos++;
            } else if (pos > 0) {
                incrementCountForWord(String.valueOf(Arrays.copyOf(line, pos)));
                pos = 0;
            }
        }
        ignoreToEndOfLine(charBuffer);
    }


    private boolean isComment(char[] line) {
        return lineStartsWith(line, XML_TAG_COMMENT_OPEN);
    }

    private boolean isIgnoreLine(char[] line) {
        return lineStartsWith(line, XML_TAG_FORMAT) ||
                lineStartsWith(line, XML_TAG_MODEL) ||
                lineStartsWith(line, XML_TAG_ID) ||
                lineStartsWith(line, XML_TAG_NS) ||
                lineStartsWith(line, XML_TAG_REVISION) ||
                lineStartsWith(line, XML_TAG_CONTRIBUTOR_OPEN) ||
                lineStartsWith(line, XML_TAG_CONTRIBUTOR_CLOSE) ||
                lineStartsWith(line, XML_TAG_PAGE) ||
                lineStartsWith(line, XML_TAG_SHA1) ||
                lineStartsWith(line, XML_TAG_REDIRECT) ||
                lineStartsWith(line, XML_TAG_PARENTID) ||
                lineStartsWith(line, XML_TAG_MINOR) ||
                lineStartsWith(line, XML_TAG_TIMESTAMP) ||
                lineStartsWith(line, XML_TAG_USERNAME);

    }

    private boolean isIndentation(char c) {
        return c == ' ' || c == '\t';
    }


    private void ignoreToEndOfLine(final CharBuffer charBuffer) {
        while (charBuffer.hasRemaining() && ((charBuffer.get()) != '\n')) ;
    }

    private void ignoreToXmlCloseTag(final CharBuffer charBuffer) {
        while (charBuffer.hasRemaining() && ((charBuffer.get()) != '>')) ;
    }


    private boolean lineStartsWith(char[] line, char[] chars) {
        return Arrays.equals(chars, Arrays.copyOfRange(line, 0, chars.length));
    }

    private void incrementCountForWord(String word) {

        if (dictionary.isDictionaryWord(word)) {
            Integer present;
            if (Objects.isNull(present = wordCountMap.get(word))) {
                present = 0;
            }
            wordCountMap.put(word, present + 1);
        }
    }


}
