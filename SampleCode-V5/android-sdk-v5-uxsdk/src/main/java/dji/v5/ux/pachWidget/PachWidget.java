package dji.v5.ux.pachWidget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import java.util.List;

import dji.v5.ux.R;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * PachWidget displays the state of the SAFARI system, including
 * the connection status to the SAFARI system and the flight state
 * of the drone (whether it is autonomous or manual), and actions
 * associated with the state of the drone.
 */

public class PachWidget extends ConstraintLayoutWidget<Object> {
    private PachWidgetModel pachWidgetModel;
    private TextView msg;
    private ImageView connection;
    public PachWidget(@NonNull Context context) {
        super(context);
    }

    public PachWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PachWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_pach, this);
        pachWidgetModel = new PachWidgetModel();
        msg = findViewById(R.id.status_msg);
        connection = findViewById(R.id.connection_icon);
        msg.setSelected(true);
    }

//    public void subscribetoDataSources(Flowable<Boolean> connectionData, Flowable<Boolean> autonamousData, Flowable<List<String>> flightActionsData, Flowable<List<String>> flightWarningsData) {
//        pachWidgetModel.subscribeToDataSources(connectionData, autonamousData, flightActionsData, flightWarningsData);
//    }

    public void subscribetoDataSources(Flowable<Boolean> connectionData, Flowable<String> msgData) {
        pachWidgetModel.subscribeToDataSources(connectionData, msgData);
    }

    @Override
    protected void reactToModelChanges() {
        pachWidgetModel._msgdata.observe((LifecycleOwner) getContext(), msgData -> {
            msg.setText(msgData);
        });
        pachWidgetModel._connectiondata.observe((LifecycleOwner) getContext(), connected -> {
            if (connected) {
                connection.setImageResource(R.drawable.uxsdk_ic_alert_good);
            } else if (!connected) {
                connection.setImageResource(R.drawable.uxsdk_ic_alert_error);
            }
        });
    }
}
