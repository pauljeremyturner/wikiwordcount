package com.paulturner.wikiwordcount.cli;

import com.paulturner.wikiwordcount.test.TestFile;
import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class CalculateCommandLineParserTest {

    @Test
    public void shouldDefaultChunkSize() throws Exception {
        String[] args = new String[]{"calculate", "--source", TestFile.testDumpFilePath(), "--mongo", "127.0.0.1:27017"};

        Optional<CalculateOptions> calculateOptions = new CalculateCommandLineParser().parse(args);
        assertThat(calculateOptions.get().getChunkSize()).isEqualTo(1073741824);
    }

    @Test
    public void shouldExtractChunkSize() throws Exception {
        String[] args = new String[]{"calculate", "--source", TestFile.testDumpFilePath(), "--mongo", "127.0.0.1:27017", "--chunk-size", "512M"};

        Optional<CalculateOptions> calculateOptions = new CalculateCommandLineParser().parse(args);
        assertThat(calculateOptions.get().getChunkSize()).isEqualTo(536870912);
    }

    @Test
    public void shouldExtractFilePath() throws Exception {
        String[] args = new String[]{"calculate", "--source", TestFile.testDumpFilePath(), "--mongo", "127.0.0.1:27017", "--chunk-size", "1G"};
        Optional<CalculateOptions> calculateOptions = new CalculateCommandLineParser().parse(args);

        assertThat(calculateOptions.get().getFile().exists()).isTrue();
    }

    @Test
    public void shouldExtractMongo() throws Exception {
        String[] args = new String[]{"calculate", "--source", TestFile.testDumpFilePath(), "--mongo", "foohost:28018", "--chunk-size", "1G"};
        Optional<CalculateOptions> calculateOptions = new CalculateCommandLineParser().parse(args);

        assertThat(calculateOptions.get().getMongoClientUri()).isEqualTo("foohost:28018");
    }

    @Test
    public void shouldDefaultMongo() throws Exception {
        String[] args = new String[]{"calculate", "--source", TestFile.testDumpFilePath(), "--chunk-size", "1G"};
        Optional<CalculateOptions> calculateOptions = new CalculateCommandLineParser().parse(args);

        assertThat(calculateOptions.get().getMongoClientUri()).isEqualTo("127.0.0.1:27017");
    }

}