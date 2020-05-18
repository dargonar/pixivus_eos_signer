package pixivus_eos_signer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class PixiUtils
{
    public static byte[] sha256 (String originalString) throws Exception{
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] encodedhash   = digest.digest(originalString.getBytes(StandardCharsets.UTF_8));
      return encodedhash;
    }

    public static String bytes2hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        String tmp = null;
        for (byte b : bytes) {
            tmp = Integer.toHexString(0xFF & b);
            if (tmp.length() == 1) {     
                tmp = "0" + tmp;
            }
            sb.append(tmp);
        }
        return sb.toString();
    }
};