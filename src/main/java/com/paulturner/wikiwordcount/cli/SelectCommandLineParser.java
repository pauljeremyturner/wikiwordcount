package com.paulturner.wikiwordcount.cli;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class SelectCommandLineParser extends AbstractCommandLineParser {

    private static final Logger logger = LoggerFactory.getLogger(SelectCommandLineParser.class);

    private static final String OPTION_SORT_DIRECTION = "sort-direction";
    private static final String OPTION_HELP = "help";
    private static final String OPTION_MONGO_SERVER = "mongo";
    private static final String OPTION_COUNT = "count";
    private static final String OPTION_WORD_LENGTH = "word-length";
    private static final String OPTION_FILE_PATH = "source";
    private static final String OPTION_CHUNK_SIZE = "chunk-size";

    public static SelectCommandLineParser getInstance() {
        return new SelectCommandLineParser();
    }

    public Optional<SelectOptions> parse(String[] args) {
        final Options options = getOptions();
        final CommandLineParser defaultParser = new DefaultParser();
        try {
            final CommandLine commandLine = defaultParser.parse(options, args);

            if (commandLine.hasOption(OPTION_HELP)) {
                CommandLineParsers.printHelp(options);
                return Optional.empty();
            }


            SelectOptions.Direction direction;
            if (commandLine.hasOption(OPTION_SORT_DIRECTION)) {
                direction = SelectOptions.Direction.valueOf(commandLine.getOptionValue(OPTION_SORT_DIRECTION));
            } else {
                direction = SelectOptions.Direction.DESC;
            }

            int count;
            if (commandLine.hasOption(OPTION_SORT_DIRECTION)) {
                count = Integer.parseInt(commandLine.getOptionValue(OPTION_COUNT));
            } else {
                count = 20;
            }

            int wordLength;
            if (commandLine.hasOption(OPTION_WORD_LENGTH)) {
                wordLength = Integer.parseInt(commandLine.getOptionValue(OPTION_WORD_LENGTH));
            } else {
                wordLength = -1;
            }


            final Optional<String> mongoOptional = Optional.ofNullable(commandLine.getOptionValue(OPTION_MONGO_SERVER));
            final Optional<String> chunkOptional = Optional.ofNullable(commandLine.getOptionValue(OPTION_CHUNK_SIZE));
            final String fileName = commandLine.getOptionValue(OPTION_FILE_PATH);

            return Optional.of(
                    SelectOptions.builder()
                            .withFile(getDumpFilePath(fileName))
                            .withChunkSize(getChunkSizeBytes(chunkOptional))
                            .withMongoClientUri(getMongoClientURI(mongoOptional))
                            .withWordLength(wordLength)
                            .withCount(count)
                            .withDirection(direction)
                            .build()
            );

        } catch (final ParseException exp) {
            CommandLineParsers.printUsage(options);
            return Optional.empty();
        }
    }


    private Options getOptions() {

        Option chunkSize = Option.builder()
                .required(false)
                .argName("size")
                .desc("ChunkSize in bytes to run")
                .longOpt(OPTION_CHUNK_SIZE)
                .hasArg()
                .build();

        Option sortDirection = Option.builder()
                .required(false)
                .hasArg(true)
                .longOpt(OPTION_SORT_DIRECTION)
                .desc("Whether to get the most or least popular words")
                .build();

        Option mongoServer = Option.builder()
                .required(false)
                .argName("server")
                .desc("MongoDb host and port")
                .longOpt(OPTION_MONGO_SERVER)
                .hasArg()
                .build();

        Option count = Option.builder()
                .required(false)
                .argName("count")
                .desc("Number of word counts to select")
                .longOpt(OPTION_COUNT)
                .hasArg()
                .build();

        Option wordLength = Option.builder()
                .required(false)
                .argName("word-length")
                .desc("Length or words to consider")
                .longOpt(OPTION_WORD_LENGTH)
                .hasArg()
                .build();


        Option filePath = Option.builder()
                .required(true)
                .argName("file")
                .desc("Wikipedia dump file path")
                .longOpt(OPTION_FILE_PATH)
                .hasArg()
                .build();

        final Options options = new Options();
        options.addOption(mongoServer);
        options.addOption(filePath);
        options.addOption(count);
        options.addOption(sortDirection);
        options.addOption(chunkSize);
        options.addOption(wordLength);

        return options;
    }


}
