package com.tomoima.tweetanalyze;


import java.text.SimpleDateFormat;
import java.util.Date;

public class TweetFormatter {
    final static String TWEET_FORMAT="yyyy/MM/dd HH:mm:ss";
    public static String formatTwitterDate(Date tweetDate){
        SimpleDateFormat sf = new SimpleDateFormat(TWEET_FORMAT);
        return sf.format(tweetDate);
    }
}
