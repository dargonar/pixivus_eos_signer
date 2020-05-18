package pixivus_eos_signer;

public class PixiKey
{
    public PixiKey(String _wif, byte[] _extended_private_key, byte[] _extended_public_key, byte[] _private_key,  byte[] _public_key){
      this.wif                  = _wif;
      this.private_key          = _private_key;
      this.public_key           = _public_key;
      this.extended_private_key = _extended_private_key;
      this.extended_public_key  = _extended_public_key;
    }
    public String wif; 
    

    public byte[] private_key;  
    public byte[] public_key;

    public byte[] extended_private_key;  
    public byte[] extended_public_key;  
};