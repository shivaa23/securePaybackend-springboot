package com.securepay.modules.userdevice.repository;

import com.securepay.modules.userdevice.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link UserDevice}. */
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
}
