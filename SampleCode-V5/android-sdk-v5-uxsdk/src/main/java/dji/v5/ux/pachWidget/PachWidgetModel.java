package dji.v5.ux.pachWidget;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PachWidgetModel extends ViewModel{
    private static PachWidgetModel instance = null;
    private IPachWidgetModel pachKeyManager;
    private MutableLiveData<String> _msgdata; // updated by PachKeyManager
    public LiveData<String> msgdata; // observable to Widgets
    private MutableLiveData<Boolean> _connectiondata; // updated by PachKeyManager
    public LiveData<Boolean> connectiondata; // observable to Widgets

    private PachWidgetModel() { // constructor
        _msgdata = new MutableLiveData<>();
        _connectiondata = new MutableLiveData<>();
        msgdata = _msgdata;
        connectiondata = _connectiondata;
    }

    // singleton pattern - only one instance of this class can exist. Many PachWidgets
    // can use this instance to update the message and connection status, but they will
    // reference the same instance of the model. This is useful because it allows for
    // a single source of truth for the message and connection status, coming from the
    // PachKeyManager class.
    public static synchronized PachWidgetModel getInstance() {
        if (instance == null) {
            instance = new PachWidgetModel();
        }
        return instance;
    }

    // update functions follow an encapsulation pattern, where the PachKeyManager class
    // updates the MutableLiveData objects, and the Widgets observe the LiveData objects.
    // This prevents the widgets from being able to change the data they get from the
    // PachKeyManager, and ensures that the data is only updated by the PachKeyManager.
    public void updateMsg(String msg) {
        _msgdata.setValue(msg);
        msgdata = _msgdata;
    }
    public void updateConnection(Boolean connection) {
        _connectiondata.setValue(connection);
        connectiondata = _connectiondata;
    }

    public void setPach(IPachWidgetModel pach) {
        this.pachKeyManager = pach;
    }
}
