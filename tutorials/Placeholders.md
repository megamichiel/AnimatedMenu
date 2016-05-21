If you have AnimatedMenu (Plus) installed, you will get a few extra placeholders added to PlaceholderAPI.

First, there are a few ping-related placeholders. Inside the config.yml, you can specify remote connections under the Connections section. An example:<br/>
<b>Connections:</b><br/>
&nbsp;&nbsp;<b>somemotd:</b><br/>
&nbsp;&nbsp;&nbsp;&nbsp;<b>ip: </b>localhost:25565<br/>
&nbsp;&nbsp;<b>somestatus</b>:<br/>
&nbsp;&nbsp;&nbsp;&nbsp;<b>ip: </b>localhost<br/>
&nbsp;&nbsp;&nbsp;&nbsp;<b>online: </b>'&aOnline'<br/>
&nbsp;&nbsp;&nbsp;&nbsp;<b>offline: </b>'&cOffline'<br/>
&nbsp;&nbsp;<b>somemotdcheck:</b><br/>
&nbsp;&nbsp;&nbsp;&nbsp;<b>ip: </b>localhost:25565<br/>
&nbsp;&nbsp;&nbsp;&nbsp;<b>'A Minecraft Server': </b>'&aDefault server motd!'<br/>
&nbsp;&nbsp;&nbsp;&nbsp;<b>default: </b>'&cNot a default motd!'

If something doesn't work the way you expect it to, set 'Warn-Offline-Servers' to true.<br/>
This will print a message to the console when the plugin can't connect to a server and will help you find the problem.<br/>
You can specify the delay between connection refreshes (in ticks) by settings 'Connection-Refresh-Delay' to a number.

<ul>
	<li id="motd">
		<b>%animatedmenu_motd_&lt;id&gt;%</b><br/>
		Retrieve the motd of a server. The &lt;id&gt; should be specified under the Connections section, as shown above.<br/>
		To interact with 'somemotd', use %animatedmenu_motd_somemotd%
	</li>
	<li id="status">
		<b>%animatedmenu_status_&lt;id&gt;%</b><br/>
		Returns a different value for an online/offline server.<br/>
		To interact with 'somestatus', use %animatedmenu_motd_somestatus%<br/>
		If the server is online, '&aOnline' will be displayed. If the server is offline, '&cOffline' is displayed.
	</li>
	<li id="motdcheck">
		<b>%animatedmenu_motdcheck_&lt;id&gt;%</b><br/>
		Returns a specific value for different motds<br/>
		To interact with 'somemotdcheck', use %animatedmenu_motd_somemotdcheck%<br/>
		If the motd is 'A Minecraft Server', '&aDefault server motd!' will be displayed, as you can see in the Connections section. If no matching motd was found, the value at 'default' will be displayed (in this case '&cNot a default motd!').
	</li>
	<li id="online">
		<b>%animatedmenu_onlineplayers_&lt;id&gt;</b><br/>
		Returns the online player count of a specific server.
	</li>
	<li id="max">
		<b>%animatedmenu_maxplayers_&lt;id&gt;</b><br/>
		Returns the max player count of a specific server.
	</li>
</ul>

Finally, there are some random placeholders:
<ul>
	<li id="worldplayers">
		<b>%animatedmenu_worldplayers_&lt;id&gt;%</b><br/>
		Retrieve the player count in the specific world. An example: '%animatedmenu_worldplayers_world'
	</li>
	<li id="formula">
		<b>%animatedmenu_formula_&lt;id&gt;</b><br/>
		Execute a specific formula. You can specify formulas inside the config.yml under the formulas section. An example:<br/>
		<b>Formulas:</b><br/>
		&nbsp;&nbsp;<b>example: </b>'%bungeecord_server1% + %bungeecord_server2%'<br/>
		To retrieve this formula, use %animatedmenu_formula_example%
	</li>
</ul>