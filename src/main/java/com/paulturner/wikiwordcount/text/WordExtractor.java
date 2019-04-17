package com.paulturner.wikiwordcount.text;

import com.mongodb.annotations.NotThreadSafe;
import com.paulturner.wikiwordcount.collections.CircularCharArrayQueue;
import com.paulturner.wikiwordcount.domain.ChunkDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

@NotThreadSafe
public class WordExtractor implements Callable<ChunkDigest> {

    private static final Dictionary dictionary = new Dictionary();
    private static final Logger logger = LoggerFactory.getLogger(WordExtractor.class);

    private static final char[] XML_TAG_TEXT_OPEN = "<text" .toCharArray();
    private static final char[] XML_TAG_TEXT_CLOSE = "</text" .toCharArray();
    private static final char[] XML_TAG_TITLE_OPEN = "<title" .toCharArray();
    private static final char[] XML_TAG_TITLE_CLOSE = "</title" .toCharArray();
    private static final char[] XML_TAG_COMMENT_OPEN = "<comment" .toCharArray();
    private static final char[] XML_TAG_COMMENT_CLOSE = "</comment" .toCharArray();

    private final ByteBuffer byteBuffer;
    private final Map<String, Integer> wordCountMap = new HashMap<>();


    public WordExtractor(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    @Override
    public ChunkDigest call() {
        try {
            CharBuffer charBuffer = StandardCharsets.UTF_8.decode(byteBuffer);

            return extract(charBuffer);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            logger.info("Completed processing a subchunk, [size={}]", byteBuffer.limit());
        }

    }

    private void incrementCountForWord(String word) {
        if (dictionary.isDictionaryWord(word)) {
            wordCountMap.computeIfAbsent(word, k -> 0);
            wordCountMap.computeIfPresent(word, (k, v) -> (v + 1));
        }

        if (logger.isTraceEnabled() && (wordCountMap.size() % 10000 == 0)) {
            logger.trace("word count: [number of words={}]", wordCountMap.size());

        }
    }

    private ChunkDigest extract(CharBuffer charBuffer) {

        boolean startOfLine = true;


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

        ChunkDigest chunkDigest = new ChunkDigest(wordCountMap);

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
