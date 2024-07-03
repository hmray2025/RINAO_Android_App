package dji.v5.ux.pachWidget;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Flowable;

public class PachWidgetModel extends ViewModel{
    private static PachWidgetModel instance = null;
    private CompositeDisposable disposables;
    private Boolean autonamousFlightState;
    private Boolean connectionState;
    private List<String> flightActions;
    private List<String> flightWarnings;
    MutableLiveData<String> _msgdata = new MutableLiveData<>();
    MutableLiveData<Boolean> _connectiondata = new MutableLiveData<>();


    PachWidgetModel() { // constructor
        disposables = new CompositeDisposable();

    }
//    void subscribeToDataSources(Flowable<Boolean> connectionData, Flowable<Boolean> autonamousData, Flowable<List<String>> flightActionsData, Flowable<List<String>> flightWarningsData) {
//        disposables.add((Disposable) connectionData
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(msg -> {
//                    connectionState = msg;
//                    publishConnectionState();
//                }));
//        disposables.add((Disposable) autonamousData
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(msg -> {
//                    autonamousFlightState = msg;
//                    parseAndPublishMsg();
//                }));
//        disposables.add((Disposable) flightActionsData
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(msg -> {
//                    flightActions = msg;
//                    parseAndPublishMsg();
//                }));
//        disposables.add((Disposable) flightWarningsData
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(msg -> {
//                    flightWarnings = msg;
//                    parseAndPublishMsg();
//                }));
//    }

        // primitive change - will be updated in the future
        void subscribeToDataSources(Flowable<Boolean> connectionData, Flowable<String> msgData) {
            disposables.add(connectionData
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            data -> {
                                connectionState = data;
                                publishConnectionState();
                                },
                            error -> Log.e("PachWidgetModel", "Error observing connection data", error)
                    ));
            disposables.add(msgData
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            data -> {
                                _msgdata.postValue(data);
                                },
                            error -> Log.e("PachWidgetModel", "Error observing message data", error)
                    ));
        }

    void parseAndPublishMsg() {
        String data = "";
        if (autonamousFlightState) {
            data = "Autonamous";
        } else {
            data = "Maunal";
        }
        if (!flightActions.isEmpty()) {
            for (String action : flightActions) {
                data += " | " + action;
            }
        }
        if (!flightWarnings.isEmpty()) {
            data += " | warnings: ";
            for (String warning : flightWarnings) {
                data += " | " + warning;
            }
        }
        _msgdata.postValue(data);
    }

    void publishConnectionState() {
        _connectiondata.postValue(connectionState);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.dispose(); // Dispose all subscriptions
    }
}
