package edu.brandeis.cs12b.PA6_Solution;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.json.JSONObject;

import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

import wordcloud.CollisionMode;
import wordcloud.WordCloud;
import wordcloud.WordFrequency;
import wordcloud.bg.RectangleBackground;
import wordcloud.font.scale.LinearFontScalar;

public class TwitterCloud {

	/**
	 * The number of tokens you should extract from tweets
	 */
	private static final int NUMBER_TOKENS = 4000;

	public static void main(String[] a) {
		TwitterCloud tc = new TwitterCloud();
		tc.makeCloud(new String[] { "Curry", "NBA" }, "test.png");
	}

	public void makeCloud(String[] args, String filename) {
		// Configure Twitter "hosebird" (derived from "fire hose" and bird which
		// is the animal that
		Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
		StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
		List<String> terms = Arrays.asList(args);
		hosebirdEndpoint.trackTerms(terms);

		Authentication hosebirdAuth = new OAuth1(System.getenv("CONSUMER_KEY"), System.getenv("CONSUMER_SECRET"),System.getenv("TOKEN"), System.getenv("TOKEN_SECRET"));
		BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(100000);
		// Connect to service and start watching for the terms of interest
		ClientBuilder builder = new ClientBuilder();

				builder.hosts(hosebirdHosts);
				builder.authentication(hosebirdAuth);
				builder.endpoint(hosebirdEndpoint);
				builder.processor(new StringDelimitedProcessor(msgQueue));
		Client hosebirdClient = builder.build();
		hosebirdClient.connect();
		HashMap<String, Integer> freq = new HashMap<String, Integer>();
		String  msg = "";
		int i =0;
		while (freq.keySet().size() < NUMBER_TOKENS) {
			try {

				msg = msgQueue.take();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			JSONObject tweet = new  JSONObject(msg);
			try{
				String tt =  tweet.getString("text");
				System.out.println(tt);
				System.out.println(freq.keySet().size());

				read(msg,tt,freq);
			}catch(org.json.JSONException e){
			}
			
			
		}
		System.out.println(freq.keySet().size());

		
		makeCloud(freq);
		hosebirdClient.stop();
		

	}

	public void read(String msg, String tt,HashMap<String, Integer> freq){
		List<String> tokens = new LinkedList<String>();

		try (EnglishAnalyzer an = new EnglishAnalyzer()) {
		    TokenStream sf = an.tokenStream(msg, tt);
		    try {
		        sf.reset();
		        while (sf.incrementToken()) {
		            CharTermAttribute cta = sf.getAttribute(CharTermAttribute.class);
		            tokens.add(cta.toString());
		            
		            if(freq.containsKey(cta.toString())){
		            	int sum = freq.get(cta.toString())+1;
		            	freq.put(cta.toString(), sum);

		            }else{
		            	freq.put(cta.toString(), 1);
		            }
		        }
		        //
		    } catch (Exception e) {
		        System.err.println("Could not tokenize string: " + e);
		    }
		}
	}
	public void makeCloud(HashMap<String, Integer> freq){
		List<WordFrequency> wf = new ArrayList<WordFrequency>();

		for(String a :freq.keySet()){
			if(a.equals("http")||a.equals("t.co")){
				continue;
			}else{
			
			WordFrequency q = new WordFrequency(a,freq.get(a));
			wf.add(q);
			
			}
		}
		WordCloud wordCloud = new WordCloud(400, 400, CollisionMode.RECTANGLE);
		wordCloud.setPadding(0);
		wordCloud.setBackground(new RectangleBackground(400, 400));
		wordCloud.setFontScalar(new LinearFontScalar(14, 40));
		wordCloud.build(wf);
		wordCloud.writeToFile("real.png");
	}

}
