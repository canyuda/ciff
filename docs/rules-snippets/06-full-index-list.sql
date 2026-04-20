-- t_user
UNIQUE INDEX uk_user_username (username)

-- t_provider
INDEX idx_provider_status (status)

-- t_model
INDEX idx_model_provider_id (provider_id)

-- t_agent
INDEX idx_agent_user_id (user_id)
INDEX idx_agent_model_id (model_id)
INDEX idx_agent_workflow_id (workflow_id)
INDEX idx_agent_status (status)

-- t_agent_tool
INDEX idx_at_agent_id (agent_id)
INDEX idx_at_tool_id (tool_id)
UNIQUE INDEX uk_at_agent_tool (agent_id, tool_id)

-- t_agent_knowledge
INDEX idx_ak_agent_id (agent_id)
INDEX idx_ak_knowledge_id (knowledge_id)
UNIQUE INDEX uk_ak_agent_knowledge (agent_id, knowledge_id)

-- t_tool
INDEX idx_tool_status (status)

-- t_workflow
INDEX idx_workflow_user_id (user_id)

-- t_knowledge
INDEX idx_knowledge_user_id (user_id)

-- t_knowledge_document
INDEX idx_kd_knowledge_id (knowledge_id)
INDEX idx_kd_status (status)

-- t_conversation
INDEX idx_conv_user_updated (user_id, update_time DESC)
INDEX idx_conv_agent_id (agent_id)

-- t_chat_message
INDEX idx_msg_conv_created (conversation_id, create_time)

-- t_api_key
UNIQUE INDEX uk_api_key_prefix (key_prefix)
INDEX idx_api_key_user_id (user_id)
INDEX idx_api_key_agent_id (agent_id)
INDEX idx_api_key_status (status)
