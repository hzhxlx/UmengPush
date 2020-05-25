package push;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;

/**
 * 友盟IOS推送组装类
 */
public abstract class IOSNotification extends UmengNotification {
    /**
     * @Description Keys can be set in the aps level
     */
    protected static final HashSet<String> APS_KEYS = new HashSet<>(Arrays.asList("alert", "badge", "sound", "content-available"));
    
    @Override
    public boolean setPredefinedKeyValue(String key, Object value) throws Exception {
        if (ROOT_KEYS.contains(key)) {
            // This key should be in the root level
            rootJson.put(key, value);
        } else if (APS_KEYS.contains(key)) {
            // This key should be in the aps level
            JSONObject apsJson;
            JSONObject payloadJson;
            if (rootJson.has("payload")) {
                payloadJson = rootJson.getJSONObject("payload");
            } else {
                payloadJson = new JSONObject();
                rootJson.put("payload", payloadJson);
            }
            if (payloadJson.has("aps")) {
                apsJson = payloadJson.getJSONObject("aps");
            } else {
                apsJson = new JSONObject();
                payloadJson.put("aps", apsJson);
            }
            apsJson.put(key, value);
        } else if (POLICY_KEYS.contains(key)) {
            // This key should be in the body level
            JSONObject policyJson;
            if (rootJson.has("policy")) {
                policyJson = rootJson.getJSONObject("policy");
            } else {
                policyJson = new JSONObject();
                rootJson.put("policy", policyJson);
            }
            policyJson.put(key, value);
        } else {
            if (Objects.equals(key, "payload") || Objects.equals(key, "aps") || Objects.equals(key, "policy")) {
                throw new Exception("You don't need to set value for " + key + " , just set values for the sub keys in it.");
            } else {
                throw new Exception("Unknownd key: " + key);
            }
        }
        return true;
    }
    
    /**
     * @Description Set customized key/value for IOS notification
     */
    public boolean setCustomizedField(String key, String value) throws Exception {
        //rootJson.put(key, value);
        JSONObject payloadJson;
        if (rootJson.has("payload")) {
            payloadJson = rootJson.getJSONObject("payload");
        } else {
            payloadJson = new JSONObject();
            rootJson.put("payload", payloadJson);
        }
        payloadJson.put(key, value);
        return true;
    }
    
    public void setAlert(JSONObject token) throws Exception {
        setPredefinedKeyValue("alert", token);
    }
    
    public void setAlert(String token) throws Exception {
        setPredefinedKeyValue("alert", token);
    }
    
    public void setBadge(Integer badge) throws Exception {
        setPredefinedKeyValue("badge", badge);
    }
    
    public void setSound(String sound) throws Exception {
        setPredefinedKeyValue("sound", sound);
    }
    
    public void setContentAvailable(Integer contentAvailable) throws Exception {
        setPredefinedKeyValue("content-available", contentAvailable);
    }
}
