package com.tomoima.tweetanalyze;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.ja.JapaneseAnalyzer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer.Mode;
import org.apache.lucene.analysis.ja.dict.UserDictionary;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.Version;

public class WordCount {
    public enum ANALYZE_TYPE {
        NORMAL,JAPANESE,CUSTOM
    }
    
    public static final FieldType TYPE_STORED = new FieldType();
    static {
        TYPE_STORED.setIndexed(true);
        TYPE_STORED.setTokenized(true);
        //reader value can not be stored
        //TYPE_STORED.setStored(true);
        TYPE_STORED.setStoreTermVectors(true);
        TYPE_STORED.setStoreTermVectorPositions(true);
        TYPE_STORED.freeze();
    }
    
    public WordCount(){
        
    }

    /**
     * indexを初期化
     * @param dataPath index対象のディレクトリ
     * @param type
     * @return
     */
    public static boolean initIndex(String dataPath, WordCount.ANALYZE_TYPE type){
        final String DIR_INDEX = "index";
        Analyzer analyzer;
        switch(type){
        case NORMAL:
            String[] stopWords = {
                    "you","your","me","my","so","it","from","here","t.co","t.c","http","https","the","to","a","an","on",
                    "in","and","of","for","n","is","this","with","i","i've","we","our","by","that","via","will","would","be",
                    "are","was","can","t","it's","we're","has","been","us","i'm","he","she"
            };
            analyzer = new StandardAnalyzer(Version.LUCENE_4_9,StopFilter.makeStopSet(Version.LUCENE_4_9, stopWords)); 
            
            break;
        case JAPANESE:
            System.out.println("use Japanese analyzer");
//            analyzer = new JapaneseAnalyzer(Version.LUCENE_4_9, 
//                    null, 
//                    Mode.NORMAL, 
//                    JapaneseAnalyzer.getDefaultStopSet(), 
//                    JapaneseAnalyzer.getDefaultStopTags());
            UserDictionary dictionary;
            try {
                Reader reader = IOUtils.getDecodingReader(WordCount.class.getResourceAsStream("userDic.txt"), StandardCharsets.UTF_8);
                dictionary = new UserDictionary(reader);
                } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
                dictionary = null;
            }
            analyzer = new CustomJapaneseAnalyzer(Version.LUCENE_4_9,
                    dictionary,
                    Mode.NORMAL,
                    CustomJapaneseAnalyzer.getStopSet(), CustomJapaneseAnalyzer.getStopTags()); 
            break;
        case CUSTOM:
            analyzer = new CustomAnalyzer(Version.LUCENE_4_9); 
            break;
        
        default:
            analyzer = new StandardAnalyzer(Version.LUCENE_4_9);
            break;
        
        }
     
        Directory indexDir = null;
        try {
            indexDir = FSDirectory.open(new File(DIR_INDEX));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("opening index dir failed:" + e.toString());
        }
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_9,analyzer);
        //Overwrite and recreate index
        config.setOpenMode(OpenMode.CREATE);
        //return false if index directory can not be created
        if(indexDir == null) {
            return false;
        }
        
        try {
            final IndexWriter indexWriter = new IndexWriter(indexDir, config);
            Path path = new File(dataPath).toPath();
            if (Files.isDirectory(path)) {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        try {
                            indexDoc(indexWriter, file, attrs.lastModifiedTime().toMillis());
                        } catch (IOException ignore) {
                            // don't index files that can't be read.
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                indexDoc(indexWriter, path, Files.getLastModifiedTime(path).toMillis());
            }
            
            indexWriter.close();
        } catch (IOException e) {

        }
       

        return true;
    }
    
    /** Indexes a single document */
    static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {
      try (InputStream stream = Files.newInputStream(file)) {
        // make a new, empty document
        Document doc = new Document();
        
        // Add the path of the file as a field named "path".  Use a
        // field that is indexed (i.e. searchable), but don't tokenize 
        // the field into separate words and don't index term frequency
        // or positional information:
        Field pathField = new StringField("path", file.toString(), Field.Store.YES);
        doc.add(pathField);
        
        // Add the last modified date of the file a field named "modified".
        // Use a LongField that is indexed (i.e. efficiently filterable with
        // NumericRangeFilter).  This indexes to milli-second resolution, which
        // is often too fine.  You could instead create a number based on
        // year/month/day/hour/minutes/seconds, down the resolution you require.
        // For example the long value 2011021714 would mean
        // February 17, 2011, 2-3 PM.
        doc.add(new LongField("modified", lastModified, Field.Store.NO));
        
        // Add the contents of the file to a field named "contents".  Specify a Reader,
        // so that the text of the file is tokenized and indexed, but not stored.
        // Note that FileReader expects the file to be in UTF-8 encoding.
        // If that's not the case searching for special characters will fail.

        doc.add(new Field("contents", new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)),TYPE_STORED));
        if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
          // New index, so we just add the document (no old document can be there):
          System.out.println("adding " + file);
          writer.addDocument(doc);
        } else {
          // Existing index (an old copy of this document may have been indexed) so 
          // we use updateDocument instead to replace the old one matching the exact 
          // path, if present:
          System.out.println("updating " + file);
          writer.updateDocument(new Term("path", file.toString()), doc);
        }
      }
    }
    
    public static void getWordFrequency(String indexPath){
        try {
            IndexReader directoryReader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
            
            //TermFrequency
            Map<String, Integer> termMap=getTermFrequencies(directoryReader, 1, "contents");
            sortTermFrequencies(termMap);
            directoryReader.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private static Map<String, Integer> getTermFrequencies(IndexReader indexReader,int docID,String field)throws IOException{
        Terms terms=indexReader.getTermVector(docID, field);
        Map<String, Integer> termFrequencies=new HashMap<>();
        TermsEnum termsEnum=null;

        termsEnum = terms.iterator(termsEnum);
        BytesRef text = null;
        while ((text = termsEnum.next()) != null) {
            String term = text.utf8ToString();
            int freq = (int) termsEnum.totalTermFreq();
            termFrequencies.put(term, freq);
        }
        return termFrequencies;
        
    }
    
    private static Map<String, Integer> sortTermFrequencies(Map<String, Integer> termMap) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(termMap.entrySet());
        Map<String, Integer> result = new HashMap<String, Integer>();

        Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
                return ((Integer) o2.getValue()).compareTo((Integer) o1.getValue());
            }
        });
        for (Entry<String, Integer> entry : entries) {
            result.put(entry.getKey(), entry.getValue());
            if ((entry.getValue() >= 15)) {
                System.out.println("val: " + entry.getValue() +"\t\t t: " + entry.getKey());
            }
        }
        return result;

    }
}
