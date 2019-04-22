package com.paulturner.wikiwordcount.cli;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class CommandLineParsers {

    private static final Logger logger = LoggerFactory.getLogger(CommandLineParsers.class);

    private CommandLineParsers() {
    }

    static void printHelp(final Options options) {
        final HelpFormatter formatter = new HelpFormatter();
        final String syntax = "java -jar wikiwordcount.jar [calculate|select]";

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

    static void printUsage(final Options options) {
        final HelpFormatter formatter = new HelpFormatter();

        final String syntax = "java -jar wikiwordcount.jar [calculate|select]";
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
}
