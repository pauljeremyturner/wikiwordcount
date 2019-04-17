package com.paulturner.wikiwordcount.cli;


import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandLineOptionsExtractor {

    private static final Logger logger = LoggerFactory.getLogger(CommandLineOptionsExtractor.class);

    private static final String OPTION_OFF_HEAP = "off-heap";
    private static final String OPTION_FILE_PATH = "file-path";
    private static final String OPTION_MONGO_SERVER = "mongo-server";
    private static final String OPTION_CHUNK_SIZE = "chunk-size";
    private static final String OPTION_HELP = "help";

    private static final int GIGA = 1 << 30;
    private static final int MEGA = 1 << 20;
    private static final int KILO = 1 << 10;


    public static Optional<RuntimeOptions> parse(String[] args) {
        final Options options = getOptions();
        final CommandLineParser defaultParser = new DefaultParser();
        try {
            final CommandLine commandLine = defaultParser.parse(options, args);


            if (commandLine.hasOption(OPTION_HELP)) {
                printHelp(options);
                return Optional.empty();
            }


            final Optional<String> mongoOptional = Optional.ofNullable(commandLine.getOptionValue(OPTION_MONGO_SERVER));
            final Optional<String> chunkOptional = Optional.ofNullable(commandLine.getOptionValue(OPTION_CHUNK_SIZE));
            final String fileName = commandLine.getOptionValue(OPTION_FILE_PATH);

            return Optional.of(new RuntimeOptions.Builder()
                    .withFile(getDumpFilePath(fileName))
                    .withChunkSize(getChunkSizeBytes(chunkOptional))
                    .withMongoClientUri(getMongoClientURI(mongoOptional))
                    .build()
            );

        } catch (final ParseException exp) {
            printUsage(options);
            return Optional.empty();
        }
    }

    private static void printUsage(final Options options) {
        final HelpFormatter formatter = new HelpFormatter();

        final String syntax = "java -jar wikiwordcount.jar";
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintWriter pw = new PrintWriter(baos);

        formatter.printUsage(pw, 80, syntax, options);
        pw.flush();
        try {
            logger.info(baos.toString(StandardCharsets.UTF_8.name()));
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private static void printHelp(final Options options) {
        final HelpFormatter formatter = new HelpFormatter();
        final String syntax = "java -jar wikiwordcount.jar";

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintWriter pw = new PrintWriter(baos);
        formatter.printHelp(pw, 80, syntax, "Wikipedia Wordcount", options, 0, 10, "Wikipedia Wordcount", true);
        pw.flush();
        try {
            logger.info(baos.toString(StandardCharsets.UTF_8.name()));
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private static Options getOptions() {


        Option chunkSize = Option.builder()
                .required(false)
                .argName("size")
                .desc("ChunkSize in bytes to run")
                .longOpt(OPTION_CHUNK_SIZE)
                .hasArg()
                .build();

        Option mongoServer = Option.builder()
                .required(false)
                .argName("server")
                .desc("MongoDb host and port")
                .longOpt(OPTION_MONGO_SERVER)
                .hasArg()
                .build();

        Option filePath = Option.builder()
                .required(true)
                .argName("file")
                .desc("Wikipedia dump file path")
                .longOpt(OPTION_FILE_PATH)
                .hasArg()
                .build();

        Option directBuffers = Option.builder()
                .required(false)
                .hasArg(false)
                .longOpt(OPTION_OFF_HEAP)
                .desc("Whether to use off-heap buffering or not")
                .build();


        final Options options = new Options();
        options.addOption(filePath);
        options.addOption(chunkSize);
        options.addOption(mongoServer);
        options.addOption(directBuffers);

        return options;
    }

    private static File getDumpFilePath(final String filename) {
        File file = new File(filename);

        if (!file.exists() || file.isDirectory() || !file.canRead()) {
            throw new IllegalArgumentException(
                    String.format("Unprocessable file: [exists=%s] [readable=%s] [is a file=%s] [filename=%s]", file.exists(), file.canRead(), file.isFile(), filename)
            );
        }

        return file;
    }

    private static String getMongoClientURI(Optional<String> mongoUriOpt) {

        return mongoUriOpt.get();


    }

    private static int getChunkSizeBytes(Optional<String> chunkSizeOpt) {
        return chunkSizeOpt.map(cs -> cliArgToBytes(cs)).orElse(512 * MEGA);
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


}
