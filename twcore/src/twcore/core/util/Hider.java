package twcore.core.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import twcore.core.BotAction;
import twcore.core.events.FileArrived;

/**
 * Hider hides.
 *
 * @author WingZero
 */
public class Hider {

    private static List<String> hiders = Collections.synchronizedList(new ArrayList<String>());
    private static final String FILE = "hiders.txt";
    
    private BotAction ba;
    
    public Hider(BotAction botAction) {
        ba = botAction;
        load();
    }
    
    /**
     * Handle FileArrived event to check if hiders.txt was received.
     * 
     * @param event
     */
    public void handleEvent(FileArrived event) {
        if (!event.getFileName().equalsIgnoreCase(FILE)) 
            return;

        File file = new File(ba.getGeneralSettings().getString("Core Location") + "/data/" + FILE);
        FileReader reader = null;
        BufferedReader buf = null;
        String line;
        
        try {
            reader = new FileReader(file);
            buf = new BufferedReader(reader);
            
            clear();
            
            while ((line = buf.readLine()) != null)
                if (!line.startsWith("#") && !line.startsWith("["))
                    add(low(line.trim()));
            
        } catch (Exception e) {
            ba.sendSmartPrivateMessage("WingZero", "Exception reading file...");
        } finally {
            try {
                if (reader != null)
                    reader.close();
                if (buf != null)
                    buf.close();
                file.delete();
            } catch (IOException e) {};
        }
    }
    
    /**
     * Check to see if any hidden names are found in the given string.
     * 
     * @param str
     * @return
     */
    public boolean isHidden(String str) {
        str = low(str);
        for (String hider : hiders)
            if (str.contains(hider))
                return true;
        return false;
    }
    
    /**
     * Handles !hiders command which lists current hiders.
     * 
     * @param name
     */
    public void cmd_list(String name) {
        String msg = "Hidden: ";
        for (String hider : hiders)
            msg += hider + ", ";
        msg = msg.substring(0, msg.length()-2);
        ba.sendSmartPrivateMessage(name, msg);
    }
    
    /**
     * Reloads hiders from hiders.txt server file.
     * Updates all instances of Hider.
     * 
     * @param name
     */
    public void cmd_reload(String name) {
        load();
        ba.sendSmartPrivateMessage(name, "Hiders have been reloaded from file.");
    }
    
    /**
     * Clears list of hiders.
     */
    public static void clear() {
        hiders.clear();
    }
    
    /**
     * Adds hider to list.
     * 
     * @param hider
     */
    public static void add(String hider) {
        if (!hiders.contains(hider))
            hiders.add(hider);
    }
    
    /**
     * Requests hiders file from game server.
     */
    private void load() {
        ba.getServerFile("hiders.txt");
    }
    
    private String low(String s) {
        return s.toLowerCase();
    }

}
