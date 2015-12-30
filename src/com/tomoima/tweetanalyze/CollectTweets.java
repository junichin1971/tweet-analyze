package com.tomoima.tweetanalyze;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class CollectTweets {

    public enum TYPE {
        CSV, TWEET_ONLY
    }
    Twitter twitter;
    private static final int COUNT_MAX=200;
    private static final int RATE_LIMITED_STATUS_CODE=400;
    
    public CollectTweets(){
        Configuration configuration = new ConfigurationBuilder()
                .setOAuthAccessToken("xxx")
                .setOAuthConsumerKey("xxx")
                .setOAuthAccessTokenSecret("xxx")
                .setOAuthConsumerSecret("xxx") 
                .build();
                
                TwitterFactory factory = new TwitterFactory(configuration);
                twitter=factory.getInstance();
    }
    
    
    public boolean collectTweetData(String userName, String filePath, TYPE type){
        switch(type){
        case CSV:
            return collectAndFormatTweetData(userName,filePath);
        case TWEET_ONLY:
            return collectTweetText(userName,filePath);
        default:
            return collectTweetText(userName,filePath);
        }

    }
    /**
     * Collect and format Tweet data 
     * @param userName
     * @param filePath where to save data
     * @return collecting data was succeeded
     */
    private boolean collectAndFormatTweetData(String userName,String filePath){
        System.out.println("start collecting data...");
        String regex="\n";
        Pattern pattern=Pattern.compile(regex);
        int count = 1;
        boolean saveSuccess = true;
        try {
            //出力ファイル
            File file = new File(filePath);
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            pw.println("id,tweet,retweetCount,isRetweetedByMe,favCount,date");
            
            Date startDate = new SimpleDateFormat("yyyy/MM/dd").parse("2015/01/01");
            List<Status> tweetList = collectUserTweets(userName);
            for (Status status: tweetList){
                if(status.getCreatedAt().after(startDate)){
                    
                    String regString=status.getText();
                    Matcher matcher=pattern.matcher(regString);
                    String resultString =matcher.replaceAll("<n>");
                    StringBuilder sb = new StringBuilder(count);
                    sb.append(",")
                    .append(resultString)
                    .append(",")
                    .append(status.getRetweetCount())
                    .append(",")
                    .append(status.isRetweetedByMe())
                    .append(",")
                    .append(status.getFavoriteCount())
                    .append(",")
                    .append(TweetFormatter.formatTwitterDate(status.getCreatedAt()));
                    pw.println(sb.toString());
                    count++;
                }
            }
            System.out.println(count + " tweets collected!");
            pw.close();
        } catch (TwitterException e) {
            e.printStackTrace();
            saveSuccess = false;
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            saveSuccess = false;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            saveSuccess = false;
        }
        
        return saveSuccess;
    }
    
    /**
     * Collect only tweetText for Indexing
     * @param userName
     * @param filePath
     * @return
     */
    private boolean collectTweetText(String userName, String filePath){
        System.out.println("start collecting tweet data...");
        String regex="\n";
        Pattern pattern=Pattern.compile(regex);
        boolean saveSuccess = true;
        int count = 0;
        try {
            //出力ファイル
            File file = new File(filePath);
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            Date startDate = new SimpleDateFormat("yyyy/MM/dd").parse("2015/01/01");
            List<Status> tweetList = collectUserTweets(userName);
            for (Status status: tweetList){
                if(status.getCreatedAt().after(startDate)){
                    
                    String regString=status.getText();
                    Matcher matcher=pattern.matcher(regString);
                    String resultString =matcher.replaceAll("<n>");
                    pw.println(resultString);
                    count++;
                }
            }
            System.out.println(count + " tweets collected!");
            pw.close();
        } catch (TwitterException e) {
            e.printStackTrace();
            saveSuccess = false;
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            saveSuccess = false;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            saveSuccess = false;
        }
        
        return saveSuccess;
    }
    
    private List<Status> collectUserTweets(String userName) throws TwitterException {
        int page = 1;
        List<Status> tweets = new ArrayList<>();
        User user = twitter.showUser(userName);
        int total = 0;
        long id = user.getId();
        while (true) {
            Paging paging = new Paging(page++, COUNT_MAX);
            try {
                if (tweets == null) {
                    tweets = twitter.getUserTimeline(id, paging);
                } else {
                    total = tweets.size();
                    tweets.addAll(twitter.getUserTimeline(id, paging));
                }
                System.out.println("page= " + page);
            } catch (TwitterException e) {
                if (RATE_LIMITED_STATUS_CODE != e.getStatusCode()) {
                    e.printStackTrace();
                    continue;
                }
                e.printStackTrace();
                break;
            }
            if (tweets.size() == total) {
                break;
            }

        }

        return tweets;
    }
}
