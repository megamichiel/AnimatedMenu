You can make simple or extended scripts to perform actions at, for example, item clicks<br/>
If you start a line with 'file: ', you can execute script from a file. The plugin search for the files inside the 'scripts' folder inside of your AnimatedMenuPlus folder.<br/>
Each time a script is performed, there are 4 default variables you can make use of:<br/>
- <b>Server</b>: This is like Java's Bukkit.getServer(). Visit https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Server.html for all possible functions<br/>
- <b>Player</b>: The Player who clicked the item. Visit https://hub.spigotmc.org/javadocs/spigot/org/bukkit/entity/Player.html for all possible functions<br/>
- <b>GlobalVariables</b>: You can store global variables (values that can be accessed from anywhere) here<br/>
There are 2 functions inside of this: get(String key) and set(String key, Object value)<br/>
You can, for example, store a variable using GlobalVariables.set("thekey", "somevalue");<br/>
To retrieve a value, you can use GlobalVariables.get("thekey");<br/>
- <b>Plugin</b>: The AnimatedMenu plugin. There is a variety of things to do with this class:<br/>
<ul>
	<li>
		<b>getMenu(String name)</b>: Get a menu by name. If no menu is found, it returns null<br/>
		The functions the returned value has are open(Player), getName() and getViewers() (an array of players)
	</li>
	<li><b>getMenus()</b>: Get all loaded menus. This is an array of menus</li>
	<li>color(String text): Make each color with an ampersand (&) change into an actual color</li>
	<li>
		<b>createItem(int type), createItem(int type, int amount) and createItem(int type, int amount, int data)</b><br/>
		These are pretty much self-explanatory. Visit https://hub.spigotmc.org/javadocs/spigot/org/bukkit/inventory/ItemStack.html for reference on the returned value
	</li>
	<li>
		<b>createItem(String type), createItem(String type, int amount) and createItem(String type, int amount, int data)</b><br/>
		Also self-explanatory, returns the same type as the above methods. Uses minecraft's item names
	</li>
	<li><b>setBalance</b>, <b>addBalance</b> and <b>takeBalance</b>: All have parameters (Player, double)</li>
	<li><b>createList()</b>: Creates a new String list</li>
	<li><b>asList(String array)</b>: Creates a new String list out of an array</li>
	<li><b>asColoredList(String array)</b>: Creates a new String list, with each item colored</li>
</ul>
Apart from this, you can use all javascript code you would normally be able to do.