package twcore.bots.multibot.twscript;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;
import java.util.Iterator;
import java.util.ArrayList;

import twcore.bots.TWScript;
import twcore.core.BotAction;
import twcore.core.game.Player;
import twcore.core.util.Tools;

/**
 * This class has nothing to do with compiling Java source; it merely compiles
 * commands from utilities implementing TWScript and returns a new message.
 * 
 * @author milosh
 */
public final class CodeCompiler {
        
    public static void handleTWScript(BotAction bot, String message, Player p, int accessLevel){
        message = replaceKeys(bot, p, message);
        if(message != null && message.startsWith("*") && !isAllowedPrivateCommand(message, accessLevel))
            message = null;
        if (message != null && message.indexOf('%') == -1)
            bot.sendUnfilteredPrivateMessage(p.getPlayerName(), message);
        else if(message != null && message.indexOf('%') != -1){
            int sound = Tools.Sound.isAllowedSound( message.substring(message.indexOf('%') + 1) );
            message = message.substring(0, message.indexOf('%'));
            bot.sendUnfilteredPrivateMessage(p.getPlayerName(), message, sound);
        }
    }
    
    public static void handleTWScript(BotAction bot, String message, int accessLevel){
        message = replaceKeys(bot, null, message);
        if(message != null && !isAllowedPublicCommand(message, accessLevel))
            message = null;
        if (message != null && message.indexOf('%') == -1)
        	sendMessage(bot,message, -1);
        else if(message != null && message.indexOf('%') != -1){
            int sound = Tools.Sound.isAllowedSound( message.substring(message.indexOf('%') + 1) );
            message = message.substring(0, message.indexOf('%'));
            sendMessage(bot,message, sound);
        }
    }
    
    public static void sendMessage(BotAction bot, String message, int sound){
    	if(message.startsWith("'")){
    		if(sound == -1)bot.sendTeamMessage(message.substring(1));
    		else bot.sendTeamMessage(message.substring(1), sound);
    	} else if(message.startsWith(":")){
    		message = message.substring(1);
    		if(message.indexOf(":") != -1){
    			if(sound == -1) bot.sendUnfilteredPrivateMessage(message.substring(0,message.indexOf(":")), message.substring(message.indexOf(":") + 1));
    			else bot.sendUnfilteredPrivateMessage(message.substring(0,message.indexOf(":")), message.substring(message.indexOf(":") + 1), sound);
    		}
    	} else {
    		if(sound == -1)bot.sendUnfilteredPublicMessage(message);
    		else bot.sendUnfilteredPublicMessage(message, sound);
    	}
    }
    
    /**
     * Replaces key phrases for modules using custom unfiltered
     * private messages.
     * @see twcore.bots.multibot.utils.custom and utilhotspots
     * @param bot - The BotAction object for the module using this method.
     * @param p - The Player object of the user receiving the message
     * @param message - The original message to be changed
     * @return - The changed message. Can return null.
     */
    public static String replaceKeys(BotAction bot, Player p, String message){
        Random rand = new Random();
        Date today = Calendar.getInstance().getTime();
        TimeZone tz = TimeZone.getDefault();
        if(p != null){
        	if(message.contains("@name"))
            	message = message.replace("@name", p.getPlayerName());
        	if(message.contains("@frequency"))
            	message = message.replace("@frequency", Integer.toString(p.getFrequency()));
        	while(message.contains("@randomfreq"))
            	message = message.replaceFirst("@randomfreq", Integer.toString(rand.nextInt( 9998 )));
        	if(message.contains("@shipname"))
            	message = message.replace("@shipname", Tools.shipName(p.getShipType()));
        	if(message.contains("@shipnum"))
            	message = message.replace("@shipnum", Integer.toString(p.getShipType()));
        	while(message.contains("@randomship"))
            	message = message.replaceFirst("@randomship", Integer.toString((rand.nextInt( 7 )) + 1));       
        	if(message.contains("@shipslang"))
            	message = message.replace("@shipslang", Tools.shipNameSlang(p.getShipType()));        
        	if(message.contains("@wins"))
            	message = message.replace("@wins", Integer.toString(p.getWins()));      
        	if(message.contains("@losses"))
            	message = message.replace("@losses", Integer.toString(p.getLosses()));      
        	if(message.contains("@bounty"))
            	message = message.replace("@bounty", Integer.toString(p.getBounty()));
        	while(message.contains("@randomsound"))
            	message = message.replaceFirst("@randomsound", Integer.toString(Tools.Sound.allowedSounds[rand.nextInt(Tools.Sound.allowedSounds.length)]));
        	if(message.contains("@id"))
            	message = message.replace("@id", Integer.toString(p.getPlayerID()));
        	if(message.contains("@ping"))
            	message = message.replace("@ping", Integer.toString(p.getPing() * 10));
        	if(message.contains("@flags"))
            	message = message.replace("@flags", Integer.toString(p.getFlagsCarried()));
        	if(message.contains("@teamflags"))
            	message = message.replace("@teamflags", Integer.toString(bot.getFlagsOnFreq(p.getFrequency())));
        	if(message.contains("@squad")){
            	if(p.getSquadName().equals(""))
                	message = message.replace("@squad", "null");
            	else
            		message = message.replace("@squad", p.getSquadName());
        	}
        	if(message.contains("@x")){
            	bot.spectatePlayer(p.getPlayerID());
            	message = message.replace("@x", Integer.toString(p.getXTileLocation()));
            	bot.stopSpectatingPlayer();
        	}
        	if(message.contains("@y")){
            	bot.spectatePlayer(p.getPlayerID());
            	message = message.replace("@y", Integer.toString(p.getYTileLocation()));
            	bot.stopSpectatingPlayer();
        	}
        }
        if(message.contains("@date"))
            message = message.replace("@date", SimpleDateFormat.getDateInstance( SimpleDateFormat.SHORT ).format(today));
        if(message.contains("@time"))
            message = message.replace("@time", SimpleDateFormat.getTimeInstance().format(today) + " (" + tz.getDisplayName(true, TimeZone.SHORT) + ")");
        while(message.contains("@randomtile"))
            message = message.replaceFirst("@randomtile", Integer.toString((rand.nextInt( 1021 )) + 1));
        if(message.contains("@botname"))
            message = message.replace("@botname", bot.getBotName());
        if(message.contains("@arenaname"))
        	message = message.replace("@arenaname", bot.getArenaName());
        if(message.contains("@arenasize"))
            message = message.replace("@arenasize", Integer.toString(bot.getArenaSize()));
        if(message.contains("@playingplayers"))
            message = message.replace("@playingplayers", Integer.toString(bot.getPlayingPlayers().size()));
        if(message.contains("@freqsize(") && message.indexOf(")", message.indexOf("@freqsize(")) != -1){
            int beginIndex = message.indexOf("@freqsize(");
            int endIndex = message.indexOf(")", beginIndex);
            try{
                int x = Integer.parseInt(message.substring(beginIndex + 10, endIndex));
                x = bot.getFrequencySize(x);
                message = message.replace(message.substring(beginIndex, endIndex + 1), Integer.toString(x));
            }catch(Exception e){
                message = message.replace(message.substring(beginIndex, endIndex + 1), "0");
            }
        }
        if(message.contains("@pfreqsize(") && message.indexOf(")", message.indexOf("@pfreqsize(")) != -1){
            int beginIndex = message.indexOf("@pfreqsize(");
            int endIndex = message.indexOf(")", beginIndex);
            try{
                int x = Integer.parseInt(message.substring(beginIndex + 11, endIndex));
                x = bot.getPlayingFrequencySize(x);
                message = message.replace(message.substring(beginIndex, endIndex + 1), Integer.toString(x));
            }catch(Exception e){
                message = message.replace(message.substring(beginIndex, endIndex + 1), "0");
            }
        }
        if(message.contains("@shipsonfreq(") && message.indexOf(")", message.indexOf("@shipsonfreq(")) != -1){
            int beginIndex = message.indexOf("@shipsonfreq(");
            int endIndex = message.indexOf(")", beginIndex);
            String temp = message.substring(beginIndex + 13, endIndex);
            try{
            	String[] nums = temp.split(",");
            	for(int i = 0; i < nums.length; i++)
            		nums[i] = nums[i].trim();
                int shipType = Integer.parseInt(nums[0]);
                int freq = Integer.parseInt(nums[1]);
                int t = 0;
                Iterator<Player> it = bot.getPlayingPlayerIterator();
                while( it.hasNext() ){
                	Player z = it.next();
                	if(z.getShipType() == shipType && z.getFrequency() == freq)
                		t++;
                }
                message = message.replace(message.substring(beginIndex, endIndex + 1), Integer.toString(t));
            }catch(Exception e){
            	message = message.replace(message.substring(beginIndex, endIndex + 1), "0");
            }
        }
        message = message.replace("\\[", "$OPEN_BRACKET$");
        message = message.replace("\\]", "$CLOSE_BRACKET$");
        message = doMathStatements(message);
        message = message.replace("$OPEN_BRACKET$", "[");
        message = message.replace("$CLOSE_BRACKET$", "]");
        if(message.trim().startsWith("{"))
                message = compile(message);
        if(message != null && message.contains("@!") && message.contains("@@")){
            while(true){
                int beginIndex = message.indexOf("@!");
                int endIndex = message.indexOf("@@");
                if(beginIndex != -1 && endIndex != -1 && endIndex > beginIndex){
                    bot.sendPrivateMessage(bot.getBotName(), message.substring(beginIndex + 1, endIndex));
                    message = message.replaceFirst("@!", " ");
                    message = message.replaceFirst("@@", " ");
                }
                else break;
            }
            message = null;
        }
        return message;
    }
        
    /**
     * This method cycles through each {condition}; If a condition is found to
     * be true its message is returned. Can return null.
     * 
     * @param message -
     *            The command to be compiled
     * @return - The message of the first true condition
     */
    public static String compile(String message) throws ArrayIndexOutOfBoundsException, NullPointerException{
        String[] ifStatements = message.split(";");
        for (int i = 0; i < ifStatements.length; i++) {
            if (doConditionalStatements(ifStatements[i]))
                return ifStatements[i].substring(ifStatements[i].indexOf("}") + 1);
        }
        return null;
    }
    
    /**
     * This method locates the main condition then solves it from inside out.
     * @param s - The condition
     * @return - True if the condition is found to be true. Else false.
     */
    private static boolean doConditionalStatements(String s) {
        s = s.substring(s.indexOf("{") + 1, s.indexOf("}")).trim();
        if (s.replace(" ", "").equals("") || s.replace(" ", "").equalsIgnoreCase("()"))
            return true;
        String temp = "(" + s + ")";
        String clone, chop;
        String[] chops;
        int index = 0, z=0;
        while (!(temp.trim().equalsIgnoreCase("TRUE") || temp.trim().equalsIgnoreCase("FALSE"))) {
            String lastSmallStatement = temp.substring(temp.lastIndexOf("("), temp.indexOf(")", temp.lastIndexOf("("))+1);
            clone = lastSmallStatement.substring(1, lastSmallStatement.length() - 1);
            if(clone.contains("&&") || clone.contains("||")){  	
            	z = min(clone.indexOf("&&", index), clone.indexOf("||", index), 0);
            	while(clone.contains("&&")){
            		if(clone.substring(z,z+2).equals("&&")){
            			chop = clone.substring(index, min(clone.indexOf("&&", z+2), clone.indexOf("||", z+2), clone.length()));
            			chops = chop.split("&&");
            			chops[0] = compileConditionalStatement(chops[0].trim());
            			chops[1] = compileConditionalStatement(chops[1].trim());
            			chop = compileConditionalStatement(chops[0] + "&&" + chops[1]);
            			clone = clone.substring(0, index) + chop + clone.substring(min(clone.indexOf("&&", z+2), clone.indexOf("||", z+2), clone.length()));
            		}else
            			index = z+2;
            		z = min(clone.indexOf("&&", index), clone.indexOf("||", index), 0);
            	}
            	index = 0;
            	z = clone.indexOf("||");
            	while(clone.contains("||")){
            		chop = clone.substring(index, min(-1,clone.indexOf("||", z+2),clone.length()));
            		chops = new String[2];
            		chops[0] = chop.substring(0, chop.indexOf("||"));
            		chops[1] = chop.substring(chop.indexOf("||") + 2);
        			chops[0] = compileConditionalStatement(chops[0].trim());
        			chops[1] = compileConditionalStatement(chops[1].trim());
        			chop = compileConditionalStatement(chops[0] + "||" + chops[1]);
        			clone = clone.substring(0, index) + chop + clone.substring(min(-1,clone.indexOf("||", z+2),clone.length()));
        			z = min(clone.indexOf("&&", index), clone.indexOf("||", index), 0);
            	}          	
            }
            temp = temp.replace(lastSmallStatement, compileConditionalStatement(clone.trim()));
        }
        if (temp.trim().equalsIgnoreCase("TRUE"))
            return true;
        else
            return false;
    }
    
    /**
     * A method similar to Math.min().
     * @return The smaller number. If both numbers are equal, return z.
     */
    private static int min(int a, int b, int z){
    	if(b < a && b != -1) return b;
    	if(a < b && a != -1) return a;
    	return z;
    }
    
    /**
     * A method similar to Math.min().
     * @return The smallest number. If two numbers are equal, return z.
     */
    private static int min(int a, int b, int c, int d, int e, int f, int z){
    	ArrayList<Integer> list = new ArrayList<Integer>();
    	int smallest = 0;
    	list.add(a);list.add(b);list.add(c);list.add(d);list.add(e);list.add(f);
    	Iterator<Integer> i = list.iterator();
    	while( i.hasNext() ){
    		int current = i.next();
    		if(current > 0){
    			if(smallest == 0)smallest = current;
    			else if(current < smallest)smallest = current;
    		}
        }
    	if(smallest == 0)return z;
    	else return smallest;
    }
    
    /**
     * This method is the comparator that determines whether or not a condition is true or false
     * @param s - The single parenthetical statement
     * @return - TRUE or FALSE
     */
    private static String compileConditionalStatement(String s) {
    	//try{
    	if(s.startsWith("(") && s.indexOf(")") == s.length() - 1)
    		s = s.substring(1, s.length() - 1);
        if (s.trim().equalsIgnoreCase("TRUE") || s.trim().equalsIgnoreCase("FALSE"))
            return s;
        else if (s.contains("&&")) {
            String[] temp = s.split("&&");
            if (temp[0].trim().equalsIgnoreCase("TRUE") && temp[1].trim().equalsIgnoreCase("TRUE"))
                return "TRUE";
        } else if (s.contains("||")) {
            String[] temp = new String[2];
            temp[0] = s.substring(0, s.indexOf("||"));
            temp[1] = s.substring(s.indexOf("||") + 2);
            if (temp[0].trim().equalsIgnoreCase("TRUE") || temp[1].trim().equalsIgnoreCase("TRUE"))
                return "TRUE";
        } else if (s.contains("==")) {
            String[] temp = s.split("==");
            if (temp[0].trim().equals(temp[1].trim()))
                return "TRUE";
        } else if (s.contains("!=")) {
            String[] temp = s.split("!=");
            if (!temp[0].trim().equals(temp[1].trim()))
                return "TRUE";
        } else if (s.contains("~~")) {
            String[] temp = s.split("~~");
            if (temp[0].trim().equalsIgnoreCase(temp[1].trim()) || temp[0].trim().toLowerCase().startsWith(temp[1].trim().toLowerCase()) || temp[1].trim().toLowerCase().startsWith(temp[0].trim().toLowerCase()))
                return "TRUE";
        } else if (s.contains("<")) {
            String[] temp = s.split("<");
            try {
                int a = Integer.parseInt(temp[0].trim());
                int b = Integer.parseInt(temp[1].trim());
                if (a < b)
                    return "TRUE";
            } catch (Exception e) {}
        } else if (s.contains(">")) {
            String[] temp = s.split(">");
            try {
                int a = Integer.parseInt(temp[0].trim());
                int b = Integer.parseInt(temp[1].trim());
                if (a > b)
                    return "TRUE";
            } catch (Exception e) {}
        } else if (s.contains("<=")) {
            String[] temp = s.split("<=");
            try {
                int a = Integer.parseInt(temp[0].trim());
                int b = Integer.parseInt(temp[1].trim());
                if (a < b || a == b)
                    return "TRUE";
            } catch (Exception e) {}
        } else if (s.contains(">=")) {
            String[] temp = s.split(">=");
            try {
                int a = Integer.parseInt(temp[0].trim());
                int b = Integer.parseInt(temp[1].trim());
                if (a > b || a == b)
                    return "TRUE";
            } catch (Exception e) {}
        }
        return "FALSE";
    	/*}catch(Exception e){
    		Tools.printStackTrace(e);
    		return "FALSE";
    	}*/
    }
    
    private static String doMathStatements(String s){
    	try{
    	
    	String clone, chop;
    	int z=0, index=0;
    	String[] chops;
    	while (s.contains("[") && s.contains("]")) {
            String lastSmallStatement = s.substring(s.lastIndexOf("["), s.indexOf("]", s.lastIndexOf("[")) + 1);            
            clone = lastSmallStatement.substring(1, lastSmallStatement.length() - 1);
            if(clone.contains("^") || clone.contains("*") || clone.contains("/") || clone.contains("%") || clone.contains("+") || clone.contains("-")){  	
            	z = min(clone.indexOf("^", index), clone.indexOf("*", index), clone.indexOf("/", index), clone.indexOf("%", index), clone.indexOf("+", index), clone.indexOf("-", index), 0);
            	while(clone.contains("^")){
            		if(clone.substring(z,z+1).equals("^")){
            			chop = clone.substring(index, min(clone.indexOf("^", z+1), clone.indexOf("*", z+1), clone.indexOf("/", z+1), clone.indexOf("%", z+1), clone.indexOf("+", z+1), clone.indexOf("-", z+1), clone.length()));
            			chops = new String[2];
                		chops[0] = chop.substring(0, chop.indexOf("^"));
                		chops[1] = chop.substring(chop.indexOf("^") + 1);
            			chops[0] = compileMathStatement(chops[0].trim());
            			chops[1] = compileMathStatement(chops[1].trim());
            			chop = compileMathStatement(chops[0] + "^" + chops[1]);
            			clone = clone.substring(0, index) + chop + clone.substring(min(clone.indexOf("^", z+1), clone.indexOf("*", z+1), clone.indexOf("/", z+1), clone.indexOf("%", z+1), clone.indexOf("+", z+1), clone.indexOf("-", z+1), clone.length()));
            		}else
            			index = z+1;
            		z = min(clone.indexOf("^", index), clone.indexOf("*", index), clone.indexOf("/", index), clone.indexOf("%", index), clone.indexOf("+", index), clone.indexOf("-", index), 0);
            	}
            	index = 0;
            	z = min(clone.indexOf("^", index), clone.indexOf("*", index), clone.indexOf("/", index), clone.indexOf("%", index), clone.indexOf("+", index), clone.indexOf("-", index), 0);
            	while(clone.contains("*")){
            		if(clone.substring(z,z+1).equals("*")){
            			chop = clone.substring(index, min(clone.indexOf("^", z+1), clone.indexOf("*", z+1), clone.indexOf("/", z+1), clone.indexOf("%", z+1), clone.indexOf("+", z+1), clone.indexOf("-", z+1), clone.length()));
            			chops = new String[2];
                		chops[0] = chop.substring(0, chop.indexOf("*"));
                		chops[1] = chop.substring(chop.indexOf("*") + 1);
            			chops[0] = compileMathStatement(chops[0].trim());
            			chops[1] = compileMathStatement(chops[1].trim());
            			chop = compileMathStatement(chops[0] + "*" + chops[1]);
            			clone = clone.substring(0, index) + chop + clone.substring(min(clone.indexOf("^", z+1), clone.indexOf("*", z+1), clone.indexOf("/", z+1), clone.indexOf("%", z+1), clone.indexOf("+", z+1), clone.indexOf("-", z+1), clone.length()));
            		}else
            			index = z+1;
            		z = min(clone.indexOf("^", index), clone.indexOf("*", index), clone.indexOf("/", index), clone.indexOf("%", index), clone.indexOf("+", index), clone.indexOf("-", index), 0);
            	}
            	index = 0;
            	z = min(clone.indexOf("^", index), clone.indexOf("*", index), clone.indexOf("/", index), clone.indexOf("%", index), clone.indexOf("+", index), clone.indexOf("-", index), 0);
            	while(clone.contains("/")){
            		if(clone.substring(z,z+1).equals("/")){
            			chop = clone.substring(index, min(clone.indexOf("^", z+1), clone.indexOf("*", z+1), clone.indexOf("/", z+1), clone.indexOf("%", z+1), clone.indexOf("+", z+1), clone.indexOf("-", z+1), clone.length()));
            			chops = new String[2];
                		chops[0] = chop.substring(0, chop.indexOf("/"));
                		chops[1] = chop.substring(chop.indexOf("/") + 1);
            			chops[0] = compileMathStatement(chops[0].trim());
            			chops[1] = compileMathStatement(chops[1].trim());
            			chop = compileMathStatement(chops[0] + "/" + chops[1]);
            			clone = clone.substring(0, index) + chop + clone.substring(min(clone.indexOf("^", z+1), clone.indexOf("*", z+1), clone.indexOf("/", z+1), clone.indexOf("%", z+1), clone.indexOf("+", z+1), clone.indexOf("-", z+1), clone.length()));
            		}else
            			index = z+1;
            		z = min(clone.indexOf("^", index), clone.indexOf("*", index), clone.indexOf("/", index), clone.indexOf("%", index), clone.indexOf("+", index), clone.indexOf("-", index), 0);
            	}
            	index = 0;
            	z = min(clone.indexOf("^", index), clone.indexOf("*", index), clone.indexOf("/", index), clone.indexOf("%", index), clone.indexOf("+", index), clone.indexOf("-", index), 0);
            	while(clone.contains("%")){
            		if(clone.substring(z,z+1).equals("%")){
            			chop = clone.substring(index, min(clone.indexOf("^", z+1), clone.indexOf("*", z+1), clone.indexOf("/", z+1), clone.indexOf("%", z+1), clone.indexOf("+", z+1), clone.indexOf("-", z+1), clone.length()));
            			chops = new String[2];
                		chops[0] = chop.substring(0, chop.indexOf("%"));
                		chops[1] = chop.substring(chop.indexOf("%") + 1);
            			chops[0] = compileMathStatement(chops[0].trim());
            			chops[1] = compileMathStatement(chops[1].trim());
            			chop = compileMathStatement(chops[0] + "%" + chops[1]);
            			clone = clone.substring(0, index) + chop + clone.substring(min(clone.indexOf("^", z+1), clone.indexOf("*", z+1), clone.indexOf("/", z+1), clone.indexOf("%", z+1), clone.indexOf("+", z+1), clone.indexOf("-", z+1), clone.length()));
            		}else
            			index = z+1;
            		z = min(clone.indexOf("^", index), clone.indexOf("*", index), clone.indexOf("/", index), clone.indexOf("%", index), clone.indexOf("+", index), clone.indexOf("-", index), 0);
            	}
            	index = 0;
            	z = min(clone.indexOf("^", index), clone.indexOf("*", index), clone.indexOf("/", index), clone.indexOf("%", index), clone.indexOf("+", index), clone.indexOf("-", index), 0);
            	while(clone.contains("+")){
            		if(clone.substring(z,z+1).equals("+")){
            			chop = clone.substring(index, min(clone.indexOf("^", z+1), clone.indexOf("*", z+1), clone.indexOf("/", z+1), clone.indexOf("%", z+1), clone.indexOf("+", z+1), clone.indexOf("-", z+1), clone.length()));
            			chops = new String[2];
                		chops[0] = chop.substring(0, chop.indexOf("+"));
                		chops[1] = chop.substring(chop.indexOf("+") + 1);
            			chops[0] = compileMathStatement(chops[0].trim());
            			chops[1] = compileMathStatement(chops[1].trim());
            			chop = compileMathStatement(chops[0] + "+" + chops[1]);
            			clone = clone.substring(0, index) + chop + clone.substring(min(clone.indexOf("^", z+1), clone.indexOf("*", z+1), clone.indexOf("/", z+1), clone.indexOf("%", z+1), clone.indexOf("+", z+1), clone.indexOf("-", z+1), clone.length()));
            		}else
            			index = z+1;
            		z = min(clone.indexOf("^", index), clone.indexOf("*", index), clone.indexOf("/", index), clone.indexOf("%", index), clone.indexOf("+", index), clone.indexOf("-", index), 0);
            	}
            	index = 0;
            	z = clone.indexOf("-");
            	while(clone.contains("-")){
            		chop = clone.substring(index, min(-1,clone.indexOf("-", z+1),clone.length()));
            		chops = new String[2];
            		chops[0] = chop.substring(0, chop.indexOf("-"));
            		chops[1] = chop.substring(chop.indexOf("-") + 1);
        			chops[0] = compileMathStatement(chops[0].trim());
        			chops[1] = compileMathStatement(chops[1].trim());
        			chop = compileMathStatement(chops[0] + "-" + chops[1]);
        			clone = clone.substring(0, index) + chop + clone.substring(min(-1,clone.indexOf("-", z+1),clone.length()));
        			z = min(-1, clone.indexOf("-", index), 0);
            	}          	
            }            
            s = s.replace(lastSmallStatement, compileMathStatement(clone.trim()));
        }
    	return s;
    	
    	}catch(Exception e){
    		Tools.printStackTrace(e);
    		return "-1";
    	}
    }
    
    private static String compileMathStatement(String s){
    	try{
    		if(s.startsWith("[") && s.indexOf("]") == s.length() - 1)
        		s = s.substring(1, s.length() - 1);
        if(s.contains("+")){
            String a = s.substring(0, s.indexOf("+"));
            String b = s.substring(s.indexOf("+") + 1);
            try{
                int x = Integer.parseInt(a.trim());
                int y = Integer.parseInt(b.trim());
                return Integer.toString(x + y);
            }catch(NumberFormatException e){
                return a + b;
            }
        } else if(s.contains("-")){
            String a = s.substring(0, s.indexOf("-"));
            String b = s.substring(s.indexOf("-") + 1);
            try{
                int x = Integer.parseInt(a.trim());
                int y = Integer.parseInt(b.trim());
                return Integer.toString(x - y);
            }catch(NumberFormatException e){
                return "-1";
            }
        } else if(s.contains("*")){
            String a = s.substring(0, s.indexOf("*"));
            String b = s.substring(s.indexOf("*") + 1);
            try{
                int x = Integer.parseInt(a.trim());
                int y = Integer.parseInt(b.trim());
                return Integer.toString(x * y);
            }catch(NumberFormatException e){
                return "-1";
            }
        } else if(s.contains("/")){
            String a = s.substring(0, s.indexOf("/"));
            String b = s.substring(s.indexOf("/") + 1);
            try{
                int x = Integer.parseInt(a.trim());
                int y = Integer.parseInt(b.trim());
                return Long.toString(Math.round((double) x / (double) y));
            }catch(NumberFormatException e){
                return "-1";
            }
        } else if(s.contains("^")){
            String a = s.substring(0, s.indexOf("^"));
            String b = s.substring(s.indexOf("^") + 1);
            try{
                int x = Integer.parseInt(a.trim());
                int y = Integer.parseInt(b.trim());
                double z = (Math.pow((double) x, (double) y));
                return Long.toString(Math.round(z));
            }catch(NumberFormatException e){
                return "-1";
            }
        } else if(s.contains("%")){
            String a = s.substring(0, s.indexOf("%"));
            String b = s.substring(s.indexOf("%") + 1);
            try{
                int x = Integer.parseInt(a.trim());
                int y = Integer.parseInt(b.trim());
                return Integer.toString(x % y);
            }catch(NumberFormatException e){
                return "-1";
            }
        } else {
            try{
                int x = Integer.parseInt(s);
                return Integer.toString(x);
            }catch(NumberFormatException e){
                return "-1";
            }
        }
    	}catch(Exception e){
    		Tools.printStackTrace(e);
    		return "-1";
    	}
    }
    
    /**
     * A white-list of allowed private commands.
     * @param s - The string
     * @return true if the string is allowed. else false.
     */
    public static boolean isAllowedPrivateCommand(String s, int accessLevel){
        if(s.startsWith("*setship ")   ||
           s.startsWith("*setfreq ")   ||
           s.startsWith("*warpto ")    ||
           s.startsWith("*objon ")     ||
           s.startsWith("*objoff ")    ||
           s.equals("*scorereset")    ||
           s.equals("*shipreset")     ||
           s.equals("*spec")          ||
           s.equals("*prize #4")      ||//Stealth
           s.equals("*prize #5")      ||//Cloak
           s.equals("*prize #6")      ||//X-radar
           s.equals("*prize #7")      ||//Warp
           s.equals("*prize #13")     ||//Full charge
           s.equals("*prize #14")     ||//Engine shutdown
           s.equals("*prize #15")     ||//Multi-fire
           s.equals("*prize #17")     ||//Super
           s.equals("*prize #18")     ||//Shields
           s.equals("*prize #19")     ||//Shrapnel
           s.equals("*prize #20")     ||//Anti-warp
           s.equals("*prize #21")     ||//Repel
           s.equals("*prize #22")     ||//Burst
           s.equals("*prize #23")     ||//Decoy
           s.equals("*prize #24")     ||//Thor
           s.equals("*prize #25")     ||//Multi-prize
           s.equals("*prize #26")     ||//Brick
           s.equals("*prize #27")     ||//Rocket
           s.equals("*prize #28")     ||//Portal
           s.equals("*prize #-4")     ||//Negative Stealth
           s.equals("*prize #-5")     ||//Negative Cloak
           s.equals("*prize #-6")     ||//Negative X-radar
           s.equals("*prize #-7")     ||//Negative Warp
           s.equals("*prize #-13")    ||//Negative Full charge
           s.equals("*prize #-14")    ||//Negative Engine shutdown
           s.equals("*prize #-15")    ||//Negative Multi-fire
           s.equals("*prize #-17")    ||//Negative Super
           s.equals("*prize #-18")    ||//Negative Shields
           s.equals("*prize #-19")    ||//Negative Shrapnel
           s.equals("*prize #-20")    ||//Negative Anti-warp
           s.equals("*prize #-21")    ||//Negative Repel
           s.equals("*prize #-22")    ||//Negative Burst
           s.equals("*prize #-23")    ||//Negative Decoy
           s.equals("*prize #-24")    ||//Negative Thor
           s.equals("*prize #-25")    ||//Negative Multi-prize
           s.equals("*prize #-26")    ||//Negative Brick
           s.equals("*prize #-27")    ||//Negative Rocket
           s.equals("*prize #-28"))     //Negative Portal
            return true;
        else if (accessLevel == TWScript.SYSOP_LEVEL)return true;
        else return false;
    }
    
    /**
     * A white-list of allowed public commands.
     * @param s - The string
     * @return true if the string is allowed. else false.
     */
    public static boolean isAllowedPublicCommand(String s, int accessLevel){
    	if(s.startsWith("*sendto"))return false;//Would crash the server.
    	else if(s.startsWith("*arena ") ||
    			s.startsWith("*timer ") ||
    			s.startsWith("*objon ") ||
    			s.startsWith("*objoff ")||
    			s.equals("*shipreset")  ||
    			s.equals("*scorereset") ||
    			s.equals("*flagreset")  ||
    			s.equals("*timereset")  ||
    			s.equals("*lock")       ||
    			s.equals("*lockspec")   ||
    			s.equals("*lockteam")   ||
    			s.equals("*lockprivate")||
    			s.equals("*lockpublic") ||
    			s.equals("*lockchat")   ||
    			s.equals("*lockall")    ||
    			s.equals("*specall"))
           return true;
        else if ((accessLevel == TWScript.SMOD_LEVEL) && s.startsWith("*zone "))
        	return true;
        else if (accessLevel == TWScript.SYSOP_LEVEL)return true;
        else return false;
    }
    
}