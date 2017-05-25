package me.raum.autcraft;

import java.util.ArrayList;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class Util {
	
	public static Player findPlayer(String name) {
		
		for ( Player p : Bukkit.getServer().getOnlinePlayers() )
		{
			if (p.getName().toLowerCase().startsWith(name.toLowerCase())
					|| p.getName().equalsIgnoreCase(name)
					|| ChatColor.stripColor(p.getDisplayName().replaceAll("-", "").toLowerCase()).startsWith(name)
					|| ChatColor.stripColor(p.getDisplayName().replaceAll("-", "").toLowerCase()).equalsIgnoreCase(name))
				return p;
		}

		return null;
	}

	public static void msg(CommandSender s, Object msg) {
		s.sendMessage( colorize(msg.toString()) );
	}

	public static String colorize(String msg) {
		
		return msg.toString().replaceAll("&([0-9mMlLoOMmnNrRkKiIA-Fa-f])","\u00A7$1");
	}

	public static void error(Object msg) {
		String txt = "[ACH Error] " + msg.toString();
		Bukkit.getServer().getLogger()
				.info(txt.replaceAll("&([0-9A-Fa-f])", "\u00A7$1"));

	}

	public static void info(Object msg) {
		String txt = "[ACH Info] " + msg.toString();
		Bukkit.getServer().getLogger()
		.info(txt.replaceAll("&([0-9A-Fa-f])", "\u00A7$1"));

	}

	public static void msgall(Object msg) {
		
		for (Player otherPlayer : Bukkit.getServer().getOnlinePlayers()) 
			otherPlayer.sendMessage( colorize(msg.toString())  );
		
		Bukkit.getServer().getLogger().info(msg.toString().replaceAll("&([0-9A-Fa-f])", "\u00A7$1"));
		
	}

	public static void debug(Object msg) {
		if (AutcraftHelper.debug) {
			String txt = "[ACH Debug] " + msg.toString();
			Bukkit.getServer().getLogger().info(txt);//.replaceAll("&([0-9A-Fa-f])", "\u00A7$1"));
		}
	}


	public static String stringFromList(ArrayList<String> a, int i) {
		String msg = null;
		
		if ( a.size() < i )
			return msg;
		
		Iterator<String> ai = a.iterator();
		msg = "";
		while ( ai.hasNext() )
		{
			String b = ai.next();
			msg = msg + " " + b;
		}
		
		return msg.trim();
	}
	
	public static String colorList(String c1, String c2, ArrayList<String> list) {
		
		String color;
		int shown = 1;
		String newstr = "";
		for ( String a : list )
		{
			if ((shown & 1) == 0) {
				color = c1;
			} else {
				color = c2;
			}
			
			newstr = newstr + color + " " + a;
			shown++;
		}
		
		return newstr;
	}
	
	public static String stringFromArray(String[] a, int i) {
		String msg = null;
		
		if ( a.length < i )
			return msg;
		
		msg = "";
		
		int num = i;
		while (num < a.length) {
			msg = msg + " " + a[num];
			num++;
		}

		
		return msg.trim();
	}
	
	public static String checkChat(String input, Player p, double max)
	  {
	    double upper = 0.0D;
	    String string = input;//input.replaceAll("[^a-zA-Z]", "");
	    double length = string.length();
	    if ( length <= 4 )
	    	return input;
	    
	    max = length * (max/100);
	    String output = input;
	    
	    for (int i = 0; i < length; i++)
	    {
	      char letter = string.charAt(i);
	      if (Character.isUpperCase(letter)) {
	        upper += 1;
	      }
	    }
	    
	    if (upper > max) {
	    	Util.debug("lcasing spam message: " + upper + " caps of " + max + " allowed");
	      output = input.toLowerCase();
	      AutcraftHelper.l(p,"TOO_MANY_CAPS");
	      
	    } else {
	      output = input;
	    }
	    
	    return output;
	  }

	static String crushPunct(String output) {

		output = output.replaceAll("!!+","!");
		output = output.replaceAll("\\?\\?+","?");
		//output = output.replaceAll("!+\\?+","!?");
		//output = output.replaceAll("\\?+!+","?!");
// TODO Auto-generated method stub
		return output;
	}
}
