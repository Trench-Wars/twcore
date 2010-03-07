package twcore.bots.multibot.twar;

public interface turretwar {

    public void cmd_start       (String name, String message);
    public void cmd_addTerr     (String name, String message);
    public void cmd_switchTerr  (String name, String message);
    public void cmd_removeTerr  ();
    public void cmd_stop        (String name, String message);
    public void cmd_warp        (String name, String message);
    public void cmd_firstShip   (String name, String message);
    
}
