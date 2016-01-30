What would a menu be without items?

Settings up items can be done with ease, especially when you are used to it.

An example of how the Items section might look:<br/>
Items:<br/>
&nbsp;&nbsp;itemname:<br/>
&nbsp;&nbsp;&nbsp;&nbsp;key: value<br/>
&nbsp;&nbsp;&nbsp;&nbsp;anotherkey: anothervalue<br/>
&nbsp;&nbsp;anotheritem:<br/>
&nbsp;&nbsp;&nbsp;&nbsp;key: value

The Material, Name and Lore are optionally animated. Use this format:<br/>
Material:<br/>
&nbsp;&nbsp;1: stone:1:0<br/>
&nbsp;&nbsp;2: stone:2:0<br/>
Name:<br/>
&nbsp;&nbsp;1: &6Hai<br/>
&nbsp;&nbsp;2: &aHai<br/>
Lore:<br/>
&nbsp;&nbsp;1:<br/>
&nbsp;&nbsp;- &aLine1<br/>
&nbsp;&nbsp;- &bLine2<br/>
&nbsp;&nbsp;2:<br/>
&nbsp;&nbsp;- &bLine1<br/>
&nbsp;&nbsp;- &aLine2<br/>
to animate them, and use<br/>
Material: stone:1:0<br/>
Name: &6Hai<br/>
Lore:<br/>
- &aLine1<br/>
- &bLine2<br/>
to not use animations.

There are a few keys where you can customize your item with:
<ul>
  <li>
    <b>Slot</b><br/>
    The slot to place the item in. Starts at 1
  </li>
  <li>
    <b>Frame-Delay</b><br/>
    The delay in ticks between item updates.
    20 ticks = 1 second<br/>
    Default value is 20
  </li>
  <li>
    <b>Material</b><br/>
    The material of the item, in format;
    &lt;type or id&gt;:&lt;amount&gt;:&lt;data value&gt;<br/>
    Default value is stone
  </li>
  <li>
    <b>Name</b><br/>
    The name of the item. Can be colored using & and supports placeholders
  </li>
  <li>
    <b>Lore</b><br/>
    The lore of the item. Each line can be colored using & and supports placeholders
  </li>
  <li>
    <b>Enchantments</b><br/>
    The enchantments to be applied to the item, e.g:<br/>
    Enchantments:<br/>
    - 32:5<br/>
    - 34:3<br/>
    This adds efficiency (ID 32) level 5 and unbreaking (ID 34) level 3 to the item.
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
    <b>Hide</b><br/>
    If set to "true", the item will not be visible if the player does not have the permission specified in Permission.
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
    <b>Close</b><br/>
    If set to "true", the player's inventory will be closed when the item is clicked.
  </li>
  <li>
    <b>Bypass-Permission</b><br/>
    If the player has this permission, the player does not have to have the permission or have the amount of money required.
  </li>
  <li>
    <b>Commands</b><br/>
    The commands to be performed when the item is clicked and the click is not stopped by things above.<br/>
    e.g.:<br/>
    Commands:<br/>
    - 'say Hello everyone!'<br/>
    - 'server: Lobby'<br/>
    Alternative types:
    <b>Right-Click-Commands</b>, <b>Left-Click-Commands</b>, <b>Shift-Right-Click-Commands</b>, <b>Shift-Left-Click-Commands</b>, <b>Non-Shift-Right-Click-Commands</b>, <b>Non-Shift-Left-Click-Commands</b><br/>
    Special command prefixes:<br/>
    - '<b>console</b>': Make the console execute a command, e.g. 'console: say Hello everybody!'<br/>
    - '<b>message</b>': Send a message to the player, e.g. 'message: Hello person!'<br/>
    - '<b>op</b>': Make the player perform a command as op, e.g. 'op: give %player_name% diamond_sword'<br/>
    - '<b>broadcast</b>': Broadcast a message for everyone on the server, e.g. 'broadcast: Hello everybody!'<br/>
    - '<b>server</b>': Send the player to a specific server, e.g. 'server: Lobby'<br/>
    - '<b>menu</b>': Make the player open a specific menu, e.g. 'menu: example'<br/>
    By default the player will perform the command
  </li>
  <li>
    <b>Buy-Commands</b><br/>
    The commands to perform if the player buys the item using Price or Points
  </li>
</ul>