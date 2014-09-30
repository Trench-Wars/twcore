package twcore.bots;


public abstract class PubBotModule extends Module
{
    protected String ipcChannel;
    protected String pubHubName;

    public void initializeModule(String ipcChannel, String pubHubName)
    {
        this.ipcChannel = ipcChannel;
        this.pubHubName = pubHubName;
    }

    public String getIPCChannel()
    {
        return ipcChannel;
    }

    public String getPubHubName()
    {
        return pubHubName;
    }
}