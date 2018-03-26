package Crypto;

import java.io.Serializable;

/**
 * Contains the params necessary to verifiy integrity
 * It's missing the IV
 * Isn't really needed now but I was doing it manually before I discovered Java's digital sig
 */
public class IntegrityCheck implements Serializable{

    private byte[] digitalSignature;
    private long nonce;
    private long timestamp;

    public IntegrityCheck(byte[] digitalSignature, long nonce, long timestamp) {
        this.digitalSignature = digitalSignature;
        this.nonce = nonce;
        this.timestamp = timestamp;
    }

    public byte[] getDigitalSignature() {
        return digitalSignature;
    }

    public long getNonce() {
        return nonce;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
