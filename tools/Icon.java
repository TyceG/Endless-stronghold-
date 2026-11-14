import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Draws the mod icon: a 16 x 16 stone brick texture with an eye of ender, scaled 8x. */
public class Icon {
	static int rgb(int r, int g, int b) {
		return (0xFF << 24) | (Math.max(0, Math.min(255, r)) << 16) | (Math.max(0, Math.min(255, g)) << 8) | Math.max(0, Math.min(255, b));
	}

	static long hash(long x) {
		x += 0x9E3779B97F4A7C15L;
		x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
		x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
		return x ^ (x >>> 31);
	}

	public static void main(String[] args) throws Exception {
		int[][] px = new int[16][16];
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int row = y / 4;
				boolean mortarRow = y % 4 == 3;
				int joint = row % 2 == 0 ? 7 : 3;
				boolean mortarCol = x % 8 == joint;
				int brick = row * 4 + (x + (row % 2 == 0 ? 1 : 5)) / 8;
				long n = hash(x * 31L + y * 17L);
				int v = (int) (n & 15) - 8;
				if (mortarRow || mortarCol) {
					px[y][x] = rgb(78 + v / 2, 78 + v / 2, 80 + v / 2);
				} else if (brick % 5 == 1 && (n >>> 8 & 3) != 0) {
					// Mossy brick.
					px[y][x] = rgb(92 + v, 112 + v, 74 + v);
				} else if (brick % 7 == 3 && (n >>> 12 & 7) == 0) {
					// A crack.
					px[y][x] = rgb(70, 70, 72);
				} else {
					int top = y % 4 == 0 ? 12 : 0;
					px[y][x] = rgb(122 + v + top, 122 + v + top, 124 + v + top);
				}
			}
		}
		// The eye of ender: a round green eye with a dark slit, centred.
		String[] eye = {
			"..dddd..",
			".dggggd.",
			"dgglggd.",
			"dggppggd",
			"dggppggd",
			"dgggggd.",
			".dggggd.",
			"..dddd..",
		};
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 8; x++) {
				int c = switch (eye[y].charAt(x)) {
					case 'd' -> rgb(16, 72, 62);
					case 'g' -> rgb(52 + (y < 4 ? 20 : 0), 150 + (y < 4 ? 20 : 0), 112);
					case 'l' -> rgb(170, 230, 190);
					case 'p' -> rgb(14, 34, 26);
					default -> 0;
				};
				if (c != 0) {
					px[y + 4][x + 4] = c;
				}
			}
		}
		BufferedImage img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 128; y++) {
			for (int x = 0; x < 128; x++) {
				img.setRGB(x, y, px[y / 8][x / 8]);
			}
		}
		ImageIO.write(img, "png", new File(args[0]));
	}
}
