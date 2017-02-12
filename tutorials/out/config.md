# Plugin Configuration #
This plugin does more than just show some menus  
There's a bunch of features you can customize in config.yml  
### Summary ###
#### Standard Features ####
- [Auto-Menu-Refresh](#user-content-auto-menu-refresh)
- [Connections](#user-content-connections)
- [Connection-Refresh-Delay](#user-content-connection-refresh-delay)
- [Warn-Offline-Servers](#user-content-warn-offline-servers)

#### Plus features ####
- [Templates](#user-content-templates)
- [PlayerInventory](#user-content-playerinventory)
- [Sql-Statements](#user-content-sql-statements)
- [Open-Animations](#user-content-open-animations)
- [Enable-Polls](#user-content-enable-polls)
- [Max-Player-Polls](#user-content-max-player-polls)

### Standard Features ###
- #### Auto-Menu-Refresh ####
  When this is set to 'true', menu config files will automatically be updated:  
    - Newly created files will be loaded into a menu
    - Modified files will get their menu reloaded
    - Deleted files will get their menu removed


- #### Connections ####
  Section of Key-Section pairs  
  \------------------------------  
  Specifies external server connections to be made, to check their motd, whether they're online etc.  
  For each section, you need to specify "**IP**", which defines the target IP (+ port)  
  Then, you can add values depending on the [Placeholders](placeholders.md) using it  
  An example configuration:  

```YAML
Connections:
  example:
    IP: localhost:12345
    #For %animatedmenu_status_<id>%
    Online: '&aThis server is online!'
    #For %animatedmenu_status_<id>%, motd and motdcheck when the server isn't online
    Offline: '&cThis server is offline!'
    #For use in %animatedmenu_motdcheck_<id>%
    'A Minecraft Server': '&bThis server has the standard MOTD!'
    #For use in %animatedmenu_motdcheck_<id>%, when no key with the server's MOTD can be found
    Default: '&cThis server has an unknown MOTD!'
```

- #### Connection-Refresh-Delay ####
  Default value: 200 (= 10 seconds)  
  \------------------------------  
  The delay in ticks between connection refreshes  

- #### Warn-Offline-Servers ####
  Default value: false  
  \------------------------------  
  Sets whether a warning should be printed when a [Connections](#user-content-connections) server cannot be reached  

### Plus features ###
- #### Templates ####
  Section  
  \------------------------------  
  A section containing template items to use in menus  
  This has the same format as menu items, so see [Items](items.md) for info  
  Once you've set this up, you can use it at an item's [Template](items.md#template)  

- #### PlayerInventory ####
  Section  
  \------------------------------  
  Takes over a player's inventory by using their hotbar as menu  
  It has the same format as a normal menu, with it having a total of 9 slots  
  You also get some new config values:  
    - **Enable** specifies whether this should actually be enabled
    - **Bypass-Permission** specifies a permission that allows someone to bypass the player inventory
    - **Enabled-Worlds** or **Disabled-Worlds** specify the worlds in which this is enabled/disabled


- #### Sql-Statements ####
  Section of Key-Section pairs  
  \------------------------------  
  A section to be used with a click handler's [Commands](click_handlers.md#commands)  
  Each section contains a few keys:  
    - **Database** specifies the Database to be used, as specified in the AnimationLib config
    - **Query** specifies the query to execute, supports placeholders. Note: these placeholders musn't be inside of quotes
    - **Complete-Commands** specifies the [Commands](click_handlers.md#commands) to execute when the query was completed.

  For example:  

```YAML
Sql-Statements:
  example:
    Database: somedbfromanimlib
    Query: 'UPDATE `Players` SET `Coins`=`Coins`+1 WHERE `UUID`=%player_uuid%'
    Complete-Commands:
    - 'message: &6You have been given a coin!'
```

- #### Open-Animations ####
  Section  
  \------------------------------  
  Specifies custom open animations to be used in menus, using JavaScript code  
  You can use menuType.width and menuType.height to obtain the menu's width and height  
  It should return an array which contains multiple arrays all containing the slots to load  

- #### Enable-Polls ####
  When set to 'true', the /poll command is added  
  In a nutshell; it's allows for creating polls to ask players for their opinion  
  The rest you can find out yourself :3  

- #### Max-Player-Polls ####
  Default value: -1  
  \------------------------------  
  The max number of polls a player can create. When set to -1 a player can create an unlimited amount  
  You can give a player permission 'animatedmenu.polls.max.&lt;amount&gt;' to specify their max amount  

