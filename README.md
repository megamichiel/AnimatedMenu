<h1>Welcome to the official AnimatedMenu repository!</h1><br>
Current Version: <b>2.1.0</b><br>
Feel free to have a look around or use the code for private use. A tutorial on how to use the AnimatedMenu API is soon to come!
<br>
<h3>Used Depencies</h3>
AnimatedMenu utilises a few plugins for stuff like commands. These are the ones included in the project:
<ul>
	<li>Craftbukkit 1.8.8</li>
	<li>Vault</li>
	<li>PlayerPoints</li>
	<li>lombok</li>
	<li>PlaceholderAPI</li>
</ul>
<h3>AnimatedMenu API</h3>
AnimatedMenu has a simple API to create your own menus, placeholders, command handlers and more to come!<br>
There are 2 ways to obtain the AnimatedMenuPlugin instance to modify it to your likings:
<ol>
	<li>Call AnimatedMenuPlugin.getInstance(). I do not recommend this because if the plugin doesn't exists it will error</li>
	<li>Cast Bukkit.getPluginManager().getPlugin("AnimatedMenu") to AnimatedMenuPlugin. Using this, you can check if the plugin exists before getting the instance. Could be used for soft depencies</li>
	<li>
		You can also listen for the AnimatedMenuPostLoadEvent/AnimatedMenuPreloadEvent. I highly recommend using this because whenever the plugin reloads, placeholders and menus are reloaded so you have to re-add them.<br>
		The AnimatedMenuPreLoadEvent event is called before menus are loaded, so you can add your own placeholders and command handlers. The AnimatedMenuPostLoadEvent is called after menus are loaded so you can add your own menus.<br>
		To ensure plugins that depend on AnimatedMenu are able to listen for the event when the plugin first enables, these events and menu loads are called 1 tick after plugins are enabled. Both of these events have a getPlugin() method
	</li>
</ol>
There are a few things you can do with the plugin. Let's start off by looking at placeholders:
<ol>
	<li>
		To create a placeholder, make a class that extends me.megamichiel.animatedmenu.placeholder.PlaceHolderInfo (or use an inner class)<br>
		There is a 3 arguments constructor:
		<ol>
			<li>A String which specifies the name/identifier (%&#60name&#62%)</li>
			<li>The default value of the placeholder (String)</li>
			<li>Whether the placeholder requires an argument or not (%name%/&#60arguments&#62) (boolean)</li>
		</ol>
		Then there are 2 abstract methods which you need to override:
		<ul>
			<li>
				<b>boolean init(PlaceHolder placeHolder, String arg)</b><br>
				Called when a PlaceHolder is loaded. This can be used to store data in the PlaceHolder using PlaceHolder#addData(Object o) and PlaceHolder#getData(int index)<br>
				The arg (If the placeholder doesn't require an argument then this is empty) is what comes behind the / in %&#60name&#62/&#60argument&#62%<br>
				The method should return true if data is successfully loaded. If this method returns false the default value specified in the constructor is used.
			</li>
			<li>
				<b>String getReplacement(PlaceHolder placeHolder)</b><br>
				Here you should specify what to replace the PlaceHolder with. This is called each time the item goes to the next frame.<br>
				Using PlaceHolder#getData(int index) you can retrieve data added in the init method
			</li>
		</ul>
	</li>
	<li>Once this has been done, you can add the placeholder by calling AnimatedMenuPlugin#getPlaceHolders().add(PlaceHolder)</li>
</ol>
If you want to add your own command handlers (Think of things like 'console: <command>', 'message: <command>') these are the things you need to do:
<ol>
	<li>
		First, you need to make a class extending me.megamichiel.animatedmenu.command.CommandHandler<br>
		This class includes:
		<ul>
			<li>A constructor with 1 argument of type String where you specify the command prefix (e.g. "message")</li>
			<li>
				<b>me.megamichiel.animatedmenu.command.Command getCommand(String command)</b><br>
				Called when the command is loaded. The argument specifies the command argument, and the created Command is returned.<br>
				The Command class includes a constructor consisting of 1 String argument, where you put in the command.<br>
				In order to give the command a usage, you need to override <b>void execute(Player p)</b>.<br>
				Using the method <b>getCommand()</b> in the Command class you can get the command as specified in the constructor.
			</li>
		</ul>
	</li>
	<li>
		Once you have created your CommandHandler, you need to register it using AnimatedMenuPlugin#getCommandHandlers().add(CommandHandler)</li>
	</li>
</ol>