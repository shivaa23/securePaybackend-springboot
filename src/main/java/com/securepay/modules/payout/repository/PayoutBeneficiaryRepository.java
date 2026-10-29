package com.securepay.modules.payout.repository;

import com.securepay.modules.payout.entity.PayoutBeneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Repository for {@link PayoutBeneficiary}. */
public interface PayoutBeneficiaryRepository extends JpaRepository<PayoutBeneficiary, Long> {

    List<PayoutBeneficiary> findAllByOrderByIdDesc();

    List<PayoutBeneficiary> findByMobileNumberAndTypeOrderByIdAsc(String mobileNumber, String type);

    /** Non-UPI beneficiaries of a sender (legacy getSender "BANK" view: type != 'UPI'). */
    List<PayoutBeneficiary> findByMobileNumberAndTypeNotOrderByIdAsc(String mobileNumber, String type);

    Optional<PayoutBeneficiary> findFirstByMobileNumberAndAccountNumberAndIfscCodeAndType(
            String mobileNumber, String accountNumber, String ifscCode, String type);

    Optional<PayoutBeneficiary> findFirstBySenderIdAndAccountNumberAndType(
            Long senderId, String accountNumber, String type);

    Optional<PayoutBeneficiary> findFirstByAccountNumberAndMobileNumber(String accountNumber, String mobileNumber);

    Optional<PayoutBeneficiary> findFirstBySenderIdAndId(Long senderId, Long id);

    List<PayoutBeneficiary> findByAccountNumberAndType(String accountNumber, String type);

    @Modifying
    @Query("delete from PayoutBeneficiary b where b.mobileNumber = :mobile and b.soliteckId = :id")
    int deleteByMobileAndSoliteckId(@Param("mobile") String mobile, @Param("id") String id);

    @Modifying
    @Query("delete from PayoutBeneficiary b where b.mobileNumber = :mobile and b.apiId = :id")
    int deleteByMobileAndApiId(@Param("mobile") String mobile, @Param("id") String id);

    @Modifying
    @Query("update PayoutBeneficiary b set b.isVerified = 1 where b.mobileNumber = :mobile and b.soliteckId = :id")
    int markVerifiedBySoliteckId(@Param("mobile") String mobile, @Param("id") String id);

    List<PayoutBeneficiary> findByTypeOrderByIdDesc(String type);

    List<PayoutBeneficiary> findByTypeAndSenderIdOrderByIdDesc(String type, Long senderId);
}
