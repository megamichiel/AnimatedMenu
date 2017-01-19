<h1>Menus</h1>
  Menus are where it all starts in the plugin<br/>
<h3>Standard features</h3>
<ul>
  <li id="menu_name"><b>Menu-Name</b><br/>
    Animatable, Supports placeholders<br/>
    <hr/>
      This menu's title<br/>
    <br/>
  <li id="menu_type"><b>Menu-Type</b><br/>
    <hr/>
      The type of this menu, one of the following:<br/>
      'Chest', 'Hopper', 'Dispenser', 'Dropper', 'Crafting'<br/>
      If the type is 'Chest', or it is not specified, see <a href="#rows">Rows</a><br/>
    <br/>
  <li id="rows"><b>Rows</b><br/>
    <hr/>
      If <a href="#menu_type">Menu-Type</a> is set to 'Chest' or not set, this is used<br/>
      A number of 1 through 6 that specifies the amount of rows in the menu<br/>
    <br/>
  <li id="permission"><b>Permission</b><br/>
    Supports placeholders<br/>
    <hr/>
      The permission required to open this menu<br/>
    <br/>
  <li id="permission_message"><b>Permission-Message</b><br/>
    Supports placeholders<br/>
    <hr/>
      The message to send when the player does not have the Permission<br/>
    <br/>
  <li id="title_update_delay"><b>Title-Update-Delay</b><br/>
    <hr/>
      The interval in ticks between each title update<br/>
    <br/>
  <li id="menu_opener"><b>Menu-Opener</b><br/>
    <hr/>
      The item to use to open the menu, format is &lt;type&gt;:&lt;amount&gt;:&lt;data&gt;<br/>
    <br/>
  <li id="menu_opener_name"><b>Menu-Opener-Name</b><br/>
    <hr/>
      The display name <a href="#menu_opener">Menu-Opener</a> must have to open the menu<br/>
    <br/>
  <li id="menu_opener_lore"><b>Menu-Opener-Lore</b><br/>
    <hr/>
      The lore <a href="#menu_opener">Menu-Opener</a> must have to open the menu<br/>
    <br/>
  <li id="menu_opener_slot"><b>Menu-Opener-Slot</b><br/>
    <hr/>
      The slot to put <a href="#menu_opener">Menu-Opener</a> in when a player joins<br/>
    <br/>
  <li id="open_on_join"><b>Open-On-Join</b><br/>
    <hr/>
      Whether the menu should open when a player joins<br/>
    <br/>
  <li id="open_sound"><b>Open-Sound</b><br/>
    <hr/>
      The sound to play when the menu is opened<br/>
      NOTE: This uses /playsound command names. For a list of sounds, see <a href="http://www.minecraftforum.net/forums/mapping-and-modding/mapping-and-modding-tutorials/1571574-all-minecraft-playsound-file-names-1-9">this page</a><br/>
    <br/>
  <li id="open_sound_pitch"><b>Open-Sound-Pitch</b><br/>
    <hr/>
      The pitch of the open sound<br/>
    <br/>
  <li id="command"><b>Command</b><br/>
    <hr/>
      The command to type to open the menu<br/>
      Use ; to specify multiple commands, e.g. 'command1; command2'<br/>
    <br/>
  <li id="hide_from_command"><b>Hide-From-Command</b><br/>
    <hr/>
      Prevent this menu from being openable through /animatedmenu open<br/>
    <br/>
  <li id="items"><b>Items</b><br/>
    Section<br/>
    <hr/>
      The items to put in this menu<br/>
      See the <a href="items.md">Items</a> page for info on how to set this up<br/>
    <br/>
</ul><h3>Plus features</h3>
<ul>
  <li id="slot_update_delay"><b>Slot-Update-Delay</b><br/>
    <hr/>
      The interval in ticks between each item slot update<br/>
    <br/>
  <li id="open_animation"><b>Open-Animation</b><br/>
    Animatable<br/>
    <hr/>
      The animation to play when the menu opens, in format &lt;type&gt;[:&lt;speed&gt;].<br/>
      &lt;type&gt; can be a custom animation specified in config.yml, or one of:<br/>
    <ul>
      <li>down, up, right, left</li>
      <li>up-left, up-right, down-left, down-right</li>
      <li>out, in</li>
      <li>snake-down, snake-up, snake-right, snake-left</li>
    </ul>
    <br/>
  <li id="animation_speed"><b>Animation-Speed</b><br/>
    <hr/>
      The default speed of the open animation(s)<br/>
    <br/>
  <li id="empty_item"><b>Empty-Item</b><br/>
    <hr/>
      The item to put in empty slots, see <a href="items.md">Items</a> for the format<br/>
    <br/>
  <li id="open_commands"><b>Open-Commands</b><br/>
    <hr/>
      The commands to perform when the menu opens, see <a href="click_handlers.md#commands">Commands</a><br/>
    <br/>
  <li id="close_commands"><b>Close-Commands</b><br/>
    <hr/>
      The commands to perform when the menu closes, see <a href="click_handlers.md#commands">Commands</a><br/>
    <br/>
  <li id="sql_await"><b>Sql-Await</b><br/>
    <hr/>
      A comma-separated list of Sql Queries to request before opening the menu<br/>
      This happens asynchronously, which means that your server won't be paused by this<br/>
      You can use <a href="#wait_message">Wait-Message</a> to specify a message to send when waiting<br/>
    <br/>
  <li id="wait_message"><b>Wait-Message</b><br/>
    Supports placeholders<br/>
    <hr/>
      The message to send when waiting for Sql-Await to complete<br/>
    <br/>
  <li id="admin"><b>Admin</b><br/>
    <hr/>
      Set to 'true' to make this an administrative menu. When this is the case:<br/>
    <ul>
      <li>The menu is hidden from /animatedmenu open</li>
      <li>The menu can only be opened through a custom command by using /&lt;command&gt; &lt;player&gt;</li>
    </ul>
      You can then use %menuadmin_&lt;placeholder&gt;% to use &lt;placeholder&gt; as the target player<br/>
      %menuadmin_player_name% would retrieve the target player's name<br/>
      When switching between admin menus, the target player will retain until the menu is closed<br/>
    <br/>
  <li id="require_other"><b>Require-Other</b><br/>
    <hr/>
      When <a href="#admin">Admin</a> is enabled, this specifies whether another player needs to be specified<br/>
      If this is set to 'false', the effects of <a href="#admin">Admin</a> no longer apply and the menuadmin placeholder uses the player itself<br/>
    <br/>
</ul>