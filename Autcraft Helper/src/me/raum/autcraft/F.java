package me.raum.autcraft;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;

public class F implements Listener {

	public static AutcraftHelper plugin;
	public static HashMap<File, FileConfiguration> filemap = new HashMap<File, FileConfiguration>();

	public static File swearFile;
	public static FileConfiguration swearConfig = null;

	public static File playerFile;
	public static FileConfiguration playerConfig = null;
	
	public static File helpFile;
	public static FileConfiguration helpConfig = null;
	
	public static File channelFile;
	public static FileConfiguration channelConfig = null;
	
	public static File stringsFile;
	public static FileConfiguration stringsConfig = null;

	public static File configFile;
	public static FileConfiguration configConfig = null;

	public F(AutcraftHelper plug) {
		F.plugin = plug;
		
	}


	
	static File getDataFolder() {
		return AutcraftHelper.plugin.getDataFolder();
	}

	public static boolean fileIsSafe(File configFile) {

		if (!configFile.exists()) {
			Util.error("File doesn't exist!");
			return false;
		}
		
		
		// Check for tab characters in config-file
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(configFile));
			int row = 0;
			String line;
			while ((line = in.readLine()) != null) {
				row++;
				if (line.indexOf('\t') != -1) {
					StringBuilder buffy = new StringBuilder();
					buffy.append("Found tab in config-file on line ")
							.append(row).append(".");
					buffy.append('\n').append(
							"NEVER use tabs! ALWAYS use spaces!");
					buffy.append('\n').append(line);
					buffy.append('\n');
					for (int i = 0; i < line.indexOf('\t'); i++) {
						buffy.append(' ');
					}
					buffy.append('^');
					Util.error(buffy.toString());
					return false;
					// throw new IllegalArgumentException(buffy.toString());
				}
			}

			FileConfiguration config = new YamlConfiguration();
			// YamlConfiguration.loadConfiguration(infoFile);
			// Actually reload the config-file
			config.load(configFile);
		} catch (InvalidConfigurationException e) {
			throw new RuntimeException(
					"\n\n>>>\n>>> There is an error in your config-file! Handle it!\n>>> Here is what snakeyaml says:\n>>>\n\n"
							+ e.getMessage());
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(
					"Config-file could not be created for some reason! <o>");
		} catch (IOException e) {
			// Util.error reading the file, just re-throw
			Util.error("There was an Util.error reading the config-file:\n"
					+ e.getMessage());
		} finally {
			// Java 6 <3
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// Swallow
				}
			}
		}
		return true;
	}



	public static File loadConfig(FileConfiguration fileconfig,String name) {
		//info("1) Loading new file config = " + name);
		File file = new File(AutcraftHelper.plugin.getDataFolder(), name);
		
		//info("1a) Loaded: " + file.getPath());

		if (fileconfig == null) {
			if ( !file.exists() )
			{
				//Util.info("Config file " + name + " doesn't exist - Creating & Saving!");
				try {
	                file.createNewFile();
	        		file = new File(AutcraftHelper.plugin.getDataFolder(), name);
	            } catch (IOException e) {
	            	Util.error("Could not create new config file!");
	            }
			}
			
			fileconfig = YamlConfiguration.loadConfiguration(file);
		}

		if (!filemap.containsKey(file)) {
			filemap.put(file, fileconfig);
		}

		F.reloadConfig(file);
		return file;
	}

	public static FileConfiguration getFC(File file) {
		

		if (!filemap.containsKey(file)) // (filemap.get(file))== null )
		{
			Util.error("File not found in filemap -> " + file.getName() );
			
			return null;
		}

		
		return filemap.get(file);

	}

	public static void reloadConfig(File file) {

		if (file == null)
		{
			
		}

		if (!fileIsSafe(file)) {
			Util.error("Unsafe file! Unable to save " + file.getName());
			return;
		}


		FileConfiguration fileconfig = getFC(file);
		fileconfig = YamlConfiguration.loadConfiguration(file);
		filemap.put(file, fileconfig);
	}

	public static FileConfiguration getConfig(File file) {
		
		FileConfiguration config = getFC(file);

		if (config == null) {
			
			reloadConfig(file);
		}
		return getFC(file);// config;
	}

	public static void saveConfig(File file, FileConfiguration config) {
		if (file == null  )
		{
			Util.error("File is null! Not saving!");
			return;
		}
		if (config == null  )
		{
			Util.error("Config is null! Not saving!");
			return;
		}
		try {
			getConfig(file).save(file);

		} catch (IOException ex) {
			Util.error("Could not save config file to " + file.getName());
		}
	}

}
