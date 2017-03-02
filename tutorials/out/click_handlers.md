# Click Handlers #
Almost every item in a menu needs to have (a) click handler(s)  
And that's where this comes in!  
You can use this in 3 ways:  
  - In the item's section itself, so where you put Slot, Name, Material etc. This limits you to 1 click handler
  - In separate sections, under 'Click-Handlers' at the item, e.g.:

```YAML
Click-Handlers:
  example:
    Click-Type: left
    Commands:
    - 'message: &6Hello there!'
  anything:
    Click-Type: right
    Commands:
    - 'message: &aI am green!'
```
  - Using a list of sections, e.g.:

```YAML
Click-Handlers:
- Click-Type: left
  Commands:
  - 'message: &6Hello there!'
- Click-Type: right
  Commands:
  - 'message: &aI am green!'
```

### Summary ###
#### Standard features ####
- [Click-Type](#user-content-click-type)
- [Shift-Click](#user-content-shift-click)
- [Price](#user-content-price)
- [Price-Message](#user-content-price-message)
- [Points](#user-content-points)
- [Points-Message](#user-content-points-message)
- [Exp](#user-content-exp)
- [Exp-Message](#user-content-exp-message)
- [Permission](#user-content-permission)
- [Permission-Message](#user-content-permission-message)
- [Bypass-Permission](#user-content-bypass-permission)
- [Close](#user-content-close)
- [Click-Delay](#user-content-click-delay)
- [Delay-Message](#user-content-delay-message)
- [Commands](#user-content-commands)
- [Buy-Commands](#user-content-buy-commands)

#### Plus features ####
- [Requirement-Script](#user-content-requirement-script)
- [Script-Message](#user-content-script-message)
- [Item](#user-content-item)
- [Item-Message](#user-content-item-message)

### Standard features ###
- #### Click-Type ####
  Default value: both  
  \------------------------------  
  The click types to accept, can be either:  
    - A comma separated list of 'right', 'left' or 'middle'
    - 'all' to accept all click types
    - 'both' for only right and left click


- #### Shift-Click ####
  Default value: both  
  \------------------------------  
  A Flag which specifies whether this handler accepts shift clicks  

- #### Price ####
  The amount of Vault money required for this handler  
  Vault and an economy handler (like Essentials) need to be installed of course  

- #### Price-Message ####
  Supports placeholders  
  Default value: '&cYou don't have enough money for that!'  
  \------------------------------  
  The message to send when the player does not have [Price](#user-content-price)  

- #### Points ####
  The amount of player points required for this handler  
  PlayerPoints is needed for this  

- #### Points-Message ####
  Supports placeholders  
  Default value: '&cYou don't have enough points for that!'  
  \------------------------------  
  The message to send when the player does not have [Points](#user-content-points)  

- #### Exp ####
  The amount of exp required to use this item.  
  Start with an L to use levels, e.g. "Exp: 'L10'"  

- #### Exp-Message ####
  The message to send when the player does not have [Exp](#user-content-exp)  

- #### Permission ####
  Supports placeholders  
  \------------------------------  
  The permission required to use this click handler  

- #### Permission-Message ####
  Supports placeholders  
  Default value: '&cYou are not permitted to do that!'  
  \------------------------------  
  The message to send when the player does not have [Permission](#user-content-permission)  

- #### Bypass-Permission ####
  Supports placeholders  
  \------------------------------  
  A permission that allows the player to bypass [Price](#user-content-price) and [Points](#user-content-points)  

- #### Close ####
  Default value: never  
  \------------------------------  
  Specifies whether the menu should close when this item is clicked  
  This accepts one of these values:  
    - **always** to always close the menu
    - **on-success** to close the menu when [Commands](#user-content-commands) were successfully executed
    - **on-failure** to close the menu when the player was not allowed to click the item
    - **never** to never close the menu on click


- #### Click-Delay ####
  The delay (in ticks) that a player must wait before clicking the item again  

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

- #### Commands ####
  Animatable, List, Supports placeholders  
  \------------------------------  
  The commands to execute when the player is allowed to click this item  
  NOTE: If you want to use { and } for things as tellraw, use \{ and \}  
  By default, the player will execute the command you enter,  
  but you can start a command with these followed by a colon to do something special:  
    - **console** to make the console execute the command
    - **message** to send a message to the player
    - **op** to make the player execute the command as operator
    - **broadcast** to broadcast a message to the entire server
    - **server** to send the player to a specific bungeecord server
    - **menu** to open a specific menu
    - **tellraw** to send a raw message (e.g. \{"text":"Hello there!"\})
    - **sound** to send a sound to a player, format '&lt;soundname&gt; [volume] [pitch]'
      - For a list of sound names, see [this page](http://www.minecraftforum.net/forums/mapping-and-modding/mapping-and-modding-tutorials/1571574-all-minecraft-playsound-file-names-1-9)

    - **give** to give an item to a player, format identical to [Item](#user-content-item) but { and } should be replaced with \{ and \}
    - If you have the Plus version, you also get access to these commands:
      - **bungee** to execute a command as the bungeecord console.
      - **bungeeplayer** to execute a bungeecord command as the player
        - Note: For the above 2 commands to work, you must put the AnimatedMenu Plus jar in the BungeeCord plugins folder!

      - **script** to execute some javascript code
      - **sql** to execute a config specified [SQL Statement](config.md#sql_statements)



- #### Buy-Commands ####
  Identical to [Commands](#user-content-commands) but it only executes when the player paid for [Price](#user-content-price) and/or [Points](#user-content-points) ([Bypass-Permission](#user-content-bypass-permission) would ignore this)  

### Plus features ###
- #### Requirement-Script ####
  Script, Supports placeholders  
  \------------------------------  
  Some JavaScript code that specifies whether a player is allowed to click this item  
  '%vault_eco_balance% &gt; 100' would check if the player has more than $100  
  For this case you could simply use [Price](#user-content-price) but it's just an example  

- #### Script-Message ####
  Supports placeholders  
  \------------------------------  
  The message to send when the [Requirement-Script](#user-content-requirement-script) doesn't allow the player to click the item  

- #### Item ####
  Supports placeholders  
  \------------------------------  
  An item that the player must have to use this item. This item will be taken from them  
  The format is '&lt;id&gt;:&lt;amount&gt;:&lt;data&gt; &lt;nbt&gt;', though &lt;amount&gt;, &lt;data&gt; and &lt;nbt&gt; are all optional  
  &lt;data&gt; also supports values from data-values.yml  
  A few examples:  

```YAML
Item: 'stone:1:granite'
Item: 'diamond-sword {ench:[{id:16,lvl:3}]}'
```

- #### Item-Message ####
  Supports placeholders  
  \------------------------------  
  The message to send to the player when they do not have [Item](#user-content-item)  

