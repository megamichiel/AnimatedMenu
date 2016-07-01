This page describes how to set up commands.

If dealt with correctly, it might look like something like this:

Commands:<br/>
&nbsp;&nbsp;example:<br/>
&nbsp;&nbsp;&nbsp;&nbsp;Commands:<br/>
&nbsp;&nbsp;&nbsp;&nbsp;- 'message: &aHello %player_name%!'</br>
&nbsp;&nbsp;&nbsp;&nbsp;Permission: 'animatedmenu.examplepermission'<br/>
The 'example' can be any name; it is not used for anything. It's just to sort things<br/>
You can have unlimited amounts of this, as long as they're all under the Commands section<br/>
Each of the below keys are not required

There are a few keys which you can customize it with:
<ul>
  <li>
    <b>Click-Type</b><br/>
    The type the click has to be for this section to handle the click.<br/>
    Can be either 'right', 'left' or 'both'. Default is 'both'
  </li>
  <li>
    <b>Shift-Click</b><br/>
    Whether or not this click has to be a shift click.<br/>
    If this is set to 'both', both shift and non-shift clicks will execute this section.<br/>
    Set to 'true', 'yes', 'on' or 'enable' to make this execute only on shift clicks. Any other value will execute when shift is not pressed.<br/>
    Default is 'both'
  </li>
  <li>
    <b>Permission</b><br/>
    The permission to use this item
  </li>
  <li>
    <b>Permission-Message</b><br/>
    The message to send if the player doesn't have the permission specified in Permission.<br/>
    Default value is "&6You are not permitted to do that!"
  </li>
  <li>
    <b>Price</b><br/>
    The amount of money required to use this item (Uses Vault)<br/>
    This removes the money from the user<br/>
  </li>
  <li>
    <b>Price-Message</b><br/>
    Sent to the player if their balance is lower than Price
  </li>
  <li>
    <b>Points</b><br/>
    The amount of points required to use this item (Uses PlayerPoints)<br/>
    This, again, removes the points from the user.
  </li>
  <li>
    <b>Points-Message</b><br/>
    Sent to the player if their points balance is lower than Points
  </li>
  <li>
    <b>Bypass-Permission</b><br/>
    If the player has this permission, the player does not have to have the permission or have the amount of money required as specified in this section.
  </li>
  <li>
    <b>Close</b><br/>
    If set to 'true', 'yes', 'on' or 'enable', the player's inventory will be closed after these commands are performed.
  </li>
  <li>
    <b>Commands</b><br/>
    The commands to be performed if this is not cancelled by permission or economy.
    e.g.:<br/>
    Commands:<br/>
    - 'say Hello everyone!'<br/>
    - 'server: Lobby'<br/>
    This dóes support animations. For example:<br/>
    Commands:<br/>
    &nbsp;&nbsp;1:<br/>
    &nbsp;&nbsp;- 'say First Message'<br/>
    &nbsp;&nbsp;2:<br/>
    &nbsp;&nbsp;- 'say Second Message'<br/>
    &nbsp;&nbsp;Random: true<br/>
    Will make it cycle between the 2 message. If you remove "Random: true" it will cycle through the numbers in order<br/>
    Special command prefixes:<br/>
    - '<b>console</b>': Make the console execute a command, e.g. 'console: say Hello everybody!'<br/>
    - '<b>message</b>': Send a message to the player, e.g. 'message: Hello person!'<br/>
    - '<b>op</b>': Make the player perform a command as op, e.g. 'op: give %player_name% diamond_sword'<br/>
    - '<b>broadcast</b>': Broadcast a message for everyone on the server, e.g. 'broadcast: Hello everybody!'<br/>
    - '<b>server</b>': Send the player to a specific server, e.g. 'server: Lobby'<br/>
    - '<b>menu</b>': Make the player open a specific menu, e.g. 'menu: example'<br/>
    - '<b>tellraw</b>': Send a raw (json) message to the player, e.g. 'tellraw: {text:"Hello ",extra:[{selector:"@p"},{text:"!"}]}'<br/>
    - '<b>sound</b>': Play a sound to a player. The format is 'sound: <name> [volume] [pitch]'<br/>
    By default (if no prefix is specified) the player will perform the command
  </li>
  <li>
    <b>Buy-Commands</b><br/>
    The commands to perform if the player buys the item using Price or Points.<br/>
    These support the same prefixes as the Commands section
  </li>
</ul>
