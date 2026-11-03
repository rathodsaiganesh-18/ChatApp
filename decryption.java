import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Scanner;

public class decryption {
    private static final String SECRET_KEY = "1234567890123456"; // Same 16-byte key

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter a string to decrypt:");
        String encryptedStr = sc.nextLine();

        try {
            String decrypted = decrypt(encryptedStr);
            System.out.println("Decrypted message: " + decrypted);
        } catch (Exception e) {
            e.printStackTrace();
        }

        sc.close();
    }

    private static String decrypt(String encryptedMsg) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedMsg));
        return new String(decryptedBytes);
    }
}
