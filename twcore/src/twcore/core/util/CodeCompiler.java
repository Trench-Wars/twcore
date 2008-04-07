package twcore.core.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;

import twcore.core.BotAction;
import twcore.core.game.Player;

/**
 * This class has nothing to do with compiling Java source; it merely compiles
 * commands from utilities implementing the replaceKeys(String) method and
 * returns a new message based upon the given conditions.
 * 
 * @author milosh
 */
public final class CodeCompiler {
    private CodeCompiler() {}
    
    
    /**
     * Replaces key phrases for modules using custom unfiltered
     * private messages.
     * @see twcore.bots.multibot.utils.utilcustom and utilhotspots
     * @param bot - The BotAction object for the module using this method.
     * @param p - The Player object of the user receiving the message
     * @param message - The original message to be changed
     * @return - The changed message. Can return null.
     */
    public static String replaceKeys(BotAction bot, Player p, String message){
        Random rand = new Random();
        Date today = Calendar.getInstance().getTime();
        TimeZone tz = TimeZone.getDefault();
        if(message.contains("&name"))
            message = message.replace("&name", p.getPlayerName());
        if(message.contains("&frequency"))
            message = message.replace("&frequency", Integer.toString(p.getFrequency()));
        if(message.contains("&date"))
            message = message.replace("&date", SimpleDateFormat.getDateInstance( SimpleDateFormat.SHORT ).format(today));
        if(message.contains("&time"))
            message = message.replace("&time", SimpleDateFormat.getTimeInstance().format(today) + " (" + tz.getDisplayName(true, TimeZone.SHORT) + ")");
        while(message.contains("&randomfreq"))
            message = message.replaceFirst("&randomfreq", Integer.toString(rand.nextInt( 9998 )));
        if(message.contains("&shipname"))
            message = message.replace("&shipname", Tools.shipName(p.getShipType()));
        if(message.contains("&shipnum"))
            message = message.replace("&shipnum", Integer.toString(p.getShipType()));
        while(message.contains("&randomship"))
            message = message.replaceFirst("&randomship", Integer.toString((rand.nextInt( 7 )) + 1));       
        if(message.contains("&shipslang"))
            message = message.replace("&shipslang", Tools.shipNameSlang(p.getShipType()));        
        if(message.contains("&wins"))
            message = message.replace("&wins", Integer.toString(p.getWins()));      
        if(message.contains("&losses"))
            message = message.replace("&losses", Integer.toString(p.getLosses()));      
        if(message.contains("&bounty"))
            message = message.replace("&bounty", Integer.toString(p.getBounty()));
        if(message.contains("&id"))
            message = message.replace("&id", Integer.toString(p.getPlayerID()));
        if(message.contains("&ping"))
            message = message.replace("&ping", Integer.toString(p.getPing() * 10));
        if(message.contains("&squad"))
            message = message.replace("&squad", p.getSquadName());      
        if(message.contains("&x")){
            bot.spectatePlayer(p.getPlayerID());
            message = message.replace("&x", Integer.toString(p.getXTileLocation()));
            bot.stopSpectatingPlayer();
        }
        if(message.contains("&y")){
            bot.spectatePlayer(p.getPlayerID());
            message = message.replace("&y", Integer.toString(p.getYTileLocation()));
            bot.stopSpectatingPlayer();
        }
        while(message.contains("&randomtile"))
            message = message.replaceFirst("&randomtile", Integer.toString((rand.nextInt( 1021 )) + 1));
        if(message.contains("&botname"))
            message = message.replace("&botname", bot.getBotName());
        if(message.contains("&arenasize"))
            message = message.replace("&arenasize", Integer.toString(bot.getArenaSize()));
        if(message.contains("&playingplayers"))
            message = message.replace("&playingplayers", Integer.toString(bot.getPlayingPlayers().size()));      
        if(message.contains("&freqsize(") && message.indexOf(")", message.indexOf("&freqsize(")) != -1){
            int beginIndex = message.indexOf("&freqsize(");
            int endIndex = message.indexOf(")", beginIndex);
            try{
                int x = Integer.parseInt(message.substring(beginIndex + 10, endIndex));
                x = bot.getFrequencySize(x);
                message = message.replace(message.substring(beginIndex, endIndex + 1), Integer.toString(x));
            }catch(Exception e){
                message = message.replace(message.substring(beginIndex, endIndex + 1), "0");
            }
        }
        if(message.contains("&pfreqsize(") && message.indexOf(")", message.indexOf("&pfreqsize(")) != -1){
            int beginIndex = message.indexOf("&pfreqsize(");
            int endIndex = message.indexOf(")", beginIndex);
            try{
                int x = Integer.parseInt(message.substring(beginIndex + 11, endIndex));
                x = bot.getPlayingFrequencySize(x);
                message = message.replace(message.substring(beginIndex, endIndex + 1), Integer.toString(x));
            }catch(Exception e){
                message = message.replace(message.substring(beginIndex, endIndex + 1), "0");
            }
        }
        message = doMathStatements(message);  
        if(message.trim().startsWith("{")){
            try{
                message = CodeCompiler.compile(message);
            }catch(Exception e){
                Tools.printStackTrace(e);
                return "Syntax error. Please notify host.";
            }
        }
        if(message.contains("&!") && message.contains("&&")){
            while(true){
                int beginIndex = message.indexOf("&!");
                int endIndex = message.indexOf("&&");
                if(beginIndex != -1 && endIndex != -1 && endIndex > beginIndex){
                    bot.sendPrivateMessage(bot.getBotName(), message.substring(beginIndex + 1, endIndex));
                    message = message.replaceFirst("&!", " ");
                    message = message.replaceFirst("&&", " ");
                }
                else break;
            }
            message = null;
        }
        return message;
    }
        
    /**
     * This method cycles through each {condition}; If a condition is found to
     * be true its message is returned.
     * 
     * @param message -
     *            The command to be compiled
     * @return - The message of the first true condition
     */
    public static String compile(String message) throws ArrayIndexOutOfBoundsException, NullPointerException{
        String[] ifStatements = message.split(";");
        for (int i = 0; i < ifStatements.length; i++) {
            if (doIfStatements(ifStatements[i]))
                return ifStatements[i].substring(ifStatements[i].indexOf("}") + 1);
        }
        return null;
    }
    
    /**
     * This method locates the main condition then solves it from inside out.
     * @param s - The condition
     * @return - True if the condition is found to be true. Else false.
     */
    private static boolean doIfStatements(String s) {
        s = s.substring(1, s.indexOf("}")).trim();
        if (s.replace(" ", "").equals("") || s.replace(" ", "").equalsIgnoreCase("ELSE"))
            return true;
        String temp = s.substring(s.indexOf("("), s.lastIndexOf(")") + 1);
        int i = 0;
        while (!(temp.trim().equalsIgnoreCase("TRUE") || temp.trim().equalsIgnoreCase("FALSE"))) {
            String lastSmallStatement = temp.substring(temp.lastIndexOf("("), temp.indexOf(")", temp.lastIndexOf("(")) + 1);
            temp = temp.replace(lastSmallStatement, compileStatement(lastSmallStatement.trim()));
            i++;
            if (i == 30) {
                temp = "Syntax error: Danger of stack overflow. Please notify host.";
                break;
            }
        }
        if (temp.trim().equalsIgnoreCase("TRUE"))
            return true;
        else
            return false;
    }
    
    /**
     * This method is the comparator that determines whether or not a condition is true or false
     * @param s - The single parenthetical statement
     * @return - TRUE or FALSE
     */
    private static String compileStatement(String s) {
        s = s.substring(1, s.indexOf(")")).trim();
        if (s.trim().equalsIgnoreCase("TRUE") || s.trim().equalsIgnoreCase("FALSE"))
            return s;
        else if (s.contains("||")) {
            String a = s.substring(0, s.indexOf("||"));
            String b = s.substring(s.indexOf("||") + 2);
            if (a.trim().equalsIgnoreCase("TRUE") || b.trim().equalsIgnoreCase("TRUE"))
                return "TRUE";
        } else if (s.contains("&&")) {
            String[] temp = s.split("&&");
            if (temp[0].trim().equalsIgnoreCase("TRUE") && temp[1].trim().equalsIgnoreCase("TRUE"))
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
        }
        return "FALSE";
    }
    
    /**
     * This method solves all math statements from inside out.
     * @param s - The message
     * @return - The message with math statements replaced by results.
     */
    private static String doMathStatements(String temp) {
        while (temp.contains("[") && temp.contains("]")) {
            String lastSmallStatement = temp.substring(temp.lastIndexOf("["), temp.indexOf("]", temp.lastIndexOf("[")) + 1);
            temp = temp.replace(lastSmallStatement, compileMathStatement(lastSmallStatement.trim()));
        }
        return temp;
    }
    
    private static String compileMathStatement(String s){
        s = s.substring(1, s.indexOf("]")).trim();
        if(false)return "-1";
        else if(s.contains("+")){
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
    }
    
    /**
     * Gets a help message of all replacement keys
     * @see twcore.core.util.CodeCompiler.replaceKeys()
     * @return - A help message displaying key types.
     */
    public static String[] getKeysMessage(){
        String msg[] = {
            "+=================== Escape Keys ===================+",
            "| &name           - The player's name.              |",
            "| &wins           - The player's wins.              |",
            "| &losses         - The player's losses.            |",
            "| &frequency      - The player's frequency.         |",
            "| &id             - The player's id(not userid)     |",
            "| &botname        - The bot's name.                 |",
            "| &shipnum        - The player's ship number.       |",
            "| &shipname       - The player's ship.              |",
            "| &shipslang      - Player's ship in vernacular.    |",
            "| &arenasize      - Number of players in arena.     |",
            "| &playingplayers - Number of players in a ship.    |",
            "| &freqsize(#)    - Number of players on freq #.    |",
            "| &pfreqsize(#)   - Num. of players in ship. Freq # |",
            "| &squad          - The player's squad.             |",
            "| &bounty         - The player's bounty.            |",
            "| &x              - X Location(Tiles)               |",
            "| &y              - Y Location(Tiles)               |",
            "| &randomfreq     - A random number(0 - 9998)       |",        
            "| &randomship     - A random number(1-8)            |",            
            "| &randomtile     - A random number(1-1022)         |",           
            "| &ping           - The player's ping in ms.        |",
            "| &date           - The current date.               |",
            "| &time           - The current time.               |",
            "| &!command&&     - Issues a command to the bot, but|",
            "|                    the player receives no message.|",
            "+===================================================+",
        };
        return msg;
    }
    
    /**
     * A white-list of allowed custom commands.
     * @param s - The string
     * @return true if the string is allowed. else false.
     */
    public static boolean isAllowed(String s){
        if(s.startsWith("*setship")   ||
           s.startsWith("*setfreq")   ||
           s.startsWith("*warpto")    ||
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
        else return false;
    }
    
}