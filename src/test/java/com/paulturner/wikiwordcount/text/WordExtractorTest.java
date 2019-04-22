package com.paulturner.wikiwordcount.text;

import com.paulturner.wikiwordcount.mongoentity.ChunkDigest;
import org.junit.Test;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

public class WordExtractorTest {


    @Test
    public void shouldExtractWordsFromSimplePage() throws Exception {

        ByteBuffer byteBuffer = classpathFileToByteBuffer("/synthetic-page.txt");

        WordExtractor wordExtractor = new WordExtractor(byteBuffer, "", 0);

        ChunkDigest chunkDigest = wordExtractor.extract();

        assertThat(chunkDigest.getWordCountMap().get("and")).isEqualTo(8);
        assertThat(chunkDigest.getWordCountMap().get("you")).isEqualTo(3);
        assertThat(chunkDigest.getWordCountMap().get("pestilent")).isEqualTo(1);

    }


    @Test
    public void shouldExtractWordsFromTitle() throws Exception {

        ByteBuffer byteBuffer = classpathFileToByteBuffer("/title-only.txt");

        WordExtractor wordExtractor = new WordExtractor(byteBuffer, "", 0);

        ChunkDigest chunkDigest = wordExtractor.extract();

        assertThat(chunkDigest.getWordCountMap().get("negative")).isEqualTo(2);
        assertThat(chunkDigest.getWordCountMap().get("surface")).isEqualTo(1);
        assertThat(chunkDigest.getWordCountMap().get("the")).isEqualTo(1);

    }

    @Test
    public void shouldExtractWordsFromComment() throws Exception {

        ByteBuffer byteBuffer = classpathFileToByteBuffer("/comment-only.txt");

        WordExtractor wordExtractor = new WordExtractor(byteBuffer, "", 0);

        ChunkDigest chunkDigest = wordExtractor.extract();

        assertThat(chunkDigest.getWordCountMap().get("twelve")).isEqualTo(1);
        assertThat(chunkDigest.getWordCountMap().get("imperial")).isEqualTo(1);
        assertThat(chunkDigest.getWordCountMap().get("the")).isEqualTo(4);

    }

    @Test
    public void shouldProcessRealPagesOk() throws Exception {
        //yes i could have taken the opportunity to check the real word counts are correct but that would take too much time.

        //just checking here that no exceptions are thrown.  I have checked the counts using synthetic pages (pages i wrote myself)

        ByteBuffer byteBuffer = classpathFileToByteBuffer("/whole-page.txt");

        WordExtractor wordExtractor = new WordExtractor(byteBuffer, "", 0);

        ChunkDigest chunkDigest = wordExtractor.extract();
        assertThat(chunkDigest.getWordCountMap().size()).isGreaterThan(0);
    }


    @Test
    public void shouldProcessProblemPage2Ok() throws Exception {

        ByteBuffer byteBuffer = classpathFileToByteBuffer("/problem-page-2.txt");

        WordExtractor wordExtractor = new WordExtractor(byteBuffer, "", 0);

        ChunkDigest chunkDigest = wordExtractor.extract();
        assertThat(chunkDigest.getWordCountMap().size()).isGreaterThan(0);
    }


    @Test
    public void shouldProcessProblemPage1Ok() throws Exception {

        ByteBuffer byteBuffer = classpathFileToByteBuffer("/problem-page-1.txt");

        WordExtractor wordExtractor = new WordExtractor(byteBuffer, "", 0);

        ChunkDigest chunkDigest = wordExtractor.extract();
        assertThat(chunkDigest.getWordCountMap().size()).isGreaterThan(0);
    }

    @Test
    public void shouldProcessProblemPage3Ok() throws Exception {

        ByteBuffer byteBuffer = classpathFileToByteBuffer("/problem-page-3.txt");

        WordExtractor wordExtractor = new WordExtractor(byteBuffer, "", 0);

        ChunkDigest chunkDigest = wordExtractor.extract();
        assertThat(chunkDigest.getWordCountMap().size()).isGreaterThan(0);
    }

    @Test
    public void shouldProcessProblemPage4Ok() throws Exception {

        ByteBuffer byteBuffer = classpathFileToByteBuffer("/problem-page-4.txt");

        WordExtractor wordExtractor = new WordExtractor(byteBuffer, "", 0);

        ChunkDigest chunkDigest = wordExtractor.extract();
        assertThat(chunkDigest.getWordCountMap().size()).isGreaterThan(0);
    }


    private ByteBuffer classpathFileToByteBuffer(String filename) throws Exception {
        File file = new File(getClass().getResource(filename).getFile());
        return ByteBuffer.wrap(Files.readAllBytes(file.toPath()));

    }

}