package pt.ulisboa.tecnico.hdscoin.Crypto;

import java.io.Serializable;

import javax.xml.bind.DatatypeConverter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Object that represents a ciphered message in the format that we specified
 * The format is:
 * AES[Message, RSApriv[hash(Message, IV, timestamp)], IV, timestamp], RSApub[AESkey]
 * Actual implementation is slightly different because RSAPriv is a JAVA digital signature but it still uses RSA
 */
public class CipheredMessage implements Serializable{

	@JsonProperty("stringContent")
    private String content;
	@JsonProperty("stringIV")
    private String IV;
	@JsonProperty("stringIntegrityCheck")
    private String integrityCheck;
	@JsonProperty("stringKey")
    private String key;
	
	public CipheredMessage(){
		
	}

    public CipheredMessage(byte[] content, byte[] IV, byte[] integrityCheck, byte[] key) {
    	StringBuffer toContent = new StringBuffer();
        for (int i = 0; i < content.length; ++i) {
        	toContent.append(Integer.toHexString(0x0100 + (content[i] & 0x00FF)).substring(1));
        }
        StringBuffer toIV = new StringBuffer();
        for (int i = 0; i < IV.length; ++i) {
        	toIV.append(Integer.toHexString(0x0100 + (IV[i] & 0x00FF)).substring(1));
        }
        StringBuffer toIntegrityCheck = new StringBuffer();
        for (int i = 0; i < integrityCheck.length; ++i) {
        	toIntegrityCheck.append(Integer.toHexString(0x0100 + (integrityCheck[i] & 0x00FF)).substring(1));
        }
        StringBuffer toKey = new StringBuffer();
        for (int i = 0; i < key.length; ++i) {
        	toKey.append(Integer.toHexString(0x0100 + (key[i] & 0x00FF)).substring(1));
        }
        this.content = toContent.toString();
        this.IV = toIV.toString();
        this.integrityCheck = toIntegrityCheck.toString();
        this.key = toKey.toString();
        
    }

    public void setContent(String content) {
        this.content = content;
    }

    @JsonIgnore
    public byte[] getKey() {
        return DatatypeConverter.parseHexBinary(key);
    }
    @JsonIgnore
    public byte[] getIntegrityCheck() {
        return DatatypeConverter.parseHexBinary(integrityCheck);
    }
    @JsonIgnore
    public byte[] getContent() {
        return DatatypeConverter.parseHexBinary(content);
    }
    @JsonIgnore
    public byte[] getIV() {
        return DatatypeConverter.parseHexBinary(IV);
    }
    
    public String getStringKey(){
    	return key;
    }
    public String getStringIV(){
    	return IV;
    }
    public String getStringContent(){
    	return content;
    }
    public String getStringIntegrityCheck(){
    	return integrityCheck;
    }

    @Override
    public String toString() {
        return new String(content) + new String(IV) ;
    }
}
