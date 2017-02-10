# Menus #
Menus are where it all starts in the plugin  
### Summary ###
#### Standard features ####
- [Menu-Name](#user-content-menu-name)
- [Menu-Type](#user-content-menu-type)
- [Rows](#user-content-rows)
- [Permission](#user-content-permission)
- [Permission-Message](#user-content-permission-message)
- [Title-Update-Delay](#user-content-title-update-delay)
- [Menu-Opener](#user-content-menu-opener)
- [Menu-Opener-Name](#user-content-menu-opener-name)
- [Menu-Opener-Lore](#user-content-menu-opener-lore)
- [Menu-Opener-Slot](#user-content-menu-opener-slot)
- [Open-On-Join](#user-content-open-on-join)
- [Open-Sound](#user-content-open-sound)
- [Open-Sound-Pitch](#user-content-open-sound-pitch)
- [Command](#user-content-command)
- [Hide-From-Command](#user-content-hide-from-command)
- [Click-Delay](#user-content-click-delay)
- [Delay-Message](#user-content-delay-message)
- [Items](#user-content-items)

#### Plus features ####
- [Slot-Update-Delay](#user-content-slot-update-delay)
- [Open-Animation](#user-content-open-animation)
- [Animation-Speed](#user-content-animation-speed)
- [Empty-Item](#user-content-empty-item)
- [Open-Commands](#user-content-open-commands)
- [Close-Commands](#user-content-close-commands)
- [Sql-Await](#user-content-sql-await)
- [Wait-Message](#user-content-wait-message)
- [Admin](#user-content-admin)
- [Require-Other](#user-content-require-other)
- [Save-Navigation](#user-content-save-navigation)

### Standard features ###
- #### Menu-Name ####
  Animatable, Supports placeholders  
  \------------------------------  
  This menu's title  

- #### Menu-Type ####
  The type of this menu, one of the following:  
  'Chest', 'Hopper', 'Dispenser', 'Dropper', 'Crafting'  
  If the type is 'Chest', or it is not specified, see [Rows](#user-content-rows)  

- #### Rows ####
  If [Menu-Type](#user-content-menu-type) is set to 'Chest' or not set, this is used  
  A number of 1 through 6 that specifies the amount of rows in the menu  

- #### Permission ####
  Supports placeholders  
  \------------------------------  
  The permission required to open this menu  

- #### Permission-Message ####
  Supports placeholders  
  \------------------------------  
  The message to send when the player does not have the Permission  

- #### Title-Update-Delay ####
  Default value: 20  
  \------------------------------  
  The interval in ticks between each title update  

- #### Menu-Opener ####
  The item to use to open the menu, format is &lt;type&gt;:&lt;amount&gt;:&lt;data&gt;  

- #### Menu-Opener-Name ####
  The display name [Menu-Opener](#user-content-menu-opener) must have to open the menu  

- #### Menu-Opener-Lore ####
  The lore [Menu-Opener](#user-content-menu-opener) must have to open the menu  

- #### Menu-Opener-Slot ####
  The slot to put [Menu-Opener](#user-content-menu-opener) in when a player joins  

- #### Open-On-Join ####
  Whether the menu should open when a player joins  

- #### Open-Sound ####
  The sound to play when the menu is opened  
  NOTE: This uses /playsound command names. For a list of sounds, see [this page](http://www.minecraftforum.net/forums/mapping-and-modding/mapping-and-modding-tutorials/1571574-all-minecraft-playsound-file-names-1-9)  

- #### Open-Sound-Pitch ####
  The pitch of the open sound  

- #### Command ####
  The command to type to open the menu  
  Use ; to specify multiple commands, e.g. 'command1; command2'  

- #### Hide-From-Command ####
  Prevent this menu from being openable through /animatedmenu open  

- #### Click-Delay ####
  The delay (in ticks) that a player needs must wait before clicking an item again  

- #### Delay-Message ####
  Supports special placeholders  
  \------------------------------  
  The message to send when [Click-Delay](#user-content-click-delay) is not over yet  
  You can use these special placeholders to customize the message:  
    - **{hoursleft}** to retrieve the amount of hours left
    - **{minutesleft}** to retrieve the amount of minutes left in the hour
    - **{secondsleft}** to retrieve the amount of seconds left in the minute
    - **{ticksleft}** to retrieve the amount of ticks left in the second

  You can use formulas (e.g. \(20{secondsleft} + {ticksleft})) to get a total of something  

- #### Items ####
  Section of Key-Section pairs  
  \------------------------------  
  The items to put in this menu  
  See the [Items](items.md) page for info on how to set this up  

### Plus features ###
- #### Slot-Update-Delay ####
  Default value: 20  
  \------------------------------  
  The interval in ticks between each item slot update  

- #### Open-Animation ####
  Animatable  
  \------------------------------  
  The animation to play when the menu opens, in format &lt;type&gt;[:&lt;speed&gt;].  
  &lt;type&gt; can be a custom animation specified in config.yml, or one of:  
    - down, up, right, left
    - up-left, up-right, down-left, down-right
    - out, in
    - snake-down, snake-up, snake-right, snake-left


- #### Animation-Speed ####
  Default value: 1.0  
  \------------------------------  
  The default speed of the open animation(s)  

- #### Empty-Item ####
  The item to put in empty slots, see [Items](items.md) for the format  

- #### Open-Commands ####
  The commands to perform when the menu opens, see [Commands](click_handlers.md#commands)  

- #### Close-Commands ####
  The commands to perform when the menu closes, see [Commands](click_handlers.md#commands)  

- #### Sql-Await ####
  A comma-separated list of Sql Queries to request before opening the menu  
  This happens asynchronously, which means that your server won't be paused by this  
  You can use [Wait-Message](#user-content-wait-message) to specify a message to send when waiting  

- #### Wait-Message ####
  Supports placeholders  
  \------------------------------  
  The message to send when waiting for Sql-Await to complete  

- #### Admin ####
  Set to 'true' to make this an administrative menu. When this is the case:  
    - The menu is hidden from /animatedmenu open
    - The menu can only be opened through a custom command by using /&lt;command&gt; &lt;player&gt;

  You can then use %menuadmin_&lt;placeholder&gt;% to use &lt;placeholder&gt; as the target player  
  %menuadmin_player_name% would retrieve the target player's name  
  When switching between admin menus, the target player will retain until the menu is closed  

- #### Require-Other ####
  Default value: true  
  \------------------------------  
  When [Admin](#user-content-admin) is enabled, this specifies whether another player needs to be specified  
  If this is set to 'false', the effects of [Admin](#user-content-admin) no longer apply and the menuadmin placeholder uses the player itself  

- #### Save-Navigation ####
  When set to 'true', any navigation to other menus will be saved  
  This will make a player open the last opened menu when opening this menu  
  If you have some sort of paged shop, this can be quite useful  

