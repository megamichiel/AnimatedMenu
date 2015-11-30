package me.megamichiel.animatedmenu.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import lombok.Getter;

import org.bukkit.ChatColor;

public class Image {
	
	private static final int[] colors = new int[16];
	
	static {
		Color[] colors = {
			new Color(0, 0, 0),			// BLACK
			new Color(0, 0, 170),		// DARK_BLUE
			new Color(0, 170, 0),		// DARK_GREEN
			new Color(0, 170, 170),		// DARK_AQUA
			new Color(170, 0, 0),		// DARK_RED
			new Color(170, 0, 170),		// DARK_PURPLE
			new Color(255, 170, 0),		// GOLD
			new Color(170, 170, 170),	// GRAY
			new Color(85, 85, 85),		// DARK_GRAY
			new Color(85, 85, 255),		// BLUE
			new Color(85, 255, 85),		// GREEN
			new Color(85, 255, 255),	// AQUA
			new Color(255, 85, 85),		// RED
			new Color(255, 85, 255),	// LIGHT_PURPLE
			new Color(255, 255, 85),	// YELLOW
			new Color(255, 255, 255)};	// WHITE
		for(int i = 0; i < 16; i++) {
			Image.colors[i] = colors[i].getRGB();
		}
	}
	
	private final ChatColor[][] pixels;
	@Getter
	private final StringBundle[] lines;
	
	public Image(File file) throws IOException {
		BufferedImage img = ImageIO.read(file);
		pixels = new ChatColor[img.getHeight()][img.getWidth()];
		for(int y = 0; y < img.getHeight(); y++) {
			pixels[y] = new ChatColor[img.getWidth()];
			for(int x = 0; x < img.getWidth(); x++) {
				pixels[y][x] = matchColor(img.getRGB(x, y));
			}
		}
		lines = new StringBundle[pixels.length];
		for(int i = 0; i < pixels.length; i++) {
			lines[i] = new StringBundle(StringUtil.join(pixels[i], String.valueOf(StringUtil.BOX), false));
		}
	}
	
	private ChatColor matchColor(int rgb) {
		int distance = 0;
		int index = -1;
		for(int i = 0; i < colors.length; i++) {
			int dist = Math.abs(colors[i] - rgb);
			if(index == -1 || dist < distance) {
				index = i;
				distance = dist;
			}
		}
		return ChatColor.values()[index];
	}
}
