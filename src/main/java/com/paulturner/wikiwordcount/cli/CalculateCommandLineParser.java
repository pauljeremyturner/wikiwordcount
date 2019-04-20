package com.paulturner.wikiwordcount.cli;


import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class CalculateCommandLineParser extends AbstractCommandLineParser {

    private static final Logger logger = LoggerFactory.getLogger(CalculateCommandLineParser.class);

    private static final String OPTION_OFF_HEAP = "off-heap";
    private static final String OPTION_FILE_PATH = "source";
    private static final String OPTION_MONGO_SERVER = "mongo";
    private static final String OPTION_CHUNK_SIZE = "chunk-size";
    private static final String OPTION_HELP = "help";

    public static CalculateCommandLineParser getInstance() {
        return new CalculateCommandLineParser();
    }


    public Optional<CalculateOptions> parse(String[] args) {
        final Options options = getOptions();
        final CommandLineParser defaultParser = new DefaultParser();
        try {
            final CommandLine commandLine = defaultParser.parse(options, args);


            if (commandLine.hasOption(OPTION_HELP)) {
                CommandLineParsers.printHelp(options);
                return Optional.empty();
            }


            final Optional<String> mongoOptional = Optional.ofNullable(commandLine.getOptionValue(OPTION_MONGO_SERVER));
            final Optional<String> chunkOptional = Optional.ofNullable(commandLine.getOptionValue(OPTION_CHUNK_SIZE));
            final String fileName = commandLine.getOptionValue(OPTION_FILE_PATH);

            return Optional.of(CalculateOptions.builder()
                    .withFile(getDumpFilePath(fileName))
                    .withChunkSize(getChunkSizeBytes(chunkOptional))
                    .withMongoClientUri(getMongoClientURI(mongoOptional))
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


}
