package twcore.core.net;

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
    
    public boolean push( String title, String body ) {
        if( pushAuth == null || pushChannel == null || title == null || body == null )
            return false;
        if( System.currentTimeMillis() < lastPush + delay )
            return false;
        
        try {
            Runtime.getRuntime().exec("curl -u " + pushAuth +
                    ": -X POST https://api.pushbullet.com/v2/pushes --header 'Content-Type:application/json' " +
                    "--data-binary '{\"type\":\"note\",\"title\":\"" + sanitize(title) +
                    "\",\"channel_tag\":\"" + pushChannel +
                    "\",\"body\":\"" + sanitize(body) + "\"}'");
        } catch( Exception e) {}
        
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
