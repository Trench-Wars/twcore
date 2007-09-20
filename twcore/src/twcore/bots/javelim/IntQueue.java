/**
 * @(#)IntQueue.java
 *
 *
 * @author
 * @version 1.00 2007/7/10
 */
package twcore.bots.javelim;

import java.util.HashMap;

/**
 * doubly linked list with an index table so that any element
 * can be quickly moved to the back of the queue
 * intended for periodically switching which player is spectated by the bot
 */
final class IntQueue {

	//head is at the left and tail is at the right
	//new items are added at the tail
	private QItem m_head;
	private QItem m_tail;
	private int m_size = 0;

	//(assuming no player ID goes above 255)
	//private QItem[] m_table = new QItem[256];
	private HashMap<Integer, QItem> m_itemMap = new HashMap<Integer, QItem>(64);

    IntQueue() {
    	m_head = new QItem();
    	m_tail = new QItem();
    	m_head.right = m_tail;
    	m_tail.left = m_head;
    }


	synchronized void add(Integer id) {
    	remove(id);
    	QItem leftItem = m_tail.left;
    	QItem newItem = new QItem(id, leftItem, m_tail);
    	leftItem.right = m_tail.left = newItem;
    	//m_table[id] = newItem;
    	m_itemMap.put(id, newItem);
    	m_size++;
	}

    synchronized void add(int id) {
    	add(new Integer(id));
    }

    synchronized int getAndSendToBack() {
    	QItem item = m_head.right;
    	QItem right = item.right;
    	if(m_size > 1) {
    		m_head.right = item.right;
    		item.right.left = m_head;
    		item.left = m_tail.left;
    		item.right = m_tail;
    		m_tail.left.right = item;
    		m_tail.left = item;
    		return item.id;
    	} else if(m_size == 1) {
    		return item.id;
    	} else {
    		return -1;
    	}
    }


    synchronized void sendToBack(Integer id) {
    	//QItem item = m_table[id];
    	QItem item = m_itemMap.get(id);
    	if(item == null) {
    		return;
    	}
    	item.left.right = item.right;
    	item.right.left = item.left;

    	item.left = m_tail.left;
    	item.right = m_tail;
    	m_tail.left.right = item;
    	m_tail.left = item;
    }

    synchronized void sendToBack(int id) {
    	sendToBack(new Integer(id));
    }


	synchronized void remove(Integer id) {
    	//QItem item = m_table[id];
    	QItem item = m_itemMap.remove(id);
    	if(item != null) {
	    	//m_table[id] = null;
	    	item.left.right = item.right;
	    	item.right.left = item.left;
	    	m_size--;
    	}
	}

    synchronized void remove(int id) {
    	remove(new Integer(id));
    }

    synchronized void clear() {
    	m_head.right = m_tail;
    	m_tail.left = m_head;

   		//Arrays.fill(m_table[i], null);

    	m_size = 0;
    }

    synchronized int size() {
    	return m_size;
    }

    private final class QItem {
    	int id;
    	QItem left;
    	QItem right;

    	QItem() {
    		left = right = null;
    		id = -1;
    	}

    	QItem(int id, QItem left, QItem right) {
    		this.id = id;
    		this.left = left;
    		this.right = right;
    	}
    }
}