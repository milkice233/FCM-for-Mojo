package moe.shizuku.fcmformojo.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.provider.DocumentFile;

import moe.shizuku.fcmformojo.FFMApplication;
import moe.shizuku.fcmformojo.FFMSettings;
import moe.shizuku.fcmformojo.FFMSettings.Vibrate;
import moe.shizuku.fcmformojo.R;
import moe.shizuku.fcmformojo.app.MessagingStyle;
import moe.shizuku.fcmformojo.model.Chat;
import moe.shizuku.fcmformojo.model.Message;
import moe.shizuku.fcmformojo.profile.Profile;
import moe.shizuku.fcmformojo.receiver.FFMBroadcastReceiver;
import moe.shizuku.fcmformojo.service.FFMIntentService;

import static moe.shizuku.fcmformojo.FFMStatic.NOTIFICATION_CHANNEL_FRIENDS;
import static moe.shizuku.fcmformojo.FFMStatic.NOTIFICATION_CHANNEL_GROUPS;
import static moe.shizuku.fcmformojo.FFMStatic.NOTIFICATION_CHANNEL_SERVER;
import static moe.shizuku.fcmformojo.FFMStatic.NOTIFICATION_ID_GROUP_SUMMARY;
import static moe.shizuku.fcmformojo.FFMStatic.NOTIFICATION_ID_SYSTEM;
import static moe.shizuku.fcmformojo.FFMStatic.NOTIFICATION_INPUT_KEY;
import static moe.shizuku.fcmformojo.FFMStatic.NOTIFICATION_MAX_MESSAGES;
import static moe.shizuku.fcmformojo.FFMStatic.REQUEST_CODE_DISMISS_SYSTEM_NOTIFICATION;
import static moe.shizuku.fcmformojo.FFMStatic.REQUEST_CODE_RESTART_WEBQQ;

/**
 * Created by Rikka on 2016/9/18.
 */
class NotificationBuilderImplBase extends NotificationBuilderImpl {

    private static final String TAG = "NotificationBuilderImplBase";

    private static final String GROUP_KEY = "messages";

    @Override
    void notify(Context context, Chat chat, NotificationBuilder nb) {
        int id = (int) chat.getUniqueId();

        notifyGroupSummary(context, chat, nb);

        NotificationCompat.Builder builder = createBuilder(context, chat)
                .setLargeIcon(chat.loadIcon(context))
                .setContentTitle(chat.getName())
                .setContentText(chat.getLatestMessage().getContent(context))
                .setGroup(GROUP_KEY)
                .setGroupSummary(false)
                .setShowWhen(true)
                .setWhen(chat.getLatestMessage().getTimestamp() * 1000)
                .setStyle(getStyle(context, chat))
                .setContentIntent(NotificationBuilder.createContentIntent(context, id, chat))
                .setDeleteIntent(NotificationBuilder.createDeleteIntent(context, id, chat))
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .addAction(createReplyAction(context, id, chat));

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(id, builder.build());
    }

    @Override
    void notifySystem(Context context, Chat chat, NotificationBuilder nb) {
        nb.clearMessages();

        switch (chat.getLatestMessage().getSender()) {
            case "login":
                // 删掉下载的二维码
                try {
                    DocumentFile dir = FFMSettings.getDownloadDir(context);
                    if (dir != null) {
                        DocumentFile file = dir.findFile("webqq-qrcode.png");
                        if (file != null) {
                            file.delete();
                        }
                    }
                } catch (Exception ignored) {
                }
                return;
            case "input_qrcode":
                FFMIntentService.startDownloadQrCode(context, chat.getLatestMessage().getContent());
                break;
            case "stop":
                NotificationCompat.Builder builder = nb.createBuilder(context, null)
                        .setChannelId(NOTIFICATION_CHANNEL_SERVER)
                        .setContentTitle(context.getString(R.string.notification_server_stop))
                        .setContentText(context.getString(R.string.notification_tap_to_restart))
                        .setSmallIcon(R.drawable.ic_noti_24dp)
                        .setWhen(System.currentTimeMillis())
                        .setOngoing(true)
                        .setAutoCancel(true)
                        .setShowWhen(true)
                        .setContentIntent(PendingIntent.getService(context, REQUEST_CODE_RESTART_WEBQQ, FFMIntentService.restartIntent(context), PendingIntent.FLAG_ONE_SHOT))
                        .addAction(R.drawable.ic_noti_dismiss_24dp, context.getString(android.R.string.cancel), PendingIntent.getBroadcast(context, REQUEST_CODE_DISMISS_SYSTEM_NOTIFICATION, FFMBroadcastReceiver.dismissSystemNotificationIntent(), 0));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//Android M+
                    builder = builder.setColor(context.getColor(R.color.colorServerNotification));
                } else {
                    builder = builder.setColor(context.getResources().getColor(R.color.colorServerNotification));
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setVibrate(new long[0]);
                }

                nb.getNotificationManager().notify(NOTIFICATION_ID_SYSTEM, builder.build());
                break;
        }
    }

    private static NotificationCompat.Style getStyle(Context context, Chat chat) {
        if (chat.isFriend()) {
            NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
            style.setBigContentTitle(chat.getName());

            StringBuilder sb = new StringBuilder();
            for (int i = chat.getMessages().size() - NOTIFICATION_MAX_MESSAGES, count = 0; i < chat.getMessages().size() && count <= 8; i++, count ++) {
                if (i < 0) {
                    continue;
                }

                Message message = chat.getMessages().get(i);
                sb.append(message.getContent(context)).append('\n');
            }
            style.bigText(sb.toString().trim());
            style.setSummaryText(context.getString(R.string.notification_messages, chat.getMessages().getSize()));

            return style;
        } else {
            MessagingStyle style = new MessagingStyle(chat.getName());
            style.setConversationTitle(chat.getName());

            for (int i = chat.getMessages().size() - NOTIFICATION_MAX_MESSAGES, count = 0; i < chat.getMessages().size() && count <= 8; i++, count ++) {
                if (i < 0) {
                    continue;
                }

                Message message = chat.getMessages().get(i);
                style.addMessage(message.getContent(context), message.getTimestamp(), message.getSender());
            }

            style.setSummaryText(context.getString(R.string.notification_messages, chat.getMessages().getSize()));

            return style;
        }
    }

    /**
     * 创建通知的回复动作。
     *
     * @param context Context
     * @param requestCode PendingIntent 的 requestId
     * @param chat 对应的 Chat
     * @return NotificationCompat.Action
     */
    private static NotificationCompat.Action createReplyAction(Context context, int requestCode, Chat chat) {
        Intent intent = FFMBroadcastReceiver.replyIntent(chat);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        String replyLabel = context.getString(R.string.notification_action_reply, chat.getName());
        RemoteInput remoteInput = new RemoteInput.Builder(NOTIFICATION_INPUT_KEY)
                .setLabel(replyLabel)
                .build();

        return new NotificationCompat.Action.Builder(R.drawable.ic_reply_24dp, replyLabel, pendingIntent)
                .addRemoteInput(remoteInput)
                .build();
    }

    /**
     * 分组消息的头部
     **/
    private void notifyGroupSummary(Context context, Chat chat, NotificationBuilder nb) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = createBuilder(context, null)
                .setSubText(String.format(context.getString(R.string.notification_messages_multi_sender), nb.getMessageCount(), nb.getSendersCount()))
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis())
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .setContentIntent(NotificationBuilder.createContentIntent(context, 0, null))
                .setDeleteIntent(NotificationBuilder.createDeleteIntent(context, 0, null));

        notificationManager.notify(NOTIFICATION_ID_GROUP_SUMMARY, builder.build());
    }

    /**
     * 返回一个设置了 SmallIcon 等任何通知都相同的属性的 NotificationCompat.Builder
     * 同时设置浮动通知 / LED / 震动等
     *
     * @param chat 聊天内容
     *
     * @return NotificationCompat.Builder
     **/
    @Override
    public NotificationCompat.Builder createBuilder(Context context, @Nullable Chat chat) {
        Profile profile = FFMSettings.getProfile();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_GROUPS)
                .setSmallIcon(profile.getNotificationIcon())
                .setVisibility(Notification.VISIBILITY_PRIVATE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//Android M+
            builder = builder.setColor(context.getColor(R.color.colorServerNotification));
        } else {
            builder = builder.setColor(context.getResources().getColor(R.color.colorServerNotification));
        }

        if (FFMApplication.get(context).isSystem()) {
            Bundle extras = new Bundle();
            extras.putString("android.substName", context.getString(profile.getDisplayName()));
            builder.addExtras(extras);
        }

        if (chat == null) {
            return builder;
        }

        // @ 消息当作好友消息处理
        boolean isFriend = chat.isFriend() || (chat.isGroup() && chat.getLatestMessage().isAt());
        if (isFriend) {
            builder.setChannelId(NOTIFICATION_CHANNEL_FRIENDS);
        }

        // support library still set them on O
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // sound
            builder.setSound(FFMSettings.getNotificationSound(!isFriend));

            // priority
            int priority = FFMSettings.getNotificationPriority(!isFriend);
            builder.setPriority(priority);

            // vibrate
            int vibrate = FFMSettings.getNotificationVibrate(!isFriend);
            switch (vibrate) {
                case Vibrate.DISABLED:
                    break;
                case Vibrate.DEFAULT:
                    builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE);
                    break;
                case Vibrate.SHORT:
                    builder.setVibrate(new long[]{0, 100, 0, 100});
                    break;
                case Vibrate.LONG:
                    builder.setVibrate(new long[]{0, 1000});
                    break;
            }

            // lights
            if (FFMSettings.getNotificationLight(!isFriend)
                    && priority >= NotificationCompat.PRIORITY_DEFAULT) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//Android M+
                    builder.setLights(context.getColor(R.color.colorNotification), 1000, 1000);
                } else {
                    builder.setLights(context.getResources().getColor(R.color.colorNotification), 1000, 1000);
                }
            }
        }

        return builder;
    }

    @Override
    void clear(Chat chat, NotificationBuilder nb) {
        int id = (int) chat.getUniqueId();

        nb.getNotificationManager().cancel(id);

        boolean clearGroup = true;
        //TODO HELP NEEDED: Get active notifications under SDK 23
        /*
        for (StatusBarNotification sbn : nb.getNotificationManager().getActiveNotifications()) {
            if (sbn.getId() != NOTIFICATION_ID_SYSTEM
                    && sbn.getId() != NOTIFICATION_ID_GROUP_SUMMARY) {
                clearGroup = false;
            }
        }*/
        if (clearGroup) {
            nb.getNotificationManager().cancel(NOTIFICATION_ID_GROUP_SUMMARY);
        }
    }
}
