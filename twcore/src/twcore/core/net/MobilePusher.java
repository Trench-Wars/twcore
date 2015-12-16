package twcore.core.net;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import twcore.core.util.Tools;
import twcore.core.util.json.JSONObject;


/**
 * Pushes notifications to a PushBullet Channel, which is essentially a modernized RSS feed.
 * It's managed by PushBullet, so the bot overhead is basically nothing.
 * 
 * @author qan
 */
public class MobilePusher {
    private String pushAuth;
    private String pushChannel;
    private long delay;
    private long lastPush;
    
    public MobilePusher( String pushAuth, String pushChannel, long minDelayBetweenPushes ) {
        this.pushAuth = pushAuth;
        this.pushChannel = pushChannel;
        delay = minDelayBetweenPushes;
        lastPush = System.currentTimeMillis() - delay;
    }
    
    public boolean push( String msg ) {
        return push( "", msg );
    }
    
    public boolean push( String title, String body ) {
        if( pushAuth == null || pushChannel == null || title == null || body == null )
            return false;
        if( System.currentTimeMillis() < lastPush + delay )
            return false;
        
        StringBuffer output = new StringBuffer();
        Process p;
        HashMap<String,String> hm = new HashMap<String,String>();
        hm.put("type", "note");
        hm.put("title", sanitize(title));
        hm.put("channel_tag", pushChannel);
        hm.put("body", sanitize(body));
        String msg = JSONObject.toJSONString(hm);
        
        try {
            msg = "curl -u " + pushAuth +
                    ": -X POST https://api.pushbullet.com/v2/pushes --header 'Content-Type:application/json' --data-binary '" + msg + "'";
            /*
                    '{\"type\":\"note\",\"title\":\"" + sanitize(title) +
                    "\",\"channel_tag\":\"" + pushChannel +
                    "\",\"body\":\"" + sanitize(body) + "\"}'";
            */

            System.out.print(msg);
            p = Runtime.getRuntime().exec(msg);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";           
            while ((line = reader.readLine())!= null) {
                output.append(line + "\n");
            }
            
            System.out.print(output);
        } catch( Exception e) {
            Tools.printStackTrace("Error encountered when pushing message to mobile: " + body, e);
        }
        
        lastPush = System.currentTimeMillis();
        return true;
    }
    
    /**
     * Sanitize input by escaping all ' as '\''
     * @param t String to sanitize
     * @return Formatted string
     */
    public static String sanitize( String t ) {
        String n = null;
        if (t != null) {
            n = "";
            for (int i = 0; i < t.length(); i++) {
                if (t.charAt(i) == '\'') {
                    n = n + "'\''"; 
                } else {
                    n = n + t.charAt(i);
                }
            }
        }
        return n;
    }

}
