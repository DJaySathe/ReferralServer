package Util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Neighbor;
import play.libs.Json;

public class APIResponse {
    public static ObjectNode createResponse(String message, boolean status) {

        ObjectNode result = Json.newObject();
        if(status) {
            result.put("status", "success");
        } else {
            result.put("status", "error");
            if (message instanceof String) {
                result.put("message", (String) message);
            } else {
                result.putPOJO("message", message);
            }
        }
        return result;
    }
    public static ObjectNode  createResponseWithAnswer(boolean status,double[] ans) {

        ObjectNode result = Json.newObject();
        result.put("status", "success");
        result.putPOJO("answer", ans);
        return result;
    }
    public static ObjectNode  createResponseForDump(boolean status, Neighbor[] n,Neighbor[] a) {

        ObjectNode result = Json.newObject();
        result.put("status", "success");
        result.putPOJO("neighbors", n);
        result.putPOJO("acquaintances", a);
        return result;
    }
}
