package push;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import push.android.AndroidBroadcast;
import push.android.AndroidCustomizedcast;
import push.android.AndroidFilecast;
import push.android.AndroidGroupcast;
import push.android.AndroidUnicast;
import push.ios.IOSBroadcast;
import push.ios.IOSCustomizedcast;
import push.ios.IOSFilecast;
import push.ios.IOSGroupcast;
import push.ios.IOSUnicast;

import java.util.HashMap;

/**
 * @author hzh
 * @description 友盟推送广播和组播
 * @date 2020/4/30
 */
@Data
@Component
@Configuration
public class UmengPushUtils {
    
    /**
     * ios Appkey
     */
    @Value("${umeng.push.ios.appkey}")
    private String iosAppkey;
    /**
     * android Appkey
     */
    @Value("${umeng.push.android.appkey}")
    private String androidAppkey;
    /**
     * ios appMasterSecret
     */
    @Value("${umeng.push.ios.appMasterSecret}")
    private String iosAppMasterSecret;
    /**
     * android appMasterSecret
     */
    @Value("${umeng.push.android.appMasterSecret}")
    private String androidAppMasterSecret;
    /**
     * 友盟消息取消接口
     */
    private String cancelUrl = "https://msgapi.umeng.com/api/cancel";
    /**
     * 冒充火狐浏览器
     */
    private PushClient client = new PushClient();
    
    /**
     * 日志.
     */
    private static Logger logger = LoggerFactory.getLogger(UmengPushUtils.class);
    
    
    /**
     * 安卓发送广播
     *
     * @param map 发送信息
     * @return 推送id
     */
    public String sendBroadcastAndroid(HashMap<String, String> map) {
        /**
         * // Android
         * {
         *     "appkey":"你的appkey",
         *     "timestamp":"你的timestamp",
         *     "type":"broadcast",
         *     "payload":
         *     {
         *         "display_type": "notification", // 通知，notification
         *         "body":
         *         {
         *             "ticker":"测试提示文字",
         *             "title":"测试标题",
         *             "text":"测试文字描述",
         *             "after_open" : "go_app"
         *         }
         *     },
         *     "policy":
         *     {
         *         "start_time": "2013-10-29 12:00:00", //定时发送
         *         "expire_time": "2013-10-30 12:00:00"
         *     },
         *     "description":"测试广播通知-Android"
         * }
         */
        try {
            AndroidBroadcast androidBroadcast = new AndroidBroadcast(androidAppkey, androidAppMasterSecret);
            //安卓配置
            androidBroadcast.setTicker(map.get("title"));
            androidBroadcast.setTitle(map.get("title"));
            androidBroadcast.setText(map.get("content"));
            androidBroadcast.goAppAfterOpen();
            androidBroadcast.setDisplayType(AndroidNotification.DisplayType.NOTIFICATION);
            if (StringUtils.isNotEmpty(map.get("sendTime"))) {
                androidBroadcast.setStartTime(map.get("sendTime"));
            }
            //设置生产模式
            androidBroadcast.setProductionMode();
            androidBroadcast.setDescription("测试广播通知-Android");
            return client.send(androidBroadcast);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("安卓组播发送失败 map" + map.toString());
            return null;
        }
    }
    
    /**
     * ios发送广播推送消息
     *
     * @param map 发送内容
     * @return 推送id
     */
    public String sendBroadcastIos(HashMap<String, String> map) {
        /**
         * // iOS
         * {
         *    "appkey":"你的appkey",
         *    "timestamp":"你的timestamp",
         *    "type":"broadcast",
         *    "payload":
         *    {
         *     "aps":{          // 苹果必填字段
         *         "alert":""/{ // 当content-available=1时(静默推送)，可选; 否则必填。
         *                      // 可为JSON类型和字符串类型
         *             "title":"title",
         *             "subtitle":"subtitle",
         *             "body":"body"
         *         }
         *     }
         *     "k1":"v1",   // 自定义key-value
         *     "k2":"v2",
         *     ...
         *    },
         *    "policy":
         *    {
         *        "start_time": "2013-10-29 12:00:00", //定时发送
         *        "expire_time": "2013-10-30 12:00:00"
         *    },
         *    "description":"测试广播通知-iOS"
         * }
         */
        try {
            IOSBroadcast iosBroadcast = new IOSBroadcast(iosAppkey, iosAppMasterSecret);
            //IOS配置
            iosBroadcast.setAlert(getJsonObject(map));
            iosBroadcast.setBadge(0);
            iosBroadcast.setSound("default");
            if (StringUtils.isNotEmpty(map.get("sendTime"))) {
                iosBroadcast.setStartTime(map.get("sendTime"));
            }
            //设置生产模式
            iosBroadcast.setProductionMode();
            iosBroadcast.setDescription("测试广播通知-IOS");
            return client.send(iosBroadcast);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("IOS广播发送失败 map" + map.toString());
            return null;
        }
    }
    
    
    /**
     * 安卓单播
     *
     * @param map 发送消息
     * @throws Exception 异常
     */
    public void sendAndroidUnicast(HashMap<String, String> map) throws Exception {
        /**
         * // Android
         * {
         *     "appkey":"你的appkey",
         *     "timestamp":"你的timestamp",
         *     "type":"unicast",
         *     "production_mode":"false",
         *     "device_tokens":"xx(Android为44位)",
         *     "payload": {
         *         "display_type": "message",   // 消息，message
         *         "body": {
         *             "custom":"自定义custom"/{} // message类型只需填写custom即可，可以是字符串或JSON。
         *         }
         *     },
         *     "policy": {
         *         "expire_time": "2013-10-30 12:00:00"
         *     },
         *     "description":"测试单播消息-Android"
         * }
         */
        AndroidUnicast unicast = new AndroidUnicast(androidAppkey, androidAppMasterSecret);
        unicast.setDeviceToken("your device token");
        unicast.setTicker(map.get("title"));
        unicast.setTitle(map.get("title"));
        unicast.setText(map.get("content"));
        unicast.goAppAfterOpen();
        unicast.setDisplayType(AndroidNotification.DisplayType.NOTIFICATION);
        //设置生产环境
        unicast.setProductionMode();
        unicast.setDescription("测试单播消息-Android");
        unicast.setExtraField("test", "helloworld");
        client.send(unicast);
    }
    
    
    /**
     * ios单播
     *
     * @param map 发送消息
     * @throws Exception 异常
     */
    public void sendIOSUnicast(HashMap<String, String> map) throws Exception {
        /**
         * // iOS
         * {
         *     "appkey":"你的appkey",
         *     "timestamp":"你的timestamp",
         *     "type":"unicast",
         *     "production_mode":"false",
         *     "device_tokens":"xx(iOS为64位)",
         *     "payload": {
         *         "aps":{    // 苹果必填字段
         *             "alert":""/{    // 当content-available=1时(静默推送)，可选; 否则必填。
         *                             // 可为JSON类型和字符串类型
         *                 "title":"title",
         *                 "subtitle":"subtitle",
         *                 "body":"body"
         *             }
         *         }
         *         "k1":"v1",    // 自定义key-value, key不可以是"d","p"
         *         "k2":"v2",
         *         ...
         *     },
         *     "policy": {
         *         "expire_time":"2013-10-30 12:00:00"
         *     },
         *     "description":"测试单播消息-iOS"
         * }
         */
        IOSUnicast unicast = new IOSUnicast(iosAppkey, iosAppMasterSecret);
        unicast.setDeviceToken("xx");
        unicast.setAlert(getJsonObject(map));
        unicast.setBadge(0);
        unicast.setSound("default");
        //设置生产模式
        unicast.setProductionMode();
        // Set customized fields
        unicast.setCustomizedField("test", "helloworld");
        unicast.setDescription("测试单播消息-iOS");
        client.send(unicast);
    }
    
    
    /**
     * 安卓组播
     *
     * @param map 发送消息
     */
    public void sendGroupcastAndroid(HashMap<String, String> map) {
        /**
         * // Android
         * {
         *     "appkey":"你的appkey",
         *     "timestamp":"你的timestamp",
         *     "type":"groupcast",
         *     "filter":
         *     {
         *       "where":
         *       {
         *         "and": [{"app_version": "1.0"}] // 发送给app_version为1.0的用户群
         *       }
         *     },
         *     "payload":
         *     {
         *         "display_type": "notification", // 通知，notification
         *         "body":
         *         {
         *             "ticker":"测试提示文字",
         *             "title":"测试标题",
         *             "text":"测试文字描述",
         *             "after_open": "go_url",
         *             "url": "http://message.umeng.com"
         *         }
         *     },
         *     "policy":
         *     {
         *         "expire_time": "2013-10-30 12:00:00"
         *     },
         *     "description":"测试组播通知-Android"
         * }
         */
        try {
            //android组播
            AndroidGroupcast androidGroupcast = new AndroidGroupcast(androidAppkey, androidAppMasterSecret);
            //当type=groupcast时，必填，用户筛选条件，如用户标签、渠道等 filter的内容长度最大为3000B
            androidGroupcast.setFilter(recipient(map.get("userId")));
            // 必填，消息类型: notification(通知)、message(消息)
            androidGroupcast.setDisplayType(AndroidNotification.DisplayType.NOTIFICATION);
            // 必填，通知栏提示文字
            androidGroupcast.setTicker(map.get("title"));
            // 必填，通知标题
            androidGroupcast.setTitle(map.get("title"));
            // 必填，通知文字描述
            androidGroupcast.setText(map.get("content"));
            if (StringUtils.isNotEmpty(map.get("url"))) {
                androidGroupcast.goActivityAfterOpen(map.get("url"));
            } else {
                androidGroupcast.goAppAfterOpen();
            }
            androidGroupcast.setProductionMode();
            androidGroupcast.setDescription("测试组播通知-Android");
            client.send(androidGroupcast);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("安卓组播消息失败 map" + map.toString());
        }
    }
    
    /**
     * ios组播推送消息
     *
     * @param map 发送消息内容
     */
    public void sendGroupcastIos(HashMap<String, String> map) {
        /**
         * // iOS
         * {
         *    "appKey":"你的appkey",
         *    "timestamp":"你的timestamp",
         *    "type":"groupcast",
         *    "filter":
         *     {
         *       "where":
         *       {
         *         "and": [{"app_version": "1.0"}]
         *       }
         *     },
         *    "payload":
         *    {
         *     "aps":{          // 苹果必填字段
         *         "alert":""/{ // 当content-available=1时(静默推送)，可选; 否则必填。
         *                      // 可为JSON类型和字符串类型
         *             "title":"title",
         *             "subtitle":"subtitle",
         *             "body":"body"
         *         }
         *     }
         *     "k1":"v1",   // 自定义key-value
         *     "k2":"v2",
         *     ...
         *    },
         *    "policy":
         *    {
         *        "expire_time": "2013-10-30 12:00:00"
         *    },
         *    "description":"测试组播通知-iOS"
         * }
         */
        try {
            //ios组播
            IOSGroupcast iosGroupcast = new IOSGroupcast(iosAppkey, iosAppMasterSecret);
            iosGroupcast.setFilter(recipient(map.get("userId")));
            //推送信息
            iosGroupcast.setAlert(getJsonObject(map));
            iosGroupcast.setBadge(0);
            iosGroupcast.setSound("default");
            iosGroupcast.setProductionMode();
            iosGroupcast.setDescription("测试组播通知-ios");
            client.send(iosGroupcast);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("ios组播消息失败 map" + map.toString());
        }
    }
    
    
    /**
     * Android自定义播
     *
     * @param map 发送消息内容
     * @throws Exception 异常
     */
    public void sendAndroidCustomizedcast(HashMap<String, String> map) throws Exception {
        /**
         * // Android
         * {
         *     "appkey":"你的appkey",
         *     "timestamp":"你的timestamp",
         *     "type":"customizedcast",
         *     "alias": "你的alias", //不能超过500个，多个alias以英文逗号风格
         *     "alias_type":"alias对应的type(SDK调用addAlias(alias,alis_type)接口指定的alias_type)",
         *     "payload":
         *     {
         *         "display_type": "notification", // 通知，notification
         *         "body":
         *         {
         *             "ticker":"测试提示文字",
         *             "title":"测试标题",
         *             "text":"测试文字描述",
         *             "after_open": "go_activity",
         *             "activity": "xxx"
         *         }
         *     },
         *     "policy":
         *     {
         *         "expire_time": "2013-10-30 12:00:00"
         *     },
         *     "description":"测试alias通知-Android"
         * }
         */
        AndroidCustomizedcast customizedcast = new AndroidCustomizedcast(androidAppkey, androidAppMasterSecret);
        // TODO Set your alias here, and use comma to split them if there are multiple alias.
        // And if you have many alias, you can also upload a file containing these alias, then
        // use file_id to send customized notification.
        customizedcast.setAlias(map.get("userId"), "android");
        customizedcast.setTicker(map.get("title"));
        customizedcast.setTitle(map.get("title"));
        customizedcast.setText(map.get("content"));
        customizedcast.goAppAfterOpen();
        customizedcast.setDisplayType(AndroidNotification.DisplayType.NOTIFICATION);
        customizedcast.setProductionMode();
        customizedcast.setDescription("测试alias通知-Android");
        client.send(customizedcast);
    }
    
    
    /**
     * IOS自定义播
     *
     * @param map 发送消息内容
     */
    public void sendIOSCustomizedcast(HashMap<String, String> map) {
        /**
         * // iOS
         * {
         *    "appKey":"你的appkey",
         *    "timestamp":"你的timestamp",
         *    "type":"customizedcast",
         *    "alias": "你的alias", //不能超过500个，多个alias以英文逗号分隔。
         *    "alias_type":"alias对应的type",
         *    "payload":
         *    {
         *     "aps":{          // 苹果必填字段
         *         "alert":""/{ // 当content-available=1时(静默推送)，可选; 否则必填。
         *                      // 可为JSON类型和字符串类型
         *             "title":"title",
         *             "subtitle":"subtitle",
         *             "body":"body"
         *         }
         *     }
         *     "k1":"v1",   // 自定义key-value
         *     "k2":"v2",
         *     ...
         *    },
         *    "policy":
         *    {
         *        "expire_time": "2013-10-30 12:00:00"
         *    }
         *    "description":"测试alias通知-iOS"
         * }
         */
        try {
            IOSCustomizedcast customizedcast = new IOSCustomizedcast(iosAppkey, iosAppMasterSecret);
            customizedcast.setAlias(map.get("userId"), "iOS");
            customizedcast.setAlert(getJsonObject(map));
            customizedcast.setBadge(0);
            customizedcast.setSound("default");
            customizedcast.setProductionMode();
            client.send(customizedcast);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("IOS自定义播发送失败 map" + map.toString());
        }
    }
    
    /**
     * @Description Android自定义文件广播
     */
    public void sendAndroidCustomizedcastFile() throws Exception {
        AndroidCustomizedcast customizedcast = new AndroidCustomizedcast(androidAppkey, androidAppMasterSecret);
        // TODO Set your alias here, and use comma to split them if there are multiple alias.
        // And if you have many alias, you can also upload a file containing these alias, then
        // use file_id to send customized notification.
        String fileId = client.uploadContents(androidAppkey, androidAppMasterSecret, "aa" + "\n" + "bb" + "\n" + "alias");
        customizedcast.setFileId(fileId, "alias_type");
        customizedcast.setTicker("Android customizedcast ticker");
        customizedcast.setTitle("中文的title");
        customizedcast.setText("Android customizedcast text");
        customizedcast.goAppAfterOpen();
        customizedcast.setDisplayType(AndroidNotification.DisplayType.NOTIFICATION);
        // TODO Set 'production_mode' to 'false' if it's a test device.
        // For how to register a test device, please see the developer doc.
        customizedcast.setProductionMode();
        client.send(customizedcast);
    }
    
    
    
    
    /**
     * @Description Android文件播
     */
    public void sendAndroidFilecast() throws Exception {
        AndroidFilecast filecast = new AndroidFilecast(androidAppkey, androidAppMasterSecret);
        // TODO upload your device tokens, and use '\n' to split them if there are multiple tokens
        String fileId = client.uploadContents(androidAppkey, androidAppMasterSecret, "aa" + "\n" + "bb");
        filecast.setFileId(fileId);
        filecast.setTicker("Android filecast ticker");
        filecast.setTitle("中文的title");
        filecast.setText("Android filecast text");
        filecast.goAppAfterOpen();
        filecast.setDisplayType(AndroidNotification.DisplayType.NOTIFICATION);
        client.send(filecast);
    }
    
    /**
     * @Description IOS 文件播
     */
    public void sendIOSFilecast() throws Exception {
        IOSFilecast filecast = new IOSFilecast(iosAppkey, iosAppMasterSecret);
        // TODO upload your device tokens, and use '\n' to split them if there are multiple tokens
        String fileId = client.uploadContents(iosAppkey, iosAppMasterSecret, "aa" + "\n" + "bb");
        filecast.setFileId(fileId);
        filecast.setAlert("IOS 文件播测试");
        filecast.setBadge(0);
        filecast.setSound("default");
        // TODO set 'production_mode' to 'true' if your app is under production mode
        filecast.setTestMode();
        client.send(filecast);
    }
    
    /**
     * 友盟推送消息取消
     *
     * @param type   android或者ios
     * @param taskId 友盟推送id
     */
    public boolean cancelNotice(String type, String taskId) throws Exception {
        HashMap<String, String> hashMap = new HashMap<>(3);
        String timestamp = Integer.toString((int) (System.currentTimeMillis() / 1000));
        if ("android".equals(type)) {
            hashMap.put("appkey", androidAppkey);
            hashMap.put("timestamp", timestamp);
            hashMap.put("task_id", taskId);
            //签名  Sign=MD5($http_method$url$post-body$app_master_secret);
            return client.cancelNotice(cancelUrl, hashMap, androidAppMasterSecret, timestamp);
        } else if ("ios".equals(type)) {
            hashMap.put("appkey", iosAppkey);
            hashMap.put("timestamp", timestamp);
            hashMap.put("task_id", taskId);
            //取消友盟推送接口
            return client.cancelNotice(cancelUrl, hashMap, iosAppMasterSecret, timestamp);
        }
        return false;
    }
    
    /**
     * 组装接受者
     *
     * @param recipientId 接受者id
     * @return 接受者集合
     */
    public JSONObject recipient(String recipientId) {
        JSONObject filterJson = new JSONObject();
        JSONObject whereJson = new JSONObject();
        JSONArray tagArray = new JSONArray();
        JSONObject testTag = new JSONObject();
        testTag.put("tag", recipientId);
        tagArray.add(testTag);
        whereJson.put("and", tagArray);
        filterJson.put("where", whereJson);
        return filterJson;
    }
    
    /**
     * 组装ios参数
     *
     * @param map hashmap 包含userId title content
     * @return ios的alert
     */
    private org.json.JSONObject getJsonObject(HashMap<String, String> map) {
        org.json.JSONObject object = new org.json.JSONObject();
        object.put("title", map.get("title"));
        object.put("subtitle", "");
        object.put("body", map.get("content"));
        return object;
    }
    
}
