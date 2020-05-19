package pixivus_eos_signer;

// import java.io.BufferedReader;
// import java.io.IOException;
// import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import pixivus_eos_signer.EosHelper;

public class PixiGraphqlClient {

  private static String BASE_URL        = "https://pixivuscore.herokuapp.com";
  // private static String BASE_URL        = "http://localhost:3600";
  private static String GQL_END_POINT   = BASE_URL+"/api/v1/graphiql";
  private static String REST_END_POINT  = BASE_URL+"/api/v1";
  private static String AUTH_CHALLENGE  = REST_END_POINT+"/eos/challenge";
  private static String AUTH_LOGIN      = REST_END_POINT+"/eos/auth";

  private static JSONObject get(String uri) throws Exception{
    URL url                = new URL(uri);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setDoOutput(true);
    conn.setRequestMethod("GET");
    conn.connect(); 

    int respCode = conn.getResponseCode(); // New items get NOT_FOUND on PUT
    System.out.println(" respCode: " + respCode);
    // if (!(respCode == HttpURLConnection.HTTP_OK || respCode == HttpURLConnection.HTTP_NOT_FOUND)) 
    if (respCode < 200 || respCode >299)
    {
      String _error = " Profile ERROR: " + respCode + " " + conn.getResponseMessage();
      System.out.println(_error);
      throw new Exception(_error);
    }
    
    StringBuilder response = new StringBuilder();
    String line;

    // Read input data stream.
    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    while ((line = reader.readLine()) != null) {
      response.append(line);
    }
    reader.close();
    String response_string = response.toString();
    System.out.println(" GET JSON: " + response_string);
    return new JSONObject(response_string);
  }

  public static JSONObject doLogin(EosHelper signer) throws Exception{
    
    String uri             = AUTH_CHALLENGE+"/"+signer.account_name; 
    JSONObject challenge   = get(uri);
    System.out.println(" doLogin #1");
    String challenge_string= challenge.getString("to_sign");
    System.out.println(" doLogin #2 . challenge_string:"+challenge_string);
    String res_signature   = signer.doSignString(challenge_string);
    System.out.println(" doLogin #3");
    String jsonText        = "{account_name:"+signer.account_name+" ,signature: "+res_signature+",challenge:"+challenge_string+"}";
    System.out.println(" About to post to AUTH JSON: " + jsonText);
    JSONObject json        = new JSONObject(jsonText);

    URL url                = new URL(AUTH_LOGIN);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setDoOutput(true);
    // conn.setRequestProperty("Content-Type", "application/json, text/plain, */*; charset=UTF-8");
    conn.setRequestProperty("Content-Type", "application/json; utf-8");
    conn.setRequestProperty("Accept", "application/json");
    conn.setRequestMethod("POST");
    
    // OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
    // writer.write(json.toString());
    // writer.close();

    try(OutputStream os = conn.getOutputStream()) {
        byte[] input = json.toString().getBytes("utf-8");
        os.write(input, 0, input.length);           
    }

    int respCode = conn.getResponseCode(); // New items get NOT_FOUND on PUT
    System.out.println(" respCode: " + respCode);
    
    if (respCode < 200 || respCode >299)
    {
      String _error = " Auth ERROR: " + respCode + " " + conn.getResponseMessage();
      System.out.println(_error);
      throw new Exception(_error);
    }
    
    StringBuilder response = new StringBuilder();
    String line;

    // Read input data stream.
    BufferedReader reader  = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    while ((line = reader.readLine()) != null) {
      response.append(line);
    }
    reader.close();
    String response_string = response.toString();
    System.out.println(" AUTH JSON: " + response_string);
    
    return new JSONObject(response_string);
     
  }

  public static JSONObject getAccountProfile(String account_name, String bearer_token) throws Exception{
    URL url = new URL(GQL_END_POINT);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    // Enable output for the connection.
    conn.setDoOutput(true);
    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    conn.setRequestProperty("Accept", "application/json");
    conn.setRequestProperty("Authorization", bearer_token);
    // Set HTTP request method.
    conn.setRequestMethod("POST");

    // String jsonText = "{ query: { profile(account_name: \""+account_name+"\") { user{ _id account_type max_buckets account_name public_key } buckets{ permission _id bucket{ bucketCounterId name description } } } } }";
    // System.out.println(jsonText);
    //{ query: { profile(account_name: atomakinnaka) { user{ _id account_type max_buckets account_name public_key } buckets{ permission _id bucket{ bucketCounterId name description } } } } }
    // JSONObject json = new JSONObject(jsonText);
    String gql_query = "{ profile(account_name: \""+account_name+"\") { user{ _id account_type max_buckets account_name public_key } buckets{ permission _id bucket{ bucketCounterId name description } } } } ";
    JSONObject json = new JSONObject();
    json.put("query", gql_query);
    try(OutputStream os = conn.getOutputStream()) {
      byte[] input = json.toString().getBytes("utf-8");
      os.write(input, 0, input.length);           
    }

    int respCode = conn.getResponseCode(); // New items get NOT_FOUND on PUT
    if (respCode < 200 || respCode >299)
    {
      String _error = " Auth ERROR: " + respCode + " " + conn.getResponseMessage();
      System.out.println(_error);
      throw new Exception(_error);
    }

    StringBuilder response = new StringBuilder();
    String line;

    // Read input data stream.
    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    while ((line = reader.readLine()) != null) {
      response.append(line);
    }
    reader.close();
    String response_string = response.toString();
    System.out.println(" Profile JSON: " + response_string);
    return new JSONObject(response_string);
    
  }
}

