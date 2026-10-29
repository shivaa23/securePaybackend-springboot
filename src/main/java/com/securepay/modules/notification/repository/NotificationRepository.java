package com.securepay.modules.notification.repository;

import com.securepay.modules.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link Notification}. */
public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
