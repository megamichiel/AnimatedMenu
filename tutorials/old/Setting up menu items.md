What would a menu be without items?

Settings up items can be done with ease, especially when you are used to it.

An example of how the Items section might look:<br/>
<b>Items:</b><br/>
&nbsp;&nbsp;<b>itemname:</b><br/>
&nbsp;&nbsp;&nbsp;&nbsp;<b>key:</b> value<br/>
&nbsp;&nbsp;&nbsp;&nbsp;<b>anotherkey:</b> anothervalue<br/>
&nbsp;&nbsp;<b>anotheritem:</b><br/>
&nbsp;&nbsp;&nbsp;&nbsp;<b>key:</b> value

NEW: Since 2.7.3, you can simply use lists to specify items,
 this way you don't have to insert a new name for each item.<br/>
 Each dash (-) specifies that a new item should start.<br/>
<b>Items:</b><br/>
- <b>key:</b> value<br/>
&nbsp;&nbsp;<b>anotherkey:</b> anothervalue<br/>
- <b>key:</b> value

Some keys are marked with [Animatable]. This means that they are optionally animated. For example, a non-animated key and value:<br/>
<b>Material:</b> stone:1:0<br/>
An animated value:<br/>
<b>Material:</b><br/>
&nbsp;&nbsp;<b>1:</b> stone:1:0<br/>
&nbsp;&nbsp;<b>2:</b> dirt:1:0<br/>
&nbsp;&nbsp;<b>3:</b> sand:1:0<br/>
&nbsp;&nbsp;<b>Random:</b> true<br/>
The 'Random: true' line is optional. If you add this, it will cycle through frames randomly
  

There are a few keys where you can customize your item with:
<ul>
  <li id="slot">
    <b>Slot</b><br/>
    The slot to place the item in. Starts at 1<br/>
    If you have AnimatedMenu Plus, you can also use placeholders & javascript in this.<br/>
    You can use the PlaceholderAPI placeholders with %, as well as:
    <ul>
        <li><b>{first_empty}</b> - The first empty slot in the menu</li>
        <li><b>{first_empty_&lt;slot&gt;}</b> - The first empty slot in the menu, starting at &lt;slot&gt;</li>
        <li><b>{last_empty}</b> - The last empty slot in the menu</li>
        <li><b>{last_empty_&lt;slot&gt;}</b> - The last empty slot in the menu, starting at &lt;slot&gt;</li>
    </ul>
  </li>
  <li id="framedelay">
    <b>Frame-Delay</b><br/>
    The delay in ticks between item frame updates.<br/>
    20 ticks = 1 second<br/>
    Default value is 20
  </li>
  <li id="refreshdelay">
    <b>Refresh-Delay</b><br/>
    The delay in ticks between item refreshes.<br/>
    20 ticks = 1 second<br/>
    Default value is equal to Frame-Delay
  </li>
  <li id="material">
    <b>Material</b> [Animatable]<br/>
    The material of the item, in format;
    &lt;type or id&gt;:&lt;amount&gt;:&lt;data value&gt;<br/>
    Default value is stone
  </li>
  <li id="name">
    <b>Name</b> [Animatable]<br/>
    The name of the item. Can be colored using & and supports placeholders
  </li>
  <li id="lore">
    <b>Lore</b> [Animatable]<br/>
    The lore of the item. Each line can be colored using & and supports placeholders
  </li>
  <li id="enchantments">
    <b>Enchantments</b> [Animatable with AnimatedMenu Plus]<br/>
    The enchantments to be applied to the item, e.g:<br/>
    Enchantments:<br/>
    - 32:5<br/>
    - unbreaking:3<br/>
    This adds efficiency (ID 32) level 5 and unbreaking (ID 34) level 3 to the item. It uses either IDs or minecraft enchantment names.
  </li>
  <li id="color">
  	<b>Color</b> [Animatable with AnimatedMenu Plus]</br>
  	Set leather armor's color, using one of these formats:<br/>
  	&lt;red>, &lt;green&gt;, &lt;blue&gt; e.g. 0, 255, 0 is green<br/>
  	&lt;hex color&gt; e.g. 00FF00 is green.
  </li>
  <li id="skullowner">
  	<b>SkullOwner</b> [Animatable with AnimatedMenu Plus]</br>
  	Set a skull's owner, self-explanatory
  </li>
  <li id="bannerpattern">
  	<b>BannerPattern</b> [Animatable with AnimatedMenu Plus]</br>
  	Set a banner's pattern. At http://www.needcoolshoes.com/ you can create a banner</br>
  	When you have customized your banner, the title will say something like http://www.needcoolshoes.com/banner?=paap<br/>
  	You copy the 'paap' part into this value
  </li>
  <li id="hideflags">
  	<b>Hide-Flags</b> [Animatable with AnimatedMenu Plus]</br>
  	Set the item's hide flags to hide things such as enchantments.<br/>
  	Visit http://minecraft.gamepedia.com/Tutorials/Command\_NBT\_tags for reference. If you can't find it, hit CTRL+F and search for "HideFlags"
  </li>
  <li id="hideperm">
    <b>Hide-Permission</b><br/>
    If the player doesn't have this permission, the item is hidden.<br/>
    Start it with a dash (-) to negate the effect, so the item will be hidden when the player dóes have the permission.
  </li>
  <li id="hidescript">
    <b>Hide-Script [AnimatedMenu Plus feature]</b><br/>
    Uses a script to determine whether the player can view this item or not
  </li>
  <li id="commands">
    <b>Commands</b><br/>
    This has been revamped from version 2.2.0<br/>
    See <a href="https://github.com/megamichiel/AnimatedMenu/blob/master/tutorials/Setting%20up%20item%20commands.md">this</a> page
  </li>
</ul>