package com.paulturner.wikiwordcount.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AbstractCommandLineParser {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCommandLineParser.class);

    private static final int GIGA = 1 << 30;
    private static final int MEGA = 1 << 20;
    private static final int KILO = 1 << 10;

    static int getChunkSizeBytes(Optional<String> chunkSizeOpt) {
        return chunkSizeOpt.map(cs -> cliArgToBytes(cs)).orElse(GIGA);
    }

    private static int cliArgToBytes(String chunkSize) {
        String pattern = "(\\d+)([B,K,M,G])";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(chunkSize);
        if (!m.find()) {
            throw new IllegalArgumentException("malformed chunksize argument");
        }

        int chunkSizeInBytes;
        int size = Integer.parseInt(m.group(1));
        Character unit = m.group(2).charAt(0);
        switch (unit) {
            case 'G':
                if (size > 1) {
                    logger.warn("Configured chunksize too big to be 32-bit addressable, defaulting to 1GB");
                }
                chunkSizeInBytes = Math.min(1, size) * GIGA;
                break;
            case 'M':
                chunkSizeInBytes = size * MEGA;
                break;
            case 'K':
                chunkSizeInBytes = size * KILO;
                break;
            case 'B':
                chunkSizeInBytes = size;
                break;
            default:
                throw new IllegalArgumentException("malformed chunksize argument");
        }

        if (chunkSizeInBytes < (128 * MEGA)) {
            logger.warn("Configured chunksize too small with respect to common size of pages, defaulting to 128MB");
            chunkSizeInBytes = 128 * MEGA;
        }

        return chunkSizeInBytes;
    }

    File getDumpFilePath(final String filename) {
        File file = new File(filename);

        if (!file.exists() || file.isDirectory() || !file.canRead()) {
            throw new IllegalArgumentException(
                    String.format("Unprocessable file: [exists=%s] [readable=%s] [is a file=%s] [filename=%s]", file.exists(), file.canRead(), file.isFile(), filename)
            );
        }

        return file;
    }

    String getMongoClientURI(Optional<String> mongoUriOpt) {

        return mongoUriOpt.orElse("127.0.0.1:27017");

    }


}
