package org.bitbucket.eunjeon.seunjeon.elasticsearch;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.*;
import org.junit.Before;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.assertEquals;

public class SeunjeonTokenizerTest {

    @Before
    public void setUp() {
    }

    @Test
    public void testTokenize() throws IOException {
        Tokenizer t = new SeunjeonTokenizer();
        assertEquals("eojeol",
                "유영호/N:1:1:0:3:N;유영호와/EOJ:0:1:0:4:EOJ;이용운/N:1:1:4:7:N;", tokenize("유영호와이용운", t));
        assertEquals("compound + eojeol",
                "삼성/N:1:1:0:2:N;삼성전자는/EOJ:0:2:0:5:EOJ;전자/N:1:1:2:4:N;", tokenize("삼성전자는", t));
        assertEquals("noun noun eojeol",
                "LG/SL:1:1:0:2:SL;전자/N:1:1:2:4:N;전자는/EOJ:0:1:2:5:EOJ;", tokenize("LG전자는", t));
        assertEquals("deInflect",
                "직무/N:1:1:0:2:N;직무를/EOJ:0:1:0:3:EOJ;행하/V:1:1:4:6:V;행한다/EOJ:0:1:4:7:EOJ;", tokenize("직무를 행한다.", t));
        assertEquals("number, symbol",
                "55/SN:1:1:0:2:SN;32/SN:1:1:3:5:SN;ms/SL:1:1:5:7:SL;", tokenize("55.32ms", t));

        System.out.println(tokenize("아버지가방에들어가신다.", t));
        System.out.println(tokenize("무궁화꽃이피었습니다.", t));
    }

    @Test
    public void testUserWords() throws IOException {
        Tokenizer t = new SeunjeonTokenizer();
        assertEquals("user words",
                "버카충/UNK:1:1:0:3:UNK;", tokenize("버카충", t));

        Tokenizer ut = new SeunjeonTokenizer(TokenizerOptions.create().setUserWords(new String[]{"버카충"}));
        assertEquals("user words",
                "버카충/N:1:1:0:3:N;", tokenize("버카충", ut));
    }

    @Test
    public void testDeCompound() throws IOException {
        assertEquals("삼성/N:1:1:0:2:N;삼성전자/EOJ:0:2:0:4:EOJ;전자/N:1:1:2:4:N;",
                tokenize("삼성전자", new SeunjeonTokenizer(TokenizerOptions.create())));

        assertEquals("삼성전자/N:1:1:0:4:N;",
                tokenize("삼성전자", new SeunjeonTokenizer(TokenizerOptions.create().setDeCompound(false))));
    }

    @Test
    public void testDeInflect() throws IOException {
        assertEquals("슬프/V:1:1:0:2:V;슬픈/EOJ:0:1:0:2:EOJ;",
                tokenize("슬픈", new SeunjeonTokenizer(TokenizerOptions.create())));

        assertEquals("슬픈/V+E:1:1:0:2:V+E;",
                tokenize("슬픈", new SeunjeonTokenizer(TokenizerOptions.create().setDeInflect(false))));
    }

    @Test
    public void testIndexPoses() throws IOException {
        assertEquals("꽃/N:1:1:0:1:N;꽃이/EOJ:0:1:0:2:EOJ;피/V:1:1:2:3:V;피다/EOJ:0:1:2:4:EOJ;",
                tokenize("꽃이피다", new SeunjeonTokenizer(TokenizerOptions.create())));

        assertEquals("꽃/N:1:1:0:1:N;",
                tokenize("꽃이피다", new SeunjeonTokenizer(TokenizerOptions.create().
                        setIndexPoses(new String[]{"N"}).
                        setIndexEojeol(false))));
    }

    @Test
    public void testPerformance() throws IOException {
        Tokenizer t = new SeunjeonTokenizer();
        ClassLoader classLoader = getClass().getClassLoader();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(classLoader.getResourceAsStream("long_contents.txt")));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        reader.close();
        String longContents = sb.toString();
        System.out.println("longContents = " + longContents);
        String result = null;
        result = tokenize(longContents, t);
        System.out.println("result = " + result);
        long startTime = System.nanoTime();
        int times = 1000;
        for (int i = 0; i < times; i++) {
            result = tokenize(longContents, t);
        }
        long endTime = System.nanoTime();
        long elapsedTime = (endTime - startTime) / times;
        System.out.println("result = " + result);
        System.out.println("elapsedTime = " + elapsedTime + " us");
    }

    public String tokenize(String document, Tokenizer tokenizer) throws IOException {
        tokenizer.setReader(new StringReader(document));
        tokenizer.reset();
        StringBuilder sb = new StringBuilder();
        while (tokenizer.incrementToken()) {
            CharTermAttribute termAttr = tokenizer.getAttribute(CharTermAttribute.class);
            PositionIncrementAttribute posIncrAttr = tokenizer.getAttribute(PositionIncrementAttribute.class);
            PositionLengthAttribute posLength = tokenizer.getAttribute(PositionLengthAttribute.class);
            OffsetAttribute offsetAttr = tokenizer.getAttribute(OffsetAttribute.class);
            TypeAttribute typeAttr = tokenizer.getAttribute(TypeAttribute.class);
            String toString = new String(termAttr.buffer(), 0, termAttr.length()) + ":" +
                    posIncrAttr.getPositionIncrement() + ":" +
                    posLength.getPositionLength() + ":" +
                    offsetAttr.startOffset() + ":" +
                    offsetAttr.endOffset() + ":" +
                    typeAttr.type();
            sb.append(toString);
            sb.append(";");
        }
        tokenizer.close();
        return sb.toString();

    }
}
