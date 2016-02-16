package me.megamichiel.animatedmenu.animation;

import java.util.Collection;

import lombok.NoArgsConstructor;
import me.megamichiel.animatedmenu.util.Nagger;
import me.megamichiel.animatedmenu.util.StringBundle;
import me.megamichiel.animatedmenu.util.StringUtil;

@NoArgsConstructor
public class AnimatedText extends Animatable<StringBundle> {
	
	private static final long serialVersionUID = 5235518796395129933L;
	
	public AnimatedText(Collection<? extends StringBundle> c) {
		super(c);
	}
	
	public AnimatedText(StringBundle... elements) {
		super(elements);
	}
	
	@Override
	protected StringBundle convert(Nagger nagger, String str) {
		return StringUtil.parseBundle(nagger, str).colorAmpersands();
	}
}
