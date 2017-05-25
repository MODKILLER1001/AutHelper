package me.raum.autcraft;

import java.io.File;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.earth2me.essentials.Essentials;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class AutcraftHelper extends JavaPlugin implements Listener {
	
	public static boolean debug = false;
	public static Economy economy = null;
	public static AutcraftHelper plugin;
	double swearPenalty = 1.0;
	public static ArrayList<String> swearWords = new ArrayList<String>();
	public static HashMap<String,String> splitChatPlayers = new HashMap<String,String>();
	boolean chatDisabled = false;
	public WorldGuardPlugin wg = null;
	public static WorldGuardPlugin worldguard;
	
	public static HashMap<String,String> channelPlayers = new HashMap<String,String>();
	public static HashMap<String,String> chatHighlight = new HashMap<String,String>();
	public HashMap<String,Integer> channels = new HashMap<String,Integer>();
	
	public int maxChannels = 5;
	public Chat chat = null;
	public Essentials ess = null;
	public String nameHighlight = "&e";
	public String swearMessage = "";
	public String chatFormat = "";
	public ArrayList<String> defaultChannels = new ArrayList<String>();
	public String channelFormat = "";
	public int maxSplitLength;
	public int maxCapLetters = 50;
	
	public Object loadPlugin(String string) {
		Object plug = null;

		try {
			plug = getServer().getPluginManager().getPlugin(string);
		} catch (Exception e) {
			Util.error("Plugin '" + string + "' not found!");
		}
		return plug;
	}
	
	public void onDisable() {
		
		for ( Player rec : Bukkit.getServer().getOnlinePlayers() ) {
			if ( channelPlayers.containsKey( rec.getUniqueId().toString() ) )
			{
				setConfig(F.playerFile,"inChan."+rec.getUniqueId().toString(), channelPlayers.get( rec.getUniqueId().toString() ));
//				l(rec,"REMOVE_RELOAD");
			}
		}
	}
	
	public void onEnable() {
		plugin = this;		
		channels.clear();
		
		setupEconomy();
		setupChat();
		setupConfig();
		
		loadSwearwords();
		registerEvents(plugin);
		
		
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { // Forces this to occur AFTER all plugins are loaded, just in case
			public void run() {
				ess = (Essentials) loadPlugin("Essentials");
				wg = (WorldGuardPlugin) loadPlugin("WorldGuard");
				worldguard = (WorldGuardPlugin) loadPlugin("WorldGuard");
			}
		}, 20L);

	}

	private void setupConfig() {
		

		F.configFile = F.loadConfig(F.configConfig,"config.yml");
		F.configConfig = F.getConfig(F.configFile);
		
		reloadConfig();
//		F.reloadConfig(F.configFile);
		//configFile = new File(plugin.getDataFolder(), "config.yml");
		//config = new YamlConfiguration();
		//reloadConfig();
		

			F.swearFile = F.loadConfig(F.swearConfig, "swear.yml");
			F.swearConfig = F.getConfig(F.swearFile);
		
			F.helpFile = F.loadConfig(F.helpConfig, "help.yml");
			F.helpConfig = F.getConfig(F.helpFile);

			F.playerFile = F.loadConfig(F.playerConfig, "players.yml");
			F.playerConfig = F.getConfig(F.playerFile);
			
			F.channelFile = F.loadConfig(F.channelConfig, "channels.yml");
			F.channelConfig = F.getConfig(F.channelFile);
	
			F.stringsFile = F.loadConfig(F.stringsConfig, "strings.yml");
			F.stringsConfig = F.getConfig(F.stringsFile);
		
		if ( !F.getConfig(F.swearFile).contains("swearPenalty")) {
			F.getConfig(F.swearFile).set("swearPenalty", 1.0);
			F.saveConfig(F.swearFile,  F.swearConfig);
		}
		swearPenalty = F.getConfig(F.swearFile).getDouble("swearPenalty");
		
		if ( !F.getConfig(F.swearFile).contains("swearMessage")) {
			F.getConfig(F.swearFile).set("swearMessage", "You are not allowed to swear on Autcraft. You have been deducted $%1 from your account.");
			F.saveConfig(F.swearFile,  F.swearConfig);
		}
		swearMessage = F.getConfig(F.swearFile).getString("swearMessage");

		loadSwearwords();

		setConfig(F.configFile,"channelFormat","(%1)");
		setConfig(F.configFile,"maxSplitLength",35);
		setConfig(F.configFile,"maxCapLetters",50);
		
			setConfig(F.helpFile,"ach","This is the main help entry for the 'ACH' command in the Autcraft Helper Plugin.");
			setConfig(F.helpFile,"highlight","Syntax: /ach highlight (color) : This command will let you have your own name highlighted in chat so that it stands out when someone says it.");
			setConfig(F.helpFile,"channel","Syntax: /channel <options> : This is the main command for channels. You can join, leave or create a channel.");
			setConfig(F.helpFile,"join","Syntax: /channel join <name> : This command will let you join a channel that already exists.");
			setConfig(F.helpFile,"leave","Syntax: /channel leave : This command will let you leave the channel you are in.");
			setConfig(F.helpFile,"create","Syntax: /channel create <name> : This command will allow you to create a new channel.");
			setConfig(F.helpFile,"forceremove","Syntax: /channel forceremove <name> : This command will forcibly remove someone from a channel.");
			setConfig(F.helpFile,"splitchat","Syntax: /splitchat <options> : This command will make your chat split up, using the line you decide.");

			setConfig(F.channelFile,"maxChannels",20);
			setConfig(F.channelFile,"defaultChannels", Arrays.asList("RP1", "RP2"));
			
		if ( !getConfig().contains("chatFormat") )
			getConfig().set("chatFormat","{channel}{prefix}{player}: {message}" );
		if ( !getConfig().contains("nameHighlight") )
			getConfig().set("nameHighlight", "&e" );
		
		maxCapLetters = getConfig().getInt("maxCapLetters");
		maxSplitLength = getConfig().getInt("maxSplitLength");
		channelFormat = getConfig().getString("channelFormat");
		nameHighlight = getConfig().getString("nameHighlight");
		chatFormat = getConfig().getString("chatFormat");
		maxChannels  = F.getConfig(F.channelFile).getInt("maxChannels");

		loadSplitChat();
		loadChatHighlight();
		loadDefaultChannels();
		loadStrings();
		reloadActiveChannels();
//		saveConfig();
		
	}

	private void reloadActiveChannels() {
	
		ConfigurationSection config = F.getConfig(F.playerFile).getConfigurationSection("inChan");
		if (config == null)
			return;
		
			for (String key : config.getKeys(false)) {
				String uuid = key;
				String chan = config.getString(key);
				
				channelPlayers.put( uuid, chan );

				if ( !channels.containsKey( chan ) )
					channels.put(chan, 0);
					
				channels.put( chan,  channels.get(chan)+1);
				removeConfig(F.playerFile, "inChan." +key);
			}
	}

	private void loadDefaultChannels() {
		
		defaultChannels.clear();

		try {
			List<?> ruleslist = F.getConfig(F.channelFile).getList("defaultChannels");

			for (int i = 0; i < ruleslist.size(); i++)
			{
				defaultChannels.add(ruleslist.get(i).toString());
				channels.put(ruleslist.get(i).toString(),0 );
				
			}
		} catch (Exception e) {
			Util.info("Swear list could not be loaded! Please provide everything in between the bars to Raum!");
			Util.info("----------------------------------");
			e.printStackTrace();
			Util.info("----------------------------------");
		}
		
	}

	private void removeConfig(File file, String field )
	{
		F.getConfig(file).set(field, null );
		F.saveConfig(file,  F.filemap.get(file) );
		
	}
	private void setConfig(File file, String field, Object data) {
		if ( F.getConfig(file).contains(field) )
			return;
		
		F.getConfig(file).set(field, data );
		F.saveConfig(file,  F.filemap.get(file) );
	}

	private void loadChatHighlight() {
			chatHighlight.clear();
		
		for ( Player p : Bukkit.getServer().getOnlinePlayers() ){ // Players already online - reload the list  just in case			
			{
				addHighlight(p);
			}
			
		}
	}

	private void addHighlight(Player p) {
		String highlight = getHighlight(p);
		chatHighlight.put(p.getUniqueId().toString(),  highlight);
	}

	public String getHighlight( Player p ) 
	{

		if ( ( F.getConfig(F.playerFile).contains("highlight." + p.getUniqueId().toString() )))
			return F.getConfig(F.playerFile).getString("highlight." + p.getUniqueId().toString() );

		return nameHighlight;
	}
	
	private void loadSplitChat() {
		splitChatPlayers.clear();
		
		for ( Player p : Bukkit.getServer().getOnlinePlayers() ){ // Players already online - reload the list  just in case			
			{
				String split = splitChat(p);
				if ( split != null )
					splitChatPlayers.put(p.getUniqueId().toString(),  split);
			}
			
		}
	}

	private String splitChat(Player p) {
	
		if ( ( F.getConfig(F.playerFile).contains("chatSplit." + p.getUniqueId().toString() )))
			return F.getConfig(F.playerFile).getString("chatSplit." + p.getUniqueId().toString() );
		
		return null;
	}

	private void loadSwearwords() {
		swearWords.clear();

		if (!F.getConfig(F.swearFile).contains("swear")) {
			F.getConfig(F.swearFile).set("swear", Arrays.asList("anal",  "anus",  "arse",  "asshole",  "ballsack",  "balls",  "bastard",  "bitch",  "biatch",  "blowjob",  "blow job",  "bollock",  "bollok",  "boner",  "boob", "buttplug",  "clitoris",  "cock",  "coon",  "crap",  "cunt",  "damn",  "dick",  "dildo",  "dyke",  "fag",  "feck",  "fellate",  "fellatio",  "felching",  "fuck",  "f u c k",  "fudgepacker",  "fudge packer",  "flange",  "goddamn",  "god damn",  "hell",  "homo",  "jerk",  "jizz",  "knobend",  "knob end",  "labia",  "lmao",  "lmfao",  "muff",  "nigger",  "nigga", "penis",  "piss",  "poop",  "prick",  "pube",  "pussy",  "queer",  "scrotum",  "sex",  "shit",  "s hit",  "sh1t",  "slut",  "smegma",  "spunk",  "tit",  "tosser",  "turd",  "twat",  "vagina",  "wank",  "whore",  "wtf", "retard" ) );
			F.saveConfig(F.swearFile, F.swearConfig);
		}

		try {
			List<?> ruleslist = F.getConfig(F.swearFile).getList("swear");

			for (int i = 0; i < ruleslist.size(); i++)
				swearWords.add(ruleslist.get(i).toString());
		} catch (Exception e) {
			Util.info("Swear list could not be loaded! Please provide everything in between the bars to Raum!");
			Util.info("----------------------------------");
			e.printStackTrace();
			Util.info("----------------------------------");
		}
	}

	private void setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return;
		}
		RegisteredServiceProvider<Economy> rsp = getServer()
				.getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return;
		}
		economy = rsp.getProvider();
		return;

	}
	

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {

		Player p = event.getPlayer();
		addHighlight(p);

		String split = splitChat(p);
		if ( split != null )
			splitChatPlayers.put(p.getUniqueId().toString(),  split);
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player p = event.getPlayer();
		String uuid = p.getUniqueId().toString();

		if ( splitChatPlayers.containsKey( uuid ) )
			splitChatPlayers.remove( uuid );
		
		if ( channelPlayers.containsKey( uuid ) )
		{
			removeChannel( p );
			channelPlayers.remove( uuid );

		}
		
		chatHighlight.remove( uuid );
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {

		if (cmd.getName().equalsIgnoreCase("ach")) {
			if (args.length == 0) {
				showHelp(sender);
			} else if (args[0].equalsIgnoreCase("help")) {
				if (args.length < 2) {
					showHelp(sender);
					return true;
				}
				doHelp(sender, args[1]);
			} else if ( args[0].equalsIgnoreCase("highlight") ) {

				if (args.length < 2) {
					l(sender,"HIGHLIGHT_SYNTAX");
					return true;
				}
				String uuid = ((Player)sender).getUniqueId().toString();
				String high = args[1].toLowerCase().trim();
				
				if ( high.length() != 2 || !high.substring(0, 1).equals("&") || !high.substring(1).toString().matches("[0-9a-f]") )
				{
					l(sender,"HIGHLIGHT_NOT_A_COLOR");
					return true;
				}

				chatHighlight.remove(uuid);
				chatHighlight.put(uuid,high);
				F.getConfig(F.playerFile).set("highlight." + uuid, high );
				F.saveConfig(F.playerFile, F.playerConfig);
				l(sender,"HIGHLIGHT_CHANGED", high);
				return true;
				
			}
			else if (args[0].equalsIgnoreCase("status")) {
			
				
				Util.msg(sender,"MaxCap: " + maxCapLetters);
				Util.msg(sender,"MaxSplit: " + maxSplitLength);
				Util.msg(sender,"Chan Format: " + channelFormat);
				Util.msg(sender,"Chat Format: " + chatFormat);
				Util.msg(sender,"MaxChan: " + maxChannels);
				Util.msg(sender,"Default Channels: " + defaultChannels.size() );
				Util.msg(sender,"Total Channels: " + channels.size() );
				
				
				return true;
			}
			else if (args[0].equalsIgnoreCase("listen")) {
				if (!perm(sender, "channel.admin")) {
					l(sender,"NO_PERMISSION");
					return true;
				}
				Player p = (Player) sender;
				boolean listen = canListen(p);
				
				if ( listen )
				{
					F.getConfig(F.playerFile).set("listening."+ p.getUniqueId().toString(), false);
					l(p,"LISTEN_OFF");
				}
				else
				{
					F.getConfig(F.playerFile).set("listening."+ p.getUniqueId().toString(), null);
					l(p,"LISTEN_ON");
				}
				
				F.saveConfig(F.playerFile, F.playerConfig);
				return true;
			}
			else if (args[0].equalsIgnoreCase("debug")) {
				if (!perm(sender, "admin")) {
				l(sender,"NO_PERMISSION");
				Util.info(sender.getName()
						+ " tried to reload the Autcraft Helper plugin without permission.");
				return true;
			}
			debug = !debug;
			Util.msg(sender, "Debug is set to: " + debug);
			return true;
			}
			else if (args[0].equalsIgnoreCase("disablechat") || args[0].equalsIgnoreCase("enablechat") ) {
				if (!perm(sender, "admin")) {
				l(sender,"NO_PERMISSION");
				Util.info(sender.getName() + " tried to disable chat without permission!");
				return true;
			}
			chatDisabled = !chatDisabled;
			if ( chatDisabled )
				Util.msgall( getString("CHAT_DISABLED") );
			else
				Util.msgall( getString("CHAT_ENABLED") );
			return true;
			}
			else if (args[0].equalsIgnoreCase("clear")) {
				if (!perm(sender, "channel.admin")) {
					l(sender,"NO_PERMISSION");
					return true;
				}
				
				for ( Player rec : Bukkit.getServer().getOnlinePlayers() ) {
					for ( int x = 0; x < 25; x++)
						Util.msg(rec, "");
				}
				
				Util.msg(sender,"Cleared!");
				return true;
			}
			else if (args[0].equalsIgnoreCase("reload")) {
				if (!perm(sender, "admin")) {
					l(sender,"NO_PERMISSION");
					Util.info(sender.getName()
							+ " tried to reload the Autcraft Helper plugin without permission.");
					return true;
				}

				setupConfig();
				Util.msg(sender, "Autcraft Helper has been reloaded.");
				return true;
			}
		}

		if (!(sender instanceof Player)) {
			Util.msg(sender,
					"Only a player can use the rest of these commands. Sorry, Console!");
			return true;
		}
		Player p = (Player) sender;


		if (cmd.getName().equalsIgnoreCase("chat") ) {

			String chan = "";
			if ( channelPlayers.containsKey( p.getUniqueId().toString()) )
				chan = channelPlayers.get( p.getUniqueId().toString() );
			
			channelPlayers.remove( p.getUniqueId().toString() );			
			doChat(p, Util.stringFromArray(args, 0));
			
			if ( chan.length() != 0 )
				channelPlayers.put( p.getUniqueId().toString(), chan );
			return true;
			
		}
		
		if (cmd.getName().equalsIgnoreCase("splitchat")) {
			String splitType = null;

			if (args.length == 0) {
				l(p,"SPLITCHAT_DEFAULT_1");
				l(p, "SPLITCHAT_DEFAULT_2");
				return true;
			}

			switch (args[0].toLowerCase()) {
			default: // Custom
				String line = Util.stringFromArray(args, 0);
				double num = maxSplitLength
						/ ChatColor.stripColor(Util.colorize(line)).length();
				if (num >= 1)
					splitType = StringUtils.repeat(line, (int) Math.round(num)).substring(0,maxSplitLength);
				break;
			case "line":
				splitType = StringUtils.repeat("-", maxSplitLength);
				break;
			case "blank":
				splitType = " ";
				break;
			case "star":
				splitType = StringUtils.repeat("*", maxSplitLength);
				break;
			case "off":
				l(p,"SPLITCHAT_OFF");

				if (splitChatPlayers.containsKey(p.getUniqueId().toString())) {
					splitChatPlayers.remove(p.getUniqueId().toString());
					F.getConfig(F.playerFile).set(
							"chatSplit." + p.getUniqueId().toString(), null);
					F.saveConfig(F.playerFile, F.playerConfig);
				}
				break;
			}

			if (splitType != null) {
				l(p,"SPLITCHAT_SET");
				
				splitChatPlayers.put(p.getUniqueId().toString(), splitType);
				F.getConfig(F.playerFile).set(
						"chatSplit." + p.getUniqueId().toString(), splitType);
				F.saveConfig(F.playerFile, F.playerConfig);
			}

			return true;
		}

		if (cmd.getName().equalsIgnoreCase("channel")) {
			if ( args.length == 0 )
			{
				Util.msg(p,"What channel command do you want to use?");
				
				ArrayList<String> list = new ArrayList<String>();
				Util.msg(sender,"\nValid commands are: " );
				if ( perm(sender,"channel.admin") ) list.add("forceleave");
				if ( perm(sender,"channel.join") ) list.add("join");
				if ( perm(sender,"channel.join") ) list.add("leave");
				if ( perm(sender,"channel.join") ) list.add("list");
				if ( perm(sender,"channel.create") ) list.add("create");
				if ( perm(sender,"channel.admin") ) list.add("delete");
				if ( perm(sender,"channel.admin") ) list.add("tell");
				
				Util.msg(sender,Util.colorList("&e", "&6", list) );
				Util.msg(sender, "&FYou can use &6/ach help <command>&F for more information on these commands, or use &e/channel <command>&F to use one!");

				return true;
			}
			if (args[0].equalsIgnoreCase("version") ) {
				Util.msg(sender,"Autcraft Helper Version: " + getDescription().getVersion() );
				
				return true;
			}
			if (args[0].equalsIgnoreCase("help") ) {
				if ( args.length < 2 )
					showHelp(sender);
				else
					doHelp(sender,args[1]);
				return true;
				
			}
			if (args[0].equalsIgnoreCase("who") ) { // Show players in their channels
			
				ArrayList<String> fullList = new ArrayList<String>();
				
				for ( String chans : channels.keySet() )
				{
					ArrayList<String> chan = new ArrayList<String>();
					
					for ( Player rec : Bukkit.getServer().getOnlinePlayers() ) {
						if ( channelPlayers.containsKey( rec.getUniqueId().toString() ) )
						{
							if ( channelPlayers.get( rec.getUniqueId().toString() ).equalsIgnoreCase(chans) )
									chan.add(rec.getName() );
						}
					}
					if ( chan.size() == 0 )
						chan.add("Nobody!");
					fullList.add( chans + ": " + Util.stringFromList(chan,0) + "\n");
				}
				
				Util.msg(p,Util.colorList("&e", "&6", fullList));
				return true;
			}
			if (args[0].equalsIgnoreCase("gui")) { // GUI for joining channels
				if (!perm(p, "channel.join")) {
					l(p,"NO_PERMISSION");
					return true;
				}

				ShowJoinGUI(p, 1);
				return true;
				
			}
			if (args[0].equalsIgnoreCase("list")) { // List Channels
				if (!perm(p, "channel.join")) {
					l(p,"NO_PERMISSION");
					return true;
				}
				ArrayList<String> chanlist = new ArrayList<String>();
				
				for ( String chan : channels.keySet() ){
					chanlist.add( chan + " &F["+channels.get(chan) +"]");
				}
			
				Util.msg(p, "The following channels exist to join:");
				Util.msg(p, Util.colorList("&e", "&6", chanlist));
				Util.msg(p, "Use &a/channel join (name)&F to join one!");
				return true;

			} else if (args[0].equalsIgnoreCase("delete")) { // Force delete a channel

				if (!perm(p, "channel.admin")) {
					l(p,"NO_PERMISSION");
					return true;
				}

				if (args.length == 0) {
					Util.msg(sender,
							"You must provide a name of a channel to delete.");
					
					return true;
				}
				String chan = "";

				for (String c : channels.keySet() ) {
					if (c.equalsIgnoreCase(args[1])) {
						chan = c;
						break;
					}
				}

				if (chan.trim().length() == 0) {
					Util.msg(p, "That channel does not exist.");
					return true;
				}

				if ( defaultChannel(chan) )
				{
					Util.msg(p,"That is a default channel - you cannot delete it.");
					return true;
				}

				channels.remove(chan);
				for ( Player rec : Bukkit.getServer().getOnlinePlayers() )
				{
					String c = "";
					if ( channelPlayers.containsKey( rec.getUniqueId().toString()) )
						c = channelPlayers.get( rec.getUniqueId().toString() );
					
					if ( c == chan )
					{
						Util.msg(rec,"The channel you were in was deleted.");
						channelPlayers.remove(rec.getUniqueId().toString() );
					}
					
				}
				Util.msg(sender,"The channel has been deleted.");
				return true;
				
			} else if (args[0].equalsIgnoreCase("tell")) { // Force delete a channel

				if (!perm(p, "channel.admin")) {
					l(p,"NO_PERMISSION");
					return true;
				}

				if (args.length == 0) {
					Util.msg(sender,
							"You must provide a name of a channel to delete.");
					
					return true;
				}
				
				String chan = "";

				for (String c : channels.keySet() ) {
					if (c.equalsIgnoreCase(args[1])) {
						chan = c;
						break;
					}
				}

				if (chan.trim().length() == 0) {
					Util.msg(p, "That channel does not exist.");
					return true;
				}

				String currchan = "";
				if ( channelPlayers.containsKey( p.getUniqueId().toString()) )
					currchan = channelPlayers.get( p.getUniqueId().toString() );
				
				channelPlayers.put(p.getUniqueId().toString(), chan);
				doChat(p, Util.stringFromArray(args,2));
				
				if ( currchan.length() != 0 )
					channelPlayers.put( p.getUniqueId().toString(), currchan );
				else
					channelPlayers.remove( p.getUniqueId().toString() );

				return true;
				

			} else if (args[0].equalsIgnoreCase("forceleave")) { // Create a
																	// channel
				if (!perm(p, "channel.admin")) {
					l(p,"NO_PERMISSION");
					return true;
				}

				if (args.length == 0) {
					Util.msg(sender,
							"You must provide a name of a player to remove from channels.");
					return true;
				}

				Player find = Util.findPlayer(args[1]);

				if (find == null) {
					Util.msg(p,
							"That player was not found. Check your spelling and try again.");
					return true;
				}

				String chan = "";
				if (channelPlayers.containsKey(find.getUniqueId().toString()))
					chan = channelPlayers.get(find.getUniqueId().toString());

				if (chan.trim().length() == 0) {
					Util.msg(p, "That player is not in a channel.");
					return true;
				}

				Util.msg(find, "You have been removed from the channel '"
						+ chan + "'.");
				removeChannel(find);
				return true;

			} else if (args[0].equalsIgnoreCase("create")) { // Create a channel
				if (!perm(p, "channel.create")) {
					l(p,"NO_PERMISSION");
					return true;
				}

				if ( args.length == 1 || args[1].trim().length() == 0)
				{
					l(p,"NO_CHAN_NAME");
					return true;
				}

				String chan = args[1];

				for (String c : channels.keySet() ) {
					if (c.equalsIgnoreCase(chan)) {
						l(p,"CHANNEL_EXISTS");
						return true;
					}
				}

				if ( (channels.size() - defaultChannels.size() ) >= maxChannels )
				{
					l(p,"TOO_MANY_CHANNELS");
					return true;
				}
				channels.put(chan,0);
				chanMsg(chan, p.getName() + " has created a new channel.");

				joinChannel(p, chan);
				return true;

			} else if (args[0].equalsIgnoreCase("leave")) { // Leave a channel
				String uuid = p.getUniqueId().toString();
				String chan = "";

				if (channelPlayers.containsKey(uuid))
					chan = channelPlayers.get(uuid);

				if (chan.trim().length() == 0) // Not actually IN a channel.
				{
					Util.msg(p, "You are not in a channel.");
					return true;
				}

				removeChannel(p);
				Util.msg(p, "You have left the channel.");
				return true;

			} else if (args[0].equalsIgnoreCase("join")) { // Join a channel

				if (!perm(p, "channel.join")) {
					l(p,"NO_PERMISSION");
					return true;
				}

				if (args.length == 1 || args[1].trim().length() == 0) {
					ShowJoinGUI(p, 1);
					return true;
				}

				String chan = "";

				for (String c : channels.keySet() ) {
					if (c.equalsIgnoreCase(args[1])) {
						chan = c;
						break;
					}
				}

				if (chan.trim().length() == 0) {
					Util.msg(p, "That channel does not exist.");
					return true;
				}

				joinChannel(p, chan);
				return true;
			}
			
			Util.msg(sender, "That does not appear to be a valid channel command.");
			
			return true;
		}

		return false;
	}
		
	private void showCreateGUI(Player p) {
		// TODO Auto-generated method stub
		 Inventory i = Bukkit.createInventory(p, InventoryType.ANVIL, "Create a Channel");
         p.openInventory(i);
         //openGUI(p);
	
	}

	private void ShowJoinGUI(Player p, int page ) {
		
		ArrayList<String> chanlist = new ArrayList<String>();
		ItemStack prev = getHead("MHF_ArrowLeft");
		ItemStack next = getHead("MHF_ArrowRight");
		ItemStack instruct = getHead("MHF_Question");
		
		int max = ((Math.round((channels.size() +2) + 8) / 9) * 9);

		ItemMeta meta = prev.getItemMeta();
		meta.setLore(Arrays.asList(Util.colorize("&cClick to go back one page.")));
		meta.setDisplayName("Previous Page");
		prev.setItemMeta(meta);
		
		meta = next.getItemMeta();
		meta.setLore(Arrays.asList(Util.colorize("&cClick to go forward one page.")));
		meta.setDisplayName("Next Page");
		next.setItemMeta(meta);
		
		meta = instruct.getItemMeta();
		meta.setDisplayName("Autcraft Helper: Channels");
		meta.setLore(Arrays.asList(Util.colorize("&cClick any channel to join!"),"A list of players can be seen by","hovering over the channel."));
		instruct.setItemMeta(meta);

		Inventory cw = Bukkit.createInventory(p, max,Util.colorize("&aAutcraft Channels"));
		if ( page == 1 )
			cw.setItem(0,instruct);
		else
			cw.setItem(0,prev);

		if ( max >= 54 ) // Multiple Pages
			cw.setItem(53,next);
		page -= 1;
		int num = 0 + ( page * 54 );
		
		for ( String chan : channels.keySet() ){
			num++;
			ItemStack channel = new ItemStack(Material.PAPER, 1);
			meta = channel.getItemMeta();
			meta.setDisplayName( Util.colorize("&a"+ chan));
			ArrayList<String> lore = new ArrayList<String>();
			lore.add("In Channel: " + channels.get(chan));
			ArrayList<String> people = new ArrayList<String>();
			
			if ( channels.get(chan) != 0 )
			{
				
				for ( Player rec : Bukkit.getServer().getOnlinePlayers() ) {
					if ( channelPlayers.containsKey( rec.getUniqueId().toString() ) )
					{
						if ( channelPlayers.get( rec.getUniqueId().toString() ).equalsIgnoreCase(chan) )
							people.add(rec.getName() );
					}
				}
				
				int shown = 0;
				String peeps = "";
				String c1 = "&e";
				String c2 = "&6";
				String c = c1;
				for ( String a : people )
				{
					peeps = peeps + " " + c + a;
					if ((shown & 1) == 0) {
						lore.add( Util.colorize(peeps.trim()));
						peeps = "";
						c = c2;
					} else {
						c = c1;
					}
					shown++;
				}
				lore.add( Util.colorize(peeps.trim()));
			
			}
			lore.add("");
			lore.add( Util.colorize("&bClick to Join!"));
			
			meta.setLore(lore);
			channel.setItemMeta(meta);
			cw.setItem(num, channel);
			
		}
		p.openInventory(cw);
	}

	private ItemStack getHead(String name) {
		ItemStack skull = new ItemStack(397, 1, (short) 3);
		SkullMeta meta = (SkullMeta) skull.getItemMeta();
		meta.setOwner(name);
		skull.setItemMeta(meta);
		return (skull);
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {

		if ( event.getInventory().getName() == null )
			return;
		
		if (!event.getInventory().getName().contains("Autcraft Channels") )
			return;

		if (event.getCurrentItem() == null
				|| event.getCurrentItem().getType() == Material.AIR)
			return;

		event.setCancelled(true);

		if ( event.getRawSlot() == 0 )
			return;
		
		try {
			ItemStack item = event.getCurrentItem();
			Player p = (Player) event.getWhoClicked();
			joinChannel(p,ChatColor.stripColor( item.getItemMeta().getDisplayName() ));
			p.closeInventory();
		}
		catch ( Exception e) 
		{
			
		}
   }
	

	private void doHelp(CommandSender sender, String help ) {

		if (F.getConfig(F.helpFile).contains(help)) {
			Util.msg(sender, Util.colorize(F.getConfig(F.helpFile).getString(help)));
		} else {
			l(sender,"NO_HELP");
		}		
	}

	private boolean canListen(Player p) {

		if ( !perm(p, "channel.admin") )
			return false;
		
		if ( F.getConfig(F.playerFile).contains("listening."+ p.getUniqueId().toString()) )
		{
			Util.debug(p.getName() + " has listening turned off");
			return F.getConfig(F.playerFile).getBoolean("listening."+ p.getUniqueId().toString());
		}
		
		return true;
	}

	private void doChat(Player p, String msg) {
	
		 String channel = "";
		 String uuid = p.getUniqueId().toString();
		 String group = chat.getPrimaryGroup(p);
		 String prefix = chat.getGroupPrefix(p.getWorld(), group);

		 Util.debug("Group: " + group);
		 Util.debug("Prefix: " + prefix);
		 
		 if ( !perm(p,"admin") ) // Admins can use colored chat - others cannot
		 {
			 msg = Util.checkChat(ChatColor.stripColor( Util.colorize(msg)), p, maxCapLetters);
			 msg = Util.crushPunct(msg);
			 msg = Normalizer.normalize(msg, Normalizer.Form.NFD);
			 msg = msg.replaceAll("[^\\x00-\\x7F]", "");
		 }
		 
		 if ( msg.trim().length() == 0 )
			 return;
			
			if ( isCalm(p) )
				return;
		
		if ( chatDisabled && !perm(p,"admin") )
		{
			l(p,"CHAT_IS_DISABLED");
			return;
		}
		
		if ( checkSwear(p,msg) )
		{
			return;
		}

	    

		 if (ess.getUser(p).isMuted()) {
			 Util.msg( p, "You are muted and may not chat.");
			 return;
		 }
		 	
		if ( channelPlayers.containsKey(uuid))
			channel = channelPlayers.get(uuid);
		
		String chat = chatFormat;
		if ( channel.trim().length() == 0 )
			chat = chat.replace("{channel}",  "");
		else
			chat = chat.replace("{channel}",  channelFormat.replaceAll("%1", channel) );
		chat = chat.replace("{player}", p.getName() );
		chat = chat.replace("{prefix}",  prefix );

		
		getServer().getLogger().info( ChatColor.stripColor( Util.colorize(chat.replace("{message}", msg))) );
		

		for ( Player rec : Bukkit.getServer().getOnlinePlayers() )
		{
			String newmsg = msg;
			
			String chan = "";
			if ( channelPlayers.containsKey( rec.getUniqueId().toString()) )
				chan = channelPlayers.get( rec.getUniqueId().toString() );

			Util.debug("Channel: ->" + channel + "<- Chan: ->"+chan+"<-" );
			
			if ( chan.equalsIgnoreCase(channel) ||
					canListen(rec) ) // Player in the same channel or Channel admin
			{
				if ( ess.getUser(rec).isIgnoredPlayer(p.getName() ))
					continue;

				if ( isCalm(rec) )
					continue;

				if ( StringUtils.containsIgnoreCase(msg, rec.getName() ) )
					newmsg = msg.replaceAll("(?i)"+rec.getName(), Util.colorize( chatHighlight.get(rec.getUniqueId().toString()) +rec.getName()+"&r") );
				
				Util.msg(rec, chat.replace("{message}", newmsg) );
				
				if ( splitChatPlayers.containsKey( rec.getUniqueId().toString()) )
						Util.msg(rec, splitChatPlayers.get( rec.getUniqueId().toString()));
			}
		}
		
		
	}

	private boolean isCalm(Player p) {
		
		  Location loc = p.getLocation();
		  ProtectedRegion rg = null;
		  RegionManager regionManager = wg.getRegionManager(loc.getWorld() );
		  ApplicableRegionSet set = regionManager.getApplicableRegions(loc);

		  for (ProtectedRegion region : set) {
			  State recChat = region.getFlag(DefaultFlag.RECEIVE_CHAT);
			  State sendChat = region.getFlag(DefaultFlag.RECEIVE_CHAT);
					  
			  if ( recChat == State.DENY || sendChat == State.DENY )
				  return true;
			}
		  
		return false;
	}

	private String getChannel(Player p ) 
	{
		String chan = "";
		String uuid = p.getUniqueId().toString();
		if ( channelPlayers.containsKey( uuid ) )
			chan = channelPlayers.get( uuid );

		return chan.trim();
	}
	
	private void removeChannel(Player p) {
		String uuid = p.getUniqueId().toString();
		
		String chan = getChannel(p);
		if ( chan.trim().length() == 0 ) // Not actually IN a channel.
		{
			Util.debug("Can't remove " + p.getName() + " from channel - not in one.");
			return; 
		}
		channelPlayers.remove( uuid );
		chanMsg(chan,  p.getName() + " has left the channel.");
		int num = channels.get(chan) - 1;
		
		if ( num == 0 && !defaultChannel(chan) )
			channels.remove(chan);
		else 
			channels.put(chan,num);
	}

	private boolean defaultChannel(String chan) {
		if ( defaultChannels.contains(chan) ) return true;
		return false;
	}

	private void joinChannel(Player p, String chan) {
		String uuid = p.getUniqueId().toString();
	
		String chan2 = getChannel(p);

		if ( chan2.trim().length() >= 1 ) // In a channel - remove them first
			removeChannel(p);
		
		channelPlayers.put( uuid, chan );
		channels.put( chan,  channels.get(chan)+1);
		
		chanMsg(chan,  p.getName() + " has joined the channel.");
	}
	
	private void chanMsg(final String channel, final String msg) {
		
		plugin.getServer().getScheduler()
		.scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				Util.debug("("+channel+") " + msg);

				for ( Player rec : Bukkit.getServer().getOnlinePlayers() )
				{
					String chan = "";
					if ( channelPlayers.containsKey( rec.getUniqueId().toString()) )
						chan = channelPlayers.get( rec.getUniqueId().toString() );
				
					if ( chan.equalsIgnoreCase(channel) || perm(rec,"channel.admin")) // Player in the same channel or Channel admin
					{

						chatFormat = getConfig().getString("chatFormat");
						Util.msg(rec, "&e"+channelFormat.replaceAll("%1", channel) + " " + msg);
					}
				}
			}
		}, 1L );

		
	}

	private void showHelp(CommandSender sender) {
		ArrayList<String> list = new ArrayList<String>();
		Util.msg(sender,"The Autcraft Helper Plugin");
		Util.msg(sender,"\nValid commands are: " );
		list.add("help");
		list.add("splitchat");
		list.add("channel");
		if ( perm(sender,"channel.admin") ) list.add("forceleave");
		if ( perm(sender,"channel.join") ) list.add("join");
		if ( perm(sender,"channel.join") ) list.add("leave");
		if ( perm(sender,"channel.join") ) list.add("list");
		if ( perm(sender,"channel.create") ) list.add("create");
		if ( perm(sender,"admin") ) list.add("reload");
		
		Util.msg(sender,Util.colorList("&e", "&6", list) );
		Util.msg(sender, "&FYou can use &6/ach help <command>&F for more information.");
	}

	public boolean perm(CommandSender s, String perm ) {
		if ( s.hasPermission("autcraft.admin") || s.hasPermission("autcraft."+perm) || s.isOp() )
			return true;
			
			return false;
	}
	

	private boolean setupChat() {
		RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager()
				.getRegistration(Chat.class);
		if (rsp != null) {
			this.chat = ((Chat) rsp.getProvider());
		}
		return this.chat != null;
	}
	
@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
public void onPlayerChat(AsyncPlayerChatEvent event) {
	
	 if ( event.isCancelled() ) {
		 Util.debug("Chat was cancelled by another plugin.");
		 return;
	 }
	 event.setCancelled(true);
	 
	 doChat(event.getPlayer(), event.getMessage() );
}

private boolean checkSwear(Player p, String msg) {
	String[] words = msg.split(" ");
	String found = "";
		for (String word : swearWords) {
			for (String check : words) {
				if (check.equalsIgnoreCase(word) )
				{
					found = word;
					break;
				}
			}
		}

	if ( found.length() > 1 )
	{
		DecimalFormat form = new DecimalFormat("0.00");
		Util.info(p.getName() + " used the swear word '"+found+"'.");
		Util.msg(p,  swearMessage.replaceAll("%1", form.format( swearPenalty)) );
		economy.withdrawPlayer(p.getName(), swearPenalty);
		return true;
	}
		
	return false;
}

private void registerEvents(Listener... o) {

	PluginManager manager = getServer().getPluginManager();

	for (Listener l : o) {
		manager.registerEvents(l, this);
		Util.debug("Registering: " + l.toString() );
	}
}

public static void l(CommandSender p, String key, Object ... obj)
{
	int num = 0;
	String string = getString(key);
	
	for (Object o : obj) {
		num++;
		string = string.replace("%"+num,  o.toString() );
	}
	Util.msg(p, Util.colorize(string) );
}
private static String getString(String key)
{
	if ( F.getConfig(F.stringsFile).contains(key) ) 
		return F.getConfig(F.stringsFile).getString(key);
	
	Util.error("The string '"+key+"' was not found in the strings file! Please add it!");
	return "String error: " + key;
	
}

private void loadStrings() {

setConfig(F.stringsFile,"REMOVE_RELOAD", "You have been removed from your channel due to a reload. You will need to rejoin the channel in a moment.");
setConfig(F.stringsFile,"NO_HELP", "That help file does not exist.");
setConfig(F.stringsFile,"HIGHLIGHT_CHANGED", "%1You have changed your chat name highlight color to this.\nWhenever someone says your name in chat, it will appear this color from now on.");
setConfig(F.stringsFile,"HIGHLIGHT_SYNTAX", "You must provide a color code to set your name highlight to.");
setConfig(F.stringsFile,"HIGHLIGHT_NOT_A_COLOR", "You must provide a 2 character color code to set your highlight" );
setConfig(F.stringsFile,"NO_PERMISSION", "You do not have permission to do that!");
setConfig(F.stringsFile,"LISTEN_OFF", "You will no longer listen to other channels - You will only see the channel you are in.");
setConfig(F.stringsFile,"LISTEN_ON", "You will see the chat in all channels.");
setConfig(F.stringsFile,"CHAT_DISABLED", "&FThe chat on Autcraft has been &cdisabled&F");
setConfig(F.stringsFile,"CHAT_ENABLED", "&FThe chat on Autcraft has been &aenabled&F");
setConfig(F.stringsFile,"CHAT_IS_DISABLED", "Chat is currently turned off on Autcraft.");
setConfig(F.stringsFile,"SPLITCHAT_DEFAULT", "The &6splitchat&F command lets you set a marker to appear in between chat lines to make it easier to split up and read.\nYou can choose from &6line&F, &eblank&F, &6star&F, or whatever character you want to see! Type &a/splitchat <type>&F to turn it on!");
setConfig(F.stringsFile,"SPLITCHAT_OFF", "You have removed your chat separator.");
setConfig(F.stringsFile,"SPLITCHAT_SET", "You have set a chat separator. Use &6/splitchat off&F to remove it!");
setConfig(F.stringsFile,"CHANNEL_EXISTS", "That channel already exists. Maybe you can join it!");
setConfig(F.stringsFile,"TOO_MANY_CHANNELS", "Too many channels exist right now. Try joining one instead!");
setConfig(F.stringsFile,"TOO_MANY_CAPS", "&a** &FYour message was automatically lowercased because it looks like you used too many capital letters.");
setConfig(F.stringsFile,"NO_CHAN_NAME","You must enter a name for your channel! Try /chan create MyChannel, or whatever name you want!");
}

}
