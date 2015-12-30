package com.tomoima.tweetanalyze;


public class TweetAnalyze {

    public static void main(String[] args) {
        CollectTweets collectTweets = new CollectTweets();
        boolean saveSuccess = collectTweets.collectTweetData("habomaijiro","/Users/tomoaki/Workspace/tweet-analyze/reviews/tweet.log", CollectTweets.TYPE.TWEET_ONLY);
        if (!saveSuccess){
            System.out.println("Data save failed");
            return;
        }
        WordCount.initIndex("/Users/tomoaki/Workspace/tweet-analyze/reviews", WordCount.ANALYZE_TYPE.JAPANESE);
        WordCount.getWordFrequency("/Users/tomoaki/Workspace/tweet-analyze/index");
    }

}
