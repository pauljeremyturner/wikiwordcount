package com.paulturner.wikiwordcount.cli;

import org.apache.commons.cli.*;

import java.util.Optional;

public class SelectCommandLineParser {

    private static final String OPTION_SORT_DIRECTION = "sort-direction";
    private static final String OPTION_HELP = "help";
    private static final String OPTION_MONGO_SERVER = "mongo";
    private static final String OPTION_COUNT = "count";
    private static final String OPTION_WORD_LENGTH = "word-length";
    private static final String OPTION_FILE_PATH = "source";


    public static Optional<SelectOptions> parse(String[] args) {
        final Options options = getOptions();
        final CommandLineParser defaultParser = new DefaultParser();
        try {
            final CommandLine commandLine = defaultParser.parse(options, args);


            if (commandLine.hasOption(OPTION_HELP)) {
                CommandLineParsers.printHelp(options);
                return Optional.empty();
            }

            String mongo;
            if (commandLine.hasOption(OPTION_MONGO_SERVER)) {
                mongo = commandLine.getOptionValue(OPTION_MONGO_SERVER);
            } else {
                mongo = "127.0.0.1:27017";
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

            return Optional.of(new SelectOptions(count, mongo, direction, wordLength, commandLine.getOptionValue(OPTION_FILE_PATH)));

        } catch (final ParseException exp) {
            CommandLineParsers.printUsage(options);
            return Optional.empty();
        }
    }

    private static Options getOptions() {
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
        options.addOption(wordLength);

        return options;
    }


}
