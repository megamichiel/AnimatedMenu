package me.megamichiel.animatedmenu.animation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import lombok.NoArgsConstructor;
import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.animation.AnimatedLore.Frame;
import me.megamichiel.animatedmenu.menu.AnimatedMenu;
import me.megamichiel.animatedmenu.util.Image;
import me.megamichiel.animatedmenu.util.StringBundle;
import me.megamichiel.animatedmenu.util.StringUtil;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

@NoArgsConstructor
public class AnimatedLore extends Animatable<Frame> {
	
	private static final long serialVersionUID = 6241886968360414688L;
	
	public AnimatedLore(Collection<? extends Frame> c) {
		super(c);
	}
	
	public AnimatedLore(Frame[] elements) {
		super(elements);
	}
	
	@Override
	public void load(AnimatedMenuPlugin plugin, AnimatedMenu menu, ConfigurationSection section) {
		List<String> list;
		for (int num = 1; !(list = section.getStringList(String.valueOf(num))).isEmpty(); num++)
		{
			Frame frame = new Frame();
			for(String item : list) {
				if(item.toLowerCase().startsWith("file: ")) {
					File file = new File(plugin.getDataFolder(), "images/" + item.substring(6));
					if(file.exists()) {
						try {
							frame.addAll(Arrays.asList(new Image(plugin, file).getLines()));
						} catch (IOException e) {
							plugin.nag("Failed to read file " + file.getName() + "! Please report this error:");
							e.printStackTrace();
						}
						continue;
					}
				}
				frame.add(StringUtil.parseBundle(plugin, item).colorAmpersands());
			}
			add(frame);
		}
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
		
		public List<String> toStringList(Player p) {
			List<String> list = new ArrayList<String>(size());
			for (StringBundle bundle : this)
				list.add(bundle.toString(p));
			return list;
		}
		
		public String toString(Player p) {
			StringBuilder sb = new StringBuilder();
			for (StringBundle bundle : this)
				sb.append(bundle.toString(p));
			return sb.toString();
		}
	}
}
