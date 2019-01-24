package edu.skku.monet.nugucall;

import android.os.Environment;

import java.io.File;

class Global {
    static final String SERVER_ADDRESS = "http://115.145.171.157:8085/";
    static final String SERVER_IP = "115.145.171.157";
    static final int SERVER_UPLOAD_PORT = 8083;
    static final int SERVER_DOWNLOAD_PORT = 8084;
    static final String TAG = "☆★☆★누구콜_로그★☆★☆";

    static final String SHARED_PREFERENCES = "nugucall";
    static final String SHARED_PREFERENCES_WIDTH = "width";
    static final String SHARED_PREFERENCES_HEIGHT = "height";
    static final String SHARED_PREFERENCES_INCOMING = "incoming";
    static final String SHARED_PREFERENCES_FILEPATH = "filepath";

    static final int NOTIFICATION_ID = 1;
    static final String NOTIFICATION_CHANNEL_ID = "NUGUCALL";
    static final String NOTIFICATION_CHANNEL_NAME = "NUGUCALL";

    static final int REQ_CODE_PERMISSION_PHONE = 1001;
    static final int REQ_CODE_PERMISSION_STORAGE = 1002;
    static final int REQ_CODE_PERMISSION_CAMERA = 1003;
    static final int REQ_CODE_PERMISSION_OVERLAY = 1004;
    static final int REQ_CODE_FILE_SELECT = 2001;
    static final int REQ_CODE_NOTIFICATION_INTENT = 3001;
    static final int REQ_IMAGE_CAPTURE = 4001;

    static final String DEFAULT_PATH = Environment.getExternalStorageDirectory().getPath() + File.separator + "NuguCall";

    static final String INTENT_ACTION_INSERT_RECORDS = "INTENT_ACTION_INSERT_RECORDS";
    static final String INTENT_ACTION_SELECT_RECORDS = "INTENT_ACTION_SELECT_RECORDS";
    static final String INTENT_ACTION_TURN_OFF_CONTENTS = "INTENT_ACTION_TURN_OFF_CONTENTS";
    static final String INTENT_ACTION_PREVIEW_CONTENTS_ON = "INTENT_ACTION_PREVIEW_CONTENTS_ON";
    static final String INTENT_ACTION_PREVIEW_CONTENTS_OFF = "INTENT_ACTION_PREVIEW_CONTENTS_OFF";
    static final String INTENT_ACTION_PREVIEW_ACTIVITY_OFF = "INTENT_ACTION_PREVIEW_ACTIVITY_OFF";

    static final String INTENT_EXTRA_PHONE_NUMBER = "INTENT_EXTRA_PHONE_NUMBER";
    static final String INTENT_EXTRA_NAME = "INTENT_EXTRA_NAME";
    static final String INTENT_EXTRA_TEXT = "INTENT_EXTRA_TEXT";
    static final String INTENT_EXTRA_SOURCE = "INTENT_EXTRA_SOURCE";
    static final String INTENT_EXTRA_FILEPATH = "INTENT_EXTRA_FILEPATH";

}