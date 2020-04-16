import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
	public static BigInteger hash(String ip, int port, int m) throws UnknownHostException, IOException, NoSuchAlgorithmException {
		//Compute the SHA-1 hash
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		digest.reset();

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		//Convert ip address and port to byte array
		output.write(InetAddress.getByName(ip).getAddress());
		output.write(intToBytes(port));
		byte[] hash = digest.digest(output.toByteArray());
		output.close();
		
		return new BigInteger(1, hash);
	}
	
	public static BigInteger hash(String value) throws NoSuchAlgorithmException {
		//Compute the SHA-1 hash
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		digest.reset();

		byte[] hash = digest.digest(value.getBytes());
		
		return new BigInteger(1, hash);
	}

	private static byte[] intToBytes(final int data) {
		return new byte[] {
				(byte)((data >> 24) & 0xff),
				(byte)((data >> 16) & 0xff),
				(byte)((data >> 8) & 0xff),
				(byte)((data >> 0) & 0xff),
		};
	}

	public static String toBitString(final byte[] b) {
		final char[] bits = new char[8 * b.length];
		for(int i = 0; i < b.length; i++) {
			final byte byteval = b[i];
			int bytei = i << 3;
			int mask = 0x1;
			for(int j = 7; j >= 0; j--) {
				final int bitval = byteval & mask;
				if(bitval == 0) {
					bits[bytei + j] = '0';
				} else {
					bits[bytei + j] = '1';
				}
				mask <<= 1;
			}
		}
		return String.valueOf(bits);
	}
}
