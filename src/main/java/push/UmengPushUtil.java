package push;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hzh
 */
@Data
@Component
@Configuration
public class UmengPushUtil {
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
     * @Description 测试广播
     */
    @SuppressWarnings("unchecked")
    @Transactional(rollbackFor = Exception.class)
    public List testSendBroadcast(HashMap<String, String> map) throws Exception {
        IOSBroadcast iosBroadcast = new IOSBroadcast(iosAppkey, iosAppMasterSecret);
        AndroidBroadcast androidBroadcast = new AndroidBroadcast(androidAppkey, androidAppMasterSecret);
        //IOS配置
        HashMap<String, String> hashMap = new HashMap<>(3);
        hashMap.put("titile", map.get("titile"));
        hashMap.put("subtitle", map.get("titile"));
        hashMap.put("body", getContent(map.get("content") + ""));
        iosBroadcast.setAlert(JSON.toJSONString(hashMap));
        iosBroadcast.setBadge(0);
        iosBroadcast.setSound("default");
        //安卓配置
        androidBroadcast.setTicker(map.get("title"));
        androidBroadcast.setTitle(map.get("title"));
        androidBroadcast.setText(getContent(map.get("content") + ""));
        androidBroadcast.goAppAfterOpen();
        androidBroadcast.setDisplayType(AndroidNotification.DisplayType.NOTIFICATION);
        if (StringUtils.isNotEmpty(map.get("sendTime"))) {
            androidBroadcast.setStartTime(map.get("sendTime"));
            iosBroadcast.setStartTime(map.get("sendTime"));
        }
        //设置测试模式
        iosBroadcast.setTestMode();
        androidBroadcast.setTestMode();
        // For how to register a test device, please see the developer doc.
        androidBroadcast.setProductionMode();
        // Set customized fields
        androidBroadcast.setExtraField(map.get("title") + "", getContent(map.get("content") + ""));
        map.forEach((k, v) -> {
            try {
                iosBroadcast.setCustomizedField(k + "", v + "");
                androidBroadcast.setExtraField(k + "", v + "");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        String iosTaskId = client.send(iosBroadcast);
        String androidTaskId = client.send(androidBroadcast);
        List<String> taskId = Arrays.asList(androidTaskId, iosTaskId);
        return taskId;
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
     * 友盟组播推送
     *
     * @throws Exception
     */
    public void sendGroupcast(HashMap<String, String> map) throws Exception {
        //android组播
        AndroidGroupcast androidGroupcast = new AndroidGroupcast(androidAppkey, androidAppMasterSecret);
        //当type=groupcast时，必填，用户筛选条件，如用户标签、渠道等 filter的内容长度最大为3000B
        androidGroupcast.setFilter(recipient(map.get("userId")));
        // 必填，消息类型: notification(通知)、message(消息)
        androidGroupcast.setDisplayType(AndroidNotification.DisplayType.NOTIFICATION);
        // 必填，通知栏提示文字
        androidGroupcast.setTicker("Android groupcast ticker");
        // 必填，通知标题
        androidGroupcast.setTitle(map.get("title"));
        // 必填，通知文字描述
        androidGroupcast.setText(getContent(map.get("content") + ""));
        if (StringUtils.isNotEmpty(map.get("url"))) {
            androidGroupcast.goActivityAfterOpen(map.get("url"));
        } else {
            androidGroupcast.goAppAfterOpen();
        }
        androidGroupcast.setProductionMode();
        client.send(androidGroupcast);
        
        //ios组播
        IOSGroupcast iosGroupcast = new IOSGroupcast(iosAppkey, iosAppMasterSecret);
        iosGroupcast.setFilter(recipient(map.get("userId")));
        //推送信息
        HashMap<String, String> hashMap = new HashMap<>(3);
        hashMap.put("titile", map.get("titile"));
        hashMap.put("subtitle", map.get("titile"));
        hashMap.put("body", getContent(map.get("content") + ""));
        iosGroupcast.setAlert(JSON.toJSONString(hashMap));
        iosGroupcast.setBadge(0);
        iosGroupcast.setSound("default");
        iosGroupcast.setTestMode();
        client.send(iosGroupcast);
    }
    
    /**
     * @Description Android多播
     */
    public void sendAndroidBroadcast() throws Exception {
        AndroidBroadcast broadcast = new AndroidBroadcast(androidAppkey, androidAppMasterSecret);
        broadcast.setTicker("Android broadcast ticker");
        broadcast.setTitle("中文的title");
        broadcast.setText("Android broadcast text");
        broadcast.goAppAfterOpen();
        broadcast.setDisplayType(AndroidNotification.DisplayType.NOTIFICATION);
        // TODO Set 'production_mode' to 'false' if it's a test device.
        broadcast.setProductionMode();
        // For how to register a test device, please see the developer doc.
        broadcast.setProductionMode();
        // Set customized fields
        broadcast.setExtraField("test", "helloworld");
        client.send(broadcast);
    }
    
    /**
     * @Description Android单播
     */
    public void sendAndroidUnicast() throws Exception {
        AndroidUnicast unicast = new AndroidUnicast(androidAppkey, androidAppMasterSecret);
        // TODO Set your device token
        unicast.setDeviceToken("your device token");
        unicast.setTicker("Android unicast ticker");
        unicast.setTitle("中文的title");
        unicast.setText("Android unicast text");
        unicast.goAppAfterOpen();
        unicast.setDisplayType(AndroidNotification.DisplayType.NOTIFICATION);
        // TODO Set 'production_mode' to 'false' if it's a test device.
        // For how to register a test device, please see the developer doc.
        unicast.setProductionMode();
        // Set customized fields
        unicast.setExtraField("test", "helloworld");
        client.send(unicast);
    }
    
    /**
     * @Description Android组播
     */
    public void sendAndroidGroupcast() throws Exception {
        AndroidGroupcast groupcast = new AndroidGroupcast(androidAppkey, androidAppMasterSecret);
        /*  TODO
         *  Construct the filter condition:
         *  "where":
         *	{
         *		"and":
         *		[
         *			{"tag":"test"},
         *			{"tag":"Test"}
         *		]
         *	}
         */
        JSONObject filterJson = new JSONObject();
        JSONObject whereJson = new JSONObject();
        JSONArray tagArray = new JSONArray();
        JSONObject testTag = new JSONObject();
        JSONObject TestTag = new JSONObject();
        testTag.put("tag", "test");
        TestTag.put("tag", "Test");
        tagArray.add(testTag);
        tagArray.add(TestTag);
        whereJson.put("and", tagArray);
        filterJson.put("where", whereJson);
        System.out.println(filterJson.toString());
        
        groupcast.setFilter(filterJson);
        groupcast.setTicker("Android groupcast ticker");
        groupcast.setTitle("中文的title");
        groupcast.setText("Android groupcast text");
        groupcast.goAppAfterOpen();
        groupcast.setDisplayType(AndroidNotification.DisplayType.NOTIFICATION);
        // TODO Set 'production_mode' to 'false' if it's a test device.
        // For how to register a test device, please see the developer doc.
        groupcast.setProductionMode();
        client.send(groupcast);
    }
    
    /**
     * @Description Android自定义播
     */
    public void sendAndroidCustomizedcast(Map map) throws Exception {
        AndroidCustomizedcast customizedcast = new AndroidCustomizedcast(androidAppkey, androidAppMasterSecret);
        // TODO Set your alias here, and use comma to split them if there are multiple alias.
        // And if you have many alias, you can also upload a file containing these alias, then
        // use file_id to send customized notification.
        customizedcast.setAlias("alias", "alias_type");
        customizedcast.setTicker("Android customizedcast ticker");
        customizedcast.setTitle("中文的title");
        customizedcast.setText("Android customizedcast text");
        customizedcast.goAppAfterOpen();
        customizedcast.setDisplayType(AndroidNotification.DisplayType.NOTIFICATION);
        // For how to register a test device, please see the developer doc.
        customizedcast.setProductionMode();
        client.send(customizedcast);
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
     * @Description IOS多播
     */
    @SuppressWarnings("unchecked")
    @Transactional(rollbackFor = Exception.class)
    public void sendIOSBroadcast(Map map) throws Exception {
        IOSBroadcast broadcast = new IOSBroadcast(iosAppkey, iosAppMasterSecret);
        broadcast.setAlert("IOS 广播测试");
        broadcast.setBadge(0);
        broadcast.setSound("default");
        broadcast.setProductionMode();
        // Set customized fields
        map.forEach((k, v) -> {
            try {
                broadcast.setCustomizedField(k + "", v + "");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        client.send(broadcast);
    }
    
    /**
     * @Description IOS单播
     */
    public void sendIOSUnicast() throws Exception {
        IOSUnicast unicast = new IOSUnicast(iosAppkey, iosAppMasterSecret);
        // TODO Set your device token
        unicast.setDeviceToken("xx");
        unicast.setAlert("IOS 单播测试");
        unicast.setBadge(0);
        unicast.setSound("default");
        // TODO set 'production_mode' to 'true' if your app is under production mode
        unicast.setTestMode();
        // Set customized fields
        unicast.setCustomizedField("test", "helloworld");
        client.send(unicast);
    }
    
    /**
     * @Description IOS 组播
     */
    public void sendIOSGroupcast() throws Exception {
        IOSGroupcast groupcast = new IOSGroupcast(iosAppkey, iosAppMasterSecret);
        /*  TODO
         *  Construct the filter condition:
         *  "where":
         *	{
         *		"and":
         *		[
         *			{"tag":"iostest"}
         *		]
         *	}
         */
        JSONObject filterJson = new JSONObject();
        JSONObject whereJson = new JSONObject();
        JSONArray tagArray = new JSONArray();
        JSONObject testTag = new JSONObject();
        testTag.put("tag", "iostest");
        tagArray.add(testTag);
        whereJson.put("and", tagArray);
        filterJson.put("where", whereJson);
        System.out.println(filterJson.toString());
        
        // Set filter condition into rootJson
        groupcast.setFilter(filterJson);
        groupcast.setAlert("IOS 组播测试");
        groupcast.setBadge(0);
        groupcast.setSound("default");
        // TODO set 'production_mode' to 'true' if your app is under production mode
        groupcast.setTestMode();
        client.send(groupcast);
    }
    
    /**
     * @Description IOS自定义播
     */
    public void sendIOSCustomizedcast(Map map) throws Exception {
        IOSCustomizedcast customizedcast = new IOSCustomizedcast(iosAppkey, iosAppMasterSecret);
        // TODO Set your alias and alias_type here, and use comma to split them if there are multiple alias.
        // And if you have many alias, you can also upload a file containing these alias, then
        // use file_id to send customized notification.
        customizedcast.setAlias("alias", "alias_type");
        customizedcast.setAlert("IOS 个性化测试");
        customizedcast.setBadge(0);
        customizedcast.setSound("default");
        // TODO set 'production_mode' to 'true' if your app is under production mode
        customizedcast.setTestMode();
        client.send(customizedcast);
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
        System.out.println(filterJson.toString());
        return filterJson;
    }
    
    /**
     * 去除内容的标签
     *
     * @param content 内容
     * @return 去掉标签的内容
     */
    public String getContent(String content) {
        return content.replace("<span>", "").replace("<span style='color:#B0C4DE'>", "").replace("</span>", "");
    }
}
