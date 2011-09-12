package me.Massacrer.Pluginmanager;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Type;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.UnknownDependencyException;
import org.bukkit.plugin.java.JavaPlugin;
import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

public class Pluginmanager extends JavaPlugin {
	PluginManager pm = null;
	Logger log = Logger.getLogger("Minecraft");
	static PermissionHandler permissionHandler;
	boolean debug = false;
	
	public void onDisable() {
		log.info("PluginManager disabled");
	}
	
	public void onEnable() {
		pm = getServer().getPluginManager();
		log.info("PluginManager enabled");
		setupPermissions();
	}
	
	public boolean onCommand(CommandSender sender, Command cmd,
			String commandLabel, String[] args) {
		
		if (commandLabel.equalsIgnoreCase("PM")) {
			
			if (!allowed(sender)) {
				sender.sendMessage(ChatColor.RED
						+ "You do not have permission to use this command");
				return true;
			}
			if (!(sender instanceof Player)
					&& args[0].equalsIgnoreCase("debug")) {
				debug = !debug;
				log.info("Debug state now " + (debug ? "enabled" : "disabled"));
				return true;
			}
			if (args.length > 0) {
				if (args[0].equalsIgnoreCase("enable")) {
					return enablePlugin(args, sender);
				}
				if (args[0].equalsIgnoreCase("disable")) {
					return disablePlugin(args, sender);
				}
				if (args[0].equalsIgnoreCase("load")) {
					return loadPlugin(args, sender);
				}
				if (args[0].equalsIgnoreCase("unload")) {
					return unloadPlugin(args, sender);
				}
				if (args[0].equalsIgnoreCase("list")) {
					return listPlugins(args, sender);
				}
			} // End of args.length > 0 code
		} // End of command label check
		return false;
	} // End of onCommand code
	
	boolean enablePlugin(String[] args, CommandSender sender) {
		if (args.length > 1 && validPlugin(args[1])) {
			pm.enablePlugin(pm.getPlugin(args[1]));
			sender.sendMessage(ChatColor.DARK_AQUA + "Enabled plugin "
					+ pm.getPlugin(args[1]).getDescription().getName());
		} else {
			sender.sendMessage(ChatColor.DARK_AQUA
					+ "Please specify a valid plugin to enable");
		}
		return true;
	}
	
	boolean disablePlugin(String[] args, CommandSender sender) {
		if (args.length > 1 && validPlugin(args[1])) {
			pm.disablePlugin(pm.getPlugin(args[1]));
			sender.sendMessage(ChatColor.DARK_AQUA + "Disabled plugin "
					+ pm.getPlugin(args[1]).getDescription().getName());
		} else {
			sender.sendMessage(ChatColor.DARK_AQUA
					+ "Please specify a valid plugin to disable");
		}
		return true;
	}
	
	boolean allowed(CommandSender sender) {
		boolean allowed = false;
		if (sender instanceof Player && permissionHandler != null
				&& permissionHandler.has((Player) sender, "pluginmanager.use")) {
			allowed = true;
		} else if (sender.isOp()) {
			allowed = true;
		}
		return allowed;
	}
	
	boolean loadPlugin(String[] args, CommandSender sender) {
		if (args.length == 2) {
			File file = new File("plugins\\" + args[1] + ".jar");
			if (file.exists()) {
				try {
					Plugin p = pm.loadPlugin(file);
					pm.enablePlugin(p);
				} catch (InvalidPluginException invalidPlugin) {
					sender.sendMessage(ChatColor.RED
							+ "Specified plugin exists, but is invalid.");
				} catch (InvalidDescriptionException invalidDescription) {
					sender.sendMessage(ChatColor.RED
							+ "Specified plugin exists, but has an invalid description.");
				} catch (UnknownDependencyException unknownDependancy) {
					sender.sendMessage(ChatColor.RED
							+ "Specified plugin exists, but is invalid. Consider checking dependencies.");
				}
			} else {
				sender.sendMessage(ChatColor.RED
						+ "Invalid plugin file specified");
				return true;
			}
			sender.sendMessage(ChatColor.DARK_AQUA + "Loaded plugin "
					+ pm.getPlugin(args[1]).getDescription().getName());
		} else {
			sender.sendMessage(ChatColor.DARK_AQUA
					+ "Please specify a valid plugin to load");
			return true;
		}
		return true;
	}
	
	boolean validPlugin(String name) {
		Plugin[] plugins = pm.getPlugins();
		for (int i = 0; i < plugins.length; i++) {
			if (name.equals(plugins[i].getDescription().getName())) {
				return true;
			}
		}
		return false;
	}
	
	boolean listPlugins(String[] args, CommandSender sender) {
		String message = ChatColor.DARK_AQUA + "Plugins present: ";
		Plugin[] plugins = pm.getPlugins();
		ChatColor colour;
		for (int i = 0; i < plugins.length; i++) {
			if (plugins[i].isEnabled()) {
				colour = ChatColor.DARK_GREEN;
			} else {
				colour = ChatColor.RED;
			}
			message += "" + colour + plugins[i].getDescription().getName()
					+ ", ";
		}
		sender.sendMessage(message);
		return true;
	}
	
	private void setupPermissions() {
		Plugin permissionsPlugin = this.getServer().getPluginManager()
				.getPlugin("Permissions");
		if (Pluginmanager.permissionHandler == null) {
			if (permissionsPlugin != null) {
				Pluginmanager.permissionHandler = ((Permissions) permissionsPlugin)
						.getHandler();
				log.info("PluginManager: Using Permissions system");
			} else {
				log.info("PluginManager: Permission system not detected, using default permissions");
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private boolean unloadPlugin(String[] args, CommandSender sender) {
		String pluginName = args[1];
		if (!(validPlugin(pluginName)))
			return false;
		
		SimplePluginManager spm = (SimplePluginManager) pm;
		
		List<Plugin> plugins = null;
		Map<String, Plugin> lookupNames = null;
		Map<Event.Type, SortedSet<RegisteredListener>> listeners = null;
		SimpleCommandMap commandMap = null;
		Map<String, Command> knownCommands = null;
		
		if (spm != null) {
			// this is fucking ugly
			// as there is no public getters for these, and no methods to
			// properly unload plugins
			// I have to fiddle directly in the private attributes of the plugin
			// manager class
			try {
				Field pluginsField = spm.getClass().getDeclaredField("plugins");
				Field lookupNamesField = spm.getClass().getDeclaredField(
						"lookupNames");
				Field listenersField = spm.getClass().getDeclaredField(
						"listeners");
				Field commandMapField = spm.getClass().getDeclaredField(
						"commandMap");
				
				pluginsField.setAccessible(true);
				lookupNamesField.setAccessible(true);
				listenersField.setAccessible(true);
				commandMapField.setAccessible(true);
				
				plugins = (List<Plugin>) pluginsField.get(spm);
				lookupNames = (Map<String, Plugin>) lookupNamesField.get(spm);
				listeners = (Map<Type, SortedSet<RegisteredListener>>) listenersField
						.get(spm);
				commandMap = (SimpleCommandMap) commandMapField.get(spm);
				
				Field knownCommandsField = commandMap.getClass()
						.getDeclaredField("knownCommands");
				
				knownCommandsField.setAccessible(true);
				
				knownCommands = (Map<String, Command>) knownCommandsField
						.get(commandMap);
			} catch (Exception e) {
				sender.sendMessage(ChatColor.RED
						+ "An exception occured while disabling " + pluginName);
			}
		} else
			return false;
		
		// in case the same plugin is loaded multiple times (could happen)
		for (Plugin pl : pm.getPlugins()) {
			if (pl.getDescription().getName().equalsIgnoreCase(pluginName)) {
				// disable the plugin itself
				pm.disablePlugin(pl);
				
				// removing all traces of the plugin in the private structures
				// (so it won't appear in the plugin list twice)
				if (plugins != null && plugins.contains(pl)) {
					plugins.remove(pl);
				}
				
				if (lookupNames != null && lookupNames.containsKey(pluginName)) {
					lookupNames.remove(pluginName);
				}
				
				// removing registered listeners to avoid registering them twice
				// when reloading the plugin
				if (listeners != null) {
					for (SortedSet<RegisteredListener> set : listeners.values()) {
						for (Iterator<RegisteredListener> it = set.iterator(); it
								.hasNext();) {
							RegisteredListener value = it.next();
							
							if (value.getPlugin() == pl) {
								it.remove();
							}
						}
					}
				}
				
				// removing registered commands, if we don't do this they can't
				// get re-registered when the plugin is reloaded
				if (commandMap != null) {
					for (Iterator<Map.Entry<String, Command>> it = knownCommands
							.entrySet().iterator(); it.hasNext();) {
						Map.Entry<String, Command> entry = it.next();
						
						if (entry.getValue() instanceof PluginCommand) {
							PluginCommand c = (PluginCommand) entry.getValue();
							
							if (c.getPlugin() == pl) {
								c.unregister(commandMap);
								
								it.remove();
							}
						}
					}
				}
				
				for (Permission permission : pl.getDescription()
						.getPermissions()) {
					pm.removePermission(permission);
				}
				
				// ta-da! we're done (hopefully)
				// I don't know if there are more things that need to be reset
				// I'll take a more in-depth look into the bukkit source if it
				// doesn't work well
			}
		}
		return true;
	}
}