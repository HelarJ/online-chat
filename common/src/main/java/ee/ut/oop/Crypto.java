package ee.ut.oop;

import com.google.crypto.tink.*;
import com.google.crypto.tink.config.TinkConfig;
import com.google.crypto.tink.hybrid.HybridKeyTemplates;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class Crypto {
    private KeysetHandle privateKeysetHandle;
    private KeysetHandle publicKeysetHandle;

    public Crypto(byte[] publicKeysetData) {
        try {
            this.publicKeysetHandle = CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(publicKeysetData));
        } catch (GeneralSecurityException | IOException e) {
            System.out.println("Error reading public key.");
        }
    }

    public Crypto() {
        try {
            TinkConfig.register();
            this.privateKeysetHandle = KeysetHandle.generateNew(
                    HybridKeyTemplates.ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM);

            publicKeysetHandle =
                    privateKeysetHandle.getPublicKeysetHandle();

        } catch (GeneralSecurityException e) {
            System.out.println("Error generating crypto: " + e.getMessage());
        }
    }

    public KeysetHandle getPublicKeysetHandle(){
        return publicKeysetHandle;
    }

    public byte[] encrypt(byte[] message){
         try {
             HybridEncrypt hybridEncrypt =
                     publicKeysetHandle.getPrimitive(HybridEncrypt.class);
             return hybridEncrypt.encrypt(message, null);

         } catch (GeneralSecurityException e) {
             System.out.println("Error encrypting message: " +e.getMessage());
         }
         return null;
    }

    public byte[] decrypt(byte[] message){
         try {
             HybridDecrypt hybridDecrypt = privateKeysetHandle.getPrimitive(
                     HybridDecrypt.class);
             return hybridDecrypt.decrypt(message, null);
         } catch (GeneralSecurityException e) {
             e.printStackTrace();
             System.out.println("Error decrypting message: " +e.getMessage());
         }
         return null;
    }
}
