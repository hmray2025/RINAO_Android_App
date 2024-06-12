package dji.v5.ux.pachWidget;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PachWidgetModel extends ViewModel{
    private static PachWidgetModel instance = null;
    private IPachWidgetModel pachKeyManager;
    private MutableLiveData<String> _msgdata;
    protected LiveData<String> msgdata;
    private MutableLiveData<Boolean> _connectiondata;
    protected LiveData<Boolean> connectiondata;
    private PachWidgetModel() {
        _msgdata = new MutableLiveData<>();
        _connectiondata = new MutableLiveData<>();
        msgdata = _msgdata;
        connectiondata = _connectiondata;
    }

    public static synchronized PachWidgetModel getInstance() {
        if (instance == null) {
            instance = new PachWidgetModel();
        }
        return instance;
    }

    public LiveData<String> getMsgData() {
        return msgdata;
    }
    public LiveData<Boolean> getConnectionData() {
        return connectiondata;
    }

    public void updateMsg(String msg) {
        _msgdata.setValue(msg);
        msgdata = _msgdata;
    }
    public void updateConnection(Boolean connection) {
        _connectiondata.setValue(connection);
        connectiondata = _connectiondata;
    }
//    public void fetchData() {
//        msgdata.postValue(pachKeyManager.getAction());
//        connectiondata.postValue(pachKeyManager.getConnectionStatus());
//    }

    public void setPach(IPachWidgetModel pach) {
        this.pachKeyManager = pach;
//        if (this.pachKeyManager != null) {
//            fetchData();
//        }
    }
}
