import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Scanner;

public class encryption {
    private static final String SECRET_KEY = "1234567890123456"; // 16-byte key

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter a string to encrypt:");
        String str = sc.nextLine();

        try {
            String encrypted = encrypt(str);
            System.out.println("Encrypted message: " + encrypted);
        } catch (Exception e) {
            e.printStackTrace();
        }

        sc.close();
    }

    private static String encrypt(String msg) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(msg.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }
}
