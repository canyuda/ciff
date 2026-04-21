package com.ciff.chat.dto;

import lombok.Data;

import java.util.List;

/**
 * Extra metadata stored in t_chat_message.metadata (JSON).
 */
@Data
public class ChatMessageMetadata {

    /** RAG reference document IDs */
    private List<Long> ragDocIds;
}
