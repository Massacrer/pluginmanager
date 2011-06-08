package me.Massacrer.Pluginmanager;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.UnknownDependencyException;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

@SuppressWarnings("unused")
public class Pluginmanager extends JavaPlugin {
	org.bukkit.plugin.PluginManager pm = null;
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
			if (args.length > 0) {
				if (args[0].equalsIgnoreCase("enable")) {
					if (args.length > 1
							&& validPlugin(args[1], pm.getPlugins())) {
						pm.enablePlugin(pm.getPlugin(args[1]));
						sender.sendMessage(ChatColor.DARK_AQUA
								+ "Enabled plugin "
								+ pm.getPlugin(args[1]).getDescription()
										.getName());
					} else {
						sender.sendMessage(ChatColor.DARK_AQUA
								+ "Please specify a valid plugin to enable");
					}
					return true;
				} // End of enable
				if (args[0].equalsIgnoreCase("disable")) {
					if (args.length > 1
							&& validPlugin(args[1], pm.getPlugins())) {
						pm.disablePlugin(pm.getPlugin(args[1]));
						sender.sendMessage(ChatColor.DARK_AQUA
								+ "Disabled plugin "
								+ pm.getPlugin(args[1]).getDescription()
										.getName());
					} else {
						sender.sendMessage(ChatColor.DARK_AQUA
								+ "Please specify a valid plugin to disable");
					}
					return true;
				} // End of disable
				if (args[0].equalsIgnoreCase("load")) {
					if (args.length == 2) {
						loadPlugin(sender, args[1]);
						sender.sendMessage(ChatColor.DARK_AQUA
								+ "Loaded plugin "
								+ pm.getPlugin(args[1]).getDescription()
										.getName());
					} else {
						sender.sendMessage(ChatColor.DARK_AQUA
								+ "Please specify a valid plugin to load");
					}
					
					return true;
				} // End of load
				if (args[0].equalsIgnoreCase("unload")) {
					if (args.length > 1
							&& validPlugin(args[1], pm.getPlugins())) {
						pm.disablePlugin(pm.getPlugin(args[1]));
						if (unloadPlugin(pm.getPlugin(args[1]), sender)) {
							sender.sendMessage(ChatColor.DARK_AQUA + "Plugin "
									+ args[1] + " unloaded");
						}
					} else {
						sender.sendMessage(ChatColor.RED
								+ "Specify a valid plugin to unload");
					}
					return true;
				}
				if (args[0].equalsIgnoreCase("list")) {
					String message = ChatColor.DARK_AQUA + "Plugins present: ";
					Plugin[] plugins = pm.getPlugins();
					ChatColor colour;
					for (int i = 0; i < plugins.length; i++) {
						if (plugins[i].isEnabled()) {
							colour = ChatColor.DARK_GREEN;
						} else {
							colour = ChatColor.RED;
						}
						message += "" + colour
								+ plugins[i].getDescription().getName()
								+ ", ";
					}
					sender.sendMessage(message);
					return true;
				}
				if (!(sender instanceof Player)
						&& args[0].equalsIgnoreCase("debug")) {
					debug = !debug;
					log.info("Debug state now "
							+ (debug ? "enabled" : "disabled"));
					return true;
				}
			} // End of args.length > 0 code
		} // End of command label check
		return false;
	} // End of onCommand code
	
	boolean allowed(CommandSender sender) {
		boolean allowed = false;
		if (sender instanceof Player) {
			if (permissionHandler.has((Player) sender, "pluginmanager.use")) {
				allowed = true;
			}
		}
		if (sender.isOp()) {
			allowed = true;
		}
		return allowed;
	}
	
	void loadPlugin(CommandSender sender, String arg) {
		File file = new File("plugins\\" + arg + ".jar");
		if (file.exists()) {
			try {
				pm.loadPlugin(file);
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
		}
	}
	
	boolean validPlugin(String name, Plugin[] plugins) {
		boolean matchFound = false;
		for (int i = 0; i < plugins.length; i++) {
			if (name.equals(plugins[i].getDescription().getName())) {
				matchFound = true;
			}
		}
		return matchFound;
	}
	
	@SuppressWarnings("unchecked")
	boolean unloadPlugin(Plugin plugin, CommandSender sender) {
		// Class<?> SPM = SimplePluginManager.class.getClass();
		// Field pluginsField = null; // ArrayList<Plugin>
		// Field lookupNamesField = null; // HashMap<String, Plugin>
		
		try {
			// Hook plugin fields
			Field pluginsField = SimplePluginManager.class
					.getDeclaredField("plugins");
			Field lookupNamesField = SimplePluginManager.class
					.getDeclaredField("lookupNames");
			Field listenersField = SimplePluginManager.class
					.getDeclaredField("listeners");
			
			Field registeredListenerField = RegisteredListener.class
					.getDeclaredField("plugin");
			
			// Also need to remove relevant entry from CraftServer's
			// SimpleCommandMap(that) commandMap. Possibly other references not
			// yet known, keep trawling the code
			
			// Set fields accessible
			pluginsField.setAccessible(true);
			lookupNamesField.setAccessible(true);
			listenersField.setAccessible(true);
			registeredListenerField.setAccessible(true);
			
			// Pull references for the hidden variables from Reflection
			ArrayList<Plugin> plugins = (ArrayList<Plugin>) pluginsField
					.get(pm);
			HashMap<String, Plugin> lookup = (HashMap<String, Plugin>) lookupNamesField
					.get(pm);
			EnumMap<Event.Type, SortedSet<RegisteredListener>> listeners = (EnumMap<Event.Type, SortedSet<RegisteredListener>>) listenersField
					.get(pm);
			
			// Analysis of certain fields to find out which elements to remove
			Set<Entry<Type, SortedSet<RegisteredListener>>> s_listeners = listeners
					.entrySet();
			Iterator<Entry<Type, SortedSet<RegisteredListener>>> itr_entries = s_listeners
					.iterator();
			Plugin storedPlugin = null;
			entries:
			while (itr_entries.hasNext()) {
				Entry<Type, SortedSet<RegisteredListener>> entry = itr_entries
						.next();
				SortedSet<RegisteredListener> registeredListeners = entry
						.getValue();
				Iterator<RegisteredListener> itr_listeners = registeredListeners
						.iterator();
				while (itr_listeners.hasNext()) {
					RegisteredListener registeredListener = itr_listeners
							.next();
					storedPlugin = (Plugin) registeredListenerField
							.get(registeredListener);
					if (storedPlugin == plugin) {
						// Remove entry from outer set
						itr_entries.remove();
						break entries;
					}
				}
			}
			
			// The final ingredient :)
			plugins.remove(plugin);
			lookup.remove(plugin.getDescription().getName());
			
			// Done :D
			
			// SimpleCommandMap relevant fields:
			// HashMap<String ?, Command ?> knownCommands
			// HashSet<String ?> aliases
			//
			// JavaPluginLoader.entry.close() needs to be called?
			
		} catch (SecurityException e) {
			reportUnloadingException(e, sender, e.getClass().getName());
			return false;
		} catch (NoSuchFieldException e) {
			reportUnloadingException(e, sender, e.getClass().getName());
			return false;
		} catch (IllegalArgumentException e) {
			reportUnloadingException(e, sender, e.getClass().getName());
			return false;
		} catch (IllegalAccessException e) {
			reportUnloadingException(e,
					sender, e.getClass().getName());
			return false;
		}/*
		 * catch (NoSuchMethodException e) { reportUnloadingException(e, sender,
		 * e.getClass().getName()); return false; } catch
		 * (InvocationTargetException e) { reportUnloadingException(e, sender,
		 * e.getClass().getName()); return false; }
		 */
		
		return true;
	}
	
	private void reportUnloadingException(Exception e, CommandSender sender,
			String type) {
		if (debug) {
			e.printStackTrace();
			log.info("Exception info: " + e.toString());
		}
		sender.sendMessage(ChatColor.RED
				+ "PluginManager: error while disabling plugin: "
				+ type);
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
}
