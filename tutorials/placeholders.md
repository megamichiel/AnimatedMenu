# Placeholders #
When you install AnimatedMenu on your server, you will get a few extra placeholders to use  
For each of the below placeholders, use %animatedmenu_&lt;id&gt;% where &lt;id&gt; is the text in bold  
### Summary ###
- [motd_&lt;id&gt;](#user-content-motd_&lt;id&gt;)
- [onlineplayers_&lt;id&gt;](#user-content-onlineplayers_&lt;id&gt;)
- [maxplayers_&lt;id&gt;](#user-content-maxplayers_&lt;id&gt;)
- [status_&lt;id&gt;](#user-content-status_&lt;id&gt;)
- [motdcheck_&lt;id&gt;](#user-content-motdcheck_&lt;id&gt;)
- [worldplayers_&lt;world&gt;](#user-content-worldplayers_&lt;world&gt;)
- [shownplayers_&lt;world&gt;](#user-content-shownplayers_&lt;world&gt;)

- #### motd_&lt;id&gt; ####
  Returns the motd of a server specified in [Connections](config.md#connections)  

- #### onlineplayers_&lt;id&gt; ####
  Returns the online players of a server specified in [Connections](config.md#connections)  

- #### maxplayers_&lt;id&gt; ####
  Returns the max players of a server specified in [Connections](config.md#connections)  

- #### status_&lt;id&gt; ####
  Checks the online status of a server specified in [Connections](config.md#connections)  
  In the connection's section, use 'online' for an online server, and 'offline' for an offline server  

- #### motdcheck_&lt;id&gt; ####
  Returns different values depending on the motd of a [Connections](config.md#connections) server  
  You can use keys in the section to specify the server motd, and values as the result  
  Use 'default' for when no matching motd is found, and 'offline' for when the server is offline  

- #### worldplayers_&lt;world&gt; ####
  Returns the amount of players in &lt;world&gt;  

- #### shownplayers_&lt;world&gt; ####
  Returns the amount of players in &lt;world&gt; that are not vanished using Essentials  

