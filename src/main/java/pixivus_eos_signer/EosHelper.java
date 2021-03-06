/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package pixivus_eos_signer;

import pixivus_eos_signer.PixiKey;
import pixivus_eos_signer.PixiUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Signature;

import com.google.common.io.BaseEncoding;
import io.github.novacrypto.bip32.ExtendedPrivateKey;
import io.github.novacrypto.bip32.ExtendedPublicKey ;
import io.github.novacrypto.bip32.networks.Bitcoin;

import org.bouncycastle.crypto.digests.RIPEMD160Digest;

// import org.apache.commons.codec.binary.Hex;
// import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.codec.binary.Base64;

// import systems.v.hdkey.ExtendedKey;
// import systems.v.hdkey.HDkeyException;
import systems.v.hdkey.Base58;

import org.bitcoinj.core.Utils;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.ECKey.ECDSASignature;
import org.bitcoinj.core.Sha256Hash;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import static org.junit.Assert.*;

public class EosHelper {
    
    private static String CHALLENGE    = "5HuAHNRFZoP5pR3BTN66HmxUp28zSZxckhP56G1PkrhAqwDp1d4";
    private static String SEED         = "atomakinnaka.1234";
    private static String EXPECTED_SIG = "SIG_K1_K98CxvV38rYjpkCVV3vjgMvUPZeYf1tADbonw6QBX7WTJdFRc1vxLyPg7DNoTc4QS8cYf9PhmxU1y5WcNTAMLCC4exPXqY";
    private static String DERIVE_PATH  = "m/44'/194'/0'/0/0";

    public String account_name = null;
    public String password     = null;
    public PixiKey my_key      = null;

    public EosHelper (String account_name, String password){
      this.account_name = account_name;
      this.password     = password;
    }

    public EosHelper (String account_name, String password, Boolean calculate_key) throws Exception{
      this.account_name = account_name;
      this.password     = password;
      if(calculate_key)
        this.calculateKey();
    }

   
    public PixiKey calculateKey() throws Exception{

      String seed               = this.account_name + "." + this.password;
      byte[] pk_seed            = PixiUtils.sha256(seed);
      ExtendedPrivateKey key    = ExtendedPrivateKey.fromSeed(pk_seed, Bitcoin.MAIN_NET);
      key                       = key.derive(DERIVE_PATH);
      for (int index = 0; index < 10; index++) 
      {
        byte[] priv_key_bytes     = Arrays.copyOfRange(key.extendedKeyByteArray(), 46, 46+32);
        key = ExtendedPrivateKey.fromSeed(priv_key_bytes, Bitcoin.MAIN_NET);
        key = key.derive(DERIVE_PATH);
      }

      byte[] ext_private_key    = key.extendedKeyByteArray();
      ExtendedPublicKey pub_key = key.neuter();
      byte[] ext_public_key     = pub_key.extendedKeyByteArray();
      byte[] _private_key       = Arrays.copyOfRange(ext_private_key, 46, 46+32);
      byte[] _public_key        = Arrays.copyOfRange(ext_public_key, 46, 46+32);

      PixiKey myKey             = new PixiKey("", ext_private_key, ext_public_key, _private_key, _public_key);
      this.my_key               = myKey;
      return myKey;

    }
    
    public String doSignString(String challenge) throws Exception{
      if(this.my_key==null)
      {
        System.out.println("EosHelper::doSignString #1");
        if(this.account_name==null || this.password==null)
            throw new Exception("NO KEY ERROR");
        System.out.println("EosHelper::doSignString #2");
        this.calculateKey();
      }
      System.out.println("EosHelper::doSignString #3");
      return this.doSignString(challenge, this.my_key);
    }

    public String doSignString(String challenge, PixiKey key) throws Exception{
      
      System.out.println("EosHelper::doSignString_EX #1");
      if(key==null)
        throw new Exception("NO KEY ERROR");
      
      ECKey ecKey          = ECKey.fromPrivate(key.private_key);
      System.out.println("EosHelper::doSignString_EX #2");
      byte[] to_signBytes  = PixiUtils.sha256(challenge);
      Sha256Hash to_sign   = Sha256Hash.wrap(to_signBytes);
      
      // ECDSASignature sign  = ecKey.sign(to_sign);
      // byte[] signatureDER  = sign.encodeToDER();
      // System.out.println(" sign.der to hex: " + PixiUtils.bytes2hex(signatureDER));
      // String signatureEncoded = BaseEncoding.base16().lowerCase().encode(sign.encodeToDER());
      System.out.println("EosHelper::doSignString_EX #3");

      byte[] result = getSignatureBytes(to_sign, ecKey);

      System.out.println("EosHelper::doSignString_EX #4");

      // System.out.println(" result: " + PixiUtils.bytes2hex(result) );
      // System.out.println(" result: " + Base64.encodeBase64String(result) );
      String base58_signature = checkEncode(result, null);
      System.out.println(" result: " + base58_signature );

      return base58_signature;
    }

    private static byte[] getSignatureBytes(Sha256Hash hashTransaction, ECKey requiredPrivateKey) {
      boolean isGrapheneCanonical = false;
      byte[] signatureData = null;

      while (!isGrapheneCanonical) {
        
        int recId = -1;
        ECKey.ECDSASignature sig = requiredPrivateKey.sign(hashTransaction);

        for (int i = 0; i < 4; i++) {
          ECKey k = ECKey.recoverFromSignature(i, sig, hashTransaction,
              requiredPrivateKey.isCompressed());
          if (k != null && k.getPubKeyPoint()
              .equals(requiredPrivateKey.getPubKeyPoint())) {
            recId = i;
            break;
          }
        }
        // 1 header + 32 bytes for R + 32 bytes for S
        signatureData = new byte[65];
        int headerByte = recId + 27 + (requiredPrivateKey.isCompressed() ? 4 : 0);
        signatureData[0] = (byte) headerByte;
        System.arraycopy(Utils.bigIntegerToBytes(sig.r, 32), 0,
            signatureData, 1, 32);
        System.arraycopy(Utils.bigIntegerToBytes(sig.s, 32), 0,
            signatureData, 33, 32);

        // Further "canonicality" tests
        if (isCanonical(signatureData)) {
          // this.setExpiration(Util.addTime(this.getExpiration(), 1));
          isGrapheneCanonical = true;
        } else {
          isGrapheneCanonical = true;
        }
      }
      return signatureData;

    }

    private static String checkEncode(byte[] buffer, @Nullable String type) throws Exception{
      String _type       = "K1";
      if(type!=null)
        _type=type;
      byte[] check       = buffer.clone();
      // byte[] _type_bytes = Utils.HEX.decode(_type); 
      byte[] _type_bytes = _type.getBytes(StandardCharsets.UTF_8);
      // create a destination array that is the size of the two arrays
      
      byte[] destination = new byte[check.length + _type_bytes.length];
      // System.out.println(" ************************************* 1 ");
      System.arraycopy(check, 0, destination, 0, check.length);
      // System.out.println(" ************************************* 2 ");
      System.arraycopy(_type_bytes, 0, destination, check.length, _type_bytes.length);
      
      RIPEMD160Digest digest = new RIPEMD160Digest();
      digest.update(destination, 0, destination.length);
      byte[] out = new byte[20];
      digest.doFinal(out, 0);
      byte[] checksum = Arrays.copyOfRange(out, 0, 4);

      byte[] result = new byte[check.length + checksum.length];
      // System.out.println(" ************************************* 3 ");
      System.arraycopy(check, 0, result, 0, check.length);
      // System.out.println(" ************************************* 4 ");
      System.arraycopy(checksum, 0, result, check.length, checksum.length);

      return "SIG_K1_" + Base58.encode(result);
    }

    private static boolean isCanonical(byte[] signature) {
      return ((signature[0] & 0x80) != 0) || (signature[0] == 0) || ((signature[1] & 0x80) != 0)
          || ((signature[32] & 0x80) != 0) || (signature[32] == 0) || ((signature[33] & 0x80) != 0);
    }

    
}
