# Scripts #
AnimatedMenu Plus makes it possible for you to use scripts in pieces of text and stuff  
In some places the values are completely parsed as javascript, but you can also use javascript in a normal piece of text  
### Summary ###
- [General info](#user-content-general info)
- [JavaScript-only options](#user-content-javascript-only options)
- [Use of JavaScript in text](#user-content-use of javascript in text)

- #### General info ####
  If you specify a file name, for example 'test.js', and that file can be found under AnimatedMenuPlus/scripts, then the script is loaded from the file  
  If it's not a file name or the file cannot be found, the text is loaded as javascript  
  You are able to use placeholders in the text, so you can for example use:  
  %vault_eco_balance% &gt;= 100  
  This will return 'true' when the player has $100 or more, and 'false' otherwise  

- #### JavaScript-only options ####
  As said before, there are some parts where a value is expected to be a piece of javascript.  
  This can be, for example, in an item's [Hide-Script](items.md#hide_script).  

- #### Use of JavaScript in text ####
  Thanks to a great feature, you can use javascript wherever you are able to use placeholders.  
  You can accomplish this by surrounding the script with {}  
  Note: Putting a } in the script will mess up the parsing, so if you need to have multiple lines do use a file instead  

