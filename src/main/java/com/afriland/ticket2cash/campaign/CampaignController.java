package com.afriland.ticket2cash.campaign;

import com.afriland.ticket2cash.audit.AuditLogService;
import com.afriland.ticket2cash.merchant.Merchant;
import com.afriland.ticket2cash.merchant.MerchantRepository;
import com.afriland.ticket2cash.product.CashbackType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignRepository campaignRepository;
    private final MerchantRepository merchantRepository;
    private final AuditLogService auditLogService;

    public CampaignController(CampaignRepository campaignRepository,
                              MerchantRepository merchantRepository,
                              AuditLogService auditLogService) {
        this.campaignRepository = campaignRepository;
        this.merchantRepository = merchantRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<Campaign> getAllCampaigns() {
        return campaignRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCampaignById(@PathVariable Long id) {
        return campaignRepository.findById(id)
            .map(c -> ResponseEntity.ok((Object) c))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/merchant/{merchantId}")
    public List<Campaign> getCampaignsByMerchant(@PathVariable Long merchantId) {
        return campaignRepository.findByMerchantId(merchantId);
    }

    @GetMapping("/status/{status}")
    public List<Campaign> getCampaignsByStatus(@PathVariable CampaignStatus status) {
        return campaignRepository.findByStatus(status);
    }

    @PostMapping
    public ResponseEntity<?> createCampaign(@RequestBody CampaignRequest request) {

        Merchant merchant = merchantRepository.findById(request.getMerchantId()).orElse(null);

        if (merchant == null) {
            auditLogService.log(
                    "CREATE_CAMPAIGN_FAILED",
                    "CAMPAIGN",
                    "Campaign",
                    null,
                    "ADMIN_DEMO",
                    "FAILED",
                    "Merchant not found: " + request.getMerchantId()
            );

            return ResponseEntity.badRequest().body("Merchant not found with id = " + request.getMerchantId());
        }

        Campaign campaign = new Campaign();

        campaign.setMerchant(merchant);
        campaign.setName(request.getName());
        campaign.setDescription(request.getDescription());
        campaign.setStartDate(request.getStartDate());
        campaign.setEndDate(request.getEndDate());
        campaign.setCashbackType(request.getCashbackType() != null ? request.getCashbackType() : CashbackType.NONE);
        campaign.setCashbackValue(request.getCashbackValue() != null ? request.getCashbackValue() : BigDecimal.ZERO);
        campaign.setDailyLimitPerClient(request.getDailyLimitPerClient());
        campaign.setMonthlyLimitPerClient(request.getMonthlyLimitPerClient());
        campaign.setTotalBudget(request.getTotalBudget());
        campaign.setStatus(request.getStatus() != null ? request.getStatus() : CampaignStatus.DRAFT);

        Campaign savedCampaign = campaignRepository.save(campaign);

        auditLogService.log(
                "CREATE_CAMPAIGN",
                "CAMPAIGN",
                "Campaign",
                savedCampaign.getId(),
                "ADMIN_DEMO",
                "SUCCESS",
                "Campaign created: " + savedCampaign.getName()
        );

        return ResponseEntity.ok(savedCampaign);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCampaign(@PathVariable Long id,
                                             @RequestBody Map<String, Object> body) {
        return campaignRepository.findById(id)
                .map(campaign -> {
                    if (body.containsKey("name")) campaign.setName((String) body.get("name"));
                    if (body.containsKey("status")) campaign.setStatus(CampaignStatus.valueOf((String) body.get("status")));
                    if (body.containsKey("cashbackType")) campaign.setCashbackType(com.afriland.ticket2cash.product.CashbackType.valueOf((String) body.get("cashbackType")));
                    if (body.containsKey("cashbackValue")) campaign.setCashbackValue(java.math.BigDecimal.valueOf(((Number) body.get("cashbackValue")).doubleValue()));
                    if (body.containsKey("totalBudget")) campaign.setTotalBudget(java.math.BigDecimal.valueOf(((Number) body.get("totalBudget")).doubleValue()));
                    if (body.containsKey("startDate")) campaign.setStartDate(java.time.LocalDate.parse((String) body.get("startDate")));
                    if (body.containsKey("endDate")) campaign.setEndDate(java.time.LocalDate.parse((String) body.get("endDate")));

                    Campaign updated = campaignRepository.save(campaign);

                    auditLogService.log("UPDATE_CAMPAIGN", "CAMPAIGN", "Campaign",
                            updated.getId(), "ADMIN", "SUCCESS",
                            "Campaign updated: " + updated.getName());

                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Campaign> updateCampaignStatus(
            @PathVariable Long id,
            @RequestParam CampaignStatus status
    ) {
        return campaignRepository.findById(id)
                .map(campaign -> {
                    campaign.setStatus(status);
                    Campaign updatedCampaign = campaignRepository.save(campaign);

                    auditLogService.log(
                            "UPDATE_CAMPAIGN_STATUS",
                            "CAMPAIGN",
                            "Campaign",
                            updatedCampaign.getId(),
                            "ADMIN_DEMO",
                            "SUCCESS",
                            "Campaign status changed to " + status
                    );

                    return ResponseEntity.ok(updatedCampaign);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(@PathVariable Long id) {

        if (!campaignRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        campaignRepository.deleteById(id);

        auditLogService.log(
                "DELETE_CAMPAIGN",
                "CAMPAIGN",
                "Campaign",
                id,
                "ADMIN_DEMO",
                "SUCCESS",
                "Campaign deleted"
        );

        return ResponseEntity.noContent().build();
    }
}