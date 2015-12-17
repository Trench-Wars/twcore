package twcore.core.net;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import twcore.core.util.Tools;
import twcore.core.util.json.JSONObject;


/**
 * Pushes notifications to a PushBullet Channel, which is essentially a modernized RSS feed.
 * It's largely managed by PushBullet, so the bot overhead is basically nothing.
 * 
 * @author qan
 */
public class MobilePusher {
    URL pushURL;
    private String pushAuth;        // base64(concat(authcode,":")) -- get authcode from PushBullet & base64 encode it + :
    private String pushChannel;
    private long delay;
    private long lastPush;
    
    public MobilePusher( String pushAuth, String pushChannel, long minDelayBetweenPushes ) {
        if( pushAuth == null || pushChannel == null )
            throw new RuntimeException("Null input where auth or channel expected");
        this.pushAuth = pushAuth;
        this.pushChannel = pushChannel;
        delay = minDelayBetweenPushes;
        lastPush = System.currentTimeMillis() - delay;
        try {
            pushURL = new URL("https://api.pushbullet.com/v2/pushes");
        } catch( Exception e ) {} 
    }
    
    /**
     * Sends a push with no title. On the user's phone, the title of the notification will be the channel name (not pushChannel).
     * @param msg Message to send
     * @return True if there are no errors
     */
    public boolean push( String msg ) {
        return push( "", msg );
    }
    
    /**
     * Sends a push with title. On Android, user will see title in larger font, then the message beneath it. On the right side,
     * the time will appear, and below that, the channel name as set on PushBullet's website (different from pushChannel).
     * 
     * In the push itself, if a user opens it in PushBullet rather than dismissing it, the title and the message will appear
     * with the same size text and be indistinguishable. The preferred method is therefore probably sending without a title.
     * However, you could send with a title to distinguish who is saying what.
     * @param title Title to send
     * @param body Message to send
     * @return True if there are no errors
     */
    public boolean push( String title, String body ) {
        if( pushAuth == null ) {           
            Tools.printLog("Null auth string in push.");
            return false;
        }
        if( pushChannel == null ) {           
            Tools.printLog("Null channel in push.");
            return false;
        }
        if( title == null ) {           
            Tools.printLog("Null title string in push.");
            return false;
        }

        if( body == null ) {           
            Tools.printLog("Null body string in push.");
            return false;
        }
            
        if( System.currentTimeMillis() < lastPush + delay )
            return false;
        
        try {
            HttpURLConnection con = (HttpURLConnection) pushURL.openConnection();

            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            con.setRequestProperty("Authorization", "Basic " + pushAuth);
            con.setDoOutput(true);

            HashMap<String,String> hm = new HashMap<String,String>();
            hm.put("type", "note");
            hm.put("title", title);
            hm.put("channel_tag", pushChannel);
            hm.put("body", body);
            String msg = JSONObject.toJSONString(hm);            

            OutputStream os = con.getOutputStream();
            os.write(msg.getBytes("UTF-8"));
            os.flush();
            con.disconnect();
            
            // Uncomment to test return status.           
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Failed : HTTP error code : " + con.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((con.getInputStream())));

            String output;
            while ((output = br.readLine()) != null) {
                System.out.println(output);
            }

        } catch (Exception e) {
            Tools.printStackTrace("Error encountered when pushing message to mobile: " + body, e);
            return false;
        }
               
        lastPush = System.currentTimeMillis();
        return true;
    }
}
