<h1>Placeholders</h1>
  When you install AnimatedMenu on your server, you will get a few extra placeholders to use<br/>
  For each of the below placeholders, use %animatedmenu_&lt;id&gt;% where &lt;id&gt; is the text in bold<br/>
<h3>Standard features</h3>
<ul>
  <li id="motd_&lt;id&gt;"><b>motd_&lt;id&gt;</b><br/>
    <hr/>
      Returns the motd of a server specified in <a href="config.md#connections">Connections</a><br/>
    <br/>
  <li id="onlineplayers_&lt;id&gt;"><b>onlineplayers_&lt;id&gt;</b><br/>
    <hr/>
      Returns the online players of a server specified in <a href="config.md#connections">Connections</a><br/>
    <br/>
  <li id="maxplayers_&lt;id&gt;"><b>maxplayers_&lt;id&gt;</b><br/>
    <hr/>
      Returns the max players of a server specified in <a href="config.md#connections">Connections</a><br/>
    <br/>
  <li id="status_&lt;id&gt;"><b>status_&lt;id&gt;</b><br/>
    <hr/>
      Checks the online status of a server specified in <a href="config.md#connections">Connections</a><br/>
      In the connection's section, use 'online' for an online server, and 'offline' for an offline server<br/>
    <br/>
  <li id="motdcheck_&lt;id&gt;"><b>motdcheck_&lt;id&gt;</b><br/>
    <hr/>
      Returns different values depending on the motd of a <a href="config.md#connections">Connections</a> server<br/>
      You can use keys in the section to specify the server motd, and values as the result<br/>
      Use 'default' for when no matching motd is found, and 'offline' for when the server is offline<br/>
    <br/>
  <li id="worldplayers_&lt;world&gt;"><b>worldplayers_&lt;world&gt;</b><br/>
    <hr/>
      Returns the amount of players in &lt;world&gt;<br/>
    <br/>
  <li id="shownplayers_&lt;world&gt;"><b>shownplayers_&lt;world&gt;</b><br/>
    <hr/>
      Returns the amount of players in &lt;world&gt; that are not vanished using essentials<br/>
    <br/>
</ul>