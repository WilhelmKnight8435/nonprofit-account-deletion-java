package org.community.privacy.domain;

import java.util.List;

public record NonprofitAccount(
        String userId,
        String credentialId,
        List<DonorReceipt> donorReceipts,
        List<VolunteerReminder> volunteerReminders,
        List<CampaignReport> campaignReports) {

    public record DonorReceipt(String receiptId, String donorName, String donorEmail, long amountCents) {
        public DonorReceipt anonymized() {
            return new DonorReceipt(receiptId, "Deleted donor", "", amountCents);
        }
    }

    public record VolunteerReminder(String reminderId, String message) {}

    public record CampaignReport(String reportId, String ownerUserId) {}
}
