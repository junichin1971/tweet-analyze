package com.tomoima.tweetanalyze;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.util.Version;

/**
 * CustomAnalyzer
 * Based on {@link http://grepcode.com/file_/repo1.maven.org/maven2/org.apache.lucene/lucene-analyzers-common/4.9.0/org/apache/lucene/analysis/standard/StandardAnalyzer.java/?v=source}
 * @author tomoaki imai
 *
 */
public class CustomAnalyzer extends StopwordAnalyzerBase{
    /** Default maximum allowed token length */
    public static final int DEFAULT_MAX_TOKEN_LENGTH = 255;
    private int maxTokenLength = DEFAULT_MAX_TOKEN_LENGTH;
    public static final CharArraySet STOP_WORDS_SET = StopAnalyzer.ENGLISH_STOP_WORDS_SET;
    protected CustomAnalyzer(Version version) {
        super(version, STOP_WORDS_SET);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
        final StandardTokenizer src = new StandardTokenizer(matchVersion, reader);
        TokenStream stream =new StandardFilter(matchVersion, src);
        // ASCIIFoldingFilterでExtended Latinな文字や全角文字をLatin1な文字に変換
        stream = new ASCIIFoldingFilter(stream);
        
        // LowerCaseFilterで小文字に統一
        stream = new LowerCaseFilter(Version.LUCENE_4_9, stream);

        // StopFilterでa, and, is, toと指定した単語を除外
        stream = new StopFilter(Version.LUCENE_4_9, stream, StandardAnalyzer.STOP_WORDS_SET);
        String[] stopWords = {
                "you","your","me","my","so","it","from","here","t.co","t.c","http","https","the","to","a","an","on",
                "in","and","of","for","n","is","this","with","i","i've","we","our","by","that","via","will","would","be",
                "are","was","can","t","it's","we're","you're","has","been","us","i'm","he","she"
        };
        stream = new StopFilter(Version.LUCENE_4_9, stream, StopFilter.makeStopSet(Version.LUCENE_4_9, stopWords) );
              
        // 短いor長い単語は省く
        stream = new LengthFilter(Version.LUCENE_4_9, stream, 2, 20);
        // EnglishPossessiveFilterで「's」を除去
        stream = new EnglishPossessiveFilter(Version.LUCENE_4_9, stream);
        // KStemFilterを使って複数形とかの揺れを吸収
        stream = new KStemFilter(stream);
        return new TokenStreamComponents(src, stream){
            @Override
            protected void setReader(final Reader reader) throws IOException {
              src.setMaxTokenLength(CustomAnalyzer.this.maxTokenLength);
              super.setReader(reader);
            }
        };
    
    }

}
