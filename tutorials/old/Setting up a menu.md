There are only a few steps required for creating a menu:

If you've just downloaded the plugin, start up your server once. It will create a bunch of files for you.
You want to go into the plugin's folder and open up the 'menus' folder. There should be an example.yml in there.
To create a new menu, you can copy another file and rename it.

Now on to the config:

<ul>
  <li>
    <b>Menu-Type</b><br/>
    With this you can specify the menu type.<br/>
    Currently supported: hopper, dispenser, dropper or crafting<br/>
    This value is not neccassary.
  </li>
  <li>
    <b>Rows</b><br/>
    Change this to specify the amount of rows in the menu (1-6)<br/>
    If Menu-Type is set, this value will not be read, so be aware!
  </li>
  <li>
    <b>Menu-Name</b><br/>
    This is the text that will be displayed above the menu (The title)<br/>
    If this is not set, the menu's file name will be used<br/>
    This can be animated, using a format like this:<br/>
    Menu-Name:<br/>
    &nbsp;&nbsp;1: '&6Title 1'<br/>
    &nbsp;&nbsp;2: '&aTitle 2'<br/>
    &nbsp;&nbsp;Random: true<br/>
    The 'Random: true' line is optional. If you add this, it will cycle through frames randomly
  </li>
  <li>
    <b>Title-Update-Delay</b></br>
    The delay (in ticks, 20 ticks = 1 second) between the menu name switched frames.<br/>
    Default value is 20
  </li>
  <li>
    <b>Menu-Opener</b><br/>
    The item that can be used to open the menu. Format is:<br/>
    &lt;type or id&gt;:&lt;amount&gt;:&lt;data value&gt;<br/>
    e.g. Menu-Opener: stone:3:0<br/>
    Amount and data value are not required, so stone:3 will work fine as well.
  </li>
  <li>
    <b>Menu-Opener-Name</b><br/>
    The name that the menu opener must have.<br/>
    If this is not specified, any name is allowed.
  </li>
  <li>
    <b>Menu-Opener-Lore</b><br/>
    The lore the menu opener must have.<br/>
    Again, if this is not specified, any lore is allowed.
  </li>
  <li>
    <b>Menu-Opener-Slot</b><br/>
    If this is set, the item will be placed in a player's inventory at this slot when they join.<br/>
    Useful for lobbies.
  </li>
  <li>
    <b>Open-On-Join</b><br/>
    If this is set to 'true', 'yes', 'on' or 'enable' the menu opens when a player joins.
  </li>
  <li>
    <b>Open-Sound</b><br/>
    The sound to make when the menu opens (not required).
  </li>
  <li>
    <b>Open-Sound-Pitch</b><br/>
    The pitch of the open sound (makes the sound higher or deeper).
  </li>
  <li>
    <b>Open-Animation [AnimatedMenu Plus feature|Animatable]</b><br/>
    Use this to specify an animation (the order of item placement) when opening a menu.<br/>
    Format is &lt;name&gt;:&lt;speed&gt; where :&lt;speed&gt; is optional<br/>
    Possible default values:<br/>
    <ul>
      <li>Down</li>
      <li>Up</li>
      <li>Left</li>
      <li>Right</li>
      <li>Down-Left</li>
      <li>Down-Right</li>
      <li>Up-Left</li>
      <li>Up-Right</li>
      <li>In</li>
      <li>Out</li>
      <li>Snake-Down</li>
      <li>Snake-Up</li>
      <li>Snake-Left</li>
      <li>Snake-Right</li>
    </ul>
  </li>
  <li>
    <b>Command</b><br/>
    The command to be executed to open the menu (not required).<br/>
    For example, if set to 'apple' and the player types /apple this menu will be opened.<br/>
    You can have multiple commands by seperating them by a semicolon (;) and a space, e.g.:<br/>
    Command: 'apple; banana'
  </li>
  <li>
    <b>Items</b><br/>
    In here you specify all the items. Visit <a href="https://github.com/megamichiel/AnimatedMenu/blob/master/tutorials/Setting%20up%20menu%20items.md">this</a> page for reference on how to setup items.
  </li>
</ul>
