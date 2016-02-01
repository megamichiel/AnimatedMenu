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
    <b>Hide-Permission</b><br/>
    If the player doesn't have this permission, the item is hidden
  </li>
  <li>
    <b>Commands</b><br/>
    This has been revamped from version 2.2.0<br/>
    See <a href="https://github.com/megamichiel/AnimatedMenu/blob/master/tutorials/Setting%20up%20item%20commands.md">this</a> page
  </li>
</ul>