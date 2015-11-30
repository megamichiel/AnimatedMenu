package me.megamichiel.animatedmenu.placeholder;

import java.util.ArrayList;
import java.util.List;

import me.megamichiel.animatedmenu.menu.AnimatedMenu;

public class PlaceHolder {
	
	private final PlaceHolderInfo info;
	private final List<Object> data = new ArrayList<>();
	private final boolean success;
	
	public PlaceHolder(PlaceHolderInfo info, String arg) {
		this.info = info;
		success = info.init(this, arg);
	}
	
	public void addData(Object o) {
		data.add(o);
	}
	
	public <T> T getData(int index, Class<T> cast) {
		return cast.cast(data.get(index));
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getData(int index) {
		return (T) data.get(index);
	}
	
	public void init(AnimatedMenu menu) {
		if(success) {
			info.postInit(this, menu);
		}
	}
	
	@Override
	public String toString() {
		return success ? info.getReplacement(this) : info.getDefaultValue();
	}
}
