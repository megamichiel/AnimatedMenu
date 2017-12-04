# Scripts #
AnimatedMenu Plus makes it possible for you to use scripts in pieces of text and stuff  
In some places the values are completely parsed as javascript, but you can also use javascript in a normal piece of text  
### Summary ###
- [General info](#user-content-general info)
- [JavaScript-only options](#user-content-javascript-only options)

- #### General info ####
  You can use javascript wherever you can use placeholders. This means in item Names, Lores, Materials and more!  
  Just surround it with \\{\\} and you're good to go!  
     
  If you specify a file name, for example '\\{test.js\\}', and that file can be found under AnimatedMenuPlus/scripts, then the script is loaded from the file  
  If it's not a file name or the file cannot be found, the text is loaded as javascript  
  You are able to use placeholders in the text, so you can for example use:  
  \\{%vault_eco_balance% &gt;= 100\\}  
  This will return 'true' when the player has $100 or more, and 'false' otherwise.  
  Be careful when comparing text, for instance when you want to check a player's world. Quotes are very important:  
  \\{"%player_world%" == "world"\\}  

- #### JavaScript-only options ####
  As said before, there are some parts where a value is expected to be a piece of javascript.  
  This can be, for example, in an item's [View-Script](items.md#view_script). These do not require you to have \\{\\}s around them  

