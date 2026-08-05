package sd2526.trab.impl.external.zoho.msgs;

public record ZohoEmail(
	    String summary,
	    String sentDateInGMT,
	    int calendarType,
	    String subject,
	    String messageId,
	    String threadCount,
	    String flagid,
	    String status2,
	    String priority,
	    String hasInline,
	    String toAddress,
	    String folderId,
	    String ccAddress,
	    String threadId,
	    String hasAttachment,
	    String size,
	    String sender,
	    String receivedTime,
	    boolean enabled
	) {}