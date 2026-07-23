package com.afriland.ticket2cash.merchant;

import com.afriland.ticket2cash.audit.AuditLogService;
import com.afriland.ticket2cash.auth.AppUserRepository;
import com.afriland.ticket2cash.campaign.CampaignRepository;
import com.afriland.ticket2cash.claim.ClaimRepository;
import com.afriland.ticket2cash.product.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantRepository merchantRepository;
    private final AuditLogService auditLogService;
    private final ClaimRepository claimRepository;
    private final CampaignRepository campaignRepository;
    private final ProductRepository productRepository;
    private final AppUserRepository userRepository;

    public MerchantController(MerchantRepository merchantRepository,
                              AuditLogService auditLogService,
                              ClaimRepository claimRepository,
                              CampaignRepository campaignRepository,
                              ProductRepository productRepository,
                              AppUserRepository userRepository) {
        this.merchantRepository = merchantRepository;
        this.auditLogService = auditLogService;
        this.claimRepository = claimRepository;
        this.campaignRepository = campaignRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Merchant> getAllMerchants() {
        return merchantRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Merchant> getMerchantById(@PathVariable Long id) {
        return merchantRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Merchant createMerchant(@RequestBody Merchant merchant) {
        Merchant savedMerchant = merchantRepository.save(merchant);

        auditLogService.log(
                "CREATE_MERCHANT",
                "MERCHANT",
                "Merchant",
                savedMerchant.getId(),
                "ADMIN_DEMO",
                "SUCCESS",
                "Merchant created: " + savedMerchant.getBrandName()
        );

        return savedMerchant;
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Merchant> updateMerchantStatus(
            @PathVariable Long id,
            @RequestParam MerchantStatus status
    ) {
        return merchantRepository.findById(id)
                .map(merchant -> {
                    merchant.setStatus(status);
                    Merchant updatedMerchant = merchantRepository.save(merchant);

                    auditLogService.log(
                            "UPDATE_MERCHANT_STATUS",
                            "MERCHANT",
                            "Merchant",
                            updatedMerchant.getId(),
                            "ADMIN_DEMO",
                            "SUCCESS",
                            "Merchant status changed to " + status
                    );

                    return ResponseEntity.ok(updatedMerchant);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMerchant(@PathVariable Long id) {
        if (!merchantRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        Merchant merchant = merchantRepository.findById(id).orElse(null);
        String merchantName = merchant != null ? merchant.getName() : "ID=" + id;

        // Delete all related data
        try {
            // Delete claims linked to this merchant
            claimRepository.findByMerchantId(id).forEach(c -> claimRepository.delete(c));

            // Delete campaigns linked to this merchant
            campaignRepository.findByMerchantId(id).forEach(c -> campaignRepository.delete(c));

            // Delete products linked to this merchant
            productRepository.findByMerchantId(id).forEach(p -> productRepository.delete(p));

            // Delete partner user accounts linked to this merchant
            userRepository.findAll().stream()
                .filter(u -> id.equals(u.getMerchantId()))
                .forEach(u -> userRepository.delete(u));

            // Finally delete the merchant
            merchantRepository.deleteById(id);

            auditLogService.log(
                    "DELETE_MERCHANT_CASCADE",
                    "MERCHANT",
                    "Merchant",
                    id,
                    "ADMIN",
                    "SUCCESS",
                    "Merchant deleted with all related data: " + merchantName
            );

            return ResponseEntity.ok(java.util.Map.of(
                "message", "Commercant '" + merchantName + "' et toutes ses donnees supprimees avec succes"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(java.util.Map.of(
                "error", "Erreur lors de la suppression: " + e.getMessage()
            ));
        }
    }
}