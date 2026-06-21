package com.community.common.constant;

/**
 * RocketMQ Topic / Consumer Group 常量
 */
public interface MQConstant {

    String SEND_MSG_TOPIC = "community_send_msg_topic";
    String PUSH_TOPIC = "community_push_topic";

    String LOGIN_MSG_TOPIC = "user_login_send_msg";
    String SCAN_MSG_TOPIC = "user_scan_send_msg";

    String MSG_SEND_CONSUMER_GROUP = "msg_send_consumer_group";
    String PUSH_CONSUMER_GROUP = "push_consumer_group";
    String LOGIN_MSG_GROUP = "user_login_send_msg_group";
    String SCAN_MSG_GROUP = "user_scan_send_msg_group";
}
