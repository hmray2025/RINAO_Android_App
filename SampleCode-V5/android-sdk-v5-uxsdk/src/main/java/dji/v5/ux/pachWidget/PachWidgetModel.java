package dji.v5.ux.pachWidget;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;

public class PachWidgetModel extends WidgetModel {
    private IPachWidgetModel pachKeyManager;
    private MutableLiveData<String> msgdata;
    private MutableLiveData<Boolean> connectiondata;
    protected PachWidgetModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
        msgdata = new MutableLiveData<>();
        connectiondata = new MutableLiveData<>();
    }

    @Override
    protected void inSetup() {

    }

    @Override
    protected void inCleanup() {

    }

    public LiveData<String> getMsgData() {
        return msgdata;
    }
    public LiveData<Boolean> getConnectionData() {
        return connectiondata;
    }
    public void fetchData() {
        msgdata.postValue(pachKeyManager.getAction());
        connectiondata.postValue(pachKeyManager.getConnectionStatus());
    }

    public void setPach(IPachWidgetModel pach) {
        this.pachKeyManager = pach;
    }
}
