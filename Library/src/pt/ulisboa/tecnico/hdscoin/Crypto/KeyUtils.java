package pt.ulisboa.tecnico.hdscoin.Crypto;

import javax.xml.bind.DatatypeConverter;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class KeyUtils {

    public static  <K extends Key> String keyToString(K key) {
        byte[] keyBytes = key.getEncoded();
        StringBuffer toReturn = new StringBuffer();
        for (int i = 0; i < keyBytes.length; ++i) {
            toReturn.append(Integer.toHexString(0x0100 + (keyBytes[i] & 0x00FF)).substring(1));
        }
        return toReturn.toString();
    }

    public static void generateRSAKeysToFile() throws FileNotFoundException, UnsupportedEncodingException, NoSuchAlgorithmException {
        PrintWriter writer = new PrintWriter("passwd", "UTF-8");
        for(int i = 0; i < 5; i++){
            KeyPair kp = KeyUtils.getRSAKeyPair();
            String publicKey = KeyUtils.keyToString(kp.getPublic());
            String privateKey = KeyUtils.keyToString(kp.getPrivate());
            writer.println(publicKey + " " + privateKey);
        }
        writer.close();
    }


    public static Key stringToKey(String key, boolean isPublic) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] bytes = DatatypeConverter.parseHexBinary(key);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        if(isPublic){
            X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(bytes);
            RSAPublicKey pubKey = (RSAPublicKey) kf.generatePublic(keySpecX509);
            return pubKey;
        } else {
            PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(bytes);
            PrivateKey privKey = kf.generatePrivate(keySpecPKCS8);
            return privKey;
        }
    }

    public static KeyPair getRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.genKeyPair();
    }
}
