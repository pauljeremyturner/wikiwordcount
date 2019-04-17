package com.paulturner.wikiwordcount.incubator;

import com.paulturner.wikiwordcount.domain.ChunkDigest;
import com.paulturner.wikiwordcount.text.WordExtractor;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;

public class WordExtractorTest {


    @Test
    public void shouldExtractWordsFromSimplePage() throws Exception {

        ByteBuffer byteBuffer = classpathFileToByteBuffer("/synthetic-page.txt");

        WordExtractor wordExtractor = new WordExtractor(byteBuffer);

        ChunkDigest chunkDigest = wordExtractor.call();

        Assertions.assertThat(chunkDigest.wordCount("and")).isEqualTo(8);
        Assertions.assertThat(chunkDigest.wordCount("you")).isEqualTo(3);
        Assertions.assertThat(chunkDigest.wordCount("pestilent")).isEqualTo(1);

    }


    @Test
    public void shouldExtractWordsFromTitle() throws Exception {

        ByteBuffer byteBuffer = classpathFileToByteBuffer("/title-only.txt");

        WordExtractor wordExtractor = new WordExtractor(byteBuffer);

        ChunkDigest chunkDigest = wordExtractor.call();

        Assertions.assertThat(chunkDigest.wordCount("negative")).isEqualTo(2);
        Assertions.assertThat(chunkDigest.wordCount("surface")).isEqualTo(1);
        Assertions.assertThat(chunkDigest.wordCount("the")).isEqualTo(1);

    }

    @Test
    public void shouldExtractWordsFromComment() throws Exception {

        ByteBuffer byteBuffer = classpathFileToByteBuffer("/comment-only.txt");

        WordExtractor wordExtractor = new WordExtractor(byteBuffer);

        ChunkDigest chunkDigest = wordExtractor.call();

        Assertions.assertThat(chunkDigest.wordCount("twelve")).isEqualTo(1);
        Assertions.assertThat(chunkDigest.wordCount("imperial")).isEqualTo(1);
        Assertions.assertThat(chunkDigest.wordCount("the")).isEqualTo(4);

    }

    @Test
    public void shouldProcessRealPagesOk() throws Exception {
        //yes i could have taken the opportunity to check the real word counts are correct but that would take too much time.

        //just checking here that no exceptions are thrown.  I have checked the counts using synthetic pages (pages i wrote myself)

        ByteBuffer byteBuffer = classpathFileToByteBuffer("/whole-page.txt");

        WordExtractor wordExtractor = new WordExtractor(byteBuffer);

        ChunkDigest chunkDigest = wordExtractor.call();
    }


    @Test
    public void shouldProcessProblemPage2Ok() throws Exception {

        ByteBuffer byteBuffer = classpathFileToByteBuffer("/problem-page-2.txt");

        WordExtractor wordExtractor = new WordExtractor(byteBuffer);

        ChunkDigest chunkDigest = wordExtractor.call();
    }


    @Test
    public void shouldProcessProblemPage1Ok() throws Exception {

        ByteBuffer byteBuffer = classpathFileToByteBuffer("/problem-page-1.txt");

        WordExtractor wordExtractor = new WordExtractor(byteBuffer);

        ChunkDigest chunkDigest = wordExtractor.call();
    }

    @Test
    public void shouldProcessProblemPage3Ok() throws Exception {

        ByteBuffer byteBuffer = classpathFileToByteBuffer("/problem-page-3.txt");

        WordExtractor wordExtractor = new WordExtractor(byteBuffer);

        ChunkDigest chunkDigest = wordExtractor.call();
    }

    @Test
    public void shouldProcessProblemPage4Ok() throws Exception {

        ByteBuffer byteBuffer = classpathFileToByteBuffer("/problem-page-4.txt");

        WordExtractor wordExtractor = new WordExtractor(byteBuffer);

        ChunkDigest chunkDigest = wordExtractor.call();
    }



    private ByteBuffer classpathFileToByteBuffer(String filename) throws Exception {
        File file = new File(getClass().getResource(filename).getFile());
        return ByteBuffer.wrap(Files.readAllBytes(file.toPath()));

    }


}