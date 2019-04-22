package com.paulturner.wikiwordcount.text;

import com.mongodb.annotations.NotThreadSafe;
import com.paulturner.wikiwordcount.collections.CircularCharArrayQueue;
import com.paulturner.wikiwordcount.mongoentity.ChunkDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@NotThreadSafe
public class WordExtractor {

    private static final Dictionary dictionary = new Dictionary();
    private static final Logger logger = LoggerFactory.getLogger(WordExtractor.class);

    private static final char[] XML_TAG_TEXT_OPEN = "<text".toCharArray();
    private static final char[] XML_TAG_TEXT_CLOSE = "</text".toCharArray();
    private static final char[] XML_TAG_TITLE_OPEN = "<title".toCharArray();
    private static final char[] XML_TAG_TITLE_CLOSE = "</title".toCharArray();
    private static final char[] XML_TAG_COMMENT_OPEN = "<comment".toCharArray();
    private static final char[] XML_TAG_COMMENT_CLOSE = "</comment".toCharArray();

    private final ByteBuffer byteBuffer;
    private final Map<String, Integer> wordCountMap = new HashMap<>();
    private int index;
    private String fileName;


    public WordExtractor(final ByteBuffer byteBuffer, String fileName, int index) {
        this.byteBuffer = byteBuffer;
        this.index = index;
        this.fileName = fileName;
    }

    public ChunkDigest extract() {

        CharBuffer charBuffer = StandardCharsets.UTF_8.decode(byteBuffer);
        return doExtract(charBuffer, index);
    }

    private void incrementCountForWord(String word) {
        if (dictionary.isDictionaryWord(word)) {
            wordCountMap.computeIfPresent(word, (k, v) -> (v + 1));
            wordCountMap.computeIfAbsent(word, k -> 1);
        }
    }

    private ChunkDigest doExtract(CharBuffer charBuffer, int index) {

        boolean startOfLine = true;

        //todo: no need for this circular q for the line, just for the tags - refactor to use the char buffer instead
        CircularCharArrayQueue lineQueue = new CircularCharArrayQueue(4096);
        CircularCharArrayQueue tagQueue = new CircularCharArrayQueue(20);

        while (charBuffer.hasRemaining()) {
            char c = charBuffer.get();

            if ('\n' == c) {
                startOfLine = true;
                continue;
            }

            if (startOfLine) {
                boolean indentation = isIndentation(c);
                if (indentation) {
                    continue;
                } else {
                    startOfLine = false;
                    lineQueue.clear();
                }
            }

            lineQueue.offer(c);

            if (isComment(lineQueue)) {
                processComment(charBuffer, lineQueue, tagQueue);
                startOfLine = true;
            } else if (isTitle(lineQueue)) {
                processTitle(charBuffer, lineQueue, tagQueue);
                startOfLine = true;
            } else if (isText(lineQueue)) {
                processText(charBuffer, lineQueue, tagQueue);
                startOfLine = true;
            }
        }

        ChunkDigest chunkDigest = new ChunkDigest(fileName, index, wordCountMap);

        return chunkDigest;

    }

    private void processUntil(CharBuffer charBuffer, CircularCharArrayQueue lineQueue, CircularCharArrayQueue tagQueue, char[] until) {
        ignoreToXmlCloseTag(charBuffer);

        char c;
        lineQueue.clear();
        tagQueue.clear();
        do {
            c = charBuffer.get();
            if ('<' == c) {
                tagQueue.clear();
            }
            tagQueue.offer(c);
            if (Character.isLetter(c)) {
                lineQueue.offer(Character.toLowerCase(c));
            } else if (!lineQueue.isEmpty()) {

                if (logger.isTraceEnabled()) {
                    logger.trace("word [{}]", String.valueOf(lineQueue.subArrayToPosition()));
                }
                incrementCountForWord(String.valueOf(lineQueue.subArrayToPosition()));
                lineQueue.clear();
            }
        } while (charBuffer.hasRemaining() && (!tagQueue.containsArray(until)));
        ignoreToXmlCloseTag(charBuffer);

    }

    private void processText(CharBuffer charBuffer, CircularCharArrayQueue lineQueue, CircularCharArrayQueue tagQueue) {
        processUntil(charBuffer, lineQueue, tagQueue, XML_TAG_TEXT_CLOSE);

    }

    private boolean isText(CircularCharArrayQueue queue) {
        return queue.containsArray(XML_TAG_TEXT_OPEN);
    }

    private void processTitle(CharBuffer charBuffer, CircularCharArrayQueue lineQueue, CircularCharArrayQueue tagQueue) {
        processUntil(charBuffer, lineQueue, tagQueue, XML_TAG_TITLE_CLOSE);
    }

    private boolean isTitle(CircularCharArrayQueue queue) {
        return queue.containsArray(XML_TAG_TITLE_OPEN);
    }

    private void processComment(CharBuffer charBuffer, CircularCharArrayQueue lineQueue, CircularCharArrayQueue tagQueue) {
        processUntil(charBuffer, lineQueue, tagQueue, XML_TAG_COMMENT_CLOSE);
    }

    private boolean isComment(CircularCharArrayQueue queue) {
        return queue.containsArray(XML_TAG_COMMENT_OPEN);
    }

    private boolean isIndentation(char c) {
        return c == ' ' || c == '\t';
    }

    private void ignoreToXmlCloseTag(final CharBuffer charBuffer) {
        while (charBuffer.hasRemaining() && ((charBuffer.get()) != '>')) ;
    }


}
