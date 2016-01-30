There are only a few steps required for creating a menu:

If you've just downloaded the plugin, start up your server once. It will create a bunch of files for you.
You want to go into the plugin's folder and open up the 'menus' folder. There should be an example.yml in there.
To create a new menu, you can copy another file and rename it.

Now on to the config:

<ul>
  <li>
    <b>Menu-Type</b>
    With this you can specify the menu type.
    Currently supported: hopper, dispenser, dropper or crafting
    This value is not neccassary.
  </li>
  <li>
    <b>Rows</b>
    Change this to specify the amount of rows in the menu (1-6)
    If Menu-Type is set, this value will not be read, so be aware!
  </li>
  <li>
    <b>Menu-Name</b>
    This is the text that will be displayed above the menu (The title)
    If this is not set, the menu's file name will be used
  </li>
  <li>
    <b>Menu-Opener</b>
    The item that can be used to open the menu. Format is:
    <type or id>:<amount>:<data value>
    e.g. Menu-Opener: stone:3:0
    Amount and data value are not required, so stone:3 will work fine as well.
  </li>
  <li>
    <b>Menu-Opener-Name</b>
    The name that the menu opener must have.
    If this is not specified, any name is allowed.
  </li>
  <li>
    <b>Menu-Opener-Lore</b>
    The lore the menu opener must have.
    Again, if this is not specified, any lore is allowed.
  </li>
  <li>
    <b>Menu-Opener-Slot</b>
    If this is set, the item will be placed in a player's inventory at this slot when they join.
    Useful for lobbies.
  </li>
  <li>
    <b>Open-Sound</b>
    The sound to make when the menu opens (not required).
  </li>
  <li>
    <b>Open-Sound-Pitch</b>
    The pitch of the open sound (makes the sound higher or deeper).
  </li>
  <li>
    <b>Command</b>
    The command to be executed to open the menu (not required).
    For example, if set to 'apple' and the player types /apple this menu will be opened.
  </li>
  <li>
    <b>Items</b>
    In here you specify all the items. Visit <a href="">this</a> page for reference on how to setup items.
  </li>
</ul>