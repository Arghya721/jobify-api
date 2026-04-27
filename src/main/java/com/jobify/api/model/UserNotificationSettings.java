package com.jobify.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_notification_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationSettings {

    @Id
    @Column(name = "user_id")
    private Long id; // This represents the user ID

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "telegram_chat_id")
    private String telegramChatId;

    @Column(name = "discord_user_id")
    private String discordUserId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
