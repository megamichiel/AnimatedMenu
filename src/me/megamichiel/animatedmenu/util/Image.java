package me.megamichiel.animatedmenu.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import lombok.Getter;

import org.bukkit.ChatColor;

public class Image {
	
	private static final int[] colors = {
		0x000000,	// BLACK
		0x0000AA,	// DARK_BLUE
		0x00AA00,	// DARK_GREEN
		0x00AAAA,	// DARK_AQUA
		0xAA0000,	// DARK_RED
		0xAA00AA,	// DARK_PURPLE
		0xFFAA00,	// GOLD
		0xAAAAAA,	// GRAY
		0x555555,	// DARK_GRAY
		0x5555FF,	// BLUE
		0x55FF55,	// GREEN
		0x55FFFF,	// AQUA
		0xFF5555,	// RED
		0xFF55FF,	// LIGHT_PURPLE
		0xFFFF55,	// YELLOW
		0xFFFFFF	// WHITE
	};
	
	@Getter
	private final StringBundle[] lines;
	
	public Image(Nagger nagger, File file) throws IOException {
		BufferedImage img = ImageIO.read(file);
		int height = img.getHeight(), width = img.getWidth();
		lines = new StringBundle[height];
		for (int y = 0; y < height; y++) {
			StringBuilder sb = new StringBuilder();
			for (int x = 0; x < width; x++) {
				sb.append(matchColor(img.getRGB(x, y)).toString() + StringUtil.BOX);
			}
			lines[y] = new StringBundle(nagger, sb.toString());
		}
	}
	
	private ChatColor matchColor(int rgb) {
		int distance = 0;
		int index = -1;
		for (int i = 0; i < colors.length; i++) {
			int dist = Math.abs(colors[i] - rgb);
			if (index == -1 || dist < distance) {
				index = i;
				distance = dist;
			}
		}
		return ChatColor.values()[index];
	}
}
