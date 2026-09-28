package org.community.privacy.infrai;

import java.util.List;

public interface AccountAccessGateway {
    List<String> listSessionIds(String userId);

    void revokeSession(String sessionId);

    void revokeCredential(String credentialId);
}
