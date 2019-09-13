import java.util.Random;
import java.util.Scanner;

public class p1 {
	
	private static int ARRAY_LENGTH = 0;

	// Test Driver
	public static void main(String[] args) {
		Random r = new Random();
		Scanner kb = new Scanner(System.in);
		System.out.print("\n\nChoices:\n1 == Algorithm A\n2 == Algorithm B\n\nWhich algorithm would you like to run? (1 or 2): ");
		int choice = kb.nextInt();
		if(choice == 1) System.out.println("Algorithm A chosen.");
		else if(choice == 2) System.out.println("Algorithm B chosen.");

		System.out.print("How many rounds of testing would you like to run? (1-10): ");
		int num_rounds = kb.nextInt();

		for(int n = 0; n < 4; n++) {
			if(n == 0) ARRAY_LENGTH = 32;
			if(n == 1) ARRAY_LENGTH = 64;
			if(n == 2) ARRAY_LENGTH = 128;
			if(n == 3) ARRAY_LENGTH = 256;

			byte[] e0   = new byte[ARRAY_LENGTH],	// Exponent (all 0 bits)
				   e1	= new byte[ARRAY_LENGTH],	// Exponent (all 1 bits)
				   b    = new byte[ARRAY_LENGTH]; 	// Base
			int    m    = r.nextInt(257) - 1;		// Modulus

			// Populate 'b' and 'e' with random byte values. 
			// I kept 'b' and 'm' small for this since large values of 'e' are what really affect runtime.
			for(int i = 0; i < ARRAY_LENGTH; i++) {
				e0[i] = (byte) (0 & 0xff);												// All 0 bits
				e1[i] = (byte) (255 & 0xff);											// All 1 bits
				if(i == 0 && n == 0) b[0] = (byte) ((r.nextInt(257) - 1) & 0xff);		// Since we don't care about big base values, make base random num in range (1,255)
				if(i != 0) 			 b[i] = (byte) 0;									// Rest of higher order bytes = 0 since we only want b[0]
																						// Only need b[] to be that long bc it has to be of equal length to e[]
			}

			if(choice == 1) {		// If Algorithm A chosen
				System.out.println("\nCalculating with all 0 bits, bit size: "+ARRAY_LENGTH*8);
					for(int i = 0; i < num_rounds; i++) algA(b,e0,m);
				System.out.println("\nCalculating with all 1 bits, bit size: "+ARRAY_LENGTH*8);
					for(int i = 0; i < num_rounds; i++) algA(b,e1,m);
			}
			else if(choice == 2) {	// If Algorithm B chosen
				System.out.println("\nCalculating with all 0 bits, bit size: "+ARRAY_LENGTH*8); 
					for(int i = 0; i < num_rounds; i++) algB(b,e0,m);
				System.out.println("\nCalculating with all 1 bits, bit size: "+ARRAY_LENGTH*8); 
					for(int i = 0; i < num_rounds; i++) algB(b,e1,m);
			}
			System.out.println();
		}
	}

	// Naive Solution
	public static byte[] algA(byte[] b, byte[] e, int m) {

		long start, delta;							// Stores start time, total elapsed time

		int curr, currBit;							// Will store masked byte of exponent to determine value of highest order bit
		byte[] r = new byte[ARRAY_LENGTH];			// Stores our running remainder
		for(int i = 0; i < ARRAY_LENGTH; i++) {		// Initialize r to 1
			if(i == 0) r[0] = (byte) (1 & 0xff);	
			else 	   r[i] = (byte) (0 & 0xff);
		}
		
		start = System.nanoTime();					// Get current system time
		for(int i = 0; i < ARRAY_LENGTH; i++) {		// For each byte in e (highest order to lowest)...
			curr = (e[i] & 0xff);					// Cast current byte to int
			for(int j = 0; j < 8; j++) {			// For each bit in curr...
				currBit = (curr >>> j) & 0x1;		// Right shift current byte by j bits
				if(currBit == 1) {
					r = modulo(multiply(r,b),m);	// Bit == 1: Square and Multiply
				}
				if(i != ARRAY_LENGTH - 1) r = modulo(multiply(r,r),m);	// If not last bit of exponent: Square
			}
		}
		delta = System.nanoTime() - start;	// Get total execution time
		System.out.println("Operation A took "+delta+" nanoseconds.");
		
		return r;
	}

	// Improved Solution
	public static byte[] algB(byte[] r1, byte[] e, int m) {

		long start, delta;	// Stores start time, total elapsed time

		int curr, currBit;							// Will store masked byte of exponent to determine value of highest order bit
		byte[] r0 = new byte[ARRAY_LENGTH];			// Stores our running remainder, similar to r in algA.
		for(int i = 0; i < ARRAY_LENGTH; i++) {
			if(i == 0) r0[0] = (byte) (1 & 0xff);	// Initialize r to 1
			else 	   r0[i] = (byte) (0 & 0xff);
		}

		start = System.nanoTime();
		for(int i = 0; i < ARRAY_LENGTH; i++) {		// For each byte in e[]...
			curr = (e[e.length - i - 1] & 0xff);	// Cast current lowest order byte to int
			for(int j = 0; j < 8; j++) {			// For each bit in curr...
				currBit = (curr >>> j) & 0x1;		// Retrieve next lowest bit of e[i]
				if(currBit == 0) {					// If bit == 0...
					r1 = modulo(multiply(r0,r1),m);		// r1 = (r0 * r1) mod m
					r0 = modulo(multiply(r0,r0),m);		// r0 = r0^2 mod m
				}
				else if(currBit == 1) {				// If bit == 1...
					r0 = modulo(multiply(r0,r1),m);		// r0 = (r0 * r1) mod m
					r1 = modulo(multiply(r1,r1),m);		// r1 = r1^2 mod m
				}
			}
		}

		delta = System.nanoTime() - start;								// Get execution time
		System.out.println("Operation B took "+delta+" nanoseconds.");

		return r0;	// Return remainder
	}

	private static byte[] multiply(byte[] a, byte[] b) {

		if(a.length != b.length)							// Only multiplying arrays of equals length
			throw new IllegalArgumentException();

		int rows = a.length, columns = a.length * 2;		// Number of rows equals length of multiplier
															// Number of columns (i.e. shifts in grade school mult.) equals twice multiplicand (MAYBE) minus one
		byte[][] rowsAndColumns = new byte[rows][columns];					
		int partial, overflow = 0;							// Partial product, overflow from multiplication

		for(int i = 0; i < rows; i++) {						// For each digit of the multiplicand...
			for(int j = 0; j < a.length; j++) {						// For each digit of the multiplier...
				partial = (a[j] & 0xff) * (b[i] & 0xff);			// Convert byte to unsigned int
				partial += overflow;								// Add overflow
				overflow = partial >>> 8;							// Right shift result 8 bits to get overflow
				rowsAndColumns[i][j+i] = (byte) partial;			// Store result in appropriate row / column
				if(j == columns - 1 && overflow > 0)				// If on last digit and there is overflow...
					rowsAndColumns[i][j+i+1] = (byte) overflow;			// Put overflow in column after the last digit of the partial product
			}
		}

		// Add all the resulting columns to get final product
		byte[] product = new byte[columns];
		int sum = 0, carry = 0;
		for(int j = 0; j < columns; j++) {		// From rightmost column to leftmost...
			sum += carry;							// Add carry to sum of column
			for(int i = 0; i < rows; i++)			// For each entry in the column...
				sum += (int) rowsAndColumns[i][j];		// Add to running sum of column
			carry = sum >>> 8;						// Right shift sum to get carry for next column
			product[j] = (byte) (sum & 0xff);		// Put sum of column into corresponding entry in final product
			sum = 0;
		}

		//for(int i = product.length -1; i >= 0; i--)
		//	if(product[i] != 0) System.out.print(product[i] + " ");

		return product;
	}

	private static byte[] modulo(byte[] x, int m) {
		int remainder = 0;
		for(int i = 0; i < x.length; i++) {		// Partial mod operations, basis of why modular exponentiation is useful
			remainder *= (256 % m);					// Multiply remainder by 256 mod m for signed to unsigned conversion
			remainder %= m;							// Mod new remainder by m
			remainder += (x[i] % m);				// Add mod m of byte-chunk to running remainder
			remainder %= m;							// Mod remainder by m again
		}
		byte[] result = new byte[ARRAY_LENGTH];		// Create byte array to hold the result of our mod operation
		for(int i = 0; i < result.length; i++) {	
			result[i] = (byte) (remainder & 0xff);		// Cast lowest 8 bits of remainder to convert from int to byte
			remainder = remainder >>> 8;				// Shift remainder by a byte to get next 8 bits to plug into result
		}
		return result;	// Return x mod m
	}

	 ////////////////////////////////
	/////     TEST METHODS     /////
   ////////////////////////////////
	/*
	private static byte[] byteTimesArray(byte[] a, byte m) {
		int rows = 1, columns = a.length;					// Number of rows equals length m (1)
															// Number of columns (i.e. shifts in grade school mult.) equals twice multiplicand (MAYBE) minus one
		int m_int = m& 0xff;
		byte[] product = new byte[columns];					
		int partial = 0, carry = 0;
		for(int i = 0; i < columns; i++) {		// From rightmost column to leftmost...
			partial = (a[i] & 0xff) * m_int;		// Convert byte to unsigned int
			partial += carry;
			carry = partial >>> 8;					// Right shift result 8 bits to get overflow
			product[i] = (byte) (partial & 0xff);
			if(i == columns - 1 && carry > 0)				// If on last digit and there is overflow...
				product[i+1] = (byte) carry;			// Put overflow in column after the last digit of the partial product
		}
		return product;
	}
	*/
}