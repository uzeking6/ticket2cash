package com.afriland.ticket2cash.claim;

import com.afriland.ticket2cash.audit.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimRepository claimRepository;
    private final AuditLogService auditLogService;

    public ClaimController(ClaimRepository claimRepository,
            AuditLogService auditLogService) {
        this.claimRepository = claimRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<Claim> getAllClaims() {
        return claimRepository.findAll();
    }

    @GetMapping("/merchant/{merchantId}")
    public List<Claim> getClaimsByMerchant(@PathVariable Long merchantId) {
        return claimRepository.findByMerchantId(merchantId);
    }

    @GetMapping("/status/{status}")
    public List<Claim> getClaimsByStatus(@PathVariable ClaimStatus status) {
        return claimRepository.findByStatus(status);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Claim> updateClaimStatus(
            @PathVariable Long id,
            @RequestParam ClaimStatus status) {
        return claimRepository.findById(id)
                .map(claim -> {
                    claim.setStatus(status);
                    Claim updatedClaim = claimRepository.save(claim);

                    auditLogService.log(
                            "UPDATE_CLAIM_STATUS",
                            "CLAIM",
                            "Claim",
                            updatedClaim.getId(),
                            "ADMIN_DEMO",
                            "SUCCESS",
                            "Claim status changed to " + status);

                    return ResponseEntity.ok(updatedClaim);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}