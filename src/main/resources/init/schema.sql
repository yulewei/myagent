-- 用户会话表
drop table if exists t_session;
create table t_session
(
    id          varchar(32),          -- 会话ID
    model_id    varchar(32) not null, -- 模型ID
    tools       text,                 -- 工具列表
    title       varchar(30) not null, -- 会话标题
    create_time bigint      not null, -- 创建时间
    update_time bigint      not null, -- 更新时间
    delete_time bigint,               -- 删除时间
    primary key (id, delete_time)
);
create index t_session_idx_create_time on t_session (create_time);
create index t_session_idx_update_time on t_session (update_time);

-- 用户会话消息表
drop table if exists t_session_message;
create table t_session_message
(
    id          varchar(32) primary key, -- 消息ID
    parent_id   varchar(32) not null,    -- 父消息ID
    session_id  varchar(32) not null,    -- 会话ID
    model_id    varchar(32) not null,    -- 模型ID
    type        varchar(10) not null,    -- 消息类型：user、assistant、system、tool
    json        text        not null,    -- 消息内容，json格式
    create_time bigint      not null     -- 消息创建时间
);
create index t_session_message_idx_session_id on t_session_message (session_id);
create index t_session_message_idx_create_time on t_session_message (create_time);