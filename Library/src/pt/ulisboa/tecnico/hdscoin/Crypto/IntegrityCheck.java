package pt.ulisboa.tecnico.hdscoin.crypto;

import java.io.Serializable;

import javax.xml.bind.DatatypeConverter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Contains the params necessary to verifiy integrity
 * It's missing the IV
 * Isn't really needed now but I was doing it manually before I discovered Java's digital sig
 */
public class IntegrityCheck implements Serializable{

	@JsonProperty("digitalSignature")
    private String digitalSignature;
    private long nonce;
    private long timestamp;
    
    public IntegrityCheck(){
    	
    }

    public IntegrityCheck(byte[] digitalSignature, long nonce, long timestamp) {
    	StringBuffer toDigitalSignature = new StringBuffer();
        for (int i = 0; i < digitalSignature.length; ++i) {
        	toDigitalSignature.append(Integer.toHexString(0x0100 + (digitalSignature[i] & 0x00FF)).substring(1));
        }
        this.digitalSignature = toDigitalSignature.toString();
        this.nonce = nonce;
        this.timestamp = timestamp;
    }
    @JsonIgnore
    public byte[] getDigitalSignature() {
        return DatatypeConverter.parseHexBinary(digitalSignature);
    }
    
    public String getStringDigitalSignature() {
        return digitalSignature;
    }

    public long getNonce() {
        return nonce;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
