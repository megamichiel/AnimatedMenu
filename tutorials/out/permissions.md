# Permissions #
This plugin comes with a few permissions  
Some of which are always applicable, but others only come with AnimatedMenu Plus  
There are also custom permissions you can set in click handlers for example. These are not discussed here however  
### Summary ###
#### Standard Permissions ####
- [animatedmenu.command.&lt;arg&gt;](#user-content-animatedmenu.command.&lt;arg&gt;)
- [animatedmenu.command.open.other](#user-content-animatedmenu.command.open.other)
- [animatedmenu.command.item.other](#user-content-animatedmenu.command.item.other)
- [animatedmenu.openWithItem](#user-content-animatedmenu.openwithitem)
- [animatedmenu.seeUpdate](#user-content-animatedmenu.seeupdate)

#### Plus Permissions ####
- [animatedmenu.command.togglepi](#user-content-animatedmenu.command.togglepi)
- [animatedmenu.command.rlbungee](#user-content-animatedmenu.command.rlbungee)
- [animatedmenu.polls.max.&lt;amount&gt;](#user-content-animatedmenu.polls.max.&lt;amount&gt;)
- [animatedmenu.polls.closeOther](#user-content-animatedmenu.polls.closeother)
- [animatedmenu.polls.removeOther](#user-content-animatedmenu.polls.removeother)

### Standard Permissions ###
- #### animatedmenu.command.&lt;arg&gt; ####
  Use /animatedmenu &lt;arg&gt;  
  &lt;arg&gt; can be one of:  
    - help, open, item, reload

  Tab complete will also only show the subcommands you have permission to see  

- #### animatedmenu.command.open.other ####
  Use /animatedmenu open &lt;menu&gt; [player]  

- #### animatedmenu.command.item.other ####
  Use /animatedmenu item &lt;menu&gt; [player]  

- #### animatedmenu.openWithItem ####
  Be able to open menus using an item  

- #### animatedmenu.seeUpdate ####
  Receive a message when you join and there is an update available  

### Plus Permissions ###
- #### animatedmenu.command.togglepi ####
  Use /animatedmenu togglepi  

- #### animatedmenu.command.rlbungee ####
  Use /animatedmenu rlbungee  

- #### animatedmenu.polls.max.&lt;amount&gt; ####
  Specifies the max amount of polls a player can create  
  You can use <i>default</i> to use the default value as specified in config.yml  
  A value of -1 indicates that there is no limit (so 0 means no polls)  

- #### animatedmenu.polls.closeOther ####
  Be able to close a poll that was created by another player  
  Closed polls can not be voted at anymore, but you can still view the results  

- #### animatedmenu.polls.removeOther ####
  Be able to remove a poll that was created by another player  

