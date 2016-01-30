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
    If this is not set, the menu's file name will be used
  </li>
  <li>
    <b>Menu-Opener</b><br/>
    The item that can be used to open the menu. Format is:<br/>
    <type or id>:<amount>:<data value><br/>
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
    <b>Open-Sound</b><br/>
    The sound to make when the menu opens (not required).<br/>
  </li>
  <li>
    <b>Open-Sound-Pitch</b><br/>
    The pitch of the open sound (makes the sound higher or deeper).<br/>
  </li>
  <li>
    <b>Command</b><br/>
    The command to be executed to open the menu (not required).<br/>
    For example, if set to 'apple' and the player types /apple this menu will be opened.
  </li>
  <li>
    <b>Items</b><br/>
    In here you specify all the items. Visit <a href="">this</a> page for reference on how to setup items.
  </li>
</ul>