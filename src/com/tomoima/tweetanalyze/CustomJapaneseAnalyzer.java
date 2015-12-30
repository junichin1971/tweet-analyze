package com.tomoima.tweetanalyze;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.io.Reader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.cjk.CJKWidthFilter;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.ja.JapaneseAnalyzer;
import org.apache.lucene.analysis.ja.JapaneseBaseFormFilter;
import org.apache.lucene.analysis.ja.JapaneseKatakanaStemFilter;
import org.apache.lucene.analysis.ja.JapanesePartOfSpeechStopFilter;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer.Mode;
import org.apache.lucene.analysis.ja.dict.UserDictionary;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.util.Version;

public class CustomJapaneseAnalyzer extends StopwordAnalyzerBase{
    private final Mode mode;
    private final Set<String> stoptags;
    private final UserDictionary userDict;
    private final CharArraySet stopwords;
    
    public CustomJapaneseAnalyzer(Version matchVersion){
        super(matchVersion);
        this.userDict = null;
        this.mode = Mode.NORMAL;
        this.stopwords =getStopSet();
        this.stoptags = getStopTags();
    }
    public CustomJapaneseAnalyzer(Version matchVersion, UserDictionary userDict, Mode mode, CharArraySet stopwords, Set<String> stoptags) {
        super(matchVersion, stopwords);
        this.userDict = userDict;
        this.mode = mode;
        this.stoptags = stoptags;
        this.stopwords = stopwords;
      }
    
    public static CharArraySet getStopSet() {
        return CustomSetHolder.CUSTOM_STOP_SET;
    }

    public static Set<String> getStopTags() {
        return CustomSetHolder.CUSTOM_STOP_TAGS;
    }
    
    private static class CustomSetHolder {
        static final CharArraySet CUSTOM_STOP_SET;
        static final Set<String> CUSTOM_STOP_TAGS;

        static {
          try {
            CUSTOM_STOP_SET = loadStopwordSet(true, CustomJapaneseAnalyzer.class, "stopwords.txt", "#");  // ignore case
            final CharArraySet tagset = loadStopwordSet(false, CustomJapaneseAnalyzer.class, "stoptags.txt", "#");
            CUSTOM_STOP_TAGS = new HashSet<>();
            for (Object element : tagset) {
              char chars[] = (char[]) element;
              CUSTOM_STOP_TAGS.add(new String(chars));
            }
          } catch (IOException ex) {
            // default set should always be present as it is part of the distribution (JAR)
            throw new RuntimeException("Unable to load default stopword or stoptag set");
          }
        }
      }
    
    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer tokenizer = new JapaneseTokenizer(reader, userDict, true, mode);
      TokenStream stream = new JapaneseBaseFormFilter(tokenizer);
      stream = new JapanesePartOfSpeechStopFilter(matchVersion, stream, stoptags);
      stream = new CJKWidthFilter(stream);
      stream = new StopFilter(matchVersion, stream, stopwords);
      stream = new JapaneseKatakanaStemFilter(stream);
      stream = new LowerCaseFilter(matchVersion, stream);
      return new TokenStreamComponents(tokenizer, stream);
    }

}
