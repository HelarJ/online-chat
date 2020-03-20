package ee.oop.onlinechat;

import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetWriter;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.AeadKeyTemplates;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class CryptoGenerate {
    public static void main(String[] args) throws GeneralSecurityException, IOException {
        AeadConfig.register();
        // Generate the key material...
        KeysetHandle keysetHandle = KeysetHandle.generateNew(
                AeadKeyTemplates.AES128_GCM);

        // and write it to a file...
        String keysetFilename = "my_keyset.json";
        // encrypted with the this key in AWS KMS
        CleartextKeysetHandle.write(keysetHandle, JsonKeysetWriter.withFile(new File(keysetFilename)));
    }

}
