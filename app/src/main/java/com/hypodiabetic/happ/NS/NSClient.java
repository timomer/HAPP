package com.hypodiabetic.happ.NS;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.hypodiabetic.happ.ApplicationContextProvider;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.NS.utils.DateUtil;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class NSClient {
    
    //HAPP Added
    DecimalFormat formatNumber1place = new DecimalFormat("0.00");
    public Handler mPingHandler = null;
    public static Runnable mPingRunnable;

    //private Bus mBus; //HAPP added
    private static Logger log = LoggerFactory.getLogger(NSClient.class);
    private Socket mSocket;
    private boolean isConnected = false;
    public String connectionStatus = "Not connected";
    private long mTimeDiff;

    private SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(MainApp.instance().getApplicationContext());
    private boolean nsEnabled = SP.getBoolean("ns_enable", true);
    private String nsURL = SP.getString("ns_url", "http://bg.hypodiabetic.co.uk");
    private String nsAPISecret = SP.getString("ns_api_secret", "MYCGMMONKEY05");
    private String nsSyncProfile = SP.getString("ns_sync_profile", "");
    private Boolean nsUploadTreatments = SP.getBoolean("ns_sync_upload_treatments", true);
    private Boolean nsAcceptCommands = SP.getBoolean("ns_accept_commands", false);

    private String nsAPIhashCode = "";

    private static final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    private JSONObject mPreparedStatus = null;
    private ScheduledFuture<?> mOutgoingStatus = null;

    public NSClient() {
        MainApp.setNSClient(this);
        //mBus = bus; //HAPP added

        mPingHandler = new Handler();
        mPingRunnable = new Runnable() {
            @Override
            public void run() {
                doPing();
                mPingHandler.postDelayed(mPingRunnable, 60000);
            }
        };
        mPingHandler.postDelayed(mPingRunnable, 60000);

        readPreferences();

        if (nsAPISecret!="") nsAPIhashCode = Hashing.sha1().hashString(nsAPISecret, Charsets.UTF_8).toString();

        //mBus.post(new NSStatusEvent(connectionStatus)); //HAPP added
        if (nsEnabled && nsURL != "") {
            try {
                connectionStatus = "Connecting ...";
                //mBus.post(new NSStatusEvent(connectionStatus)); //HAPP added
                mSocket = IO.socket(nsURL);
                mSocket.connect();
                mSocket.on("dataUpdate", onDataUpdate);
                mSocket.on("bolus", onBolus);
                // resend auth on reconnect is needed on server restart
                mSocket.on("reconnect", resendAuth);
                sendAuthMessage(new NSAuthAck());

            } catch (URISyntaxException e) {
                connectionStatus = "Wrong URL syntax";
                //mBus.post(new NSStatusEvent(connectionStatus)); //HAPP added
            }
        } else {
            connectionStatus = "Disabled";
            //mBus.post(new NSStatusEvent(connectionStatus)); //HAPP added
        }
    }

    private void sendAuthMessage(NSAuthAck ack) {
        JSONObject authMessage = new JSONObject();
        try {
            authMessage.put("client", "pump");
            authMessage.put("secret", nsAPIhashCode);
        } catch (JSONException e) {
            return;
        }
        mSocket.emit("authorize", authMessage, ack);
        synchronized(ack) {
            try {
                // Calling wait() will block this thread until another thread
                // calls notify() on the object.
                ack.wait(10000);
            } catch (InterruptedException e) {
                // Happens if someone interrupts your thread.
                ack.interrupted = true;
            }
        }
        if (ack.interrupted) {
            isConnected = false;
            connectionStatus = "Auth interrupted";
            //mBus.post(new NSStatusEvent(connectionStatus)); //HAPP added
        }
        else if (ack.received){
            connectionStatus = "Authenticated (";
            if (ack.read) connectionStatus += "R";
            if (ack.write) connectionStatus += "W";
            if (ack.write_treatment) connectionStatus += "T";
            connectionStatus += ')';
            isConnected = true;
            //mBus.post(new NSStatusEvent(connectionStatus)); //HAPP added
        } else {
            isConnected = false;
            connectionStatus = "Timed out";
            //mBus.post(new NSStatusEvent(connectionStatus)); //HAPP added
        }
    }

    public void readPreferences() {
        SP = PreferenceManager.getDefaultSharedPreferences(MainApp.instance().getApplicationContext());
        nsEnabled = SP.getBoolean("ns_enable", true);
        nsURL = SP.getString("ns_url", "http://bg.hypodiabetic.co.uk");
        nsAPISecret = SP.getString("ns_api_secret", "MYCGMMONKEY05");
        nsSyncProfile = SP.getString("ns_sync_profile", "");
        nsUploadTreatments = SP.getBoolean("ns_sync_upload_treatments", true);
        nsAcceptCommands = SP.getBoolean("ns_accept_commands", false);
    }

    private Emitter.Listener resendAuth = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            NSAuthAck ack = new NSAuthAck();
            sendAuthMessage(ack);
        }
   };

    private Emitter.Listener onBolus = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject data = (JSONObject) args[0];
            String _id;
            double insulin;
            try {
                if (nsAcceptCommands) {
                    _id = data.getString("_id");
                    insulin = data.getDouble("insulin");
                    JSONObject response = new JSONObject();
                    response.put("_id", _id);
                    response.put("collection", "treatments");
                    JSONObject responseData = new JSONObject();
                    responseData.put("status", "Received");
                    response.put("data", responseData);
                    mSocket.emit("dbUpdate", response);
                    //mBus.post(new NSCommandEvent(_id, "bolus", insulin)); //HAPP added
                }
            } catch (JSONException e) {
                return;
            }

        }
    };

    private Emitter.Listener onDataUpdate = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject data = (JSONObject) args[0];
         }
    };

    public void sendTreatmentStatusUpdate(String _id, String status) {
        try {
            JSONObject message = new JSONObject();
            message.put("_id", _id);
            message.put("collection", "treatments");
            JSONObject messageData = new JSONObject();
            messageData.put("status", status);
            message.put("data", messageData);
            mSocket.emit("dbUpdate", message);
        } catch (JSONException e) {
            return;
        }
    };

    public void sendAddTreatment(JSONObject data, NSAck ack) {
        try {
            JSONObject message = new JSONObject();
            message.put("collection", "treatments");
            message.put("data", data);
            mSocket.emit("dbAdd", message, ack);
            synchronized(ack) {
                try {
                    // Calling wait() will block this thread until another thread
                    // calls notify() on the object.
                    ack.wait(3000);
                } catch (InterruptedException e) {
                    // Happens if someone interrupts your thread.
                }
            }

        } catch (JSONException e) {
            return;
        }
    };

    public void sendAddTreatment(JSONObject data) {
        try {
            JSONObject message = new JSONObject();
            message.put("collection", "treatments");
            message.put("data", data);
            mSocket.emit("dbAdd", message);
        } catch (JSONException e) {
            return;
        }
    };

    private void sendAddStatus(JSONObject data) {
        try {
            JSONObject message = new JSONObject();
            message.put("collection", "devicestatus");
            message.put("data", data);
            mSocket.emit("dbAdd", message);
        } catch (JSONException e) {
            return;
        }
    };

    //public void sendStatus(StatusEvent ev, String btStatus){
    //    JSONObject nsStatus = new JSONObject();
    //    JSONObject pump = new JSONObject();
    //    JSONObject battery = new JSONObject();
    //    JSONObject status = new JSONObject();
    //    try {
    //        battery.put("pumpBattery", ev.remainBattery);
    //        pump.put("battery", battery);

    //        status.put("lastbolus", ev.last_bolus_amount);
    //        status.put("lastbolustime", DateUtil.toISOString(ev.last_bolus_time));
    //        if (ev.tempBasalRatio != -1) {
    //            status.put("tempbasalpct", ev.tempBasalRatio);
    //            if (ev.tempBasalStart != null) status.put("tempbasalstart", DateUtil.toISOString(ev.tempBasalStart));
    //            if (ev.tempBasalRemainMin != 0) status.put("tempbasalremainmin", ev.tempBasalRemainMin);
    //        }
    //        status.put("connection", btStatus);
    //        pump.put("status", status);

    //        pump.put("reservoir", formatNumber1place.format(ev.remainUnits));
    //        pump.put("clock", DateUtil.toISOString(new Date()));
    //        nsStatus.put("pump", pump);
    //    } catch (JSONException e) {
    //    }

    //    class RunnableWithParam implements Runnable {
    //        JSONObject param;
    //        RunnableWithParam(JSONObject param) {
    //            this.param = param;
     //       }
    //        public void run(){
    //            sendAddStatus(param);
    //            mPreparedStatus = null;

    //        };
    //    }

        // prepare task for execution in 3 sec
        // cancel waiting task to prevent sending multiple statuses
    //    if (mPreparedStatus != null) mOutgoingStatus.cancel(false);
    //    Runnable task = new RunnableWithParam(nsStatus);
    //    mPreparedStatus = nsStatus;
    //    mOutgoingStatus = worker.schedule(task, 3, TimeUnit.SECONDS);
    //}

    public void doPing() {
        if (isConnected) {
            try {
                NSPingAck ack = new NSPingAck();
                JSONObject message = new JSONObject();
                message.put("mills", System.currentTimeMillis());
                mSocket.emit("ping", message, ack);
                synchronized (ack) {
                    try {
                        // Calling wait() will block this thread until another thread
                        // calls notify() on the object.
                        ack.wait(10000);
                    } catch (InterruptedException e) {
                        // Happens if someone interrupts your thread.
                    }
                }
                mTimeDiff = System.currentTimeMillis() - ack.mills;
                if (!ack.received) {
                    connectionStatus = "Not responding";
                } else {
                    connectionStatus = "Connected";
                }
                //mBus.post(new NSStatusEvent(connectionStatus)); //HAPP added
            } catch (JSONException e) {
             }
        }
    };

}
