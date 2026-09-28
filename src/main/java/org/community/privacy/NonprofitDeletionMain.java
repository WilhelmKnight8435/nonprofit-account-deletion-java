package org.community.privacy;

import org.community.privacy.application.AccountDeletionService;
import org.community.privacy.config.InfraiConfig;
import org.community.privacy.domain.NonprofitAccount;
import org.community.privacy.infrai.InfraiAccountClient;

import java.util.List;

public final class NonprofitDeletionMain {
    private NonprofitDeletionMain() {}

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: NonprofitDeletionMain <user-id> <credential-id>");
            System.exit(2);
        }

        NonprofitAccount account = new NonprofitAccount(
                args[0],
                args[1],
                List.of(new NonprofitAccount.DonorReceipt("receipt-2026-014", "Account holder", "donor@example.org", 5000)),
                List.of(new NonprofitAccount.VolunteerReminder("reminder-8", "Saturday food bank shift")),
                List.of(new NonprofitAccount.CampaignReport("report-22", args[0])));

        AccountDeletionService.DeletionResult result = new AccountDeletionService(
                new InfraiAccountClient(InfraiConfig.fromEnvironment())).delete(account);

        System.out.printf(
                "deleted=%s receipts_anonymized=%d reminders_deleted=%d reports_deleted=%d sessions_revoked=%d credential_revoked=%s%n",
                result.deletedUserId(), result.anonymizedReceipts().size(), result.deletedReminderCount(),
                result.deletedCampaignReportCount(), result.revokedSessionCount(), result.credentialRevoked());
    }
}
