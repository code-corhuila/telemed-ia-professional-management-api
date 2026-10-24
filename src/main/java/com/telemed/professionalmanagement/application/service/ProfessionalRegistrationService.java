package com.telemed.professionalmanagement.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.telemed.professionalmanagement.application.command.ProfessionalRegistrationCommand;
import com.telemed.professionalmanagement.application.port.in.Caller;
import com.telemed.professionalmanagement.application.port.in.IdempotencyKeyReusedException;
import com.telemed.professionalmanagement.application.port.in.RegisterProfessionalAccountUseCase;
import com.telemed.professionalmanagement.application.port.in.Registration;
import com.telemed.professionalmanagement.application.port.out.IdentityAccountPort;
import com.telemed.professionalmanagement.application.port.out.NewProfessionalAccount;
import com.telemed.professionalmanagement.application.port.out.ProfessionalRegistrationStore;
import com.telemed.professionalmanagement.application.port.out.StoredRegistration;
import com.telemed.professionalmanagement.domain.DomainException;
import com.telemed.professionalmanagement.domain.DuplicateProfessionalIdentityException;
import com.telemed.professionalmanagement.domain.DuplicateProfessionalLicenseException;
import com.telemed.professionalmanagement.domain.Professional;

/**
 * Registers a professional (DEC-003): the account in the identity domain, then the profile
 * here. The idempotency key is checked before the account is created, so a retry never asks
 * identity for a second account; the profile and the key are stored in one transaction.
 */
@Service
public class ProfessionalRegistrationService implements RegisterProfessionalAccountUseCase {

    private final IdentityAccountPort identityAccounts;
    private final ProfessionalRegistrationStore store;

    public ProfessionalRegistrationService(IdentityAccountPort identityAccounts, ProfessionalRegistrationStore store) {
        this.identityAccounts = identityAccounts;
        this.store = store;
    }

    @Override
    public Registration register(Caller caller, String idempotencyKey, ProfessionalRegistrationCommand command) {
        caller.requireAdmin();
        Professional.checkProfile(command.licenseNumber(), command.yearsExperience());
        String requestHash = requestHash(command);

        Optional<StoredRegistration> previous = store.findByIdempotencyKey(idempotencyKey);
        if (previous.isPresent()) {
            return replay(previous.get(), requestHash);
        }
        if (store.existsByLicenseNumber(command.licenseNumber())) {
            return replayOrThrow(idempotencyKey, requestHash,
                    new DuplicateProfessionalLicenseException(command.licenseNumber().trim()));
        }

        UUID accountId = identityAccounts.createProfessionalAccount(new NewProfessionalAccount(
                command.fullName(), command.email(), command.identityDocument(), command.password()), idempotencyKey);

        Optional<Professional> saved;
        try {
            saved = store.saveRegistration(Professional.register(accountId, command.licenseNumber(),
                    command.yearsExperience()), idempotencyKey, requestHash);
        } catch (DuplicateProfessionalLicenseException | DuplicateProfessionalIdentityException duplicate) {
            return replayOrThrow(idempotencyKey, requestHash, duplicate);
        }
        return saved.map(professional -> new Registration(professional, true))
                .orElseGet(() -> replay(store.findByIdempotencyKey(idempotencyKey).orElseThrow(), requestHash));
    }

    private static Registration replay(StoredRegistration previous, String requestHash) {
        if (!previous.requestHash().equals(requestHash)) {
            throw new IdempotencyKeyReusedException();
        }
        return new Registration(previous.professional(), false);
    }

    /**
     * A concurrent twin with the same key may have stored the profile after the key was
     * checked: then the duplicate is that same registration, answered as a retry.
     */
    private Registration replayOrThrow(String idempotencyKey, String requestHash, DomainException duplicate) {
        return store.findByIdempotencyKey(idempotencyKey)
                .map(twin -> replay(twin, requestHash))
                .orElseThrow(() -> duplicate);
    }

    /**
     * What identifies the request behind a key. The password is left out on purpose, so no
     * value derived from it is ever stored.
     */
    static String requestHash(ProfessionalRegistrationCommand command) {
        String canonical = String.join("\n",
                trim(command.fullName()),
                trim(command.email()).toLowerCase(Locale.ROOT),
                trim(command.identityDocument()),
                trim(command.licenseNumber()),
                String.valueOf(command.yearsExperience()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by every Java runtime", impossible);
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
