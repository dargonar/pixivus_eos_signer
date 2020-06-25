package pixivus_eos_signer;

import java.util.concurrent.ThreadLocalRandom;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class PostHelper {
  public static JSONArray create_post(String memo, String bucket_id, String category_id, String filename){
    
    String filetype        = "text/plain";
    // String jsonFileString  = "{ name:"+filename+", type:"+filetype+", size:1372}";
    // JSONObject jsonFile    = new JSONObject(jsonFileString);
    
    // String jsonPostString  = "[{memo: "+memo+", bucket_id: "+bucket_id+", category_id:"+category_id+", file_name: "+filename+".txt , file_type:"+ filetype+", metadata:"+ "size\:1372 bytes"+ ", tags: "+"tag_1,tag_2"+", location: "+"-34.883183900000006,-58.0239298"+"} ]";
    JSONObject jsonPost = new JSONObject();
    jsonPost.put("memo", memo);
    jsonPost.put("bucket_id", bucket_id);
    jsonPost.put("category_id" , category_id);
    jsonPost.put("file_name" , filename+".txt");
    jsonPost.put("file_type", filetype);
    jsonPost.put("metadata", "size:1372 bytes");
    jsonPost.put("tags", "tag_1,tag_2");
    jsonPost.put("location", "-34.883183900000006,-58.0239298");
    
    JSONArray jsonPostArray    = new JSONArray();
    jsonPostArray.put(jsonPost);

    System.out.println(" PostHelper::create_post: " + jsonPostArray.toString());

    return jsonPostArray;
  }
  public static File getFile(String filename) throws IOException {
    // int randomNum = ThreadLocalRandom.current().nextInt(min, max + 1);
    // String filename = random.toString();
    String lyrics = "But still I m having memories of high speeds when the cops crashed\n" + 
                 "As I laugh, pushin the gas while my Glocks blast\n" + 
                 "We was young and we was dumb but we had heart - " + filename;
    File temp = File.createTempFile(filename, ".txt");
    // temp.deleteOnExit();
    // Write to temp file
    BufferedWriter out = new BufferedWriter(new FileWriter(temp));
    out.write(lyrics);
    out.close();
    return temp;
  }
}