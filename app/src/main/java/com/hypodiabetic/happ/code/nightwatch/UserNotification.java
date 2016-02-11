package com.hypodiabetic.happ.code.nightwatch;

import android.content.Context;
import android.provider.BaseColumns;
import android.support.design.widget.Snackbar;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import java.util.Date;
import java.util.List;

/**
 * Created by tim on 07/08/2015.
 * Cloned from https://github.com/StephenBlackWasAlreadyTaken/NightWatch
 */


@Table(name = "Notifications", id = BaseColumns._ID)
public class UserNotification extends Model {

    @Column(name = "timestamp", index = true)
    public Long timestamp;

    @Column(name = "message")
    public String message;

    @Column(name = "bg_alert")
    public boolean bg_alert;

}