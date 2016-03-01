package me.megamichiel.animatedmenu.util;

import java.util.Arrays;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;

import com.google.common.collect.ImmutableMap;

public class StringUtil {
	
	static final char BOX = '\u2588';
	
	public static String join(Object[] array, String split, boolean end) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < array.length; i++) {
			sb.append(array[i] + (i == array.length - 1 || end ? split : ""));
		}
		return sb.toString();
	}
	
	public static StringBundle parseBundle(Nagger nagger, String str) {
		if (str == null) return null;
		StringBundle bundle = new StringBundle(nagger);
		StringBuilder builder = new StringBuilder();
		char[] array = str.toCharArray();
		int index = 0;
		boolean escape = false;
		loop:
			while(index < array.length) {
				char c = array[index];
				if(c == '\\') {
					if(!(escape = !escape)) //Escaped back-slash
						builder.append(c);
				} else if(escape) {
					escape = false;
					switch(c) {
					case 'u': //Unicode character
						try {
							char[] unicode = Arrays.copyOfRange(array, index + 1, index + 5);
							index += 4;
							builder.append((char) Integer.parseInt(new String(unicode), 16));
						} catch (IndexOutOfBoundsException ex) {
							nagger.nag("Error on parsing unicode character in string " + str + ": "
									+ "Expected number to have 4 digits!");
							break loop;
						} catch (NumberFormatException ex) {
							nagger.nag("Error on parsing unicode character in string " + str + ": "
									+ "Invalid characters (Allowed: 0-9 and A-F)!");
							break loop;
						}
						break;
					case '%': //Escaped placeholder character
						builder.append(c);
						break;
					case 'x': case 'X':
						builder.append(BOX);
						break;
					}
				} else {
					switch(c) {
					case '%': //Start of placeholder
						if(builder.length() > 0) {
							bundle.add(builder.toString());
							builder = new StringBuilder();
						}
						index++;
						while(index < array.length && array[index] != '%')
							builder.append(array[index++]);
						if(index == array.length) {
							nagger.nag("Placeholder " + builder + " wasn't closed off! Was it a mistake or "
									+ "did you forget to escape the '%' symbol?");
							break;
						}
						Placeholder placeholder = new Placeholder(builder.toString());
						bundle.add(placeholder);
						builder = new StringBuilder();
						break;
					default:
						builder.append(c);
						break;
					}
				}
				index++;
			}
		if(builder.length() > 0)
			bundle.add(builder.toString());
		return bundle;
	}
	
	public static boolean parseBoolean(ConfigurationSection section, String key, boolean def)
	{
		return parseBoolean(section.getString(key), def);
	}
	
	public static boolean parseBoolean(String str, boolean def)
	{
		Flag flag = parseFlag(str, def ? Flag.TRUE : Flag.FALSE);
		return flag == Flag.BOTH ? false : flag.booleanValue();
	}
	
	public static Flag parseFlag(ConfigurationSection section, String key, Flag def)
	{
		return parseFlag(section.getString(key), def);
	}
	
	private static final Map<String, Flag> flags = new ImmutableMap.Builder<String, Flag>()
			.put("true", Flag.TRUE).put("yes", Flag.TRUE).put("on", Flag.TRUE).put("enable", Flag.TRUE)
			.put("both", Flag.BOTH).build();
	
	public static Flag parseFlag(String str, Flag def)
	{
		if (str == null) return def;
		Flag flag = flags.get(str.toLowerCase());
		return flag == null ? Flag.FALSE : flag;
	}
}
