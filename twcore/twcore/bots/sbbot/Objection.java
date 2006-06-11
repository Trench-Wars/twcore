package twcore.bots.sbbot;

public class Objection {
    private String reason;

    public Objection(String r) {
	reason = r;
    }

    public String getReason() { return reason; }
}