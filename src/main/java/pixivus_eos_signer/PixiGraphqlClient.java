package pixivus_eos_signer;

// import java.io.BufferedReader;
// import java.io.IOException;
// import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.concurrent.ThreadLocalRandom;

import pixivus_eos_signer.EosHelper;
// import pixivus_eos_signer.MultipartUtility;
import pixivus_eos_signer.PostHelper;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class PixiGraphqlClient {

  // private static String BASE_URL        = "https://pixivuscore.herokuapp.com";
  private static String BASE_URL        = "http://localhost:3600";
  private static String GQL_END_POINT   = BASE_URL+"/api/v1/graphiql";
  private static String REST_END_POINT  = BASE_URL+"/api/v1";
  private static String AUTH_CHALLENGE  = REST_END_POINT+"/eos/challenge";
  private static String AUTH_LOGIN      = REST_END_POINT+"/eos/auth";
  private static String POST_END_POINT  = REST_END_POINT+"/posts_multi/";

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
    // System.out.println(" doLogin #1");
    String challenge_string= challenge.getString("to_sign");
    // System.out.println(" doLogin #2 . challenge_string:"+challenge_string);
    String res_signature   = signer.doSignString(challenge_string);
    // System.out.println(" doLogin #3");
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
    String gql_query = "{ profile(account_name: \""+account_name+"\") { user{ _id account_type max_buckets account_name public_key } buckets{ permission _id bucket{ bucketCounterId name description _id categories{_id name} } } } } ";
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

  public static JSONArray postFile(String account_name, String bearer_token, String bucketId, String category_id){
    
    /*
    .set('Authorization', users.guestaccoun1.bearer_token)
    .type('form')
    .set('Content-Type', 'application/json')
    .set('Accept', 'application/json')
    .field('post', JSON.stringify(post.post))
    .attach('pixivus_file_attachment', post.pixivus_file_attachment)
    .expect(404)
    .end((err, res) => {
       // expect(res.body.ok).to.equal(false);
       if (err) return done(err);
       // expect(res.body).to.have.property('_id')
       done();  
    });
    */    

    String charset    = "UTF-8";
    String requestURL = POST_END_POINT+bucketId;
    
    int randomNum     = ThreadLocalRandom.current().nextInt(0, 99999999 + 1);
    String filename   = String.valueOf(randomNum); //randomNum.toString();
    JSONArray post   = PostHelper.create_post("hola", bucketId, category_id, filename);


    try{    
      File file         = PostHelper.getFile(filename);
      
      CloseableHttpClient httpclient = HttpClients.createDefault();
      try {
          HttpPost httppost = new HttpPost(requestURL);
          httppost.setHeader(HttpHeaders.AUTHORIZATION, bearer_token);
          FileBody bin = new FileBody(file);
          
          StringBody postBody = new StringBody(post.toString(), ContentType.TEXT_PLAIN);

          HttpEntity reqEntity = MultipartEntityBuilder.create()
                  .addPart("pixivus_file_attachment", bin)
                  .addPart("post", postBody)
                  .build();

          httppost.setEntity(reqEntity);

          System.out.println("executing request " + httppost.getRequestLine());
          CloseableHttpResponse response = httpclient.execute(httppost);
          try {
              System.out.println("----------------------------------------");
              System.out.println(response.getStatusLine());

              HttpEntity httpEntity = response.getEntity();
              String responseBody   = EntityUtils.toString(httpEntity);
              System.out.println("Response content: " + responseBody);
              return new JSONArray(responseBody);
          } finally {
              response.close();
          }
      } finally {
          httpclient.close();
      }

    }catch (Exception ex) {
      System.out.println(" ERROR PixiGraphqlClient::postFile:: " + ex.toString());
      return new JSONArray("[{error:Ooops1}]");
    }
   

    
  }
}

