package org.community.privacy.application;

import org.community.privacy.domain.NonprofitAccount;
import org.community.privacy.infrai.AccountAccessGateway;

import java.util.List;

public final class AccountDeletionService {
    private final AccountAccessGateway access;

    public AccountDeletionService(AccountAccessGateway access) {
        this.access = access;
    }

    public DeletionResult delete(NonprofitAccount account) {
        List<NonprofitAccount.DonorReceipt> retainedReceipts = account.donorReceipts().stream()
                .map(NonprofitAccount.DonorReceipt::anonymized)
                .toList();

        List<String> sessions = access.listSessionIds(account.userId());
        sessions.forEach(access::revokeSession);
        access.revokeCredential(account.credentialId());

        return new DeletionResult(
                account.userId(),
                retainedReceipts,
                account.volunteerReminders().size(),
                account.campaignReports().size(),
                sessions.size(),
                true);
    }

    public record DeletionResult(
            String deletedUserId,
            List<NonprofitAccount.DonorReceipt> anonymizedReceipts,
            int deletedReminderCount,
            int deletedCampaignReportCount,
            int revokedSessionCount,
            boolean credentialRevoked) {}
}
