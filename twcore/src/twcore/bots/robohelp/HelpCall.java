package twcore.bots.robohelp;

import java.util.LinkedList;

public class HelpCall extends Call {
    
    public static final int LINE_SIZE = 100;

    static final int FREE = 0;
    static final int TAKEN = 1;
    static final int MINE = 2;
    static final int FORGOT = 3;
    static final int CLEAN = 4;

    boolean allowSummons;
    String[] responses;
    int nextResponse;
    
    public HelpCall(String playerName, String question, String[] responses, int type) {
        super();
        this.playerName = playerName;
        this.message = question;
        this.callType = type;
        this.nextResponse = 0;
        this.responses = responses;
        this.allowSummons = false;
    }

    public void reset() {
        claimer = "";
        claimed = false;
        claimType = FREE;
    }

    public void claim(String name) {
        claimer = name;
        claimed = true;
        claimType = TAKEN;
        timeClaim = System.currentTimeMillis();
    }

    public void mine(String name) {
        claimer = name;
        claimed = true;
        claimType = MINE;
        timeClaim = System.currentTimeMillis();
    }

    public void forget() {
        claimer = "[forgot]";
        claimed = true;
        claimType = FORGOT;
        timeClaim = System.currentTimeMillis();
    }

    public void setQuestion(String question, String[] responses) {

        nextResponse = 0;
        this.message = question;
        this.responses = responses;
        this.timeSent = System.currentTimeMillis();
    }

    public void setAllowSummons(boolean allowSummons) {

        this.allowSummons = allowSummons;
    }

    public boolean getAllowSummons() {

        return allowSummons;
    }

    public boolean isValidHelpRequest() {

        if (message != null)
            return true;
        else
            return false;
    }

    public String[] getAllResponses() {

        return responses;
    }

    public String[] getFirstResponse() {

        if (responses == null)
            return null;
        else if (responses.length != 0)
            return formatResponse(responses[0]);
        else
            return null;
    }

    public String[] getLastResponse() {

        if (nextResponse <= 0 || responses == null)
            return null;
        else
            return formatResponse(responses[nextResponse - 1]);
    }

    public String[] getNextResponse() {

        if (responses == null)
            return null;

        if (nextResponse >= responses.length)
            return null;
        else
            return formatResponse(responses[nextResponse++]);
    }

    public boolean hasMoreResponses() {

        if (responses == null)
            return false;

        if (nextResponse >= responses.length)
            return false;
        else
            return true;
    }

    private int indexNotOf(String string, char target, int fromIndex) {
        for (int index = fromIndex; index < string.length(); index++)
            if (string.charAt(index) != target)
                return index;
        return -1;
    }

    private int getBreakIndex(String string, int fromIndex) {
        if (fromIndex + LINE_SIZE > string.length())
            return string.length();
        int breakIndex = string.lastIndexOf(' ', fromIndex + LINE_SIZE);
        if ("?".equals(string.substring(breakIndex + 1, breakIndex + 2)) || "*".equals(string.substring(breakIndex + 1, breakIndex + 2)))
            breakIndex = string.lastIndexOf(' ', fromIndex + LINE_SIZE - 3);
        if (breakIndex == -1)
            return fromIndex + LINE_SIZE;
        return breakIndex;
    }

    private String[] formatResponse(String response) {
        LinkedList<String> formattedResp = new LinkedList<String>();
        int startIndex = indexNotOf(response, ' ', 0);
        int breakIndex = getBreakIndex(response, 0);

        while (startIndex != -1) {
            formattedResp.add(response.substring(startIndex, breakIndex));
            startIndex = indexNotOf(response, ' ', breakIndex);
            breakIndex = getBreakIndex(response, startIndex);
        }
        return formattedResp.toArray(new String[formattedResp.size()]);
    }

}