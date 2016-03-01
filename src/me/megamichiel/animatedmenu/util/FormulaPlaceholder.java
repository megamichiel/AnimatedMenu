package me.megamichiel.animatedmenu.util;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.bukkit.entity.Player;

public class FormulaPlaceholder implements IPlaceholder<Integer> {
	
	private final Formula formula;
	
	public FormulaPlaceholder(Nagger nagger, String value) {
		value = value.trim();
<<<<<<< HEAD
		String[] multiply = value.split("\\+");
		List<Object> list = new ArrayList<Object>();
		for (String str : multiply)
		{
			String[] plus = str.trim().split("\\*");
			List<Object> list1 = new ArrayList<Object>();
			for (String str1 : plus)
				list1.add(StringUtil.parseBundle(nagger, str1.trim()));
			list.add(new Formula(FormulaType.MULTIPLY, list1));
		}
		formula = new Formula(FormulaType.SUM, list);
=======
		String[] multiply = value.split("\\*");
		List<Object> list = new ArrayList<Object>();
		for (String str : multiply)
		{
			String[] plus = str.trim().split("\\+");
			List<Object> list1 = new ArrayList<Object>();
			for (String str1 : plus)
				list1.add(StringUtil.parseBundle(nagger, str1.trim()));
			list.add(new Formula(FormulaType.SUM, list1));
		}
		formula = new Formula(FormulaType.MULTIPLY, list);
>>>>>>> 41fa8baba6eb1ea2c9a35c62bff02fad1e1de5a3
	}
	
	@Override
	public Integer invoke(Nagger nagger, Player who) {
		return formula.calculate(nagger, who);
	}
	
	public static enum FormulaType {
		SUM {
			@Override
			int calculate(List<Object> values, Nagger nagger, Player who) {
				int sum = 0;
				for (Object o : values)
				{
					if (o instanceof Formula)
						sum += ((Formula) o).calculate(nagger, who);
					else if (o instanceof StringBundle)
					{
						try
						{
							sum += Integer.parseInt(((StringBundle) o).toString(who));
						}
						catch (NumberFormatException ex) { }
					}
				}
				return sum;
			}
		},
		MULTIPLY {
			@Override
			int calculate(List<Object> values, Nagger nagger, Player who) {
				int num = 1;
				for (Object o : values)
				{
					if (o instanceof Formula)
						num *= ((Formula) o).calculate(nagger, who);
					else if (o instanceof StringBundle)
					{
						try
						{
							num *= Integer.parseInt(((StringBundle) o).toString(who));
						}
						catch (NumberFormatException ex) { }
					}
				}
				return num;
			}
		};
		
		abstract int calculate(List<Object> values, Nagger nagger, Player who);
	}
	
	@RequiredArgsConstructor
	private static class Formula {
		
		private final FormulaType formulaType;
		private final List<Object> values;
		
		int calculate(Nagger nagger, Player who)
		{
			return formulaType.calculate(values, nagger, who);
		}
	}
}
