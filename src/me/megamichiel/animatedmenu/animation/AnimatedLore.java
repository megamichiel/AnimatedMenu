package me.megamichiel.animatedmenu.animation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.NoArgsConstructor;
import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.animation.AnimatedLore.Frame;
import me.megamichiel.animatedmenu.menu.AnimatedMenu;
import me.megamichiel.animatedmenu.util.Image;
import me.megamichiel.animatedmenu.util.StringBundle;
import me.megamichiel.animatedmenu.util.StringUtil;

import org.bukkit.configuration.ConfigurationSection;

@NoArgsConstructor
public class AnimatedLore extends Animatable<Frame> {
	
	private static final long serialVersionUID = 6241886968360414688L;
	private static final Pattern FILE_PATTERN = Pattern.compile("(?i)file:\\s*(\\w+)");
	
	public AnimatedLore(Collection<? extends Frame> c) {
		super(c);
	}
	
	public AnimatedLore(Frame[] elements) {
		super(elements);
	}
	
	@Override
	public void load(AnimatedMenu menu, ConfigurationSection section) {
		int num = 1;
		while(true) {
			List<String> list = section.getStringList(String.valueOf(num));
			if(list.isEmpty()) break;
			Frame frame = new Frame();
			for(String item : list) {
				Matcher m = FILE_PATTERN.matcher(item);
				if(m.matches()) {
					File file = new File(AnimatedMenuPlugin.getInstance().getDataFolder(), "images/" + m.group(1));
					if(file.exists()) {
						try {
							frame.addAll(Arrays.asList(new Image(file).getLines()));
						} catch (IOException e) {
							AnimatedMenuPlugin.getInstance().getLogger().warning("Failed to read file " + file.getName() + "! Please report this error:");
							e.printStackTrace();
						}
						continue;
					}
				}
				frame.add(StringUtil.parseBundle(item).colorAmpersands().loadPlaceHolders(menu));
			}
			add(frame);
			num++;
		}
	}
	
	@Override
	Frame convert(AnimatedMenu menu, String str) {
		return null;
	}
	
	public static class Frame extends ArrayList<StringBundle> {
		
		private static final long serialVersionUID = 3655877616632972680L;
		
		public Frame() {
			super();
		}
		
		public Frame(List<StringBundle> list) {
			super(list);
		}
		
		public Frame(StringBundle... bundles) {
			super(Arrays.asList(bundles));
		}
		
		public List<String> toStringList() {
			return StringUtil.toStringList(this);
		}
		
		@Override
		public String toString() {
			return StringUtil.join(this, "\n", true);
		}
	}
}
