# Tweet Analyzer for java
* A simple program to collect Tweets from a user and analyze words frequencies. 

##How to use

* Please setup build path to libraries below:
  - twitter4j-core-4.0.1,jar
  - lucene-core-4.9.1.jar
  - lucene-queryparser-4.9.1.jar
  - lucene-analyzers-common-4.9.1.jar
  - lucene-analyzers-kuromoji-4.9.1.jar
  
* Get your AccessToken,AccessTokenSecret,ConsumerKey, ConsumerSecret from Twitter Developer Console. You need Twitter account to get them.

* Set each keys to configuration at `CollectTweets.java`
```
Configuration configuration = new ConfigurationBuilder()
  .setOAuthAccessToken("xxx")
  .setOAuthConsumerKey("xxx")
  .setOAuthAccessTokenSecret("xxx")
  .setOAuthConsumerSecret("xxx")  
```