package org.community.privacy.application;

import org.community.privacy.domain.NonprofitAccount;
import org.community.privacy.infrai.AccountAccessGateway;

import java.util.ArrayList;
import java.util.List;

public final class AccountDeletionServiceTest {
    public static void main(String[] args) {
        RecordingAccess access = new RecordingAccess(List.of("session-a", "session-b"));
        NonprofitAccount account = new NonprofitAccount(
                "user-42",
                "key-42",
                List.of(new NonprofitAccount.DonorReceipt("receipt-9", "Ada Donor", "ada@example.org", 12500)),
                List.of(new NonprofitAccount.VolunteerReminder("reminder-3", "Call volunteers")),
                List.of(new NonprofitAccount.CampaignReport("report-7", "user-42")));

        AccountDeletionService.DeletionResult result = new AccountDeletionService(access).delete(account);

        check(result.anonymizedReceipts().get(0).donorName().equals("Deleted donor"), "receipt name must be anonymized");
        check(result.anonymizedReceipts().get(0).donorEmail().isEmpty(), "receipt email must be erased");
        check(result.anonymizedReceipts().get(0).amountCents() == 12500, "receipt amount must be retained");
        check(result.deletedReminderCount() == 1, "volunteer reminder must be deleted");
        check(result.deletedCampaignReportCount() == 1, "owned campaign report must be deleted");
        check(access.calls.equals(List.of("list:user-42", "session:session-a", "session:session-b", "key:key-42")),
                "credential must be revoked after every session");
        System.out.println("PASS: receipts anonymized; reminders and reports deleted; 2 sessions and credential revoked");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class RecordingAccess implements AccountAccessGateway {
        private final List<String> sessions;
        private final List<String> calls = new ArrayList<>();

        private RecordingAccess(List<String> sessions) {
            this.sessions = sessions;
        }

        @Override
        public List<String> listSessionIds(String userId) {
            calls.add("list:" + userId);
            return sessions;
        }

        @Override
        public void revokeSession(String sessionId) {
            calls.add("session:" + sessionId);
        }

        @Override
        public void revokeCredential(String credentialId) {
            calls.add("key:" + credentialId);
        }
    }
}
