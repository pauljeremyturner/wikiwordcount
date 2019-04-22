package com.paulturner.wikiwordcount.test;

import java.io.File;

public class TestFile {

    public static String testDumpFilePath() {

        File projectRootDir = new File(".");
        return projectRootDir.getAbsolutePath() + "<SEP>src<SEP>test<SEP>resources<SEP>quite-a-few-pages.xml".replace("<SEP>", File.separator);

    }
}
