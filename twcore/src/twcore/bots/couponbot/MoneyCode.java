package twcore.bots.couponbot;

import java.util.Calendar;
import java.util.Date;

public class MoneyCode {

	
	private final String code;
	private final int money;
	private final Date createdAt;
	private final String createdBy;
		
	private int id;
	private Date startAt;
	private Date endAt;
	private int used;
	private String description;
	private int maxUsed;
	
	private boolean enabled;
	
	public MoneyCode(String code, int money, String createdBy) {
		this(code, money, new Date(), createdBy);
	}
	
	public MoneyCode(String code, int money, Date createdAt, String createdBy) {
		this.code = code;
		this.money = money;
		this.createdAt = createdAt;
		this.createdBy = createdBy;
		
		this.used = 0;
		this.maxUsed = 1;
		this.description = "";
		this.startAt = new Date();
		this.enabled = true;
		
		Calendar c = Calendar.getInstance(); 
		c.add(Calendar.YEAR,1);
		this.endAt = c.getTime();
	}
	
	public int getId() {
		return id;
	}
	
	public String getCode() {
		return code;
	}

	public int getMoney() {
		return money;
	}

	public Date getCreatedAt() {
		return createdAt;
	}
	
	public String getDescription() {
		return description;
	}

	public int getMaxUsed() {
		return maxUsed;
	}

	public String getCreatedBy() {
		return createdBy;
	}
	
	public Date getStartAt() {
		return startAt;
	}

	public Date getEndAt() {
		return endAt;
	}

	public int getUsed() {
		return used;
	}

	public boolean isEnabled() {
		return enabled;
	}
	
	public void setId(int id) {
		this.id = id;
	}

	public void setStartAt(Date startAt) {
		this.startAt = startAt;
	}

	public void setEndAt(Date endAt) {
		this.endAt = endAt;
	}

	public void setUsed(int used) {
		this.used = used;
	}
	
	public void setMaxUsed(int max) {
		this.maxUsed = max;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getInvalidReason() 
	{
		if (!startAt.before(new Date()))
			return "Cannot be used before start date";
		
		if (!endAt.after(new Date()))
			return "Date expired";
		
		if (maxUsed == used)
			return "Maximum of use reached";
		
		if (!enabled)
			return "Disabled";
		
		return "Unknown";
	}
	
	public boolean isValid() 
	{
		if (!enabled)
			return false;
		
		if (!startAt.before(new Date()) || !endAt.after(new Date()))
			return false;
		
		if (maxUsed == used)
			return false;
		
		return true;
	}


}
