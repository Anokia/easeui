package com.hyphenate.easeui.utils;

import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMCmdMessageBody;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.easeui.EaseConstant;
import com.hyphenate.util.EMLog;

/**
 * 消息处理工具类，主要做谢谢EMMessage对象的处理
 */
public class EaseMessageUtils {

    /**
     * 发送一条撤回消息的透传，这里需要和接收方协商定义，通过一个透传，并加上扩展去实现消息的撤回
     *
     * @param message 需要撤回的消息
     * @param callBack 发送消息的回调，通知调用方发送撤回消息的结果
     */
    public static void sendRecallMessage(EMMessage message, final EMCallBack callBack) {
        boolean result = false;
        // 获取当前时间，用来判断后边撤回消息的时间点是否合法，这个判断不需要在接收方做，
        // 因为如果接收方之前不在线，很久之后才收到消息，将导致撤回失败
        long currTime = System.currentTimeMillis();
        long msgTime = message.getMsgTime();
        // 判断当前消息的时间是否已经超过了限制时间，如果超过，则不可撤回消息
        if (currTime < msgTime || (currTime - msgTime > EaseConstant.TIME_RECALL)) {
            callBack.onError(EaseConstant.ERROR_I_RECALL_TIME,
                    EaseConstant.ERROR_S_RECALL_TIME);
            return;
        }
        // 获取消息 id，作为撤回消息的参数
        String msgId = message.getMsgId();
        // 创建一个CMD 类型的消息，将需要撤回的消息通过这条CMD消息发送给对方
        EMMessage cmdMessage = EMMessage.createSendMessage(EMMessage.Type.CMD);
        // 判断下消息类型，如果是群聊就设置为群聊
        if (message.getChatType() == EMMessage.ChatType.GroupChat) {
            cmdMessage.setChatType(EMMessage.ChatType.GroupChat);
        }
        // 设置消息接收者
        cmdMessage.setTo(message.getTo());
        // 创建CMD 消息的消息体 并设置 action 为 recall
        String action = EaseConstant.REVOKE_FLAG;
        EMCmdMessageBody body = new EMCmdMessageBody(action);
        cmdMessage.addBody(body);
        // 设置消息的扩展为要撤回的 msgId
        cmdMessage.setAttribute(EaseConstant.MSG_ID, msgId);
        // 确认无误，开始发送撤回消息的透传
        cmdMessage.setMessageStatusCallback(new EMCallBack() {
            @Override public void onSuccess() {
                callBack.onSuccess();
            }

            @Override public void onError(int i, String s) {
                callBack.onError(i, s);
            }

            @Override public void onProgress(int i, String s) {
            }
        });
        // 准备工作完毕，发送消息
        EMClient.getInstance().chatManager().sendMessage(cmdMessage);
    }

    /**
     * 收到撤回消息，这里需要和发送方协商定义，通过一个透传，并加上扩展去实现消息的撤回
     *
     * @param cmdMessage 收到的透传消息，包含需要撤回的消息的 msgId
     * @return 返回撤回结果是否成功
     */
    public static boolean receiveRecallMessage(EMMessage cmdMessage) {
        boolean result = false;
        EMConversation conversation = EMClient.getInstance().chatManager().getConversation(cmdMessage.getFrom());
        // 从cmd扩展中获取要撤回消息的id
        String msgId = cmdMessage.getStringAttribute(EaseConstant.MSG_ID, null);
        if (msgId == null) {
            return result;
        }
        // 根据得到的msgId 去本地查找这条消息，如果本地已经没有这条消息了，就不用撤回
        EMMessage message = EMClient.getInstance().chatManager().getMessage(msgId);
        EMLog.e("message", message.toString());
        if (message == null) {
            return result;
        }

        // 设置扩展为撤回消息类型，是为了区分消息的显示
        message.setAttribute(EaseConstant.REVOKE_FLAG, true);
        // 更新消息
        result = EMClient.getInstance().chatManager().updateMessage(message);
        conversation.getMessage(msgId,true);

        return result;
    }

    /**
     * 发送输入状态，这里通过cmd消息来进行发送，告知对方自己正在输入
     *
     * @param to 接收方的名字
     */
    public static void sendInputStatusMessage(String to) {
        EMMessage cmdMessage = EMMessage.createSendMessage(EMMessage.Type.CMD);
        cmdMessage.setTo(to);
        // 创建CMD 消息的消息体 并设置 action 为输入状态
        EMCmdMessageBody body = new EMCmdMessageBody(EaseConstant.INPUT_TYPE);
        cmdMessage.addBody(body);
        // 确认无误，开始表示发送输入状态的透传
        EMClient.getInstance().chatManager().sendMessage(cmdMessage);
    }

    /**
     * 保存一条加入群组通知的消息
     * @param groupId
     * @param notifyContext
     */
    public static void saveGroupNotifyMessage(String groupId, String notifyContext){
        // 根据groupid 和当前时间组成 msgId
        String msgId = groupId+System.currentTimeMillis();

        // 创建一条接收消息，用来保存申请信息
        EMMessage message = EMMessage.createReceiveMessage(EMMessage.Type.TXT);
        // 保存消息内容，用于直接显示
        EMTextMessageBody body = new EMTextMessageBody(notifyContext);
        message.addBody(body);
        // 加入或退出的username
//        message.setAttribute(MLConstants.ML_ATTR_USERNAME, username);
//        // 设置理由
//        message.setAttribute(MLConstants.ML_ATTR_REASON, reason);
//        // 申请与通知类型
//        message.setAttribute(MLConstants.ML_ATTR_TYPE, MLConstants.ML_APPLY_TYPE_USER);
//        // 设置当前申请信息状态
//        message.setAttribute(MLConstants.ML_ATTR_STATUS, "");
//        // 设置消息发送方
//        message.setFrom(MLConstants.ML_CONVERSATION_ID_APPLY);
        // 设置msgId
        message.setMsgId(msgId);
        // 将消息保存到本地和内存
        EMClient.getInstance().chatManager().saveMessage(message);
    }

}
