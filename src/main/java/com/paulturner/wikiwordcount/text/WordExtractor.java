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
import java.util.stream.IntStream;

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

    private static final ThreadLocal<char[][]> recycleCharArrayTlocal = ThreadLocal.withInitial(
            () -> {
                char[][] charArrayRecycle = new char[32][];
                IntStream.rangeClosed(1, 32).forEach(
                        i -> charArrayRecycle[i - 1] = new char[i]
                );
                return charArrayRecycle;
            }
    );

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

        final CircularCharArrayQueue tagQueue = new CircularCharArrayQueue(64);

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
                }
            }

            tagQueue.offer(c);


            if (isComment(tagQueue)) {
                processComment(charBuffer, tagQueue);
                startOfLine = true;
            } else if (isTitle(tagQueue)) {
                processTitle(charBuffer, tagQueue);
                startOfLine = true;
            } else if (isText(tagQueue)) {
                processText(charBuffer, tagQueue);
                startOfLine = true;
            }
        }

        ChunkDigest chunkDigest = new ChunkDigest(fileName, index, wordCountMap);

        return chunkDigest;

    }

    private void processUntil(final CharBuffer charBuffer, final CircularCharArrayQueue tagQueue, final char[] until) {
        ignoreToXmlCloseTag(charBuffer);

        if (!charBuffer.hasRemaining()) {
            return;
        }
        char c;
        boolean isCollectingWord = false;
        int wordLength = 0;
        do {
            c = charBuffer.get();
            tagQueue.offer(Character.toLowerCase(c));
            if (Character.isLetter(c)) {

                if (!isCollectingWord) {
                    wordLength = 1;
                    isCollectingWord = true;
                } else {
                    wordLength++;
                }

            } else if (isCollectingWord) {
                isCollectingWord = false;
                if (wordLength <= dictionary.longestWordLength()) {
                    incrementCountForWord(String.valueOf(tagQueue.subarray(wordLength, recycleCharArrayTlocal.get()[wordLength - 1])));
                }
                wordLength = 0;
            }
        } while (charBuffer.hasRemaining() && (!tagQueue.contains(until)));
        ignoreToXmlCloseTag(charBuffer);
    }

    private void processText(CharBuffer charBuffer, CircularCharArrayQueue tagQueue) {
        processUntil(charBuffer, tagQueue, XML_TAG_TEXT_CLOSE);

    }

    private boolean isText(CircularCharArrayQueue queue) {
        return queue.contains(XML_TAG_TEXT_OPEN);
    }

    private void processTitle(CharBuffer charBuffer, CircularCharArrayQueue tagQueue) {
        processUntil(charBuffer, tagQueue, XML_TAG_TITLE_CLOSE);
    }

    private boolean isTitle(CircularCharArrayQueue queue) {
        return queue.contains(XML_TAG_TITLE_OPEN);
    }

    private void processComment(CharBuffer charBuffer, CircularCharArrayQueue tagQueue) {
        processUntil(charBuffer, tagQueue, XML_TAG_COMMENT_CLOSE);
    }

    private boolean isComment(CircularCharArrayQueue queue) {
        return queue.contains(XML_TAG_COMMENT_OPEN);
    }

    private boolean isIndentation(char c) {
        return c == ' ' || c == '\t';
    }

    private void ignoreToXmlCloseTag(final CharBuffer charBuffer) {
        while (charBuffer.hasRemaining() && ((charBuffer.get()) != '>')) ;
    }


}
