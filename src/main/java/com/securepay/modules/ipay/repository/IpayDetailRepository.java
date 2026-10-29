package com.securepay.modules.ipay.repository;

import com.securepay.modules.ipay.entity.IpayDetail;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link IpayDetail}. */
public interface IpayDetailRepository extends JpaRepository<IpayDetail, Long> {
}
