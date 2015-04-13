/**
 * @(#)LvzObjects.java
 *
 *
 * @author flibb
 * @version 1.00 2007/7/21
 */
package twcore.bots.birdelim;

import twcore.core.BotAction;
import java.lang.StringBuilder;

public final class LvzObjects {
    private int[] m_objectIDs;
    private int m_size = 0;
    private BotAction m_botAction;
    private String[] m_onStrings = new String[2];
    private String[] m_offStrings = new String[2];
    private int m_onSize = 0;
    private int m_offSize = 0;
    final static int MSG_LENGTH_THRESHOLD = 236;
    private boolean m_needToBuild = false;


    public LvzObjects(int capacity) {
        m_objectIDs = new int[capacity];
        m_botAction = BotAction.getBotAction();
    }

    synchronized void add(int objectID) {
        if(m_size >= m_objectIDs.length) {
            return;
        }
        m_objectIDs[m_size++] = objectID;
        m_needToBuild = true;
    }

    synchronized void remove(int objectID) {
        for(int i = 0; i < m_size; i++) {
            if(m_objectIDs[i] == objectID) {
                System.arraycopy(m_objectIDs, i + 1, m_objectIDs, i, --m_size - i);
                m_botAction.sendUnfilteredPublicMessage("*objoff " + objectID);
                m_needToBuild = true;
                break;
            }
        }
    }

    synchronized void turnOn() {
        if(m_needToBuild) {
            buildStrings();
        }
        for(int i = 0; i < m_onSize; i++) {
            m_botAction.sendUnfilteredPublicMessage(m_onStrings[i]);
        }
    }

    synchronized void turnOn(int playerID) {
        if(m_needToBuild) {
            buildStrings();
        }
        for(int i = 0; i < m_onSize; i++) {
            m_botAction.sendUnfilteredPrivateMessage(playerID, m_onStrings[i]);
        }
    }

    synchronized void turnOff() {
        for(int i = 0; i < m_offSize; i++) {
            m_botAction.sendUnfilteredPublicMessage(m_offStrings[i]);
        }
    }

    synchronized void turnOff(int playerID) {
        for(int i = 0; i < m_offSize; i++) {
            m_botAction.sendUnfilteredPrivateMessage(playerID, m_offStrings[i]);
        }
    }

    synchronized void clear() {
        turnOff();
        m_size = m_onSize = m_offSize = 0;
    }

    synchronized void buildStrings() {
        if(!m_needToBuild) {
            return;
        }
        m_onSize = m_offSize = 0;
        StringBuilder onsb = new StringBuilder(256);
        StringBuilder offsb = new StringBuilder(256);

        onsb.append("*objset ");
        offsb.append("*objset ");

        for(int i = 0; i < m_size; i++) {
            if(onsb.length() > MSG_LENGTH_THRESHOLD) {
                m_onStrings[m_onSize++] = onsb.toString();
                onsb.setLength(8);
                if(m_onSize == m_onStrings.length) {
                    m_onStrings = growStringArray(m_onStrings);
                }
            }

            if(offsb.length() > MSG_LENGTH_THRESHOLD) {
                m_offStrings[m_offSize++] = offsb.toString();
                offsb.setLength(8);
                if(m_offSize == m_offStrings.length) {
                    m_offStrings = growStringArray(m_offStrings);
                }
            }
            onsb.append(m_objectIDs[i]).append(',');
            offsb.append('-').append(m_objectIDs[i]).append(',');
        }
        m_onStrings[m_onSize++] = onsb.toString();
        m_offStrings[m_offSize++] = offsb.toString();
        m_needToBuild = false;
    }

    private String[] growStringArray(String[] arr) {
        String[] newArr = new String[arr.length + 1];
        for(int i = 0; i < arr.length; i++) {
            newArr[i] = arr[i];
        }
        return newArr;
    }

}
