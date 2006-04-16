package twcore.bots.sbbot;
import java.io.*;

public class SBTeam {
    private String captain;
    private String name;
    private boolean ready = false;
    private int freq;
    
    public SBTeam(String n, int f) {
	name = n;
	freq = f;
    }

    public void setCaptain(String cap) { captain = cap; }
    public boolean isCaptain(String cap) {
	if(captain == null) return false;
	return captain.equalsIgnoreCase(cap);
    }
    public String getCaptain() { return captain; }
    
    public void setFreq(int i) {
	assert(i >= 0 && i < 9999);
	freq = i;
    }
    public int getFreq() { return freq; }

    public void setName(String n) { name = n; }
    public String getName() { return name; }
    public boolean isReady() { return ready; }
    public void setReady(boolean b) { ready = b; }
}