/**
 * @(#)WatchQueue.java
 *
 *
 * @author
 * @version 1.00 2007/7/10
 */
package twcore.bots.kimbot;

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
	private QItem[] m_table = new QItem[256];

    IntQueue() {
    	m_head = new QItem();
    	m_tail = new QItem();
    	m_head.right = m_tail;
    	m_tail.left = m_head;
    }

    synchronized void add(int id) {
    	QItem leftItem = m_tail.left;
    	QItem newItem = new QItem(id, leftItem, m_tail);
    	leftItem.right = m_tail.left = newItem;
    	m_size++;
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
    	} else return -1;
    }

    synchronized void sendToBack(int id) {
    	QItem item = m_table[id];
    	item.left.right = item.right;
    	item.right.left = item.left;

    	item.left = m_tail.left;
    	item.right = m_tail;
    	m_tail.left.right = item;
    	m_tail.left = item;
    }

    synchronized void remove(int id) {
    	QItem item = m_table[id];
    	if(item != null) {
	    	m_table[id] = null;
	    	item.left.right = item.right;
	    	item.right.left = item.left;
	    	m_size--;
    	}
    }

    synchronized void clear() {
    	m_head.right = m_tail;
    	m_tail.left = m_head;

    	for(int i = 0; i < 256; i++) {
    			m_table[i] = null;
    	}

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
    		if(id < 0 || id > 255)
    			throw new RuntimeException("id != [0..255]");
    		this.id = id;
    		this.left = left;
    		this.right = right;
    	}
    }
}