import dev.tyceg.endlessstronghold.gen.Kind;
import dev.tyceg.endlessstronghold.gen.Part;
import dev.tyceg.endlessstronghold.gen.Region;
import dev.tyceg.endlessstronghold.gen.Stronghold;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.imageio.ImageIO;

/** A top-down map of 2 x 2 regions, each piece coloured by kind; higher pieces drawn over lower ones. */
public class MapRender {
	static Color colour(Part p) {
		if (p.trunk) {
			return new Color(230, 200, 120);
		}
		return switch (p.kind) {
			case LIBRARY -> new Color(150, 95, 50);
			case PORTAL_ROOM -> new Color(40, 170, 140);
			case ROOM_CROSSING -> new Color(120, 150, 200);
			case FIVE_CROSSING -> new Color(190, 120, 200);
			case PRISON_HALL -> new Color(200, 80, 80);
			case CHEST_CORRIDOR -> new Color(230, 160, 60);
			case STAIRS_DOWN, STRAIGHT_STAIRS_DOWN -> new Color(90, 90, 90);
			default -> new Color(165, 165, 165);
		};
	}

	public static void main(String[] args) throws Exception {
		long seed = Long.parseLong(args[1]);
		Stronghold s = new Stronghold(seed);
		int minX = Region.origin(0);
		int minZ = Region.origin(0);
		int size = Region.SIZE * 2;
		int scale = 2;
		int legend = 150;
		BufferedImage img = new BufferedImage(size * scale + legend, size * scale, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setColor(new Color(18, 18, 24));
		g.fillRect(0, 0, img.getWidth(), img.getHeight());
		List<Part> parts = new ArrayList<>();
		for (int rx = 0; rx <= 1; rx++) {
			for (int rz = 0; rz <= 1; rz++) {
				parts.addAll(s.region(rx, rz).parts);
			}
		}
		parts.sort(Comparator.comparingInt(p -> p.box.maxY()));
		for (Part p : parts) {
			int x = (p.box.minX() - minX) * scale;
			int z = (p.box.minZ() - minZ) * scale;
			int w = (p.box.maxX() - p.box.minX() + 1) * scale;
			int h = (p.box.maxZ() - p.box.minZ() + 1) * scale;
			Color c = colour(p);
			// Deeper pieces darker.
			float shade = Math.max(0.45f, Math.min(1f, 0.55f + (p.box.minY() + 30) / 120f));
			g.setColor(new Color((int) (c.getRed() * shade), (int) (c.getGreen() * shade), (int) (c.getBlue() * shade)));
			g.fillRect(x, z, w, h);
			g.setColor(new Color(0, 0, 0, 120));
			g.drawRect(x, z, w - 1, h - 1);
		}
		g.setColor(new Color(255, 255, 255, 70));
		g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[] {6f, 6f}, 0f));
		g.drawLine(Region.SIZE * scale, 0, Region.SIZE * scale, size * scale);
		g.drawLine(0, Region.SIZE * scale, size * scale, Region.SIZE * scale);
		int[] spawn = s.spawn();
		g.setColor(Color.WHITE);
		g.fillOval((spawn[0] - minX) * scale - 5, (spawn[2] - minZ) * scale - 5, 10, 10);

		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
		Object[][] keys = {
			{"Main corridors", new Color(230, 200, 120)},
			{"Corridors, turns", new Color(165, 165, 165)},
			{"Room crossings", new Color(120, 150, 200)},
			{"Five-way crossings", new Color(190, 120, 200)},
			{"Prison halls", new Color(200, 80, 80)},
			{"Chest corridors", new Color(230, 160, 60)},
			{"Libraries", new Color(150, 95, 50)},
			{"Staircases", new Color(90, 90, 90)},
			{"Portal rooms", new Color(40, 170, 140)},
			{"Spawn", Color.WHITE},
		};
		int lx = size * scale + 12;
		int ly = 20;
		for (Object[] k : keys) {
			g.setColor((Color) k[1]);
			g.fillRect(lx, ly - 10, 12, 12);
			g.setColor(new Color(220, 220, 220));
			g.drawString((String) k[0], lx + 18, ly);
			ly += 22;
		}
		g.setColor(new Color(160, 160, 160));
		g.drawString("4 regions,", lx, ly + 10);
		g.drawString("512 x 512 blocks.", lx, ly + 26);
		g.drawString("Darker = deeper.", lx, ly + 42);
		g.dispose();
		ImageIO.write(img, "png", new File(args[0]));
	}
}
