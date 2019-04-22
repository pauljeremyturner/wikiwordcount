package com.paulturner.wikiwordcount;

import com.mongodb.ServerAddress;
import com.paulturner.wikiwordcount.cli.CalculateCommandLineParser;
import com.paulturner.wikiwordcount.cli.CalculateOptions;
import com.paulturner.wikiwordcount.cli.SelectCommandLineParser;
import com.paulturner.wikiwordcount.cli.SelectOptions;
import com.paulturner.wikiwordcount.command.CalculateCommand;
import com.paulturner.wikiwordcount.command.SelectCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;

import static com.paulturner.wikiwordcount.Application.Mode.CALCULATE;
import static com.paulturner.wikiwordcount.Application.Mode.SELECT;

@SpringBootApplication
public class Application implements CommandLineRunner, ApplicationContextAware {

    private static final String CALCULATE_OPTION = "calculate";
    private static final String SELECT_OPTION = "select";
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    private static SelectOptions selectOptions;
    private static CalculateOptions calculateOptions;
    private static ServerAddress serverAddress;
    private static Mode mode;
    private ApplicationContext applicationContext;

    @Autowired
    private CalculateCommand calculateCommand;

    @Autowired
    private SelectCommand selectCommand;

    public static void main(final String[] args) {

        if (args.length == 0) {
            logger.error("Need to specify calculate or select mode");
            System.exit(1);
        }

        logger.info("Command line: [args={}]", Arrays.asList(args));


        final String modeString = args[0];

        final String[] options = Arrays.copyOfRange(args, 1, args.length);

        if (CALCULATE_OPTION.equals(modeString)) {
            calculateOptions = CalculateCommandLineParser.getInstance().parse(options).orElseThrow(() -> new IllegalStateException("Could not parse command line options"));
            mode = Mode.CALCULATE;
            String[] hostTokens = calculateOptions.getMongoClientUri().split(":");
            serverAddress = new ServerAddress(hostTokens[0], Integer.parseInt(hostTokens[1]));
            selectOptions = SelectOptions.builder().withChunkSize(calculateOptions.getChunkSize()).withMongoClientUri(calculateOptions.getMongoClientUri()).build();
        } else if (SELECT_OPTION.equals(modeString)) {
            mode = Mode.SELECT;
            selectOptions = SelectCommandLineParser.getInstance().parse(options).orElseThrow(() -> new IllegalStateException("Could not parse command line options"));
            String[] hostTokens = selectOptions.getMongoClientUri().split(":");
            serverAddress = new ServerAddress(hostTokens[0], Integer.parseInt(hostTokens[1]));
            calculateOptions = CalculateOptions.builder().withMongoClientUri(selectOptions.getMongoClientUri()).withFile(selectOptions.getFile()).build();
        }


        logger.info("Processing command line options for [mode={}]", mode);
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CalculateOptions calculateOptions() {
        return calculateOptions;
    }

    @Bean
    public SelectOptions selectOptions() {
        return selectOptions;
    }

    @Bean
    public ServerAddress serverAddress() {
        return serverAddress;
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(final String... args) {

        if (SELECT == mode) {
            selectCommand.processChunks();
        } else if (CALCULATE == mode) {
            calculateCommand.process();
        }

    }

    enum Mode {CALCULATE, SELECT}

}
