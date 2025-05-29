//package com.test.messages.demo.Util
//
//import com.test.messages.demo.data.Database.Starred.StarredMessage
//import com.test.messages.demo.data.Model.MessageItem
//
//fun StarredMessage.toMessageItem(): MessageItem {
//    return MessageItem(
//        threadId = thread_id,
//        body = body,
//        sender = sender,
//        number = number,
//        timestamp = timestamp,
//        isRead = true, // default value
//        reciptid = -1, // default or unknown
//        reciptids = "", // default or empty
//        profileImageUrl = profile_image ?: "",
//        isPinned = false, // default unless stored
//        isGroupChat = is_group_chat,
//        isMuted = false, // default unless you store it
//        lastMsgDate = timestamp // you can use timestamp as fallback
//    )
//}
//
