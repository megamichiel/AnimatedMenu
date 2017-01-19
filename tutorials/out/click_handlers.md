<h1>Click Handlers</h1>
  Almost every item in a menu needs to have (a) click handler(s)<br/>
  And that's where this comes in!<br/>
  You can use this in 3 ways:<br/>
<ul>
  <li>In the item's section itself, so where you put Slot, Name, Material etc. This limits you to 1 click handler</li>
  <li>In separate sections, under 'Click-Handlers' at the item, e.g.:</li>
    <b>Click-Handlers:</b><br/>
      <b>example:</b><br/>
        <b>Click-Type:</b> left<br/>
        <b>Commands:</b><br/>
        - 'message: &amp;6Hello there!'<br/>
      <b>anything:</b><br/>
        <b>Click-Type:</b> right<br/>
        <b>Commands:</b><br/>
        - 'message: &amp;aI am green!'<br/>
  <li>Using a list of sections, e.g.:</li>
    <b>Click-Handlers:</b><br/>
      - <b>Click-Type:</b> left<br/>
        <b>Commands:</b><br/>
        - 'message: &amp;6Hello there!'<br/>
      - <b>Click-Type:</b> right<br/>
        <b>Commands:</b><br/>
        - 'message: &amp;aI am green!'<br/>
</ul>
<h3>Standard features</h3>
<ul>
  <li id="click_type"><b>Click-Type</b><br/>
    <hr/>
      The click types to accept, can be either:<br/>
    <ul>
      <li>A comma separated list of 'right', 'left' or 'middle'</li>
      <li>'all' to accept all click types</li>
      <li>'both' for only right and left click</li>
    </ul>
    <br/>
  <li id="shift_click"><b>Shift-Click</b><br/>
    <hr/>
      A Flag which specifies whether this handler accepts shift clicks<br/>
    <br/>
  <li id="price"><b>Price</b><br/>
    <hr/>
      The amount of Vault money required for this handler<br/>
      Vault and an economy handler (like Essentials) need to be installed of course<br/>
    <br/>
  <li id="price_message"><b>Price-Message</b><br/>
    Supports placeholders<br/>
    Default value: '&cYou don't have enough money for that!'<br/>
    <hr/>
      The message to send when the player does not have <a href="#price">Price</a><br/>
    <br/>
  <li id="points"><b>Points</b><br/>
    <hr/>
      The amount of player points required for this handler<br/>
      PlayerPoints is needed for this<br/>
    <br/>
  <li id="points_message"><b>Points-Message</b><br/>
    Supports placeholders<br/>
    Default value: '&cYou don't have enough points for that!'<br/>
    <hr/>
      The message to send when the player does not have <a href="#points">Points</a><br/>
    <br/>
  <li id="permission"><b>Permission</b><br/>
    Supports placeholders<br/>
    <hr/>
      The permission required to use this click handler<br/>
    <br/>
  <li id="permission_message"><b>Permission-Message</b><br/>
    Supports placeholders<br/>
    Default value: '&cYou are not permitted to do that!'<br/>
    <hr/>
      The message to send when the player does not have <a href="#permission">Permission</a><br/>
    <br/>
  <li id="bypass_permission"><b>Bypass-Permission</b><br/>
    Supports placeholders<br/>
    <hr/>
      A permission that allows the player to bypass <a href="#price">Price</a> and <a href="#points">Points</a><br/>
    <br/>
  <li id="close"><b>Close</b><br/>
    <hr/>
      Specifies whether the menu should close when this item is clicked<br/>
      This accepts one of these values:<br/>
    <ul>
      <li><b>always</b> to always close the menu</li>
      <li><b>on-success</b> to close the menu when <a href="#commands">Commands</a> were successfully executed</li>
      <li><b>on-failure</b> to close the menu when the player was not allowed to click the item</li>
      <li><b>never</b> to never close the menu on click</li>
    </ul>
    <br/>
  <li id="click_delay"><b>Click-Delay</b><br/>
    <hr/>
      The delay (in ticks) that a player needs must wait before clicking the item again<br/>
    <br/>
  <li id="delay_message"><b>Delay-Message</b><br/>
    Supports special placeholders<br/>
    <hr/>
      The message to send when <a href="#click_delay">Click-Delay</a> is not over yet<br/>
      You can use these special placeholders to customize the message:<br/>
    <ol>
      <li><b>{hoursleft}</b> to retrieve the amount of hours left</li>
      <li><b>{minutesleft}</b> to retrieve the amount of minutes left</li>
      <li><b>{secondsleft}</b> to retrieve the amount of seconds left</li>
      <li><b>{ticksleft}</b> to retrieve the amount of ticks left</li>
    </ol>
    <br/>
  <li id="commands"><b>Commands</b><br/>
    Animatable, List, Supports placeholders<br/>
    <hr/>
      The commands to execute when the player is allowed to click this item<br/>
      By default, the player will execute the command you enter,<br/>
      but you can start a command with these followed by a colon to do something special:<br/>
    <ul>
      <li><b>console</b> to make the console execute the command</li>
      <li><b>message</b> to send a message to the player</li>
      <li><b>op</b> to make the player execute the command as operator</li>
      <li><b>broadcast</b> to broadcast a message to the entire server</li>
      <li><b>server</b> to send the player to a specific bungeecord server</li>
      <li><b>menu</b> to open a specific menu</li>
      <li><b>tellraw</b> to send a raw message (e.g. {"text":"Hello there!"})</li>
      <li><b>sound</b> to send a sound to a player, format '&lt;soundname&gt; [volume] [pitch]'</li>
      <ul>
        <li>For a list of sound names, see <a href="http://www.minecraftforum.net/forums/mapping-and-modding/mapping-and-modding-tutorials/1571574-all-minecraft-playsound-file-names-1-9">this page</a></li>
      </ul>
      <li>If you have the Plus version, you also get access to these commands:</li>
      <ul>
        <li><b>bungee</b> to execute a command as the bungeecord console.</li>
        <li><b>bungeeplayer</b> to execute a bungeecord command as the player</li>
        <ul>
          <li>Note: For the above 2 commands to work, you must put the AnimatedMenu Plus jar in the BungeeCord plugins folder!</li>
        </ul>
        <li><b>script</b> to execute some javascript code</li>
        <li><b>sql</b> to execute a config specified <a href="config.md#sql_statements">SQL Statement</a></li>
      </ul>
    </ul>
    <br/>
  <li id="buy_commands"><b>Buy-Commands</b><br/>
    <hr/>
      Identical to <a href="#commands">Commands</a> but it only executes when the player paid for <a href="#price">Price</a> and/or <a href="#points">Points</a> (<a href="#bypass_permission">Bypass-Permission</a> would ignore this)<br/>
    <br/>
</ul><h3>Plus features</h3>
<ul>
  <li id="requirement_script"><b>Requirement-Script</b><br/>
    Script, Supports placeholders<br/>
    <hr/>
      Some JavaScript code that specifies whether a player is allowed to click this item<br/>
      '%vault_eco_balance% &gt; 100' would check if the player has more than $100<br/>
      For this case you could simply use <a href="#price">Price</a> but it's just an example<br/>
    <br/>
  <li id="script_message"><b>Script-Message</b><br/>
    Supports placeholders<br/>
    <hr/>
      The message to send when the <a href="#requirement_script">Requirement-Script</a> doesn't allow the player to click the item<br/>
    <br/>
</ul>