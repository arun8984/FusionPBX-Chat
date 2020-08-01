package com.chatapp;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;


public class NotificationWorker extends Worker {
    private Context context;
    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context=context;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @NonNull
    @Override
    public Result doWork() {
        NoitficationUtils.showNotification(context);
        return Result.success();
    }
}
