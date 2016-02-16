If you have AnimatedMenu Plus, you can customize the player's hotbar to make it animated as well.<br/>
It is very similar to normal menus, so it isn't much harder than settings that up.

If you first startup the plugin, it will create a 'config.yml' in the 'AnimatedMenuPlus' folder.<br/>
By default, it will look something like this:<br/>
<img src="https://i.gyazo.com/4b2e0cf828f3de4c16496837d0922187.png"/>

Using 'Startup-Script' you can execute a script each time the plugin (re)loads. This is the only place where the 'Player' variable is not defined. More on scripts can be found 
<a href="https://github.com/megamichiel/AnimatedMenu/blob/master/tutorials/Writing%20scripts.md">here</a>.

Inside the PlayerInventory section are also a few things:<br/>
If you set the "Enable" to true, the player won't be able to modify their inventory's items and the customized items will be placed in it.<br/>
As you can see, below the 'Items' section everything is just like in menus, so this won't be so hard. The only difference between the Player Inventory and normal menus, is that the clicks are triggered when the player right/left clicks. Shift-Click now means whether the player is sneaking or not. Apart from that, everything is the same.<br/>
There are a few keys that are not visible by default:
<ul>
	<li>
		<b>Bypass-Permission</b><br/>
		If the player has this permission, they will be able to modify their inventory.
	</li>
	<li>
		<b>Bypass-Script</b><br/>
		Has the same effect as Bypass-Permission, except for using this you can execute some script for bypassing.
	</li>
</ul>