package com.hypodiabetic.happ;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.UnsupportedEncodingException;

/**
 * Created by Tim on 29/10/2015.
 * JSONArray Request that always returns a JSONArray
 */
public class JSONArrayPOST extends JsonRequest<JSONArray> {


    //private Response.Listener<JSONArray> listener;
    //private Response.ErrorListener errorListener;


    /**
     * Creates a new request.
     * @param method the HTTP method to use
     * @param url URL to fetch the JSON from
     * @param requestBody A {@link String} to post with the request. Null is allowed and
     *   indicates no parameters will be posted along with request.
     * @param listener Listener to receive the JSON response
     * @param errorListener Error listener, or null to ignore errors.
     */
    public JSONArrayPOST(int method, String url, String requestBody,
                                    Response.Listener<JSONArray> listener, Response.ErrorListener errorListener) {
        super(method, url, requestBody, listener,
                errorListener);
    }

    /**
     * Creates a new request.
     * @param url URL to fetch the JSON from
     * @param listener Listener to receive the JSON response
     * @param errorListener Error listener, or null to ignore errors.
     */
    public JSONArrayPOST(String url, Response.Listener<JSONArray> listener, Response.ErrorListener errorListener) {
        super(Method.GET, url, null, listener, errorListener);
    }

    /**
     * Creates a new request.
     * @param method the HTTP method to use
     * @param url URL to fetch the JSON from
     * @param listener Listener to receive the JSON response
     * @param errorListener Error listener, or null to ignore errors.
     */
    public JSONArrayPOST(int method, String url, Response.Listener<JSONArray> listener, Response.ErrorListener errorListener) {
        super(method, url, null, listener, errorListener);
    }

    /**
     * Creates a new request.
     * @param method the HTTP method to use
     * @param url URL to fetch the JSON from
     * @param jsonRequest A {@link JSONArray} to post with the request. Null is allowed and
     *   indicates no parameters will be posted along with request.
     * @param listener Listener to receive the JSON response
     * @param errorListener Error listener, or null to ignore errors.
     */
    public JSONArrayPOST(int method, String url, JSONArray jsonRequest,
                                    Response.Listener<JSONArray> listener, Response.ErrorListener errorListener) {
        super(method, url, (jsonRequest == null) ? null : jsonRequest.toString(), listener,
                errorListener);





    }

    /**
     * Creates a new request.
     * @param method the HTTP method to use
     * @param url URL to fetch the JSON from
     * @param jsonRequest A {@link JSONObject} to post with the request. Null is allowed and
     *   indicates no parameters will be posted along with request.
     * @param listener Listener to receive the JSON response
     * @param errorListener Error listener, or null to ignore errors.
     */
    public JSONArrayPOST(int method, String url, JSONObject jsonRequest,
                                    Response.Listener<JSONArray> listener, Response.ErrorListener errorListener) {
        super(method, url, (jsonRequest == null) ? null : jsonRequest.toString(), listener,
                errorListener);
    }

    /**
     * Constructor which defaults to <code>GET</code> if <code>jsonRequest</code> is
     * <code>null</code>, <code>POST</code> otherwise.
     *
     * @see #JSONArrayPOST(int, String, JSONArray, Response.Listener, Response.ErrorListener)
     */
    public JSONArrayPOST(String url, JSONArray jsonRequest, Response.Listener<JSONArray> listener,
                                    Response.ErrorListener errorListener) {
        this(jsonRequest == null ? Method.GET : Method.POST, url, jsonRequest,
                listener, errorListener);
    }

    /**
     * Constructor which defaults to <code>GET</code> if <code>jsonRequest</code> is
     * <code>null</code>, <code>POST</code> otherwise.
     *
     * @see #JSONArrayPOST(int, String, JSONObject, Response.Listener, Response.ErrorListener)
     */
    public JSONArrayPOST(String url, JSONObject jsonRequest, Response.Listener<JSONArray> listener,
                                    Response.ErrorListener errorListener) {
        this(jsonRequest == null ? Method.GET : Method.POST, url, jsonRequest,
                listener, errorListener);
    }





    @Override
    protected Response<JSONArray> parseNetworkResponse(NetworkResponse response) {
        JSONArray replyJSONArray = new JSONArray();
        try {
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));

            Object json = new JSONTokener(jsonString).nextValue();
            if(json instanceof JSONObject){
                replyJSONArray.put(json);
            }else if (json instanceof JSONArray){
                replyJSONArray = (JSONArray)json;
            }

        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }

        return Response.success(replyJSONArray, HttpHeaderParser.parseCacheHeaders(response));




    }
}
