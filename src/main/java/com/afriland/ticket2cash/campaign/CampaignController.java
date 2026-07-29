package com.afriland.ticket2cash.campaign;

import com.afriland.ticket2cash.audit.AuditLogService;
import com.afriland.ticket2cash.merchant.Merchant;
import com.afriland.ticket2cash.merchant.MerchantRepository;
import com.afriland.ticket2cash.product.CashbackType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Campaign management with role-scoped access:
 * <ul>
 *   <li><b>PARTNER</b>: owns campaigns. Can create/edit/delete only their own
 *       merchant's campaigns. Sees only their own in listings.</li>
 *   <li><b>ADMIN / OPERATEUR / LECTEUR</b>: read-only oversight. Sees all
 *       campaigns across all merchants. Cannot create/edit/delete.</li>
 * </ul>
 * The merchant a campaign belongs to is derived from the logged-in PARTNER's
 * session; the request body's merchantId is ignored for security (prevents a
 * partner from posting campaigns on behalf of someone else).
 */
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

    // ---------------------------------------------------------------- helpers

    private String currentRole(HttpServletRequest http) {
        HttpSession session = http.getSession(false);
        if (session == null) return null;
        Object r = session.getAttribute("AUTH_ROLE");
        return r == null ? null : String.valueOf(r);
    }

    private Long currentMerchantId(HttpServletRequest http) {
        HttpSession session = http.getSession(false);
        if (session == null) return null;
        Object mid = session.getAttribute("AUTH_MERCHANT_ID");
        if (mid == null) return null;
        try {
            return Long.valueOf(String.valueOf(mid));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String currentActor(HttpServletRequest http) {
        HttpSession session = http.getSession(false);
        if (session == null) return "ANONYMOUS";
        Object u = session.getAttribute("AUTH_USERNAME");
        return u == null ? "ANONYMOUS" : String.valueOf(u);
    }

    /** Returns true if the request is authenticated and can WRITE (only PARTNER). */
    private boolean canWrite(HttpServletRequest http) {
        return "PARTNER".equalsIgnoreCase(currentRole(http));
    }

    /** Returns true if the given campaign belongs to the currently logged-in PARTNER. */
    private boolean isMine(Campaign c, HttpServletRequest http) {
        Long me = currentMerchantId(http);
        if (me == null) return false;
        Merchant m = c.getMerchant();
        return m != null && m.getId() != null && m.getId().equals(me);
    }

    // ---------------------------------------------------------------- READ

    @GetMapping
    public List<Campaign> getAllCampaigns(HttpServletRequest http) {
        String role = currentRole(http);
        if ("PARTNER".equalsIgnoreCase(role)) {
            Long me = currentMerchantId(http);
            if (me == null) return Collections.emptyList();
            return campaignRepository.findByMerchantId(me);
        }
        // ADMIN / OPERATEUR / LECTEUR / anonymous — see all
        return campaignRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCampaignById(@PathVariable Long id, HttpServletRequest http) {
        return campaignRepository.findById(id)
                .map(c -> {
                    // Partners can only see their own campaigns
                    if ("PARTNER".equalsIgnoreCase(currentRole(http)) && !isMine(c, http)) {
                        return ResponseEntity.status(403).body((Object) "Not your campaign");
                    }
                    return ResponseEntity.ok((Object) c);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<?> getCampaignsByMerchant(@PathVariable Long merchantId,
                                                    HttpServletRequest http) {
        if ("PARTNER".equalsIgnoreCase(currentRole(http))) {
            Long me = currentMerchantId(http);
            if (me == null || !me.equals(merchantId)) {
                return ResponseEntity.status(403).body("Not your merchant");
            }
        }
        return ResponseEntity.ok(campaignRepository.findByMerchantId(merchantId));
    }

    @GetMapping("/status/{status}")
    public List<Campaign> getCampaignsByStatus(@PathVariable CampaignStatus status,
                                                HttpServletRequest http) {
        String role = currentRole(http);
        if ("PARTNER".equalsIgnoreCase(role)) {
            Long me = currentMerchantId(http);
            if (me == null) return Collections.emptyList();
            return campaignRepository.findByMerchantId(me).stream()
                    .filter(c -> c.getStatus() == status)
                    .toList();
        }
        return campaignRepository.findByStatus(status);
    }

    // ---------------------------------------------------------------- WRITE (PARTNER only)

    @PostMapping
    public ResponseEntity<?> createCampaign(@RequestBody CampaignRequest request,
                                             HttpServletRequest http) {
        if (!canWrite(http)) {
            return ResponseEntity.status(403).body("Only PARTNER accounts can create campaigns");
        }
        Long myMid = currentMerchantId(http);
        if (myMid == null) {
            return ResponseEntity.status(403).body("No merchant linked to this account");
        }
        Merchant merchant = merchantRepository.findById(myMid).orElse(null);
        if (merchant == null) {
            auditLogService.log("CREATE_CAMPAIGN_FAILED", "CAMPAIGN", "Campaign", null,
                    currentActor(http), "FAILED", "Linked merchant not found: " + myMid);
            return ResponseEntity.badRequest().body("Linked merchant not found");
        }

        Campaign campaign = new Campaign();
        // Force merchant to the logged-in partner — never trust request body's merchantId
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

        Campaign saved = campaignRepository.save(campaign);

        auditLogService.log("CREATE_CAMPAIGN", "CAMPAIGN", "Campaign", saved.getId(),
                currentActor(http), "SUCCESS",
                "Campaign created: " + saved.getName() + " for merchant " + merchant.getName());

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCampaign(@PathVariable Long id,
                                             @RequestBody Map<String, Object> body,
                                             HttpServletRequest http) {
        if (!canWrite(http)) {
            return ResponseEntity.status(403).body("Only PARTNER accounts can edit campaigns");
        }
        return campaignRepository.findById(id)
                .map(campaign -> {
                    if (!isMine(campaign, http)) {
                        return ResponseEntity.status(403).body((Object) "Not your campaign");
                    }
                    if (body.containsKey("name")) campaign.setName((String) body.get("name"));
                    if (body.containsKey("description")) campaign.setDescription((String) body.get("description"));
                    if (body.containsKey("status")) campaign.setStatus(CampaignStatus.valueOf((String) body.get("status")));
                    if (body.containsKey("cashbackType")) campaign.setCashbackType(CashbackType.valueOf((String) body.get("cashbackType")));
                    if (body.containsKey("cashbackValue")) campaign.setCashbackValue(java.math.BigDecimal.valueOf(((Number) body.get("cashbackValue")).doubleValue()));
                    if (body.containsKey("totalBudget") && body.get("totalBudget") != null)
                        campaign.setTotalBudget(java.math.BigDecimal.valueOf(((Number) body.get("totalBudget")).doubleValue()));
                    if (body.containsKey("startDate")) campaign.setStartDate(java.time.LocalDate.parse((String) body.get("startDate")));
                    if (body.containsKey("endDate")) campaign.setEndDate(java.time.LocalDate.parse((String) body.get("endDate")));

                    Campaign updated = campaignRepository.save(campaign);

                    auditLogService.log("UPDATE_CAMPAIGN", "CAMPAIGN", "Campaign",
                            updated.getId(), currentActor(http), "SUCCESS",
                            "Campaign updated: " + updated.getName());
                    return ResponseEntity.ok((Object) updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateCampaignStatus(@PathVariable Long id,
                                                    @RequestParam CampaignStatus status,
                                                    HttpServletRequest http) {
        if (!canWrite(http)) {
            return ResponseEntity.status(403).body("Only PARTNER accounts can change campaign status");
        }
        return campaignRepository.findById(id)
                .map(campaign -> {
                    if (!isMine(campaign, http)) {
                        return ResponseEntity.status(403).body((Object) "Not your campaign");
                    }
                    campaign.setStatus(status);
                    Campaign updated = campaignRepository.save(campaign);
                    auditLogService.log("UPDATE_CAMPAIGN_STATUS", "CAMPAIGN", "Campaign",
                            updated.getId(), currentActor(http), "SUCCESS",
                            "Campaign status → " + status);
                    return ResponseEntity.ok((Object) updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCampaign(@PathVariable Long id, HttpServletRequest http) {
        if (!canWrite(http)) {
            return ResponseEntity.status(403).body("Only PARTNER accounts can delete campaigns");
        }
        return campaignRepository.findById(id).map(campaign -> {
            if (!isMine(campaign, http)) {
                return ResponseEntity.status(403).body((Object) "Not your campaign");
            }
            campaignRepository.deleteById(id);
            auditLogService.log("DELETE_CAMPAIGN", "CAMPAIGN", "Campaign", id,
                    currentActor(http), "SUCCESS",
                    "Campaign deleted: " + campaign.getName());
            return ResponseEntity.ok((Object) Map.of("message", "Campaign deleted"));
        }).orElse(ResponseEntity.notFound().build());
    }
}
