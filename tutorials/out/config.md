<h1>Plugin Configuration</h1>
  This plugin does more than just show some menus<br/>
  There's a bunch of features you can customize in config.yml<br/>
<h3>Standard Features</h3>
<ul>
  <li id="auto_menu_refresh"><b>Auto-Menu-Refresh</b><br/>
    <hr/>
      When this is set to 'true', menu config files will automatically be updated:<br/>
    <ul>
      <li>Newly created files will be loaded into a menu</li>
      <li>Modified files will get their menu reloaded</li>
      <li>Deleted files will get their menu removed</li>
    </ul>
    <br/>
  <li id="connections"><b>Connections</b><br/>
    Section<br/>
    <hr/>
      Specifies external server connections to be made, to check their motd, whether they're online etc.<br/>
      For each section, you need to specify "<b>IP</b>", which defines the target IP (+ port)<br/>
      Then, you can add values depending on the <a href="placeholders.md">Placeholders</a> using it<br/>
    <br/>
  <li id="connection_refresh_delay"><b>Connection-Refresh-Delay</b><br/>
    <hr/>
      The delay in ticks between connection refreshes<br/>
    <br/>
  <li id="warn_offline_servers"><b>Warn-Offline-Servers</b><br/>
    <hr/>
      Sets whether a warning should be printed when a <a href="#connections">Connections</a> server cannot be reached<br/>
    <br/>
</ul><h3>Plus features</h3>
<ul>
  <li id="templates"><b>Templates</b><br/>
    Section<br/>
    <hr/>
      A section containing template items to use in menus<br/>
      This has the same format as menu items, so see <a href="items.md">Items</a> for info<br/>
      Once you've set this up, you can use it at an item's <a href="items.md#template">Template</a><br/>
    <br/>
  <li id="playerinventory"><b>PlayerInventory</b><br/>
    Section<br/>
    <hr/>
      Takes over a player's inventory by using their hotbar as menu<br/>
      It has the same format as a normal menu, with it having a total of 9 slots<br/>
      You also get some new config values:<br/>
    <ul>
      <li><b>Enable</b> specifies whether this should actually be enabled</li>
      <li><b>Bypass-Permission</b> specifies a permission that allows someone to bypass the player inventory</li>
      <li><b>Enabled-Worlds</b> or <b>Disabled-Worlds</b> specify the worlds in which this is enabled/disabled</li>
    </ul>
    <br/>
  <li id="sql_statements"><b>Sql-Statements</b><br/>
    Section<br/>
    <hr/>
      A section to be used with a click handler's <a href="click_handlers.md#commands">Commands</a><br/>
      Each section contains a few keys:<br/>
    <ul>
      <li><b>Database</b> specifies the Database to be used, as specified in the AnimationLib config</li>
      <li><b>Query</b> specifies the query to execute, supports placeholders. Note: these placeholders musn't be inside of quotes</li>
      <li><b>Complete-Commands</b> specifies the <a href="click_handlers.md#commands">Commands</a> to execute when the query was completed.</li>
    </ul>
    <br/>
  <li id="open_animations"><b>Open-Animations</b><br/>
    Section<br/>
    <hr/>
      Specifies custom open animations to be used in menus, using JavaScript code<br/>
      You can use menuType.width and menuType.height to obtain the menu's width and height<br/>
      It should return an array which contains multiple arrays all containing the slots to load<br/>
    <br/>
</ul>