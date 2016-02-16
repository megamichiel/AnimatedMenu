package me.megamichiel.animatedmenu.util;

public enum Flag {
	
	TRUE {
		@Override
		public boolean matches(boolean b) {
			return b;
		}
	}, FALSE {
		@Override
		public boolean matches(boolean b) {
			return !b;
		}
	}, BOTH {
		@Override
		public boolean matches(boolean b) {
			return true;
		}
	};
	
	public boolean matches(boolean b)
	{
		return false;
	}
	
	public boolean booleanValue()
	{
		switch (this)
		{
		case FALSE:
			return false;
		default:
			return true;
		}
	}
}
