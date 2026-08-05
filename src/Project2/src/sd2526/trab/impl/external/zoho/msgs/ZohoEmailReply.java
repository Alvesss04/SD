package sd2526.trab.impl.external.zoho.msgs;

import java.util.List;

public record ZohoEmailReply(ZohoStatus status, List<ZohoEmail> data) {
	
}
