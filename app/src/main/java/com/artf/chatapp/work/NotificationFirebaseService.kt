package com.artf.chatapp.work

import com.artf.chatapp.App
import com.artf.chatapp.model.Message
import com.artf.chatapp.model.User
import com.artf.chatapp.utils.convertToString
import com.artf.chatapp.utils.mapper.RemoteMessageMapper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotificationFirebaseService : FirebaseMessagingService() {

    private val firebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val dbRefUsers by lazy { firebaseFirestore.collection("users") }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        remoteMessage.data.isNotEmpty().let {
            val msg = RemoteMessageMapper.map(remoteMessage.data)
            if (msg.senderId == App.receiverId) return
            GlobalScope.launch { createNotifications(msg) }
        }
    }

    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)
        val refreshedToken = FirebaseInstanceId.getInstance().instanceId.result?.token
    }

    private suspend fun createNotifications(message: Message) {
        val senderId = message.senderId ?: return

        val documentSnapshot = dbRefUsers.document(senderId).get().await()
        val user = documentSnapshot.toObject(User::class.java) ?: return

        NotificationUtils.makeStatusNotification(
            context = applicationContext,
            notificationId = senderId.hashCode(),
            userString = convertToString(user),
            senderName = user.username,
            senderPhotoUrl = user.photoUrl,
            message = getNotificationText(message)
        )
    }

    private fun getNotificationText(message: Message): String {
        var text = ""
        message.audioUrl?.let { text = "\uD83C\uDFA4 Record" }
        message.photoUrl?.let { text = "\uD83D\uDCF7 Photo" }
        message.text?.let { text = it }
        return text
    }
}