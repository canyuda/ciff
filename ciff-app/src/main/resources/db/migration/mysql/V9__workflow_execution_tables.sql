-- Workflow execution persistence: replaces Redis-based task storage

CREATE TABLE t_workflow_execution (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id         VARCHAR(64)  NOT NULL,
    workflow_id     BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    status          VARCHAR(32)  NOT NULL DEFAULT 'STARTED',
    current_step_id VARCHAR(64),
    current_step_name VARCHAR(128),
    completed_steps INT          NOT NULL DEFAULT 0,
    total_steps     INT          NOT NULL DEFAULT 0,
    inputs          JSON,
    final_outputs   JSON,
    error_message   VARCHAR(1024),
    start_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time        DATETIME,
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_we_workflow_user (workflow_id, user_id),
    INDEX idx_we_task_id (task_id),
    INDEX idx_we_status (status)
);

CREATE TABLE t_workflow_node_execution (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id    BIGINT       NOT NULL,
    step_id         VARCHAR(64)  NOT NULL,
    step_name       VARCHAR(128) NOT NULL,
    step_type       VARCHAR(32)  NOT NULL,
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    inputs          JSON,
    outputs         JSON,
    error_message   VARCHAR(1024),
    start_time      DATETIME,
    end_time        DATETIME,
    duration_ms     INT,
    retry_count     INT          NOT NULL DEFAULT 0,
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_wn_execution_id (execution_id),
    INDEX idx_wn_step_id (step_id)
);
