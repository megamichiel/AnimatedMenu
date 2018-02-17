# Items #
What are menus without items?  
### Summary ###
#### Standard features ####
- [Slot](#user-content-slot)
- [X](#user-content-x)
- [Y](#user-content-y)
- [Frame-Delay](#user-content-frame-delay)
- [Refresh-Delay](#user-content-refresh-delay)
- [Material](#user-content-material)
- [Name](#user-content-name)
- [Lore](#user-content-lore)
- [Enchantments](#user-content-enchantments)
- [Color](#user-content-color)
- [Skull-Owner](#user-content-skull-owner)
- [Egg-Type](#user-content-egg-type)
- [Banner-Pattern](#user-content-banner-pattern)
- [Hide-Flags](#user-content-hide-flags)
- [View-Permission](#user-content-view-permission)
- [Unbreakable](#user-content-unbreakable)
- [Fireworks-Color](#user-content-fireworks-color)
- [Click-Handlers](#user-content-click-handlers)

#### Plus features ####
- [Slot-Update-Delay](#user-content-slot-update-delay)
- [Template](#user-content-template)
- [Weight](#user-content-weight)
- [View-Script](#user-content-view-script)
- [NBT](#user-content-nbt)
- [Animate-NBT](#user-content-animate-nbt)
- [State](#user-content-state)
- [State-Template](#user-content-state-template)
- [States](#user-content-states)

### Standard features ###
- #### Slot ####
  Animatable, Formula, Required, Supports special placeholders  
  \------------------------------  
  The slot to put this item in, between 1 and the menu's size.  
  Instead of a slot, format '&lt;X&gt;, &lt;Y&gt;' is also supported. See [X](#user-content-x) and [Y](#user-content-y) for info on those values  
  You can also use [X](#user-content-x) and [Y](#user-content-y) separately instead  
  If you wish to put certain items at multiple slots, that is possible as well  
  You can specify a list of slots to put it in, and for each entry also a range  
  A range has format &lt;from&gt;-&lt;to&gt;. These values do not support placeholders however  
  If you have the Plus version, this accepts formulas as well, with these placeholders:  
    - **{first_empty}**: The first empty slot in the menu
    - **{first_empty_&lt;slot&gt;}**: The first empty slot starting at &lt;slot&gt;
    - **{last_empty}**: The last empty slot in the menu
    - **{last_empty_&lt;slot&gt;}**: The last empty slot starting at the menu's size - &lt;slot&gt;
    - **{random**}: A random slot
    - **{random_&lt;from&gt;_&lt;to&gt;}**: A random slot, between &lt;from&gt; and &lt;to&gt;
    - **{random_empty}**: A random empty slot
    - **{highest_amount}**: Puts this item before items with a lower amount
    - **{lowest_amount}**: Puts this item before items with a higher amount
    - **{highest_weight}**: Puts this item before items with a lower [Weight](#user-content-weight)
    - **{lowest_weight}**: Puts this item before items with a higher [Weight](#user-content-weight)


- #### X ####
  The X (horizontal) position of the item, from 1 to the menu's width  

- #### Y ####
  The Y (vertical) position of the item, from 1 to the menu's height  

- #### Frame-Delay ####
  Default value: 20  
  \------------------------------  
  The delay in ticks between each frame  

- #### Refresh-Delay ####
  Default value: Value of Frame-Delay  
  \------------------------------  
  The delay in ticks between placeholder updates  

- #### Material ####
  Animatable, Supports placeholders  
  Default value: stone  
  \------------------------------  
  The material of this item, in format &lt;type&gt;:&lt;amount&gt;:&lt;data&gt;  
  Note: Numerical IDs are still supported, but I recommended using names.  
  &lt;amount&gt; and &lt;data&gt; are optional, so 'diamond:5' and 'diamond_sword' both work as well.  
  A nice list of item ids can be found on [this page](http://minecraft-ids.grahamedgecombe.com/)  

- #### Name ####
  Animatable, Supports placeholders  
  Default value: The key of the item's section  
  \------------------------------  
  The display name of this item  

- #### Lore ####
  The lore of this item. This fully supports colors and placeholders and is animatable.  
  If you start a line with 'file:', an image will be loaded from the 'images' folder in the plugin's folder and put in the lore.  
  For example:  

```YAML
Lore:
- '&aThis is the first line, &6%player_name%!'
- '&eThis is the second line'
- 'file: imagename.png'
```

```YAML
Lore:
  1:
  - '&aThis is the first line, &6%player_name%!'
  - '&eThis is the second line'
  2:
  - '&6This is the first line, &a%player_name%!'
  - '&cThis is the second line'
```
     
  AnimatedMenu Plus has a much more comprehensive lore system, including modules and putting text next to images, as well as changing the pixel characters and more!  
  Modules are basically a way to separate a lore into multiple parts, making it easy to animate some parts while other parts aren't animated.  

```YAML
Lore:
- '&aThis line is not animated!'
- 1:
  - '&bThis line is animated!'
  2:
  - '&cThis line is animated!'
  3:
  - '&eThis line is animated!'
  - '&5This is another line that is not always visible!'
```
  Images are another great feature that can make your menu look much more stunning.  
  As mentioned above, you can start a line with 'file:' to display an image. However, you can also use a more customizable version:  

```YAML
Lore:
- '&aThis is the first line'
- image: 'imagefolder'
  pixel: 'O'
  transparent: '&0+'
  blank-line: '&cThis is a blank line'
  random: 'yes'
```
  These are all keys explained:  
    - 'image' specifies a file or directory name. If it's a directory, all images from this directory are loaded. GIF files are also supported
    - 'pixel' specifies the text to display for each pixel.
    - 'transparent' specifies the text of a transparent pixel
    - 'blank-line': When multiple files are loaded with some images smaller than others, this text is placed to fill the blank lines
    - 'random' specifies if images should be displayed at random instead of in order


- #### Enchantments ####
  Animatable with Plus version, List  
  \------------------------------  
  The enchantments that should be applied to this item  
  [Here](http://minecraft.gamepedia.com/Enchanting/ID) is a list of ids  

- #### Color ####
  Animatable with Plus version  
  \------------------------------  
  The color to give to leather armor, can be either:  
    - **&lt;red&gt;, &lt;green&gt;, &lt;blue&gt;** where each color is from 0 to 255
    - **RRGGBB** where each position ranges from 0-9 and A-F


- #### Skull-Owner ####
  Animatable with Plus version, Supports placeholders  
  \------------------------------  
  The skull owner, if this item is a player skull, can be either:  
    - A player's username
    - A player's UUID (with or without hyphens)
    - A json object containing profile information
    - Base64 encoded profile texture
    - If you have Head Database, you can use 'hdb: &lt;id&gt;' (without the &lt;&gt;s) to get a head database skull


- #### Egg-Type ####
  Animatable with Plus version  
  \------------------------------  
  The entity type of a spawn egg. See [this](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/entity/EntityType.html) page for a list of types  

- #### Banner-Pattern ####
  Animatable with Plus version  
  \------------------------------  
  The banner pattern to apply to a banner  
  It uses a pattern generated by [Miners Need Cool Shoes](http://www.needcoolshoes.com/banner)  
  When you have created your banner, use this part as value:  
  ![Image](https://i.gyazo.com/851f4e73a253aeeefc539baac385bba3.png)  

- #### Hide-Flags ####
  Animatable with Plus version  
  \------------------------------  
  The hide flags to apply to this item. List of flags:  
    - 1 = **Enchants**
    - 2 = **Attributes**
    - 4 = **Unbreakable**
    - 8 = **Destroys**
    - 16 = **Placed-On**
    - 32 = **Potion-Effects** (note: also includes shield pattern info and other stuff)

  You can either specify the sum of the numbers you need, or have a comma-separated list of their names  

- #### View-Permission ####
  A permission required to see this item  
  Start with a dash ( - ) to negate the effect (only visible without permission)  

- #### Unbreakable ####
  Whether this item should be unbreakable or not  
  Could be useful for custom item textures per durability  
  NOTE: When you have the Plus version, you should use [NBT](#user-content-nbt) instead (this won't even work)  

- #### Fireworks-Color ####
  Animatable with Plus version  
  \------------------------------  
  Firework charge color, can be either:  
    - **&lt;red&gt;, &lt;green&gt;, &lt;blue&gt;** where each color is from 0 to 255
    - **RRGGBB** where each position ranges from 0-9 and A-F


- #### Click-Handlers ####
  Section of Key-Section pairs  
  \------------------------------  
  (This used to be called Commands, but I changed it because it makes more sense)  
  The click handlers of this item, that specify what commands to perform at which clicks  
  See the [Click-Handlers](click_handlers.md) page for info on this  

### Plus features ###
- #### Slot-Update-Delay ####
  Default value: 20  
  \------------------------------  
  The interval in ticks between each slot update  
  Only applicable when the slot is not fixed  

- #### Template ####
  Specifies a template to use from the defined ones in [Templates](config.md#templates)  

- #### Weight ####
  Formula  
  Default value: 0  
  \------------------------------  
  The weight of this item, to use with {highest_weight} and {lowest_weight}  
  You can use this to, for example, sort a menu by player count on different servers  
  It accepts negative and large values to force certain items at specific positions  

- #### View-Script ####
  Script, Supports placeholders  
  \------------------------------  
  Some JavaScript code that checks if a player can see this item  
  For example, '%vault_eco_balance% &gt;= 100' checks if the player has at least $100  

- #### NBT ####
  Animatable, Section  
  \------------------------------  
  A section containing specific NBT values, such as Unbreakable  
  This works just like in the /give command, but now it's in YML format  
  If you set [Animate-NBT](#user-content-animate-nbt) to true, this value is animated  

- #### Animate-NBT ####
  In AnimationLib 1.5.2 I added an option to animate values that are sections themselves  
  Specifies whether NBT should be animated  

- #### State ####
  Supports placeholders  
  \------------------------------  
  Specifies a certain text to evaluate to determine what item this should be  
  You can use [States](#user-content-states) to set config options per result  
  When this is set, options are loaded as followed:  
    - [Slot](#user-content-slot), [Frame-Delay](#user-content-frame-delay) and [Refresh-Delay](#user-content-refresh-delay) should be specified here
    - All the other config options are only loaded under each [States](#user-content-states) value
    - You can even use nested states! Go nuts!

  You can prefix the state for specific results (doesn't require placeholders):  
    - **permission:** returns 'yes' if the player has a permission, 'no' otherwise
    - **money:** returns 'yes' if the player has X amount of money, 'no' otherwise
    - **points:** returns 'yes' if the player has X amount of (player)points, 'no' otherwise
    - **level:** returns 'yes' if the player has at least a certain XP level, 'no' otherwise


- #### State-Template ####
  Section  
  \------------------------------  
  Specifies a template to use in each section of [States](#user-content-states).  
  You could also use [Template](#user-content-template) in each state value, but it can be prettier to use this instead of specifying a config.yml template  
  You could also combine this with [Template](#user-content-template), in which case this will override the values of the config template  

- #### States ####
  Section of Key-Section pairs  
  \------------------------------  
  Specifies mappings from values retrieved in [State](#user-content-state) to item info  
  The key is the value to accept. You can separate it by a semicolon (;) to accept multiple results  
  Each value supports placeholders and special placeholders  
  You can use every standard item config option here, except for the ones mentioned in [State](#user-content-state)  
  A small but working example:  

```YAML
#The :#: makes it a whole number, floor rounds a number down and round rounds a number to the nearest whole number
State: '\(:#:floor(round(%player_health%) / 5))'
#Specify some default values reused in each state (can be overridden):
State-Template:
  Click-Handlers:
  - Commands:
    - 'message: &aYou got me :D'
States:
  0: # Red wool
    Material: wool:1:14
    Name: '&cYou nearly dead man!'
  1: # Orange wool
    Material: wool:1:1
    Name: '&6Careful, your health is getting low!'
  2;3: # Yellow wool
    Material: wool:1:4
    Name: '&eKeep your health up like that!'
  4: # Green wool
    Material: wool:1:5
    Name: '&aYou got health for days!'
```

